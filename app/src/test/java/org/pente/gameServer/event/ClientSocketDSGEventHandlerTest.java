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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Drives ClientSocketDSGEventHandler over a real loopback socket pair: a connection that dies
 * from an error is torn down and reported exactly once; one the owner destroys is not reported.
 */
public class ClientSocketDSGEventHandlerTest {

    private static final long REPORT_TIMEOUT_MS = 5_000;
    // Long enough for a stray second report (or a report after destroy) to arrive.
    private static final long NO_REPORT_WAIT_MS = 500;

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

    @Test
    public void streamSetupFailure_throwsInsteadOfReturningADeadHandler() throws IOException {
        client.close();

        assertThrows(IOException.class, this::newHandler);
    }
}
