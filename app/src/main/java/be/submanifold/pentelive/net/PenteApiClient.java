package be.submanifold.pentelive.net;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Schedules synchronous {@link PenteApi}-style calls on a single serial
 * background thread and delivers their {@link Result} on the Android main
 * thread. A returned {@link Cancelable} suppresses the callback after
 * cancellation so a destroyed Activity is never touched (no leak).
 */
public final class PenteApiClient {

    public interface Cb<T> {
        void onResult(Result<T> r);
    }

    public interface Cancelable {
        void cancel();
    }

    private final ExecutorService worker;
    private final Executor main;

    /** Production: one serial worker thread, callbacks delivered on the UI thread. */
    public PenteApiClient() {
        this(Executors.newSingleThreadExecutor(), mainThreadExecutor());
    }

    /** Package-private seam for unit tests: inject worker + main executors. */
    PenteApiClient(ExecutorService worker, Executor main) {
        this.worker = worker;
        this.main = main;
    }

    public <T> Cancelable enqueue(final Callable<Result<T>> call, final Cb<T> cb) {
        final Task<T> task = new Task<>(call, cb);
        worker.execute(task);
        return task;
    }

    private static Executor mainThreadExecutor() {
        final Handler handler = new Handler(Looper.getMainLooper());
        return new Executor() {
            @Override public void execute(Runnable r) {
                handler.post(r);
            }
        };
    }

    private final class Task<T> implements Runnable, Cancelable {
        private final Callable<Result<T>> call;
        private final Cb<T> cb;
        private volatile boolean canceled;

        Task(Callable<Result<T>> call, Cb<T> cb) {
            this.call = call;
            this.cb = cb;
        }

        @Override public void cancel() {
            canceled = true;
        }

        @Override public void run() {
            if (canceled) {
                return;
            }
            Result<T> r;
            try {
                r = call.call();
            } catch (Exception e) {
                r = Result.<T>fail(new Result.Failure(Result.Reason.SERVER, 0, e));
            }
            final Result<T> result = r;
            main.execute(new Runnable() {
                @Override public void run() {
                    if (!canceled) {
                        cb.onResult(result);
                    }
                }
            });
        }
    }
}
