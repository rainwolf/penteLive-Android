package be.submanifold.pentelive.db;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/** Queues runnables; tests run them in any order. */
final class ManualExecutor implements Executor {
    final List<Runnable> queue = new ArrayList<>();

    @Override
    public void execute(Runnable r) {
        queue.add(r);
    }

    int size() {
        return queue.size();
    }

    void run(int index) {
        queue.remove(index).run();
    }

    void runAll() {
        while (!queue.isEmpty()) run(0);
    }
}
