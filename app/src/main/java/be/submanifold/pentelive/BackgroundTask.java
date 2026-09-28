package be.submanifold.pentelive;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-repo replacement for the deprecated {@link android.os.AsyncTask}. Keeps AsyncTask's
 * semantics: tasks run one at a time, FIFO, on a process-wide serial background thread,
 * results are delivered on the main looper, and an exception thrown by doInBackground
 * is rethrown on the background thread, crashing the app as AsyncTask did.
 */
public abstract class BackgroundTask<Params, Result> {

    private enum Status { PENDING, RUNNING, FINISHED }

    private static final class SerialExecutorHolder {
        static final AtomicInteger THREAD_COUNT = new AtomicInteger(1);
        static final Executor INSTANCE = Executors.newSingleThreadExecutor(
                r -> new Thread(r, "BackgroundTask #" + THREAD_COUNT.getAndIncrement()));
    }

    private static final class MainHandlerHolder {
        static final Handler HANDLER = new Handler(Looper.getMainLooper());
    }

    private static final Executor MAIN_EXECUTOR = r -> MainHandlerHolder.HANDLER.post(r);

    private final Executor background;
    private final Executor main;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    // Ensures cancel(true) can only interrupt the thread while it runs this task's doInBackground.
    private final Object runnerLock = new Object();
    private Thread runner; // guarded by runnerLock
    private boolean completed; // guarded by runnerLock
    private volatile Status status = Status.PENDING;

    public BackgroundTask() {
        this(serialExecutor(), MAIN_EXECUTOR);
    }

    BackgroundTask(Executor background, Executor main) {
        this.background = background;
        this.main = main;
    }

    static Executor serialExecutor() {
        return SerialExecutorHolder.INSTANCE;
    }

    @SafeVarargs
    public final BackgroundTask<Params, Result> execute(Params... params) {
        switch (status) {
            case RUNNING:
                throw new IllegalStateException("Cannot execute task: the task is already running.");
            case FINISHED:
                throw new IllegalStateException("Cannot execute task: the task has already been executed "
                        + "(a task can be executed only once)");
            default:
                break;
        }
        status = Status.RUNNING;
        background.execute(() -> runInBackground(params));
        return this;
    }

    public final boolean cancel(boolean mayInterruptIfRunning) {
        boolean wasCancelled = cancelled.getAndSet(true);
        synchronized (runnerLock) {
            if (wasCancelled || completed) {
                return false;
            }
            if (mayInterruptIfRunning && runner != null) {
                runner.interrupt();
            }
            return true;
        }
    }

    public final boolean isCancelled() {
        return cancelled.get();
    }

    @SuppressWarnings("unchecked")
    protected abstract Result doInBackground(Params... params);

    protected void onPostExecute(Result result) {
    }

    protected void onCancelled(Result result) {
        onCancelled();
    }

    protected void onCancelled() {
    }

    private void runInBackground(Params[] params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        boolean invoke;
        synchronized (runnerLock) {
            invoke = !cancelled.get();
            if (invoke) {
                runner = Thread.currentThread();
            }
        }
        Result result = null;
        if (invoke) {
            try {
                result = doInBackground(params);
            } catch (Throwable t) {
                throw new RuntimeException("An error occurred while executing doInBackground()", t);
            } finally {
                synchronized (runnerLock) {
                    runner = null;
                    completed = true;
                }
            }
        }
        final Result finalResult = result;
        main.execute(() -> finish(finalResult));
    }

    private void finish(Result result) {
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
        status = Status.FINISHED;
    }
}
