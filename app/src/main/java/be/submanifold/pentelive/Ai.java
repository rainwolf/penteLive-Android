package be.submanifold.pentelive;

import java.io.InputStream;
import java.util.*;

//import org.pente.gameServer.core.AlphaNumericGridCoordinates;
//import org.pente.gameServer.core.GridCoordinates;

public class Ai {

	public native long init(int atbl[], int asrc[], int size);
	public native void privateDestroy(long ptr);
	public native void toggleCallbacks(long ptr, int callbacks);

	private native void start(long ptr);
	private native void stop(long ptr);
	private native int move(long ptr, int moves[], int game, int level, int vct);

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
	
	private MarksAIPlayer marksAi = new MarksAIPlayer();
	

	public Ai(int game, int level, int vct, int seat, int size) {
		this.game = game;
		this.level = level;
		this.vct = vct;
		this.seat = seat;
		this.size = size;
		
		marksAi.setGame(game);
		marksAi.setLevel(level);
		marksAi.setSeat(seat);
		
		runnable = new AIRunnable();
		runnable.reset();
		thread = new Thread(runnable);
        thread.start();
	}
	public void init(InputStream scs, InputStream opnbk, InputStream tblIn) 
	    throws Throwable {
	    marksAi.init(scs, opnbk, tblIn);
        cPtr = init(marksAi.getTbl(), marksAi.getSrc(), size);
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
		}
		else {
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
	public void getMove(final int moves[]) {
//	    new Throwable().printStackTrace();
	    marksAi.clear();
	    for (int m : moves) {
	        marksAi.addMove(m);
	    }
	    int m = marksAi.getMove();
	    if (m != -1) {
			if (board != null) {
				board.processAImove(m);
			}
			if (dbBoard != null) {
				dbBoard.processAImove(m);
			}
//            for (AiListener aiListener : aiListeners) {
//                aiListener.moveReady(moves, m);
//            }
	    }
	    else {
	        startThinking();
    		start(cPtr);
    		runnable.go(moves);
	    }
	}
    private Thread thread;
    private AIRunnable runnable;
	private class AIRunnable implements Runnable {
        private volatile boolean alive = true;
        private Object lock = new Object();
        
        private int moves[];
        public void go(int moves[]) {
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
					if (board != null) {
						board.processAImove(newMove);
					}
					if (dbBoard != null) {
						dbBoard.processAImove(newMove);
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
	private void aiVisualizationCallBack(int bd[]) {
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
		marksAi.setLevel(level);
	}
	public void setVct(int vct) {
		this.vct = vct;
	}
	public void setSeat(int seat) {
		this.seat = seat;
		marksAi.setSeat(seat);
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public void setGame(int game) {
		this.game = game;
		marksAi.setGame(game);
	}
	public int getLevel() {
		return level;
	}
	public int getVct() {
		return vct;
	}
	public boolean useOpeningBook() {
		return marksAi.useOpeningBook();
	}

	public void useOpeningBook(boolean useBook) {
		marksAi.useOpeningBook(useBook);
	}
}
