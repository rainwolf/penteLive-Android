package be.submanifold.pentelive;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.CRC32;

//import org.pente.gameServer.core.AlphaNumericGridCoordinates;
//import org.pente.gameServer.core.GridCoordinates;

public class Ai {

    // Canonical mmai game IDs accepted by the native engine (configFor()):
    //   1 = Pente, 3 = Keryo, 11 = Poof, 13 = Connect6, 15 = Boat, 25 = O-Pente.
    // Even IDs are Speed twins with identical board rules. Unknown IDs fall back
    // to plain Pente inside the engine.
    //
    // Connect6 (game 13/14) packs TWO stones per turn into one move int, base 362:
    //   m1 = move / 362; m2 = move % 362; m2 == 361 is the single-stone sentinel
    //   (used for Black's opening stone). Other variants return a plain 0..360.
    // The native side loads pente.tbl / pente.scs / opngbk.pen from filesDir.
    public native long init(String filesDir, int size);

    public native void privateDestroy(long ptr);

    public native void toggleCallbacks(long ptr, int callbacks);

    private native void start(long ptr);

    private native void stop(long ptr);

    private native int move(long ptr, int[] moves, int game, int level, int vct);

    static {
        System.loadLibrary("Ai");
    }

    private volatile boolean running;
    private volatile boolean destroyed;
    private long cPtr;
    private int game;
    private int level = 1;
    private int vct;
    private int seat = 1;
    private int size = 19;

    private MMAIBoardView board;
    private DBBoardView dbBoard;


    public void setBoard(MMAIBoardView board) {
        this.board = board;
    }

    public void setDbBoard(DBBoardView dbBoard) {
        this.dbBoard = dbBoard;
    }

    private boolean active;

//	private List<AiListener> aiListeners = new ArrayList<AiListener>();

    // Opening book is now owned by the native engine (built into the CAi ctor),
    // so it is always on; the flag is retained only for API compatibility.
    private boolean useOpeningBook = true;


    public Ai(int game, int level, int vct, int seat, int size) {
        this.game = game;
        this.level = level;
        this.vct = vct;
        this.seat = seat;
        this.size = size;

        runnable = new AIRunnable();
        runnable.reset();
        thread = new Thread(runnable);
        thread.start();
    }

    // New init flow: the native engine loads pente.tbl / pente.scs / opngbk.pen
    // itself from a directory, so we materialise the three raw resources into
    // filesDir/mmai/ (skipping any that are already present with a matching
    // size) and hand the directory to JNI.
    public void init(InputStream scs, InputStream opnbk, InputStream tblIn,
                     File filesDir) throws Throwable {
        File mmaiDir = new File(filesDir, "mmai");
        if (!mmaiDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            mmaiDir.mkdirs();
        }
        copyResource(tblIn, new File(mmaiDir, "pente.tbl"));
        copyResource(scs, new File(mmaiDir, "pente.scs"));
        copyResource(opnbk, new File(mmaiDir, "opngbk.pen"));
        cPtr = init(mmaiDir.getAbsolutePath(), size);
    }

    private static void copyResource(InputStream in, File dest) throws Throwable {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        try {
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
        } finally {
            in.close();
        }
        byte[] data = bos.toByteArray();
        // Skip the write only if the on-disk file is byte-identical, verified by a
        // CRC32 over the whole file (length alone can collide, e.g. a truncated or
        // partially-written resource of the same length as a previous version).
        if (dest.exists() && dest.length() == data.length && crc32(dest) == crc32(data)) {
            return;
        }
        FileOutputStream out = new FileOutputStream(dest);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    private static long crc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

    private static long crc32(File file) throws Throwable {
        CRC32 crc = new CRC32();
        InputStream fin = new FileInputStream(file);
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fin.read(buf)) != -1) {
                crc.update(buf, 0, n);
            }
        } finally {
            fin.close();
        }
        return crc.getValue();
    }

