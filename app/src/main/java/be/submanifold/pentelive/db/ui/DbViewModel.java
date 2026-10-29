package be.submanifold.pentelive.db.ui;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import be.submanifold.pentelive.db.DbController;

/** Keeps the {@link DbController} and its IO pool across configuration changes. */
public class DbViewModel extends ViewModel {
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private final Executor main = new Handler(Looper.getMainLooper())::post;
    private DbController controller;
    private String gameLabel;

    public boolean hasController() {
        return controller != null;
    }

    public DbController controller() {
        return controller;
    }

    public void setController(DbController controller, String gameLabel) {
        this.controller = controller;
        this.gameLabel = gameLabel;
    }

    /** Spinner-style label of the game on screen ("Speed Pente"); may name an unsupported game. */
    public String gameLabel() {
        return gameLabel;
    }

    public void setGameLabel(String gameLabel) {
        this.gameLabel = gameLabel;
    }

    public ExecutorService io() {
        return io;
    }

    public Executor main() {
        return main;
    }

    @Override
    protected void onCleared() {
        io.shutdown();
    }
}
