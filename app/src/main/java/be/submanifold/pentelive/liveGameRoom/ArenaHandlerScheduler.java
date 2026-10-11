package be.submanifold.pentelive.liveGameRoom;

import android.os.Handler;
import android.os.Looper;

/** Runs ArenaRequestListModel timers on the main thread, where the model and its views live. */
final class ArenaHandlerScheduler implements ArenaRequestListModel.Scheduler {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public ArenaRequestListModel.Cancellable schedule(Runnable task, long delayMs) {
        // A fresh wrapper per call, so cancel() removes exactly this posting.
        final Runnable posted = task::run;
        handler.postDelayed(posted, delayMs);
        return () -> handler.removeCallbacks(posted);
    }
}
