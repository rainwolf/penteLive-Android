package be.submanifold.pentelive;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BackgroundTaskTest {

    private static final long TIMEOUT_SECONDS = 5;

    private static class RecordingTask extends BackgroundTask<String, String> {
        final List<String> received = new ArrayList<>();
        int doInBackgroundCalls;
        int onPostExecuteCalls;
        String postResult;
        int onCancelledResultCalls;
        String cancelledResult;
        String result = "result";

        RecordingTask(Executor background, Executor main) {
            super(background, main);
        }

        @Override
        protected String doInBackground(String... params) {
            doInBackgroundCalls++;
            received.addAll(Arrays.asList(params));
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            onPostExecuteCalls++;
            postResult = result;
        }

        @Override
        protected void onCancelled(String result) {
            onCancelledResultCalls++;
            cancelledResult = result;
        }
    }

    private static void drain(Queue<Runnable> queue) {
        Runnable r;
        while ((r = queue.poll()) != null) {
            r.run();
        }
    }

    @Test
    public void paramsAndResultFlowThroughAndOnPostExecuteRunsOnMainExecutor() {
        Queue<Runnable> background = new ArrayDeque<>();
        Queue<Runnable> main = new ArrayDeque<>();
        RecordingTask task = new RecordingTask(background::add, main::add);

        assertSame(task, task.execute("a", "b"));
        assertEquals(1, background.size());
        assertTrue(main.isEmpty());

        drain(background);
        assertEquals(Arrays.asList("a", "b"), task.received);
        assertEquals(0, task.onPostExecuteCalls);
        assertEquals(1, main.size());

        drain(main);
        assertEquals(1, task.onPostExecuteCalls);
        assertEquals("result", task.postResult);
        assertEquals(0, task.onCancelledResultCalls);
        assertFalse(task.isCancelled());
    }

    @Test
    public void onPostExecuteRunsOffTheBackgroundThread() throws Exception {
        ExecutorService background = Executors.newSingleThreadExecutor();
        BlockingQueue<Runnable> main = new LinkedBlockingQueue<>();
        AtomicReference<Thread> backgroundThread = new AtomicReference<>();
        AtomicReference<Thread> postThread = new AtomicReference<>();
        try {
            BackgroundTask<Void, Boolean> task = new BackgroundTask<Void, Boolean>(background, main::add) {
                @Override
                protected Boolean doInBackground(Void... params) {
                    backgroundThread.set(Thread.currentThread());
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    postThread.set(Thread.currentThread());
                }
            };
            task.execute((Void) null);

            Runnable post = main.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(post);
            post.run();
            assertSame(Thread.currentThread(), postThread.get());
            assertNotNull(backgroundThread.get());
            assertFalse(backgroundThread.get() == postThread.get());
        } finally {
            background.shutdownNow();
        }
    }

    @Test
    public void executeTwiceThrows() {
        Queue<Runnable> background = new ArrayDeque<>();
        Queue<Runnable> main = new ArrayDeque<>();
        RecordingTask task = new RecordingTask(background::add, main::add);
        task.execute();

        try {
            task.execute();
            fail("expected IllegalStateException while running");
        } catch (IllegalStateException e) {
            assertEquals("Cannot execute task: the task is already running.", e.getMessage());
        }

        drain(background);
        drain(main);
        try {
            task.execute();
            fail("expected IllegalStateException after finished");
        } catch (IllegalStateException e) {
            assertEquals("Cannot execute task: the task has already been executed "
                    + "(a task can be executed only once)", e.getMessage());
        }
        assertEquals(1, task.doInBackgroundCalls);
        assertEquals(1, task.onPostExecuteCalls);
    }

    @Test
    public void doInBackgroundExceptionPropagatesWrappedAndNothingIsPosted() {
        Queue<Runnable> background = new ArrayDeque<>();
        Queue<Runnable> main = new ArrayDeque<>();
        IllegalArgumentException original = new IllegalArgumentException("boom");
        RecordingTask task = new RecordingTask(background::add, main::add) {
            @Override
            protected String doInBackground(String... params) {
                throw original;
            }
        };
        task.execute();

        try {
            background.poll().run();
            fail("expected RuntimeException from the background runnable");
        } catch (RuntimeException e) {
            assertEquals("An error occurred while executing doInBackground()", e.getMessage());
            assertSame(original, e.getCause());
        }
        assertTrue(main.isEmpty());
        assertEquals(0, task.onPostExecuteCalls);
    }

    @Test
    public void cancelBeforeRunSkipsDoInBackgroundAndDeliversOnCancelledNull() {
        Queue<Runnable> background = new ArrayDeque<>();
        Queue<Runnable> main = new ArrayDeque<>();
        RecordingTask task = new RecordingTask(background::add, main::add);
        task.execute("a");

        assertTrue(task.cancel(false));
        assertTrue(task.isCancelled());
        assertFalse(task.cancel(false));

        drain(background);
        assertEquals(0, task.doInBackgroundCalls);
        assertEquals(1, main.size());

        task.cancelledResult = "sentinel";
        drain(main);
        assertEquals(1, task.onCancelledResultCalls);
        assertNull(task.cancelledResult);
        assertEquals(0, task.onPostExecuteCalls);
        assertTrue(task.isCancelled());
    }

    @Test
    public void cancelWhileRunningDeliversOnCancelledWithResultInsteadOfOnPostExecute() {
        Queue<Runnable> background = new ArrayDeque<>();
        Queue<Runnable> main = new ArrayDeque<>();
        AtomicBoolean cancelReturned = new AtomicBoolean();
        RecordingTask task = new RecordingTask(background::add, main::add) {
            @Override
            protected String doInBackground(String... params) {
                cancelReturned.set(cancel(false));
                return "partial";
            }
        };
        task.execute();

        drain(background);
        assertTrue(cancelReturned.get());
        drain(main);
        assertEquals(1, task.onCancelledResultCalls);
        assertEquals("partial", task.cancelledResult);
        assertEquals(0, task.onPostExecuteCalls);
        assertFalse(task.cancel(false));
    }

    @Test
    public void cancelWithInterruptInterruptsTheBackgroundThread() throws Exception {
        ExecutorService background = Executors.newSingleThreadExecutor();
        BlockingQueue<Runnable> main = new LinkedBlockingQueue<>();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch neverReleased = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        List<Boolean> delivered = Collections.synchronizedList(new ArrayList<>());
        try {
            BackgroundTask<Void, Boolean> task = new BackgroundTask<Void, Boolean>(background, main::add) {
                @Override
                protected Boolean doInBackground(Void... params) {
                    started.countDown();
                    try {
                        neverReleased.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        interrupted.set(true);
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    fail("onPostExecute must not run for a cancelled task");
                }

                @Override
                protected void onCancelled(Boolean result) {
                    delivered.add(result);
                }
            };
            task.execute((Void) null);
            assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            assertTrue(task.cancel(true));

            Runnable post = main.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(post);
            assertTrue(interrupted.get());
            post.run();
            assertEquals(Collections.singletonList(false), delivered);
        } finally {
            background.shutdownNow();
        }
    }

    @Test
    public void onCancelledWithResultDefaultsToOnCancelled() {
        Queue<Runnable> background = new ArrayDeque<>();
        Queue<Runnable> main = new ArrayDeque<>();
        AtomicBoolean onCancelledCalled = new AtomicBoolean();
        BackgroundTask<Void, Boolean> task = new BackgroundTask<Void, Boolean>(background::add, main::add) {
            @Override
            protected Boolean doInBackground(Void... params) {
                return true;
            }

            @Override
            protected void onCancelled() {
                onCancelledCalled.set(true);
            }
        };
        task.execute((Void) null);
        task.cancel(false);
        drain(background);
        drain(main);
        assertTrue(onCancelledCalled.get());
    }

    @Test
    public void sharedBackgroundExecutorIsSerialAndFifo() throws Exception {
        Executor executor = BackgroundTask.serialExecutor();
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch bothDone = new CountDownLatch(2);
        AtomicBoolean overlapped = new AtomicBoolean();
        AtomicReference<String> threadName = new AtomicReference<>();

        executor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            order.add("first");
            try {
                // A parallel executor would start the second runnable during this wait.
                overlapped.set(secondStarted.await(200, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
            bothDone.countDown();
        });
        executor.execute(() -> {
            secondStarted.countDown();
            order.add("second");
            bothDone.countDown();
        });

        assertTrue(bothDone.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse(overlapped.get());
        assertEquals(Arrays.asList("first", "second"), order);
        assertTrue(threadName.get(), threadName.get().startsWith("BackgroundTask #"));
    }
}
