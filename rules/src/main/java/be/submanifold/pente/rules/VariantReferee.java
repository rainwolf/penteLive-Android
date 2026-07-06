package be.submanifold.pente.rules;

import java.util.List;

/**
 * Plain-Java referee for the capture / poof board variants: Pente (game 1),
 * Keryo-Pente (3), Poof-Pente (11), Boat-Pente (15) and O-Pente (25), plus
 * Connect6 (13/14). Connect6 is structurally different from the others: it
 * plays TWO stones per turn (see {@link #colorForMove}), wins on a row of SIX
 * OR MORE (see {@link #detectConnect6Of}, overlines count), and has NO
 * captures at all -- the whole capture switch in {@link #replay} is skipped
 * for game 13/14. This was extracted verbatim from {@code MMAIBoardView} (a
 * View subclass) so the board / captures / winner logic can be exercised in a
 * JVM unit test against its authority, {@code react_mmai/src/Classes/GameClass.js}.
 * The View now only presents the result of {@link #replay}.
 *
 * <p>Board convention (identical to the authority and to the old View code):
 * {@code board[x][y]} with {@code x = move % 19}, {@code y = move / 19};
 * {@code 0} = empty, {@code 1} = white, {@code 2} = black, {@code -1} =
 * rated-tournament restriction (treated as empty for capture / poof / boat
 * purposes). {@link #whiteCaptures} / {@link #blackCaptures} hold the number of
 * stones LOST by white / black respectively
 * ({@code == GameClass.captures[1] / captures[2]}).
 *
 * <p>Two GameClass.js quirks, already reconciled upstream, are preserved here:
 * the keryo-poof "down" form guards {@code j+3} (not {@code j+2}), and the
 * five-in-a-row scan uses {@code >= 0} bounds so fives touching row 0 / col 0
 * count.
 */
public class VariantReferee {

    /** Stones lost by white (== GameClass.captures[1]). */
    public int whiteCaptures;
    /** Stones lost by black (== GameClass.captures[2]). */
    public int blackCaptures;
    /** 0 = no winner yet, 1 = white, 2 = black. Same convention as the authority. */
    public int winner;
    /** {@code winner != 0}. */
    public boolean gameOver;

