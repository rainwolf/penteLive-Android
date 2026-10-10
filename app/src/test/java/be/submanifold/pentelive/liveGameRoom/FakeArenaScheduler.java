package be.submanifold.pentelive.liveGameRoom;

import java.util.ArrayList;
import java.util.List;

/** Manual clock for ArenaRequestListModel tests: tasks run only inside advance(), in due order. */
final class FakeArenaScheduler implements ArenaRequestListModel.Scheduler {

    private static final class Task {
        final long due;
        final Runnable runnable;
        boolean cancelled;

        Task(long due, Runnable runnable) {
            this.due = due;
            this.runnable = runnable;
        }
    }

    private final List<Task> tasks = new ArrayList<>();
    private long now = 0;

    @Override
    public ArenaRequestListModel.Cancellable schedule(Runnable task, long delayMs) {
        final Task t = new Task(now + delayMs, task);
        tasks.add(t);
        return () -> t.cancelled = true;
    }

    /** Moves the clock forward by ms, running every task that falls due (including ones scheduled meanwhile). */
    void advance(long ms) {
        long target = now + ms;
        while (true) {
            Task next = null;
            for (Task t : tasks) {
                if (!t.cancelled && t.due <= target && (next == null || t.due < next.due)) {
                    next = t;
                }
            }
            if (next == null) break;
            tasks.remove(next);
            now = next.due;
            next.runnable.run();
        }
        now = target;
    }

    int pendingCount() {
        int n = 0;
        for (Task t : tasks) {
            if (!t.cancelled) n++;
        }
        return n;
    }
}
