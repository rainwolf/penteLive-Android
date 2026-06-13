package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PenteApiClientTest {

    /** Collects posted runnables; runs them only when the test asks. */
    static final class ManualExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<>();
        private final CountDownLatch posted;

        ManualExecutor() { this(null); }
        ManualExecutor(CountDownLatch posted) { this.posted = posted; }

        @Override public synchronized void execute(Runnable r) {
            tasks.add(r);
            if (posted != null) {
                posted.countDown();
            }
        }

        synchronized void runAll() {
            for (Runnable r : new ArrayList<>(tasks)) {
                r.run();
            }
            tasks.clear();
        }
    }

    @Test
    public void enqueue_runsOffCallerThread_andPreservesOrder() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        ManualExecutor main = new ManualExecutor();
        PenteApiClient client = new PenteApiClient(worker, main);

        final List<String> runOrder = Collections.synchronizedList(new ArrayList<String>());
        final CountDownLatch both = new CountDownLatch(2);
        final Thread callerThread = Thread.currentThread();
        final AtomicReference<Thread> workerThread = new AtomicReference<>();
        final PenteApiClient.Cb<String> noop = new PenteApiClient.Cb<String>() {
            @Override public void onResult(Result<String> r) { }
        };

        client.enqueue(new Callable<Result<String>>() {
            @Override public Result<String> call() {
                workerThread.set(Thread.currentThread());
                runOrder.add("a");
                both.countDown();
                return Result.ok("a");
            }
        }, noop);

        client.enqueue(new Callable<Result<String>>() {
            @Override public Result<String> call() {
                runOrder.add("b");
                both.countDown();
                return Result.ok("b");
            }
        }, noop);

        assertTrue("both callables should run", both.await(2, TimeUnit.SECONDS));
        assertEquals(Arrays.asList("a", "b"), runOrder);
        assertNotSame("callable must run off the caller thread", callerThread, workerThread.get());

        worker.shutdownNow();
    }

    @Test
    public void cancel_beforeCompletion_callbackNeverFires() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        final CountDownLatch posted = new CountDownLatch(1);
        ManualExecutor main = new ManualExecutor(posted);
        PenteApiClient client = new PenteApiClient(worker, main);

        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch proceed = new CountDownLatch(1);
        final AtomicBoolean cbFired = new AtomicBoolean(false);

        PenteApiClient.Cancelable c = client.enqueue(new Callable<Result<String>>() {
            @Override public Result<String> call() throws Exception {
                started.countDown();
                proceed.await(2, TimeUnit.SECONDS);
                return Result.ok("hi");
            }
        }, new PenteApiClient.Cb<String>() {
            @Override public void onResult(Result<String> r) {
                cbFired.set(true);
            }
        });

        assertTrue("callable should have started", started.await(2, TimeUnit.SECONDS));
        c.cancel();                 // cancel while the callable is still blocked
        proceed.countDown();        // let the callable finish
        assertTrue("callback should be posted to main", posted.await(2, TimeUnit.SECONDS));
        main.runAll();              // execute the posted callback runnable
        assertFalse("canceled callback must not fire", cbFired.get());

        worker.shutdownNow();
    }
}