    /**
     * Replays {@code moves} for {@code game} into {@code board} (mutated in
     * place), updating {@link #whiteCaptures} / {@link #blackCaptures} /
     * {@link #winner} / {@link #gameOver}, and returns the winner (0/1/2).
     * Mirrors {@code GameClass.addMove} + {@code isGameOver} for games
     * 1 / 3 / 11 / 15 / 25.
     */
    public int replay(int game, byte[][] board, List<Integer> moves, boolean rated) {
        winner = 0;
        gameOver = false;
        resetAbstractBoard(board);
        for (int i = 0; i < moves.size(); i++) {
            byte color = colorForMove(game, i);
            int x = moves.get(i) % 19, y = moves.get(i) / 19;
            board[x][y] = color;
            switch (game) {
                case 13:
                case 14:
                    // Connect6: two stones per turn, NO captures. Skip the capture
                    // switch entirely (spec: detectConnect6Of only, checked below).
                    break;
                case 11:
                    detectPoof(board, x, y, color);
                    detectPenteCapture(board, x, y, color);
                    break;
                case 15:
                    detectPenteCapture(board, x, y, color);
                    break;
                case 25:
                    detectPoof(board, x, y, color);
                    detectKeryoPoof(board, x, y, color);
                    detectPenteCapture(board, x, y, color);
                    detectKeryoPenteCapture(board, x, y, color);
                    break;
                case 1:
                    detectPenteCapture(board, x, y, color);
                    break;
                case 3:
                default:
                    detectPenteCapture(board, x, y, color);
                    detectKeryoPenteCapture(board, x, y, color);
                    break;
            }
        }
        applyRatedTournamentBlock(board, moves, rated);
        if (moves.isEmpty()) {
            return 0;
        }
        int last = moves.get(moves.size() - 1);
        // colorForMove(game, size-1) is byte-identical to the old strict-alternation
        // form (byte)(2 - (size % 2)) for every non-Connect6 game, and correct for 13/14.
        byte lastColor = colorForMove(game, moves.size() - 1);
        int w = 0;
        switch (game) {
            case 11:
                // Poof-Pente: five wins only if the placed stone was not poofed
                // away (mirrors SimplePoofPenteState.isGameOver returning false
                // when the last move poofed). Then a >= 10 capture lead wins.
                if (board[last % 19][last / 19] == lastColor && detectPenteOf(board, lastColor, last)) {
                    w = lastColor;
                }
                if (w == 0) {
                    if (whiteCaptures >= 10 && whiteCaptures > blackCaptures) {
                        w = 2;
                    } else if (blackCaptures >= 10 && blackCaptures > whiteCaptures) {
                        w = 1;
                    }
                }
                break;
            case 15:
                // Boat-Pente: row survival outranks the capture win; capture win
                // is 10, immediate (no advantage clause). Two INDEPENDENT ifs
                // mirror GameClass.isGameOver (base === 15): captures[1] >= 10 ->
                // winner 2, then captures[2] >= 10 -> winner 1 (the second
                // overwrites the first). captures[1] == whiteCaptures,
                // captures[2] == blackCaptures.
                w = boatRowWinner(board, moves, false);
                if (w == 0) {
                    if (whiteCaptures >= 10) {
                        w = 2;
                    }
                    if (blackCaptures >= 10) {
                        w = 1;
                    }
                }
                break;
            case 25:
                // O-Pente: boat-style survival with pair AND triple
                // capturability, then a >= 15 capture lead (advantage).
                w = boatRowWinner(board, moves, true);
                if (w == 0) {
                    if (whiteCaptures >= 15 && whiteCaptures > blackCaptures) {
                        w = 2;
                    } else if (blackCaptures >= 15 && blackCaptures > whiteCaptures) {
                        w = 1;
                    }
                }
                break;
            case 13:
            case 14:
                // Connect6: 6 OR MORE contiguous through the just-placed stone wins
                // (overlines count); no captures, no advantage/threshold logic.
                if (detectConnect6Of(board, lastColor, last)) {
                    w = lastColor;
                }
                break;
            case 1:
                // Pente: 10-capture win (exact), or a plain five through the last move.
                if (whiteCaptures == 10) {
                    w = 2;
                } else if (blackCaptures == 10) {
                    w = 1;
                } else if (detectPente(board, lastColor, last)) {
                    w = lastColor;
                }
                break;
            case 3:
            default:
                // Keryo-Pente: 15-capture win, or a plain five through the last move.
                if (whiteCaptures >= 15) {
                    w = 2;
                } else if (blackCaptures >= 15) {
                    w = 1;
                } else if (detectPente(board, lastColor, last)) {
                    w = lastColor;
                }
                break;
        }
        winner = w;
        gameOver = (w != 0);
        return w;
    }

    private void resetAbstractBoard(byte[][] abstractBoard) {
        whiteCaptures = 0;
        blackCaptures = 0;
        for (int i = 0; i < 19; i++) {
            for (int j = 0; j < 19; j++) {
                abstractBoard[i][j] = 0;
            }
        }
    }

    private void applyRatedTournamentBlock(byte[][] abstractBoard, List<Integer> moves, boolean rated) {
        if (rated && (moves.size() == 2)) {
            for (int i = 7; i < 12; ++i) {
                for (int j = 7; j < 12; ++j) {
                    if (abstractBoard[i][j] == 0) {
                        abstractBoard[i][j] = -1;
                    }
                }
            }
        }
    }

    private void addCapture(byte color, int n) {
        // color is the OWNER of the stones removed; whiteCaptures / blackCaptures
        // count stones lost by white / black. Poofs credit the mover's own color.
        if (color == 1) {
            whiteCaptures += n;
        } else {
            blackCaptures += n;
        }
    }

    private static boolean isEmptyCell(int v) {
        return v == 0 || v == -1; // a playable-empty flank (tournament mark counts)
    }

    private static boolean onBoard(int x, int y) {
        return x >= 0 && x < 19 && y >= 0 && y < 19;
    }

