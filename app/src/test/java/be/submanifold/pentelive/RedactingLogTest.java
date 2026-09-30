package be.submanifold.pentelive;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class RedactingLogTest {

    private static final String LOGIN_URL =
            "https://www.pente.org/gameServer/login.jsp?mobile=&name2=bob&password2=secret";
    private static final String KOTH_URL =
            "https://www.pente.org/gameServer/mobile/json/koth.jsp?game=1&name=bob&name2=bob&password2=secret";

    private static final class CustomException extends Exception {
        CustomException(String message) {
            super(message);
        }
    }

    private static String printed(Throwable t) {
        StringWriter out = new StringWriter();
        t.printStackTrace(new PrintWriter(out, true));
        return out.toString();
    }

    @Test
    public void fileNotFoundWithUrlMessage_redactsWholeQuery() {
        FileNotFoundException original = new FileNotFoundException(LOGIN_URL);

        Throwable copy = RedactingLog.redacted(original);

        assertEquals("java.io.FileNotFoundException: https://www.pente.org/gameServer/login.jsp?<redacted>",
                copy.toString());
        assertFalse(printed(copy).contains("secret"));
    }

    @Test
    public void serverReturnedHttpError_redactsUrlQueryAndKeepsTheRest() {
        IOException original = new IOException("Server returned HTTP response code: 500 for URL: " + KOTH_URL);

        Throwable copy = RedactingLog.redacted(original);

        assertEquals("java.io.IOException: Server returned HTTP response code: 500 for URL: "
                        + "https://www.pente.org/gameServer/mobile/json/koth.jsp?<redacted>",
                copy.toString());
    }

    @Test
    public void passwordContainingAmpersand_isFullyRedacted() {
        IOException original = new IOException(
                "https://www.pente.org/gameServer/login.jsp?mobile=&name2=bob&password2=se&cret&x=1");

        Throwable copy = RedactingLog.redacted(original);

        assertEquals("java.io.IOException: https://www.pente.org/gameServer/login.jsp?<redacted>", copy.toString());
        assertFalse(printed(copy).contains("cret"));
    }

    @Test
    public void passwordContainingQuestionMark_isFullyRedactedInUrl() {
        IOException original = new IOException(
                "https://www.pente.org/gameServer/login.jsp?mobile=&name2=bob&password2=se?cret");

        Throwable copy = RedactingLog.redacted(original);

        assertEquals("java.io.IOException: https://www.pente.org/gameServer/login.jsp?<redacted>", copy.toString());
    }

    @Test
    public void passwordContainingSpace_isFullyRedactedInUrl() {
        IOException original = new IOException("Server returned HTTP response code: 500 for URL: "
                + "https://www.pente.org/gameServer/login.jsp?mobile=&name2=bob&password2=se cret word");

        Throwable copy = RedactingLog.redacted(original);

        assertEquals("java.io.IOException: Server returned HTTP response code: 500 for URL: "
                + "https://www.pente.org/gameServer/login.jsp?<redacted>", copy.toString());
        assertFalse(printed(copy).contains("cret"));
    }

    @Test
    public void passwordContainingTab_isFullyRedactedInUrl() {
        FileNotFoundException original = new FileNotFoundException(
                "https://www.pente.org/gameServer/login.jsp?mobile=&name2=bob&password2=se\tcret");

        Throwable copy = RedactingLog.redacted(original);

        assertEquals("java.io.FileNotFoundException: https://www.pente.org/gameServer/login.jsp?<redacted>",
                copy.toString());
        assertFalse(printed(copy).contains("cret"));
    }

    @Test
    public void barePasswordContainingSpace_isFullyRedacted() {
        IOException original = new IOException("rejected name2=bob&password2=se cret word");

        Throwable copy = RedactingLog.redacted(original);

        assertEquals("java.io.IOException: rejected name2=bob&password2=<redacted>", copy.toString());
        assertFalse(printed(copy).contains("cret"));
    }

    @Test
    public void barePasswordContainingTab_isFullyRedacted() {
        IOException original = new IOException("rejected name2=bob&password=se\tcret");

        Throwable copy = RedactingLog.redacted(original);

        assertEquals("java.io.IOException: rejected name2=bob&password=<redacted>", copy.toString());
        assertFalse(printed(copy).contains("cret"));
    }

    @Test
    public void bareQuestionMarkPasswordFragment_isFullyRedacted() {
        IOException original = new IOException("rejected password2=se?cret by server");

        Throwable copy = RedactingLog.redacted(original);

        // The rest of the line goes too: the password may contain spaces.
        assertEquals("java.io.IOException: rejected password2=<redacted>", copy.toString());
    }

    @Test
    public void barePasswordFragmentsWithoutQuery_areRedacted() {
        IOException original = new IOException("bad request name2=bob&password2=hunter2 and password=hunter3 end");

        Throwable copy = RedactingLog.redacted(original);

        // The first password parameter takes the rest of the line, the second one with it.
        assertEquals("java.io.IOException: bad request name2=bob&password2=<redacted>", copy.toString());
        assertFalse(printed(copy).contains("hunter"));
    }

    @Test
    public void causeChain_isRedactedAtEveryLevel() {
        FileNotFoundException root = new FileNotFoundException(LOGIN_URL);
        IOException middle = new IOException("Server returned HTTP response code: 500 for URL: " + KOTH_URL, root);
        RuntimeException top = new RuntimeException("wrapped: " + LOGIN_URL, middle);

        Throwable copy = RedactingLog.redacted(top);
        String trace = printed(copy);

        assertFalse(trace, trace.contains("secret"));
        assertEquals("java.lang.RuntimeException: wrapped: https://www.pente.org/gameServer/login.jsp?<redacted>",
                copy.toString());
        assertEquals("java.io.IOException: Server returned HTTP response code: 500 for URL: "
                        + "https://www.pente.org/gameServer/mobile/json/koth.jsp?<redacted>",
                copy.getCause().toString());
        assertEquals("java.io.FileNotFoundException: https://www.pente.org/gameServer/login.jsp?<redacted>",
                copy.getCause().getCause().toString());
        assertTrue(trace, trace.contains(
                "Caused by: java.io.FileNotFoundException: https://www.pente.org/gameServer/login.jsp?<redacted>"));
    }

    @Test
    public void suppressedExceptions_areRedacted_includingOnCauses() {
        IOException cause = new IOException(KOTH_URL);
        cause.addSuppressed(new FileNotFoundException(LOGIN_URL));
        IOException top = new IOException(LOGIN_URL, cause);
        top.addSuppressed(new IOException("close failed for " + KOTH_URL));

        Throwable copy = RedactingLog.redacted(top);
        String trace = printed(copy);

        assertFalse(trace, trace.contains("secret"));
        assertEquals(1, copy.getSuppressed().length);
        assertEquals("java.io.IOException: close failed for https://www.pente.org/gameServer/mobile/json/koth.jsp?<redacted>",
                copy.getSuppressed()[0].toString());
        assertEquals(1, copy.getCause().getSuppressed().length);
        assertEquals("java.io.FileNotFoundException: https://www.pente.org/gameServer/login.jsp?<redacted>",
                copy.getCause().getSuppressed()[0].toString());
        assertTrue(trace, trace.contains(
                "Suppressed: java.io.IOException: close failed for https://www.pente.org/gameServer/mobile/json/koth.jsp?<redacted>"));
    }

    @Test
    public void stackFramesAndClassNames_arePreserved() {
        FileNotFoundException cause = new FileNotFoundException(LOGIN_URL);
        CustomException original = new CustomException(KOTH_URL);
        original.initCause(cause);
        IOException suppressed = new IOException(LOGIN_URL);
        original.addSuppressed(suppressed);

        Throwable copy = RedactingLog.redacted(original);

        assertTrue(copy.toString(), copy.toString().startsWith(CustomException.class.getName() + ": "));
        assertTrue(copy.getCause().toString().startsWith("java.io.FileNotFoundException: "));
        assertArrayEquals(original.getStackTrace(), copy.getStackTrace());
        assertArrayEquals(cause.getStackTrace(), copy.getCause().getStackTrace());
        assertArrayEquals(suppressed.getStackTrace(), copy.getSuppressed()[0].getStackTrace());
        assertTrue(printed(copy).contains("\tat " + original.getStackTrace()[0]));
    }

    @Test
    public void nullMessage_printsTheClassNameOnly_likeTheJdk() {
        IOException original = new IOException();

        Throwable copy = RedactingLog.redacted(original);

        assertEquals(original.toString(), copy.toString());
        assertEquals("java.io.IOException", copy.toString());
        assertTrue(printed(copy).startsWith("java.io.IOException" + System.lineSeparator()));
    }

    @Test
    public void nonUrlMessages_areUnchanged() {
        Throwable[] originals = {
                new UnknownHostException("Unable to resolve host \"www.pente.org\": No address associated with hostname"),
                new SocketTimeoutException("timeout"),
                new IOException("unexpected end of stream on www.pente.org:443"),
        };
        for (Throwable original : originals) {
            assertEquals(original.toString(), RedactingLog.redacted(original).toString());
        }
    }

    @Test
    public void originalIsLeftUntouched() {
        IOException cause = new IOException(KOTH_URL);
        IOException original = new IOException(LOGIN_URL, cause);

        Throwable copy = RedactingLog.redacted(original);

        assertNotSame(original, copy);
        assertEquals("java.io.IOException: " + LOGIN_URL, original.toString());
        assertEquals(cause, original.getCause());
        assertEquals(0, original.getSuppressed().length);
    }

    @Test
    public void circularCauseChain_terminates() {
        IOException a = new IOException(LOGIN_URL);
        IOException b = new IOException(KOTH_URL, a);
        a.initCause(b);

        Throwable copy = RedactingLog.redacted(a);
        String trace = printed(copy);

        assertFalse(trace, trace.contains("secret"));
        assertTrue(trace, trace.contains("CIRCULAR REFERENCE"));
    }
}
