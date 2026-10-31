package be.submanifold.pentelive.db;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Moves plus a redo stack for swipe back/forward. The initial list is always
 * copied, so callers may pass an immutable list. In turn-step mode (Connect6)
 * back/forward move whole turns: boundaries fall at sizes 1, 3, 5, ...
 */
public final class RedoHistory {
    private final ArrayList<Integer> moves = new ArrayList<>();
    private final ArrayDeque<Integer> redo = new ArrayDeque<>();
    private final int floor;
    private final boolean turnSteps;

    public RedoHistory(List<Integer> initial, int floor, boolean turnSteps) {
        this.floor = floor;
        this.turnSteps = turnSteps;
        reset(initial);
    }

    public void play(int cell) {
        moves.add(cell);
        redo.clear();
    }

    public boolean back() {
        int size = moves.size();
        if (size <= floor) {
            return false;
        }
        int target = size - 1;
        if (turnSteps) {
            // largest odd size strictly below the current size
            target = (size % 2 == 1) ? size - 2 : size - 1;
        }
        target = Math.max(target, floor);
        while (moves.size() > target) {
            redo.addFirst(moves.remove(moves.size() - 1));
        }
        return true;
    }

    public boolean forward() {
        if (redo.isEmpty()) {
            return false;
        }
        int count = 1;
        if (turnSteps) {
            int size = moves.size();
            count = (size % 2 == 1) ? 2 : 1;
        }
        while (count-- > 0 && !redo.isEmpty()) {
            moves.add(redo.removeFirst());
        }
        return true;
    }

    /** Jumps back to the floor position, keeping the undone moves for redo. */
    public boolean toStart() {
        if (moves.size() <= floor) {
            return false;
        }
        while (moves.size() > floor) {
            redo.addFirst(moves.remove(moves.size() - 1));
        }
        return true;
    }

    /** Replays the whole redo stack. */
    public boolean toEnd() {
        if (redo.isEmpty()) {
            return false;
        }
        while (!redo.isEmpty()) {
            moves.add(redo.removeFirst());
        }
        return true;
    }

    public List<Integer> moves() {
        return Collections.unmodifiableList(new ArrayList<>(moves));
    }

    public List<Integer> redo() {
        return Collections.unmodifiableList(new ArrayList<>(redo));
    }

    /** Restores a saved position: moves plus redo stack (next move to redo first). */
    public void restore(List<Integer> savedMoves, List<Integer> savedRedo) {
        moves.clear();
        moves.addAll(savedMoves);
        redo.clear();
        for (Integer c : savedRedo) redo.addLast(c);
    }

    public void reset(List<Integer> initial) {
        moves.clear();
        moves.addAll(initial);
        redo.clear();
    }
}