    private void detectPenteCapture(byte[][] abstractBoard, int i, int j, byte myColor) {
        byte opponentColor = (byte) (1 + (myColor % 2));
        if ((i - 3) > -1) {
            if (abstractBoard[i - 3][j] == myColor) {
                if ((abstractBoard[i - 1][j] == opponentColor) && (abstractBoard[i - 2][j] == opponentColor)) {
                    abstractBoard[i - 1][j] = 0;
                    abstractBoard[i - 2][j] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if (((i - 3) > -1) && ((j - 3) > -1)) {
            if (abstractBoard[i - 3][j - 3] == myColor) {
                if ((abstractBoard[i - 1][j - 1] == opponentColor) && (abstractBoard[i - 2][j - 2] == opponentColor)) {
                    abstractBoard[i - 1][j - 1] = 0;
                    abstractBoard[i - 2][j - 2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if ((j - 3) > -1) {
            if (abstractBoard[i][j - 3] == myColor) {
                if ((abstractBoard[i][j - 1] == opponentColor) && (abstractBoard[i][j - 2] == opponentColor)) {
                    abstractBoard[i][j - 1] = 0;
                    abstractBoard[i][j - 2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if (((i + 3) < 19) && ((j - 3) > -1)) {
            if (abstractBoard[i + 3][j - 3] == myColor) {
                if ((abstractBoard[i + 1][j - 1] == opponentColor) && (abstractBoard[i + 2][j - 2] == opponentColor)) {
                    abstractBoard[i + 1][j - 1] = 0;
                    abstractBoard[i + 2][j - 2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if ((i + 3) < 19) {
            if (abstractBoard[i + 3][j] == myColor) {
                if ((abstractBoard[i + 1][j] == opponentColor) && (abstractBoard[i + 2][j] == opponentColor)) {
                    abstractBoard[i + 1][j] = 0;
                    abstractBoard[i + 2][j] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if (((i + 3) < 19) && ((j + 3) < 19)) {
            if (abstractBoard[i + 3][j + 3] == myColor) {
                if ((abstractBoard[i + 1][j + 1] == opponentColor) && (abstractBoard[i + 2][j + 2] == opponentColor)) {
                    abstractBoard[i + 1][j + 1] = 0;
                    abstractBoard[i + 2][j + 2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if ((j + 3) < 19) {
            if (abstractBoard[i][j + 3] == myColor) {
                if ((abstractBoard[i][j + 1] == opponentColor) && (abstractBoard[i][j + 2] == opponentColor)) {
                    abstractBoard[i][j + 1] = 0;
                    abstractBoard[i][j + 2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if (((i - 3) > -1) && ((j + 3) < 19)) {
            if (abstractBoard[i - 3][j + 3] == myColor) {
                if ((abstractBoard[i - 1][j + 1] == opponentColor) && (abstractBoard[i - 2][j + 2] == opponentColor)) {
                    abstractBoard[i - 1][j + 1] = 0;
                    abstractBoard[i - 2][j + 2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
    }

    private boolean detectPente(byte[][] abstractBoard, byte color, int rowCol) {
        boolean pente = false;
        int penteCounter = 1;
        int row = rowCol % 19, col = rowCol / 19, i, j;
        i = row - 1;
        j = col;
        while (i > 0 && i < 19 && j > 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            i -= 1;
        }
        i = row + 1;
        j = col;
        while (i > 0 && i < 19 && j > 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            i += 1;
        }
        if (pente) {
            return pente;
        }
        penteCounter = 1;
        i = row;
        j = col - 1;
        while (i > 0 && i < 19 && j > 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            j -= 1;
        }
        i = row;
        j = col + 1;
        while (i > 0 && i < 19 && j > 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            j += 1;
        }
        if (pente) {
            return pente;
        }
        penteCounter = 1;
        i = row - 1;
        j = col - 1;
        while (i > 0 && i < 19 && j > 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            j -= 1;
            i -= 1;
        }
        i = row + 1;
        j = col + 1;
        while (i > 0 && i < 19 && j > 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            i += 1;
            j += 1;
        }
        if (pente) {
            return pente;
        }
        penteCounter = 1;
        i = row - 1;
        j = col + 1;
        while (i > 0 && i < 19 && j > 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            j += 1;
            i -= 1;
        }
        i = row + 1;
        j = col - 1;
        while (i > 0 && i < 19 && j > 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            i += 1;
            j -= 1;
        }

        return pente;
    }

    private void detectKeryoPenteCapture(byte[][] abstractBoard, int i, int j, byte myColor) {
        byte opponentColor = (byte) (1 + (myColor % 2));
        if ((i - 4) > -1) {
            if (abstractBoard[i - 4][j] == myColor) {
                if ((abstractBoard[i - 1][j] == opponentColor) && (abstractBoard[i - 2][j] == opponentColor) && (abstractBoard[i - 3][j] == opponentColor)) {
                    abstractBoard[i - 1][j] = 0;
                    abstractBoard[i - 2][j] = 0;
                    abstractBoard[i - 3][j] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if (((i - 4) > -1) && ((j - 4) > -1)) {
            if (abstractBoard[i - 4][j - 4] == myColor) {
                if ((abstractBoard[i - 1][j - 1] == opponentColor) && (abstractBoard[i - 2][j - 2] == opponentColor) && (abstractBoard[i - 3][j - 3] == opponentColor)) {
                    abstractBoard[i - 1][j - 1] = 0;
                    abstractBoard[i - 2][j - 2] = 0;
                    abstractBoard[i - 3][j - 3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if ((j - 4) > -1) {
            if (abstractBoard[i][j - 4] == myColor) {
                if ((abstractBoard[i][j - 1] == opponentColor) && (abstractBoard[i][j - 2] == opponentColor) && (abstractBoard[i][j - 3] == opponentColor)) {
                    abstractBoard[i][j - 1] = 0;
                    abstractBoard[i][j - 2] = 0;
                    abstractBoard[i][j - 3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if (((i + 4) < 19) && ((j - 4) > -1)) {
            if (abstractBoard[i + 4][j - 4] == myColor) {
                if ((abstractBoard[i + 1][j - 1] == opponentColor) && (abstractBoard[i + 2][j - 2] == opponentColor) && (abstractBoard[i + 3][j - 3] == opponentColor)) {
                    abstractBoard[i + 1][j - 1] = 0;
                    abstractBoard[i + 2][j - 2] = 0;
                    abstractBoard[i + 3][j - 3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if ((i + 4) < 19) {
            if (abstractBoard[i + 4][j] == myColor) {
                if ((abstractBoard[i + 1][j] == opponentColor) && (abstractBoard[i + 2][j] == opponentColor) && (abstractBoard[i + 3][j] == opponentColor)) {
                    abstractBoard[i + 1][j] = 0;
                    abstractBoard[i + 2][j] = 0;
                    abstractBoard[i + 3][j] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if (((i + 4) < 19) && ((j + 4) < 19)) {
            if (abstractBoard[i + 4][j + 4] == myColor) {
                if ((abstractBoard[i + 1][j + 1] == opponentColor) && (abstractBoard[i + 2][j + 2] == opponentColor) && (abstractBoard[i + 3][j + 3] == opponentColor)) {
                    abstractBoard[i + 1][j + 1] = 0;
                    abstractBoard[i + 2][j + 2] = 0;
                    abstractBoard[i + 3][j + 3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if ((j + 4) < 19) {
            if (abstractBoard[i][j + 4] == myColor) {
                if ((abstractBoard[i][j + 1] == opponentColor) && (abstractBoard[i][j + 2] == opponentColor) && (abstractBoard[i][j + 3] == opponentColor)) {
                    abstractBoard[i][j + 1] = 0;
                    abstractBoard[i][j + 2] = 0;
                    abstractBoard[i][j + 3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if (((i - 4) > -1) && ((j + 4) < 19)) {
            if (abstractBoard[i - 4][j + 4] == myColor) {
                if ((abstractBoard[i - 1][j + 1] == opponentColor) && (abstractBoard[i - 2][j + 2] == opponentColor) && (abstractBoard[i - 3][j + 3] == opponentColor)) {
                    abstractBoard[i - 1][j + 1] = 0;
                    abstractBoard[i - 2][j + 2] = 0;
                    abstractBoard[i - 3][j + 3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
    }

    // Pair poof: the just-placed stone forms ENEMY - own - [placed] - ENEMY (or
    // the mirror), vanishing the placed stone and its neighbour; both are the
    // mover's own stones so the mover loses them. Verbatim port of #detectPoof.
    private void detectPoof(byte[][] abstractBoard, int i, int j, byte myColor) {
        byte opponentColor = (byte) (1 + (myColor % 2));
        boolean poofed = false;
        if (((i - 2) > -1) && ((i + 1) < 19)) {
            if (abstractBoard[i - 1][j] == myColor) {
                if ((abstractBoard[i - 2][j] == opponentColor) && (abstractBoard[i + 1][j] == opponentColor)) {
                    abstractBoard[i - 1][j] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 1);
                    poofed = true;
                }
            }
        }
        if (((i - 2) > -1) && ((j - 2) > -1) && ((i + 1) < 19) && ((j + 1) < 19)) {
            if (abstractBoard[i - 1][j - 1] == myColor) {
                if ((abstractBoard[i - 2][j - 2] == opponentColor) && (abstractBoard[i + 1][j + 1] == opponentColor)) {
                    abstractBoard[i - 1][j - 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 1);
                    poofed = true;
                }
            }
        }
        if (((j - 2) > -1) && ((j + 1) < 19)) {
            if (abstractBoard[i][j - 1] == myColor) {
                if ((abstractBoard[i][j - 2] == opponentColor) && (abstractBoard[i][j + 1] == opponentColor)) {
                    abstractBoard[i][j - 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 1);
                    poofed = true;
                }
            }
        }
        if (((i - 1) > -1) && ((j - 2) > -1) && ((i + 2) < 19) && ((j + 1) < 19)) {
            if (abstractBoard[i + 1][j - 1] == myColor) {
                if ((abstractBoard[i - 1][j + 1] == opponentColor) && (abstractBoard[i + 2][j - 2] == opponentColor)) {
                    abstractBoard[i + 1][j - 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 1);
                    poofed = true;
                }
            }
        }
        if (((i + 2) < 19) && ((i - 1) > -1)) {
            if (abstractBoard[i + 1][j] == myColor) {
                if ((abstractBoard[i + 2][j] == opponentColor) && (abstractBoard[i - 1][j] == opponentColor)) {
                    abstractBoard[i + 1][j] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 1);
                    poofed = true;
                }
            }
        }
        if (((i - 1) > -1) && ((j - 1) > -1) && ((i + 2) < 19) && ((j + 2) < 19)) {
            if (abstractBoard[i + 1][j + 1] == myColor) {
                if ((abstractBoard[i - 1][j - 1] == opponentColor) && (abstractBoard[i + 2][j + 2] == opponentColor)) {
                    abstractBoard[i + 1][j + 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 1);
                    poofed = true;
                }
            }
        }
        if (((j + 2) < 19) && ((j - 1) > -1)) {
            if (abstractBoard[i][j + 1] == myColor) {
                if ((abstractBoard[i][j - 1] == opponentColor) && (abstractBoard[i][j + 2] == opponentColor)) {
                    abstractBoard[i][j + 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 1);
                    poofed = true;
                }
            }
        }
        if (((i - 2) > -1) && ((j - 1) > -1) && ((i + 1) < 19) && ((j + 2) < 19)) {
            if (abstractBoard[i - 1][j + 1] == myColor) {
                if ((abstractBoard[i + 1][j - 1] == opponentColor) && (abstractBoard[i - 2][j + 2] == opponentColor)) {
                    abstractBoard[i - 1][j + 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 1);
                    poofed = true;
                }
            }
        }
        if (poofed) {
            addCapture(myColor, 1);
        }
    }

    // Triple poof: end form (own +1,+2, enemy +3 and -1) over 8 directions, plus
    // centre form (own +/-1, enemy +/-2) over 4 axes. Verbatim port of
    // #detectKeryoPoof (note the "down" form guards j+3, not j+2).
    private void detectKeryoPoof(byte[][] abstractBoard, int i, int j, byte myColor) {
        byte opponentColor = (byte) (1 + (myColor % 2));
        boolean poofed = false;
        if (((i - 3) > -1) && ((i + 1) < 19)) { // left
            if (abstractBoard[i - 1][j] == myColor && abstractBoard[i - 2][j] == myColor) {
                if ((abstractBoard[i - 3][j] == opponentColor) && (abstractBoard[i + 1][j] == opponentColor)) {
                    abstractBoard[i - 2][j] = 0;
                    abstractBoard[i - 1][j] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (((i - 3) > -1) && ((j - 3) > -1) && ((i + 1) < 19) && ((j + 1) < 19)) { // up left
            if (abstractBoard[i - 1][j - 1] == myColor && abstractBoard[i - 2][j - 2] == myColor) {
                if ((abstractBoard[i - 3][j - 3] == opponentColor) && (abstractBoard[i + 1][j + 1] == opponentColor)) {
                    abstractBoard[i - 2][j - 2] = 0;
                    abstractBoard[i - 1][j - 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (((j - 3) > -1) && ((j + 1) < 19)) { // up
            if (abstractBoard[i][j - 1] == myColor && abstractBoard[i][j - 2] == myColor) {
                if ((abstractBoard[i][j - 3] == opponentColor) && (abstractBoard[i][j + 1] == opponentColor)) {
                    abstractBoard[i][j - 2] = 0;
                    abstractBoard[i][j - 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (((i - 1) > -1) && ((j - 3) > -1) && ((i + 3) < 19) && ((j + 1) < 19)) { // up right
            if (abstractBoard[i + 1][j - 1] == myColor && abstractBoard[i + 2][j - 2] == myColor) {
                if ((abstractBoard[i - 1][j + 1] == opponentColor) && (abstractBoard[i + 3][j - 3] == opponentColor)) {
                    abstractBoard[i + 2][j - 2] = 0;
                    abstractBoard[i + 1][j - 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (((i + 3) < 19) && ((i - 1) > -1)) { // right
            if (abstractBoard[i + 1][j] == myColor && abstractBoard[i + 2][j] == myColor) {
                if ((abstractBoard[i + 3][j] == opponentColor) && (abstractBoard[i - 1][j] == opponentColor)) {
                    abstractBoard[i + 2][j] = 0;
                    abstractBoard[i + 1][j] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (((i - 1) > -1) && ((j - 1) > -1) && ((i + 3) < 19) && ((j + 3) < 19)) { // down right
            if (abstractBoard[i + 1][j + 1] == myColor && abstractBoard[i + 2][j + 2] == myColor) {
                if ((abstractBoard[i - 1][j - 1] == opponentColor) && (abstractBoard[i + 3][j + 3] == opponentColor)) {
                    abstractBoard[i + 2][j + 2] = 0;
                    abstractBoard[i + 1][j + 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (((j + 3) < 19) && ((j - 1) > -1)) { // down
            if (abstractBoard[i][j + 1] == myColor && abstractBoard[i][j + 2] == myColor) {
                if ((abstractBoard[i][j - 1] == opponentColor) && (abstractBoard[i][j + 3] == opponentColor)) {
                    abstractBoard[i][j + 1] = 0;
                    abstractBoard[i][j + 2] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (((i - 3) > -1) && ((j - 1) > -1) && ((i + 1) < 19) && ((j + 3) < 19)) { // down left
            if (abstractBoard[i - 1][j + 1] == myColor && abstractBoard[i - 2][j + 2] == myColor) {
                if ((abstractBoard[i + 1][j - 1] == opponentColor) && (abstractBoard[i - 3][j + 3] == opponentColor)) {
                    abstractBoard[i - 2][j + 2] = 0;
                    abstractBoard[i - 1][j + 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        // 4 directions with the centre of 3 placed stones poofing
        if (((i - 2) > -1) && ((i + 2) < 19)) { // horizontal
            if (abstractBoard[i - 1][j] == myColor && abstractBoard[i + 1][j] == myColor) {
                if ((abstractBoard[i - 2][j] == opponentColor) && (abstractBoard[i + 2][j] == opponentColor)) {
                    abstractBoard[i + 1][j] = 0;
                    abstractBoard[i - 1][j] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (((i - 2) > -1) && ((j - 2) > -1) && ((i + 2) < 19) && ((j + 2) < 19)) { // up left
            if (abstractBoard[i - 1][j - 1] == myColor && abstractBoard[i + 1][j + 1] == myColor) {
                if ((abstractBoard[i - 2][j - 2] == opponentColor) && (abstractBoard[i + 2][j + 2] == opponentColor)) {
                    abstractBoard[i + 1][j + 1] = 0;
                    abstractBoard[i - 1][j - 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (((j - 2) > -1) && ((j + 2) < 19)) { // vertical
            if (abstractBoard[i][j - 1] == myColor && abstractBoard[i][j + 1] == myColor) {
                if ((abstractBoard[i][j - 2] == opponentColor) && (abstractBoard[i][j + 2] == opponentColor)) {
                    abstractBoard[i][j + 1] = 0;
                    abstractBoard[i][j - 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (((i - 2) > -1) && ((j - 2) > -1) && ((i + 2) < 19) && ((j + 2) < 19)) { // up right
            if (abstractBoard[i + 1][j - 1] == myColor && abstractBoard[i - 1][j + 1] == myColor) {
                if ((abstractBoard[i - 2][j + 2] == opponentColor) && (abstractBoard[i + 2][j - 2] == opponentColor)) {
                    abstractBoard[i + 1][j - 1] = 0;
                    abstractBoard[i - 1][j + 1] = 0;
                    abstractBoard[i][j] = 0;
                    addCapture(myColor, 2);
                    poofed = true;
                }
            }
        }
        if (poofed) {
            addCapture(myColor, 1);
        }
    }

    // Five-in-a-row through rowCol for `color`, four axes, >= 0 bounds (so fives
    // touching row 0 / col 0 count). Verbatim port of #detectPenteOf.
    private boolean detectPenteOf(byte[][] abstractBoard, byte color, int rowCol) {
        boolean pente = false;
        int penteCounter = 1;
        int col = rowCol / 19, row = rowCol % 19, i, j;
        i = row - 1;
        j = col;
        while (i >= 0 && i < 19 && j >= 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            i -= 1;
        }
        i = row + 1;
        j = col;
        while (i >= 0 && i < 19 && j >= 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            i += 1;
        }
        if (pente) {
            return true;
        }
        penteCounter = 1;
        i = row;
        j = col - 1;
        while (i >= 0 && i < 19 && j >= 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            j -= 1;
        }
        i = row;
        j = col + 1;
        while (i >= 0 && i < 19 && j >= 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            j += 1;
        }
        if (pente) {
            return true;
        }
        penteCounter = 1;
        i = row - 1;
        j = col - 1;
        while (i >= 0 && i < 19 && j >= 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            j -= 1;
            i -= 1;
        }
        i = row + 1;
        j = col + 1;
        while (i >= 0 && i < 19 && j >= 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            i += 1;
            j += 1;
        }
        if (pente) {
            return true;
        }
        penteCounter = 1;
        i = row - 1;
        j = col + 1;
        while (i >= 0 && i < 19 && j >= 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            j += 1;
            i -= 1;
        }
        i = row + 1;
        j = col - 1;
        while (i >= 0 && i < 19 && j >= 0 && j < 19 && !pente) {
            if (color == abstractBoard[i][j]) {
                penteCounter += 1;
                pente = (penteCounter > 4);
            } else {
                break;
            }
            i += 1;
            j -= 1;
        }
        return pente;
    }

    // Boat/O-Pente five-in-a-row survival. A completed five wins only if no stone
    // in it is part of a capturable group (pair, plus triple when withTriples). A
    // five owned by the player to move (it survived the opponent's reply) wins
    // immediately. Verbatim port of #boatRowWinner; returns 0/1/2.
    private int boatRowWinner(byte[][] B, List<Integer> moves, boolean withTriples) {
        final int[][] surr = {{-1, 0}, {1, 0}, {-1, -1}, {1, 1}, {0, -1}, {0, 1}, {1, -1}, {-1, 1}};
        int numMoves = moves.size();
        int current = 1 + (numMoves % 2); // player to move
        int winner = 0;
        for (int m = 0; m < numMoves; m++) {
            int p = 1 + (m % 2);
            int op = 3 - p;
            int mx = moves.get(m) % 19, my = moves.get(m) / 19;
            if (!onBoard(mx, my) || B[mx][my] != p) {
                continue; // move was poofed / captured away: no five through it
            }
            for (int axis = 0; axis < 4; axis++) {
                java.util.ArrayList<int[]> run = new java.util.ArrayList<>();
                run.add(new int[]{mx, my});
                int[] both = {2 * axis, 2 * axis + 1};
                for (int di : both) {
                    int dx = surr[di][0], dy = surr[di][1];
                    int cx = mx + dx, cy = my + dy;
                    while (onBoard(cx, cy) && B[cx][cy] == p) {
                        run.add(new int[]{cx, cy});
                        cx += dx;
                        cy += dy;
                    }
                }
                if (run.size() < 5) {
                    continue;
                }
                if (p == current) {
                    return p; // survived the opponent's reply -> immediate win
                }
                boolean breakable = false;
                for (int s = 0; s < run.size() && !breakable; s++) {
                    int sx = run.get(s)[0], sy = run.get(s)[1];
                    // capturable pair {stone, stone+dir} flanked by op and playable-empty
                    for (int k = 0; k < 8; k++) {
                        int dx = surr[k][0], dy = surr[k][1];
                        int ax = sx + dx, ay = sy + dy;
                        int bx = sx + 2 * dx, by = sy + 2 * dy;
                        int ex = sx - dx, ey = sy - dy;
                        if (!onBoard(ax, ay) || !onBoard(bx, by) || !onBoard(ex, ey)) {
                            continue;
                        }
                        int p1 = B[ax][ay], p2 = B[bx][by], p3 = B[ex][ey];
                        if ((p1 == p && p2 == op && isEmptyCell(p3)) ||
                                (p1 == p && isEmptyCell(p2) && p3 == op)) {
                            breakable = true;
                            break;
                        }
                    }
                    if (breakable || !withTriples) {
                        continue;
                    }
                    // capturable triple with the run stone at an end
                    for (int k = 0; k < 8; k++) {
                        int dx = surr[k][0], dy = surr[k][1];
                        int p1 = onBoard(sx + dx, sy + dy) ? B[sx + dx][sy + dy] : -2;
                        int p2 = onBoard(sx + 2 * dx, sy + 2 * dy) ? B[sx + 2 * dx][sy + 2 * dy] : -2;
                        int p3 = onBoard(sx + 3 * dx, sy + 3 * dy) ? B[sx + 3 * dx][sy + 3 * dy] : -2;
                        int p4 = onBoard(sx - dx, sy - dy) ? B[sx - dx][sy - dy] : -2;
                        if (p1 == -2 || p2 == -2 || p3 == -2 || p4 == -2) {
                            continue;
                        }
                        if ((p1 == p && p2 == p && p4 == op && isEmptyCell(p3)) ||
                                (p1 == p && p2 == p && isEmptyCell(p4) && p3 == op)) {
                            breakable = true;
                            break;
                        }
                    }
                    if (breakable) {
                        continue;
                    }
                    // capturable triple with the run stone in the middle (4 axes only)
                    for (int k = 0; k < 8; k += 2) {
                        int dx = surr[k][0], dy = surr[k][1];
                        int p1 = onBoard(sx + dx, sy + dy) ? B[sx + dx][sy + dy] : -2;
                        int p2 = onBoard(sx - dx, sy - dy) ? B[sx - dx][sy - dy] : -2;
                        int p3 = onBoard(sx - 2 * dx, sy - 2 * dy) ? B[sx - 2 * dx][sy - 2 * dy] : -2;
                        int p4 = onBoard(sx + 2 * dx, sy + 2 * dy) ? B[sx + 2 * dx][sy + 2 * dy] : -2;
                        if (p1 == -2 || p2 == -2 || p3 == -2 || p4 == -2) {
                            continue;
                        }
                        if ((p1 == p && p2 == p && p4 == op && isEmptyCell(p3)) ||
                                (p1 == p && p2 == p && isEmptyCell(p4) && p3 == op)) {
                            breakable = true;
                            break;
                        }
                    }
                }
                if (!breakable) {
                    winner = p; // a surviving five; a current-player five would outrank
                }
            }
        }
        return winner;
    }

    // Per-move color for the replay loop. Connect6 (game 13/14) plays TWO stones
    // per turn: owner of move index i is 1 (white) iff i % 4 in {0, 3}, else 2
    // (matches GameClass.currentPlayer). Every other game keeps strict alternation
    // 1 + (i % 2), so this is byte-identical for games 1/3/11/15/25.
    private static byte colorForMove(int game, int i) {
        if (game == 13 || game == 14) {
            int m = i % 4;
            return (byte) ((m == 0 || m == 3) ? 1 : 2);
        }
        return (byte) (1 + (i % 2));
    }

    // Connect6 win: 6 OR MORE contiguous stones of `color` through rowCol, on any
    // of the four axes, with >= 0 / < 19 guards (mirrors detectPenteOf's fixed
    // bounds; NO >0 row/col-0 bug). The FULL run length is counted (never stopping
    // at the threshold) so overlines (7+) win too AND so an "exactly 6" mutation is
    // detectable by the fixture net. Winner is the placing color.
    private boolean detectConnect6Of(byte[][] abstractBoard, byte color, int rowCol) {
        int row = rowCol % 19, col = rowCol / 19;
        final int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            int dx = d[0], dy = d[1];
            int count = 1;
            int i = row + dx, j = col + dy;
            while (i >= 0 && i < 19 && j >= 0 && j < 19 && abstractBoard[i][j] == color) {
                count++;
                i += dx;
                j += dy;
            }
            i = row - dx;
            j = col - dy;
            while (i >= 0 && i < 19 && j >= 0 && j < 19 && abstractBoard[i][j] == color) {
                count++;
                i -= dx;
                j -= dy;
            }
            if (count >= 6) {
                return true;
            }
        }
        return false;
    }
}
