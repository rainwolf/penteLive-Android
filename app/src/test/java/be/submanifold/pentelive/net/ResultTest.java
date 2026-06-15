package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import be.submanifold.pentelive.net.Result.Failure;
import be.submanifold.pentelive.net.Result.Reason;

import org.junit.Test;

public class ResultTest {

    @Test
    public void ok_carriesValue_andIsOk() {
        Result<String> r = Result.ok("hello");
        assertTrue(r.isOk());
        assertEquals("hello", r.value);
        assertNull(r.failure);
    }

    @Test
    public void fail_carriesFailure_andIsNotOk() {
        Throwable cause = new RuntimeException("boom");
        Failure f = new Failure(Reason.SERVER, 500, cause);
        Result<String> r = Result.fail(f);
        assertFalse(r.isOk());
        assertNull(r.value);
        assertSame(f, r.failure);
        assertEquals(Reason.SERVER, r.failure.reason);
        assertEquals(500, r.failure.httpCode);
        assertSame(cause, r.failure.cause);
    }

    @Test
    public void everyReason_isRepresentable() {
        Reason[] reasons = {
            Reason.NETWORK,
            Reason.AUTH_EXPIRED,
            Reason.INVALID_CREDENTIALS,
            Reason.SERVER,
            Reason.PARSE
        };
        assertEquals(5, Reason.values().length);
        for (Reason reason : reasons) {
            Result<Void> r = Result.fail(new Failure(reason, 0, null));
            assertFalse(r.isOk());
            assertEquals(reason, r.failure.reason);
        }
    }
}
