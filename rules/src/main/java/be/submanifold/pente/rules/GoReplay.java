package be.submanifold.pente.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Go replay for the rules module: a port of the live {@code Game.replayGoGame(int)} and its
 * group/capture/ko helpers onto a local row-major {@code byte[n][n]} board.
 *
 * <p>Conventions kept from the live board on purpose (locked by the app's
 * {@code GoReplayEquivalenceTest}):
 * <ul>
 *   <li>Black moves first: move {@code i} is player {@code 1 + i % 2}, colour {@code 3 - player}.</li>
 *   <li>{@code n * n} is a pass. Passes and off-board moves still count for colour parity.</li>
 *   <li>After two consecutive passes every further on-board move is a dead-stone mark: the
 *       stone is removed and not counted as a capture.</li>
 *   <li>A suicidal stone stays on the board.</li>
 *   <li>Ko = the cell of a single captured stone when the capturing stone is itself in atari
 *       shape; any placed stone that does not capture exactly one stone resets it. A pass does
 *       not reset it.</li>
 * </ul>
 * {@code whiteCaptures} counts white stones lost, {@code blackCaptures} black stones lost.
 */
public final class GoReplay {

    private final int n;
    private final int passMove;
    private final byte[][] board;
    private final Map<Integer, Map<Integer, List<Integer>>> groupsByPlayerAndID = new HashMap<>();
    private final Map<Integer, Map<Integer, Integer>> stoneGroupIDsByPlayer = new HashMap<>();
    private int koMove = -1;
    private int whiteCaptures;
    private int blackCaptures;

    private GoReplay(int n) {
        this.n = n;
        this.passMove = n * n;
        this.board = new byte[n][n];
        groupsByPlayerAndID.put(1, new HashMap<Integer, List<Integer>>());
        groupsByPlayerAndID.put(2, new HashMap<Integer, List<Integer>>());
        stoneGroupIDsByPlayer.put(1, new HashMap<Integer, Integer>());
        stoneGroupIDsByPlayer.put(2, new HashMap<Integer, Integer>());
    }

    /** Replays {@code moves[0..untilMove)} of a Go game. {@code v} must be a Go variant. */
    public static BoardState replay(List<Integer> moves, Variant v, int untilMove) {
        if (v == null || !v.isGo()) {
            throw new IllegalArgumentException("GoReplay needs a Go variant, got " + v);
        }
        GoReplay r = new GoReplay(Variants.gridSize(v));
        int lastPlaced = r.run(moves, untilMove);
        return new BoardState(r.board, r.whiteCaptures, r.blackCaptures, r.n, lastPlaced,
                null, false, false, r.koMove);
    }

    private int run(List<Integer> moves, int until) {
        int lastPlaced = -1;
        boolean hasPass = false, doublePass = false;
        int count = Math.min(moves == null ? 0 : moves.size(), until);
        for (int i = 0; i < count; i++) {
            int move = moves.get(i);
            if (move == passMove) {
                if (hasPass) {
                    doublePass = true;
                } else {
                    hasPass = true;
                }
            } else {
                hasPass = false;
            }

            if (doublePass) {
                if (0 <= move && move < passMove) {
                    setPosition(move, 0);
                }
            } else {
                int currentPlayer = 1 + i % 2;
                int color = 3 - currentPlayer;
                if (0 <= move && move < passMove) {
                    setPosition(move, color);
                    lastPlaced = move;
                    settleGroups(move, groupsByPlayerAndID.get(currentPlayer),
                            stoneGroupIDsByPlayer.get(currentPlayer));
                    int opponent = 3 - currentPlayer;
                    makeCaptures(move, groupsByPlayerAndID.get(opponent),
                            stoneGroupIDsByPlayer.get(opponent));
                }
            }
        }
        return lastPlaced;
    }

    private int getPosition(int move) {
        return board[move / n][move % n];
    }

    private void setPosition(int move, int val) {
        board[move / n][move % n] = (byte) val;
    }