//	public void addAiListener(AiListener aiListener) {
//		aiListeners.add(aiListener);
//	}

    public int getSeat() {
        return seat;
    }

    private boolean alreadyDestroyed = false;

    public void destroy() {
        stopThinking();
        //System.out.println("destroyed flag set");

        if (alreadyDestroyed) return;
        alreadyDestroyed = true;
        // if thread is still alive in getMove call, then allow it
        // to finish and destroy from there.  otherwise can crash
        if (thread == null || !thread.isAlive()) {
            //System.out.println("thread not alive, java destroy");
            privateDestroy(cPtr);
        } else {
            if (runnable != null) runnable.kill();
            if (thread != null) thread.interrupt();
            destroyed = true;
        }
    }

    public void stopThinking() {
        if (running) {
            stop(cPtr);
            thread.interrupt();
        }
        notifyStopThinking();
    }

    private void notifyStopThinking() {
//		for (AiListener aiListener : aiListeners) {
//			aiListener.stopThinking();
//		}
    }

//	public void setVisualization(boolean visualization) {
//		toggleCallbacks(cPtr, visualization ? 1 : 0);
//	}

    //	public int getMoveNoThreaded(final int moves[]) {
//        return move(cPtr, moves, game, level, vct);
//	}
    public void getMove(final int[] moves) {
//	    new Throwable().printStackTrace();
        // The native engine now owns the opening book, so every move goes
        // through the (asynchronous) native search on the AI thread.
        startThinking();
        start(cPtr);
        runnable.go(moves);
    }

    private final Thread thread;
    private final AIRunnable runnable;

    private class AIRunnable implements Runnable {
        private volatile boolean alive = true;
        private final Object lock = new Object();

        private int[] moves;

        public void go(int[] moves) {
            this.moves = moves;

            synchronized (lock) {
                lock.notifyAll();
            }
        }

        public void run() {

            while (alive) {

                try {
                    synchronized (lock) {
                        lock.wait();
                    }

                    int newMove = move(cPtr, moves, game, level, vct);
//	    			int newMove = marksAi.getMove();
                    // sleep for 0.x seconds
                    Thread.sleep(180);
                    // -1 is the cancelled/no-move sentinel from the native search
                    // (e.g. the search was stopped). Feeding it to a board would push
                    // -1 onto its move list and index the board at a negative row, so
                    // skip processing entirely for a cancelled move.
                    if (newMove != -1) {
                        if (board != null) {
                            board.processAImove(newMove);
                        }
                        if (dbBoard != null) {
                            dbBoard.processAImove(newMove);
                        }
                    }
                    if (alive && !destroyed) {
//                        for (AiListener aiListener : aiListeners) {
//                            aiListener.moveReady(moves, newMove);
//                        }
                        notifyStopThinking();
                    }
                    if (destroyed) {
                        //System.out.println("destroy from getMove() java");
                        privateDestroy(cPtr);
                        alive = false;
                    }

                } catch (InterruptedException e) {
                    //System.out.println("ai interrupted, loop");
                } catch (Throwable t) {
                    //Log.v("ai", "Unknown error in ai thread", t);
                    alive = false;//stop thread
                    //TODO tell the user?
                }
            }
        }

        public String toString() {
            return getName();
        }

        public void kill() {
            alive = false;
        }

        public void reset() {
            alive = true;
        }

        public String getName() {
            return "AIThread";
        }
    }

    private void aiEvaluatedCallBack() {
//		for (AiListener aiListener : aiListeners) {
//			aiListener.aiEvaluateCallBack();
//		}
    }

    private void aiVisualizationCallBack(int[] bd) {
//		for (AiListener aiListener : aiListeners) {
//			aiListener.aiVisualizationCallBack(bd);
//		}
    }

    private void startThinking() {
//		for (AiListener aiListener : aiListeners) {
//			aiListener.startThinking();
//		}
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setVct(int vct) {
        this.vct = vct;
    }

    public void setSeat(int seat) {
        this.seat = seat;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setGame(int game) {
        this.game = game;
    }

    public int getLevel() {
        return level;
    }

    public int getVct() {
        return vct;
    }

    public boolean useOpeningBook() {
        return useOpeningBook;
    }

    public void useOpeningBook(boolean useBook) {
        // Retained for API compatibility; the native engine always uses its
        // built-in opening book (CAi ctor openingBook = true).
        this.useOpeningBook = useBook;
    }
}
