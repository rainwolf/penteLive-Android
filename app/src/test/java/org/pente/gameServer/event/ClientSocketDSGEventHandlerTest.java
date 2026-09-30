package org.pente.gameServer.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Drives ClientSocketDSGEventHandler over a real loopback socket pair: a connection that dies
 * from an error, including a read timeout on a server gone silent, is torn down and reported
 * exactly once; one the owner destroys is not reported.
 */
public class ClientSocketDSGEventHandlerTest {

    private static final long REPORT_TIMEOUT_MS = 5_000;
    // Long enough for a stray second report (or a report after destroy) to arrive.
    private static final long NO_REPORT_WAIT_MS = 500;
    // Stands in for the live room's read timeout, scaled down so the tests run quickly.
    private static final int SHORT_READ_TIMEOUT_MS = 400;

    private ServerSocket serverSocket;
    private Socket client;
    private Socket serverSide;
    private final BlockingQueue<Throwable> reports = new LinkedBlockingQueue<>();

    @Before
    public void connect() throws IOException {
        serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        client = new Socket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort());
        serverSide = serverSocket.accept();
    }

    @After
    public void close() throws IOException {
        serverSide.close();
        client.close();
        serverSocket.close();
    }

    private ClientSocketDSGEventHandler newHandler() throws IOException {
        return new ClientSocketDSGEventHandler(client, reports::add);
    }

    @Test
    public void serverClosingTheConnection_isReportedOnceAsEOF_andTornDown() throws Exception {
        ClientSocketDSGEventHandler handler = newHandler();

        serverSide.close();

        Throwable cause = reports.poll(REPORT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull("server close was not reported", cause);
        assertTrue("expected EOFException, got " + cause, cause instanceof EOFException);
        assertNull("reported more than once", reports.poll(NO_REPORT_WAIT_MS, TimeUnit.MILLISECONDS));
        assertTrue("client socket left open", client.isClosed());
        assertFalse("handler still running", handler.running);
    }

    @Test
    public void readerIOException_isReportedOnce_andTornDown() throws Exception {
        ClientSocketDSGEventHandler handler = newHandler();

        // Linger 0 makes close() send a RST, so the client's blocked read throws
        // "Connection reset" instead of seeing end of stream.
        serverSide.setSoLinger(true, 0);
        serverSide.close();

        Throwable cause = reports.poll(REPORT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull("reset was not reported", cause);
        assertTrue("expected an IOException, got " + cause, cause instanceof IOException);
        assertFalse("reset reported as end of stream", cause instanceof EOFException);
        assertNull("reported more than once", reports.poll(NO_REPORT_WAIT_MS, TimeUnit.MILLISECONDS));
        assertTrue("client socket left open", client.isClosed());
        assertFalse("handler still running", handler.running);
    }

    @Test
    public void silentServer_timesOut_isReportedOnce_andTornDown() throws Exception {
        // A half-open connection: the server side stays open but sends nothing, as when a NAT
        // mapping expires or the server host dies without a FIN or RST.
        client.setSoTimeout(SHORT_READ_TIMEOUT_MS);
        ClientSocketDSGEventHandler handler = newHandler();

        Throwable cause = reports.poll(REPORT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull("silent connection was not reported", cause);
        assertTrue("expected SocketTimeoutException, got " + cause,
                cause instanceof SocketTimeoutException);
        assertNull("reported more than once", reports.poll(NO_REPORT_WAIT_MS, TimeUnit.MILLISECONDS));
        assertTrue("client socket left open", client.isClosed());
        assertFalse("handler still running", handler.running);
    }

    @Test
    public void serverPingingFasterThanTheReadTimeout_isNotReported() throws Exception {
        client.setSoTimeout(SHORT_READ_TIMEOUT_MS);
        ClientSocketDSGEventHandler handler = newHandler();
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        handler.addListener(received::add);

        // Ping at a quarter of the timeout for three timeouts' worth of time, like the server's
        // 15 s pings against the room's 60 s timeout.
        long pingIntervalMs = SHORT_READ_TIMEOUT_MS / 4;
        int pings = (int) (3 * SHORT_READ_TIMEOUT_MS / pingIntervalMs);
        for (int i = 0; i < pings; i++) {
            serverSide.getOutputStream().write("{\"dsgPingEvent\":{}}".getBytes(StandardCharsets.UTF_8));
            serverSide.getOutputStream().write(255);
            serverSide.getOutputStream().flush();
            Thread.sleep(pingIntervalMs);
        }

        assertNull("connection reported lost while the server was pinging", reports.poll());
        assertTrue("handler stopped while the server was pinging", handler.running);
        assertEquals(pings, received.size());
    }

    @Test
    public void writerIOException_whileReaderBlocked_isReportedOnce_andTornDown() throws Exception {
        ClientSocketDSGEventHandler handler = newHandler();
        // Both threads back in their blocking calls, the reader waiting on a server that
        // stays open and silent.
        exchangeOneEventEachWay(handler);

        // Only the client's writes fail: the reader stays blocked until the teardown closes
        // the socket under it, and that second failure must not be reported.
        client.shutdownOutput();
        handler.eventOccurred("{\"a\":2}");

        Throwable cause = reports.poll(REPORT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull("write failure was not reported", cause);
        assertTrue("expected an IOException, got " + cause, cause instanceof IOException);
        assertTrue("expected the writer's failure, got " + cause, thrownBy(cause, "ObjectWriter"));
        assertNull("reported more than once", reports.poll(NO_REPORT_WAIT_MS, TimeUnit.MILLISECONDS));
        assertTrue("client socket left open", client.isClosed());
        assertFalse("handler still running", handler.running);
    }

    @Test
    public void destroyByOwner_isNotReported() throws Exception {
        ClientSocketDSGEventHandler handler = newHandler();
        // Get both threads past their first loop check and back into their blocking calls, so
        // destroy() really fails them (writer interrupted, reader's socket closed under it)
        // instead of them just seeing running == false.
        exchangeOneEventEachWay(handler);

        handler.destroy();

        assertNull("owner-initiated destroy was reported",
                reports.poll(NO_REPORT_WAIT_MS, TimeUnit.MILLISECONDS));
        assertTrue("client socket left open", client.isClosed());
        assertFalse("handler still running", handler.running);
    }

    @Test
    public void eventsStillFlowBothWays() throws Exception {
        ClientSocketDSGEventHandler handler = newHandler();

        exchangeOneEventEachWay(handler);

        assertTrue(handler.running);
        assertNull(reports.poll(NO_REPORT_WAIT_MS, TimeUnit.MILLISECONDS));
    }

    private void exchangeOneEventEachWay(ClientSocketDSGEventHandler handler) throws Exception {
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        handler.addListener(received::add);

        handler.eventOccurred("{\"a\":1}");
        for (byte b : "{\"a\":1}".getBytes(StandardCharsets.UTF_8)) {
            assertEquals(b, (byte) serverSide.getInputStream().read());
        }
        assertEquals(255, serverSide.getInputStream().read());

        serverSide.getOutputStream().write("{\"b\":2}".getBytes(StandardCharsets.UTF_8));
        serverSide.getOutputStream().write(255);
        serverSide.getOutputStream().flush();
        assertEquals("{\"b\":2}", received.poll(REPORT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // The reader returns to read() right after notifying listeners; give it that moment.
        Thread.sleep(100);
    }

    /** Whether t was thrown on a thread running the handler's inner class of that name. */
    private static boolean thrownBy(Throwable t, String innerClassSimpleName) {
        for (StackTraceElement frame : t.getStackTrace()) {
            if (frame.getClassName().endsWith("$" + innerClassSimpleName)) return true;
        }
        return false;
    }

    @Test
    public void streamSetupFailure_throwsInsteadOfReturningADeadHandler() throws IOException {
        client.close();

        assertThrows(IOException.class, this::newHandler);
    }
}