    private void makeCaptures(int move, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs) {
        int captures = 0;
        if (move % n != 0) {
            captures = getCaptures(move, groupsByID, stoneGroupIDs, captures, move - 1);
        }
        if (move % n != n - 1) {
            captures = getCaptures(move, groupsByID, stoneGroupIDs, captures, move + 1);
        }
        if (move / n != 0) {
            captures = getCaptures(move, groupsByID, stoneGroupIDs, captures, move - n);
        }
        if (move / n != n - 1) {
            captures = getCaptures(move, groupsByID, stoneGroupIDs, captures, move + n);
        }
        if (captures != 1) {
            koMove = -1;
        }
    }

    private int getCaptures(int move, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs,
                            int captures, int neighborStone) {
        Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
        if (neighborStoneGroupID != null) {
            List<Integer> neighborStoneGroup = groupsByID.get(neighborStoneGroupID);
            if (!groupHasLiberties(neighborStoneGroup)) {
                if (koMove < 0 && neighborStoneGroup.size() == 1 && checkKo(move)) {
                    koMove = neighborStone;
                } else {
                    koMove = -1;
                }
                captures += neighborStoneGroup.size();
                captureGroup(neighborStoneGroupID, groupsByID, stoneGroupIDs);
            }
        }
        return captures;
    }

    private boolean checkKo(int move) {
        int position = getPosition(move);
        if (move % n != 0 && position != 3 - getPosition(move - 1)) {
            return false;
        }
        if (move % n != n - 1 && position != 3 - getPosition(move + 1)) {
            return false;
        }
        if (move / n != 0 && position != 3 - getPosition(move - n)) {
            return false;
        }
        if (move / n != n - 1) {
            return position == 3 - getPosition(move + n);
        }
        return true;
    }

    private void captureGroup(int groupID, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs) {
        List<Integer> group = groupsByID.get(groupID);
        if (!group.isEmpty()) {
            int pos = getPosition(group.get(0));
            if (pos == 1) {
                whiteCaptures += group.size();
            } else if (pos == 2) {
                blackCaptures += group.size();
            }
        }
        for (int stone : group) {
            setPosition(stone, 0);
            stoneGroupIDs.remove(stone);
        }
        groupsByID.remove(groupID);
    }

    private boolean groupHasLiberties(List<Integer> group) {
        for (int stone : group) {
            if (stoneHasLiberties(stone)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmptyish(int cell) {
        int position = getPosition(cell);
        return position != 1 && position != 2;
    }

    private boolean stoneHasLiberties(int stone) {
        if (stone % n != 0 && isEmptyish(stone - 1)) {
            return true;
        }
        if (stone % n != n - 1 && isEmptyish(stone + 1)) {
            return true;
        }
        if (stone / n != 0 && isEmptyish(stone - n)) {
            return true;
        }
        if (stone / n != n - 1) {
            return isEmptyish(stone + n);
        }
        return false;
    }

    private void settleGroups(int move, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs) {
        List<Integer> newGroup = new ArrayList<>();
        newGroup.add(move);
        groupsByID.put(move, newGroup);
        stoneGroupIDs.put(move, move);
        if (move % n != 0) {
            Integer id = stoneGroupIDs.get(move - 1);
            if (id != null) mergeGroups(move, id, groupsByID, stoneGroupIDs);
        }
        if (move % n != n - 1) {
            Integer id = stoneGroupIDs.get(move + 1);
            if (id != null) mergeGroups(stoneGroupIDs.get(move), id, groupsByID, stoneGroupIDs);
        }
        if (move / n != 0) {
            Integer id = stoneGroupIDs.get(move - n);
            if (id != null) mergeGroups(stoneGroupIDs.get(move), id, groupsByID, stoneGroupIDs);
        }
        if (move / n != n - 1) {
            Integer id = stoneGroupIDs.get(move + n);
            if (id != null) mergeGroups(stoneGroupIDs.get(move), id, groupsByID, stoneGroupIDs);
        }
    }

    private void mergeGroups(int group1, int group2, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs) {
        if (group1 == group2) {
            return;
        }
        int oldGroupID = Math.min(group1, group2);
        int newGroupID = Math.max(group1, group2);
        List<Integer> oldGroup = groupsByID.get(oldGroupID);
        List<Integer> newGroup = groupsByID.get(newGroupID);
        groupsByID.remove(oldGroupID);
        newGroup.addAll(oldGroup);
        for (int oldStone : oldGroup) {
            stoneGroupIDs.put(oldStone, newGroupID);
        }
    }
}
