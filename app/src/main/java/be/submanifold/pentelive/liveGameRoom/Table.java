package be.submanifold.pentelive.liveGameRoom;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.R;

/**
 * Created by waliedothman on 08/01/2017.
 */

public class Table {
    private int blackColor = Color.BLACK, whiteColor = Color.WHITE, penteColor = Color.parseColor("#FDDEA3"),
            keryoPenteColor = Color.parseColor("#BAFDA3"), gomokuColor = Color.parseColor("#A3FDEB"),
            dPenteColor = Color.parseColor("#A3CDFD"), gPenteColor = Color.parseColor("#AEA3FD"),
            poofPenteColor = Color.parseColor("#EDA3FD"), connect6Color = Color.parseColor("#EDA3FD"),
            boatPenteColor = Color.parseColor("#25BAFF"), dkeryoColor = Color.parseColor("#FFA500");

    private int id = 0;
    private Map<String, LivePlayer> players = new HashMap<>();
    private boolean timed = false;
    private int game = 1;
    private boolean open = true;
    private boolean rated = false;
    private String owner = "";
    private int whiteCaptures = 0;
    private int blackCaptures = 0;
    private static Map<Integer, String> gameNames;
    static {
        gameNames = new HashMap<>();
        gameNames.put(1, "Pente"); gameNames.put(3, "Keryo-Pente"); gameNames.put(5, "Gomoku");
        gameNames.put(7, "D-Pente"); gameNames.put(9, "G-Pente"); gameNames.put(11, "Poof-Pente");
        gameNames.put(13, "Connect6"); gameNames.put(15, "Boat-Pente"); gameNames.put(17, "DK-Pente");
        gameNames.put(2, "Speed Pente"); gameNames.put(4, "Speed Keryo-Pente");
        gameNames.put(6, "Speed Gomoku"); gameNames.put(8, "Speed D-Pente");
        gameNames.put(10, "Speed G-Pente"); gameNames.put(12, "Speed Poof-Pente");
        gameNames.put(14, "Speed Connect6"); gameNames.put(16, "Speed Boat-Pente");
        gameNames.put(18, "Speed DK-Pente");
    }
    private List<Integer> moves = new ArrayList<>();
    private Map<String, Integer> timer;
    private Map<Integer, LivePlayer> seats = new HashMap<>();
    private GameState gameState = new GameState();
    private Context ctx = MyApplication.getContext();

    public byte[][] abstractBoard = {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};

    public Table() {
        timer = new HashMap<>();
        timer.put("initialMinutes", 0); timer.put("incrementalSeconds", 0);
    }

    public void undoMove() {
        if (moves.size() > 1) {
            List<Integer> oldMoves = moves;
            oldMoves.remove(oldMoves.size() - 1);
            resetBoard();
            addMoves(oldMoves);
        }
    }
    public void addMoves(List<Integer> moveList) {
        for (int move: moveList) {
            addMove(move);
        }
    }
    public void addMove(int move) {
        byte color = (byte) currentColor();
        moves.add(move);
        int move_i = move / 19;
        int move_j = move % 19;
        abstractBoard[move_i][move_j] = color;
        if (game != 5 && game != 6 && game != 13 && game != 14) {
            if (game == 11 || game == 12) {
                detectPoof(move, color);
            }
            detectPenteCapture(move, color);
            if (game == 3 || game == 4 || game == 17 || game == 18) {
                detectKeryoPenteCapture(move, color);
            }
        }
        if (game != 5 && game != 6 && game != 13 && game != 14 && game != 7 && game != 8 && game != 17 && game != 18 && (rated || game == 9 || game == 10)) {
            if (moves.size() == 2) {
                for (int i = 7; i < 12; i++) {
                    for (int j = 7; j < 12; j++) {
                        if (abstractBoard[i][j] == 0) {
                            abstractBoard[i][j] = -1;
                        }
                    }
                }
                if (game == 9 || game == 10) {
                    for(int i = 1; i < 3; ++i) {
                        if (abstractBoard[9][11 + i] == 0) {
                            abstractBoard[9][11 + i] = -1;
                        }
                        if (abstractBoard[9][7 - i] == 0) {
                            abstractBoard[9][7 - i] = -1;
                        }
                        if (abstractBoard[11 + i][9] == 0) {
                            abstractBoard[11 + i][9] = -1;
                        }
                        if (abstractBoard[7 - i][9] == 0) {
                            abstractBoard[7 - i][9] = -1;
                        }
                    }
                }
            } else if (moves.size() == 3) {
                for (int i = 7; i < 12; i++) {
                    for (int j = 7; j < 12; j++) {
                        if (abstractBoard[i][j] == -1) {
                            abstractBoard[i][j] = 0;
                        }
                    }
                }
                if (game == 9 || game == 10) {
                    for(int i = 1; i < 3; ++i) {
                        if (abstractBoard[9][11 + i] == -1) {
                            abstractBoard[9][11 + i] = 0;
                        }
                        if (abstractBoard[9][7 - i] == -1) {
                            abstractBoard[9][7 - i] = 0;
                        }
                        if (abstractBoard[11 + i][9] == -1) {
                            abstractBoard[11 + i][9] = 0;
                        }
                        if (abstractBoard[7 - i][9] == -1) {
                            abstractBoard[7 - i][9] = 0;
                        }
                    }
                }
            }
        }
    }
    public boolean isDPente() {
        return (game == 7 || game == 8 || game == 17 || game == 18);
    }
    public int currentColor() {
        if (game != 13 && game != 14) {
            return 1 + (moves.size() % 2);
        } else {
            if (moves.size() == 0) {
                return 1;
            }
            return 2 - (((moves.size() - 1) / 2) % 2);
        }
    }
    public int currentPlayer() {
        if (game != 13 && game != 14) {
            if (isDPente()) {
                if (moves.size()<4) {
                    return 1;
                }
                if (moves.size() == 4 && gameState.dPenteState == DPenteState.NOCHOICE) {
                    return 2;
                }
            }
            return 1 + (moves.size() % 2);
        } else {
            if (moves.size() == 0) {
                return 1;
            }
            return 2 - (((moves.size() - 1) / 2) % 2);
        }
    }
    public String currentPlayerName() {
        int seat = currentPlayer();
        LivePlayer player = seats.get(seat);
        if (player != null) {
            return player.getName();
        }
        return "";
    }
    public void swapSeats(boolean swap, boolean silent) {
        if (swap){
            if (!silent) {
                LivePlayer player1 = seats.get(1);
                LivePlayer player2 = seats.get(2);
                seats.put(1, player2);
                seats.put(2, player1);
                Map<String, Integer> timer1 = gameState.timers.get(1), timer2 = gameState.timers.get(2);
                gameState.timers.put(1, timer2);
                gameState.timers.put(2, timer1);
            }
            gameState.dPenteState = DPenteState.SWAPPED;
        } else {
            gameState.dPenteState = DPenteState.NOTSWAPPED;
        }
    }
    public synchronized void updateTimer(boolean reset, int currentPlayer, int minutes, int seconds) {
        if (reset) {
            int timerMinutes = timer.get("initialMinutes");
            gameState.timers.get(1).put("minutes", timerMinutes);
            gameState.timers.get(2).put("minutes", timerMinutes);
            gameState.timers.get(1).put("seconds", 0);
            gameState.timers.get(2).put("seconds", 0);
        } else if (minutes > -1) {
            Map<String, Integer> timer = gameState.timers.get(currentPlayer);
            timer.put("seconds", seconds);
            timer.put("minutes", minutes);
        } else {
            Map<String, Integer> timer = gameState.timers.get(currentPlayer);
            int timerMinutes = timer.get("minutes");
            int timerSeconds = timer.get("seconds");
            if (timerSeconds > 0) {
                timerSeconds = timerSeconds - 1;
                timer.put("seconds", timerSeconds);
            } else if (timerMinutes > 0) {
                timerSeconds = 59;
                timerMinutes = timerMinutes - 1;
                timer.put("seconds", timerSeconds);
                timer.put("minutes", timerMinutes);
            }
        }
    }
    public void resetState() {
        resetAbstractBoard();
        gameState.dPenteState = DPenteState.NOCHOICE;
        moves = new ArrayList<>();
        whiteCaptures = 0;
        blackCaptures = 0;
        updateTimer(true,0,0,0);
    }
    public void resetBoard() {
        resetAbstractBoard();
        moves = new ArrayList<>();
        whiteCaptures = 0;
        blackCaptures = 0;
    }

    public boolean gameHasCaptures() {
        return (game != 5 && game != 6 && game != 13 && game != 14);
    }
    public int getGameColor() {
        if (game < 3) {
            return penteColor;
        } else if (game < 5) {
            return keryoPenteColor;
        } else if (game < 7) {
            return gomokuColor;
        } else if (game < 9) {
            return dPenteColor;
        } else if (game < 11) {
            return gPenteColor;
        } else if (game < 13) {
            return poofPenteColor;
        } else if (game < 15) {
            return connect6Color;
        } else if (game < 17) {
            return boatPenteColor;
        } else {
            return dkeryoColor;
        }
    }
    public String getGameName() {
        return gameNames.get(game);
    }
    public String getSettingsText() {
        String str;
        if (timed) {
            str = ctx.getString(R.string.timer) + ": " + timer.get("initialMinutes") + "/" + timer.get("incrementalSeconds");
        } else {
            str = ctx.getString(R.string.timer) + ": " + ctx.getString(R.string.no_timer);
        }
        if (rated) {
            str = str + ", " + ctx.getString(R.string.rated);
        } else {
            str = str + ", " + ctx.getString(R.string.notRated);
        }
        return str;
    }
    public SpannableStringBuilder getSeatsText(int lineHeight) {
        SpannableStringBuilder sb = new SpannableStringBuilder("");
        LivePlayer player = seats.get(1);
        Drawable icon;
        if (player != null) {
            icon = ContextCompat.getDrawable(MyApplication.getContext(), R.drawable.white_nobg);
            icon.setBounds(0, 0, lineHeight * 2 / 3, lineHeight * 2 / 3);
            sb.append("  ").setSpan(new ImageSpan(icon, ImageSpan.ALIGN_BASELINE), sb.length() - 1, sb.length(), 0);
            sb.append("  ").append(player.coloredNameString(lineHeight));
        }
        if (seats.size() > 1) {
            sb.append(" vs ");
        }
        player = seats.get(2);
        if (player != null) {
            icon = ContextCompat.getDrawable(MyApplication.getContext(), R.drawable.black_nobg);
            icon.setBounds(0, 0, lineHeight * 2 / 3, lineHeight * 2 / 3);
            sb.append("  ").setSpan(new ImageSpan(icon, ImageSpan.ALIGN_BASELINE), sb.length() - 1, sb.length(), 0);
            sb.append("  ").append(player.coloredNameString(lineHeight));
        }
        return sb;
    }
    public SpannableStringBuilder getWatchingText(int lineHeight) {
        SpannableStringBuilder sb = new SpannableStringBuilder(ctx.getString(R.string.watching) + " ");
        for (LivePlayer livePlayer: players.values()) {
            if (!isSeated(livePlayer.getName())) {
                sb.append(livePlayer.coloredNameString(lineHeight));
                sb.append(", ");
            }
        }
        return sb;
    }

    public SpannableStringBuilder getCapturesText(int lineHeight) {
        SpannableStringBuilder sb = new SpannableStringBuilder("");
        if (!gameHasCaptures()) {
            return sb;
        }
        Drawable icon;
        icon = ContextCompat.getDrawable(MyApplication.getContext(), R.drawable.white_nobg);
        icon.setBounds(0, 0, lineHeight * 4 / 5, lineHeight * 4 / 5);
        sb.append(" ").setSpan(new ImageSpan(icon, ImageSpan.ALIGN_BASELINE), sb.length() - 1, sb.length(), 0);
        sb.append(" ").append("x " + whiteCaptures + " - ");
        icon = ContextCompat.getDrawable(MyApplication.getContext(), R.drawable.black_nobg);
        icon.setBounds(0, 0, lineHeight * 4 / 5, lineHeight * 4 / 5);
        sb.append(" ").setSpan(new ImageSpan(icon, ImageSpan.ALIGN_BASELINE), sb.length() - 1, sb.length(), 0);
        sb.append(" ").append("x " + blackCaptures);
        return sb;
    }


    public void updateGameState(int state) {
        if (state == 2 && gameState.state != State.PAUSED) {
            resetState();
        }
        switch (state) {
            case 1: gameState.state = State.NOTSTARTED; break;
            case 2: gameState.state = State.STARTED; break;
            case 3: gameState.state = State.PAUSED; break;
            case 4: gameState.state = State.HALFSET; break;
        }
    }

    public boolean isSeated(String player) {
        LivePlayer livePlayer = seats.get(1);
        if (livePlayer != null && livePlayer.getName().equals(player)) {
            return true;
        }
        livePlayer = seats.get(2);
        if (livePlayer != null && livePlayer.getName().equals(player)) {
            return true;
        }
        return false;
    }
    public void sit(int seat, String player) {
        LivePlayer livePlayer = players.get(player);
        seats.put(seat, livePlayer);
    }
    public void stand(String player) {
        LivePlayer livePlayer = seats.get(1);
        if (livePlayer != null && livePlayer.getName().equals(player)) {
            seats.remove(1);
        }
        livePlayer = seats.get(2);
        if (livePlayer != null && livePlayer.getName().equals(player)) {
            seats.remove(2);
        }
    }
    public void exit(String player) {
        players.remove(player);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<String, LivePlayer> getPlayers() {
        return players;
    }

    public void setPlayers(Map<String, LivePlayer> players) {
        this.players = players;
    }

    public boolean isTimed() {
        return timed;
    }

    public void setTimed(boolean timed) {
        this.timed = timed;
    }

    public int getGame() {
        return game;
    }

    public void setGame(int game) {
        this.game = game;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isRated() {
        return rated;
    }

    public void setRated(boolean rated) {
        this.rated = rated;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getWhiteCaptures() {
        return whiteCaptures;
    }

    public void setWhiteCaptures(int whiteCaptures) {
        this.whiteCaptures = whiteCaptures;
    }

    public int getBlackCaptures() {
        return blackCaptures;
    }

    public void setBlackCaptures(int blackCaptures) {
        this.blackCaptures = blackCaptures;
    }

    public static Map<Integer, String> getGameNames() {
        return gameNames;
    }

    public static void setGameNames(Map<Integer, String> gameNames) {
        Table.gameNames = gameNames;
    }

    public List<Integer> getMoves() {
        return moves;
    }

    public void setMoves(List<Integer> moves) {
        this.moves = moves;
    }

    public Map<String, Integer> getTimer() {
        return timer;
    }

    public void setTimer(Map<String, Integer> timer) {
        this.timer = timer;
    }

    public Map<Integer, LivePlayer> getSeats() {
        return seats;
    }

    public void setSeats(Map<Integer, LivePlayer> seats) {
        this.seats = seats;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    private void detectPenteCapture(int move, byte myColor) {
        int i = move / 19;
        int j = move % 19;
        byte opponentColor = (byte) (3 - myColor);
        if ((i-3) > -1) {
            if (abstractBoard[i-3][j] == myColor) {
                if ((abstractBoard[i-1][j] == opponentColor) && (abstractBoard[i-2][j] == opponentColor)) {
                    abstractBoard[i-1][j] = 0;
                    abstractBoard[i-2][j] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if (((i-3) > -1) && ((j-3) > -1)) {
            if (abstractBoard[i-3][j-3] == myColor) {
                if ((abstractBoard[i-1][j-1] == opponentColor) && (abstractBoard[i-2][j-2] == opponentColor)) {
                    abstractBoard[i-1][j-1] = 0;
                    abstractBoard[i-2][j-2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if ((j-3) > -1) {
            if (abstractBoard[i][j-3] == myColor) {
                if ((abstractBoard[i][j-1] == opponentColor) && (abstractBoard[i][j-2] == opponentColor)) {
                    abstractBoard[i][j-1] = 0;
                    abstractBoard[i][j-2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if (((i+3) < 19) && ((j-3) > -1)) {
            if (abstractBoard[i+3][j-3] == myColor) {
                if ((abstractBoard[i+1][j-1] == opponentColor) && (abstractBoard[i+2][j-2] == opponentColor)) {
                    abstractBoard[i+1][j-1] = 0;
                    abstractBoard[i+2][j-2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if ((i+3) < 19) {
            if (abstractBoard[i+3][j] == myColor) {
                if ((abstractBoard[i+1][j] == opponentColor) && (abstractBoard[i+2][j] == opponentColor)) {
                    abstractBoard[i+1][j] = 0;
                    abstractBoard[i+2][j] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if (((i+3) < 19) && ((j+3) < 19)) {
            if (abstractBoard[i+3][j+3] == myColor) {
                if ((abstractBoard[i+1][j+1] == opponentColor) && (abstractBoard[i+2][j+2] == opponentColor)) {
                    abstractBoard[i+1][j+1] = 0;
                    abstractBoard[i+2][j+2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if ((j+3) < 19) {
            if (abstractBoard[i][j+3] == myColor) {
                if ((abstractBoard[i][j+1] == opponentColor) && (abstractBoard[i][j+2] == opponentColor)) {
                    abstractBoard[i][j+1] = 0;
                    abstractBoard[i][j+2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
        if (((i-3) > -1) && ((j+3) < 19)) {
            if (abstractBoard[i-3][j+3] == myColor) {
                if ((abstractBoard[i-1][j+1] == opponentColor) && (abstractBoard[i-2][j+2] == opponentColor)) {
                    abstractBoard[i-1][j+1] = 0;
                    abstractBoard[i-2][j+2] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                }
            }
        }
    }

    private void detectKeryoPenteCapture(int move, byte myColor) {
        int i = move / 19;
        int j = move % 19;
        byte opponentColor = (byte) (3 - myColor);
        if ((i-4) > -1) {
            if (abstractBoard[i-4][j] == myColor) {
                if ((abstractBoard[i-1][j] == opponentColor) && (abstractBoard[i-2][j] == opponentColor) && (abstractBoard[i-3][j] == opponentColor)) {
                    abstractBoard[i-1][j] = 0;
                    abstractBoard[i-2][j] = 0;
                    abstractBoard[i-3][j] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if (((i-4) > -1) && ((j-4) > -1)) {
            if (abstractBoard[i-4][j-4] == myColor) {
                if ((abstractBoard[i-1][j-1] == opponentColor) && (abstractBoard[i-2][j-2] == opponentColor) && (abstractBoard[i-3][j-3] == opponentColor)) {
                    abstractBoard[i-1][j-1] = 0;
                    abstractBoard[i-2][j-2] = 0;
                    abstractBoard[i-3][j-3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if ((j-4) > -1) {
            if (abstractBoard[i][j-4] == myColor) {
                if ((abstractBoard[i][j-1] == opponentColor) && (abstractBoard[i][j-2] == opponentColor) && (abstractBoard[i][j-3] == opponentColor)) {
                    abstractBoard[i][j-1] = 0;
                    abstractBoard[i][j-2] = 0;
                    abstractBoard[i][j-3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if (((i+4) < 19) && ((j-4) > -1)) {
            if (abstractBoard[i+4][j-4] == myColor) {
                if ((abstractBoard[i+1][j-1] == opponentColor) && (abstractBoard[i+2][j-2] == opponentColor) && (abstractBoard[i+3][j-3] == opponentColor)) {
                    abstractBoard[i+1][j-1] = 0;
                    abstractBoard[i+2][j-2] = 0;
                    abstractBoard[i+3][j-3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if ((i+4) < 19) {
            if (abstractBoard[i+4][j] == myColor) {
                if ((abstractBoard[i+1][j] == opponentColor) && (abstractBoard[i+2][j] == opponentColor) && (abstractBoard[i+3][j] == opponentColor)) {
                    abstractBoard[i+1][j] = 0;
                    abstractBoard[i+2][j] = 0;
                    abstractBoard[i+3][j] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if (((i+4) < 19) && ((j+4) < 19)) {
            if (abstractBoard[i+4][j+4] == myColor) {
                if ((abstractBoard[i+1][j+1] == opponentColor) && (abstractBoard[i+2][j+2] == opponentColor) && (abstractBoard[i+3][j+3] == opponentColor)) {
                    abstractBoard[i+1][j+1] = 0;
                    abstractBoard[i+2][j+2] = 0;
                    abstractBoard[i+3][j+3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if ((j+4) < 19) {
            if (abstractBoard[i][j+4] == myColor) {
                if ((abstractBoard[i][j+1] == opponentColor) && (abstractBoard[i][j+2] == opponentColor) && (abstractBoard[i][j+3] == opponentColor)) {
                    abstractBoard[i][j+1] = 0;
                    abstractBoard[i][j+2] = 0;
                    abstractBoard[i][j+3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
        if (((i-4) > -1) && ((j+4) < 19)) {
            if (abstractBoard[i-4][j+4] == myColor) {
                if ((abstractBoard[i-1][j+1] == opponentColor) && (abstractBoard[i-2][j+2] == opponentColor) && (abstractBoard[i-3][j+3] == opponentColor)) {
                    abstractBoard[i-1][j+1] = 0;
                    abstractBoard[i-2][j+2] = 0;
                    abstractBoard[i-3][j+3] = 0;
                    if (opponentColor == 1) {
                        whiteCaptures += 3;
                    } else {
                        blackCaptures += 3;
                    }
                }
            }
        }
    }

    private void detectPoof(int move, byte myColor) {
        int i = move / 19;
        int j = move % 19;
        byte opponentColor = (byte) (3 - myColor);
        boolean poofed = false;
        if (((i-2) > -1) && ((i+1) < 19)) {
            if (abstractBoard[i-1][j] == myColor) {
                if ((abstractBoard[i-2][j] == opponentColor) && (abstractBoard[i+1][j] == opponentColor)) {
                    abstractBoard[i-1][j] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        ++whiteCaptures;
                    } else {
                        ++blackCaptures;
                    }
                    poofed = true;
                }
            }
        }
        if (((i-2) > -1) && ((j-2) > -1) && ((i+1) < 19) && ((j+1) < 19)) {
            if (abstractBoard[i-1][j-1] == myColor) {
                if ((abstractBoard[i-2][j-2] == opponentColor) && (abstractBoard[i+1][j+1] == opponentColor)) {
                    abstractBoard[i-1][j-1] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        ++whiteCaptures;
                    } else {
                        ++blackCaptures;
                    }
                    poofed = true;
                }
            }
        }
        if (((j-2) > -1) && ((j+1) < 19)) {
            if (abstractBoard[i][j-1] == myColor) {
                if ((abstractBoard[i][j-2] == opponentColor) && (abstractBoard[i][j+1] == opponentColor)) {
                    abstractBoard[i][j-1] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        ++whiteCaptures;
                    } else {
                        ++blackCaptures;
                    }
                    poofed = true;
                }
            }
        }
        if (((i-1) > -1) && ((j-2) > -1) && ((i+2) < 19) && ((j+1) < 19)) {
            if (abstractBoard[i+1][j-1] == myColor) {
                if ((abstractBoard[i-1][j+1] == opponentColor) && (abstractBoard[i+2][j-2] == opponentColor)) {
                    abstractBoard[i+1][j-1] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        ++whiteCaptures;
                    } else {
                        ++blackCaptures;
                    }
                    poofed = true;
                }
            }
        }
        if (((i+2) < 19) && ((i-1) > -1)) {
            if (abstractBoard[i+1][j] == myColor) {
                if ((abstractBoard[i+2][j] == opponentColor) && (abstractBoard[i-1][j] == opponentColor)) {
                    abstractBoard[i+1][j] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        ++whiteCaptures;
                    } else {
                        ++blackCaptures;
                    }
                    poofed = true;
                }
            }
        }
        if (((i-1) > -1) && ((j-1) > -1) && ((i+2) < 19) && ((j+2) < 19)) {
            if (abstractBoard[i+1][j+1] == myColor) {
                if ((abstractBoard[i-1][j-1] == opponentColor) && (abstractBoard[i+2][j+2] == opponentColor)) {
                    abstractBoard[i+1][j+1] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        ++whiteCaptures;
                    } else {
                        ++blackCaptures;
                    }
                    poofed = true;
                }
            }
        }
        if (((j+2) < 19) && ((j-1) > -1)) {
            if (abstractBoard[i][j+1] == myColor) {
                if ((abstractBoard[i][j-1] == opponentColor) && (abstractBoard[i][j+2] == opponentColor)) {
                    abstractBoard[i][j+1] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        ++whiteCaptures;
                    } else {
                        ++blackCaptures;
                    }
                    poofed = true;
                }
            }
        }
        if (((i-2) > -1) && ((j-1) > -1) && ((i+1) < 19) && ((j+2) < 19)) {
            if (abstractBoard[i-1][j+1] == myColor) {
                if ((abstractBoard[i+1][j-1] == opponentColor) && (abstractBoard[i-2][j+2] == opponentColor)) {
                    abstractBoard[i-1][j+1] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        ++whiteCaptures;
                    } else {
                        ++blackCaptures;
                    }
                    poofed = true;
                }
            }
        }

        if (poofed) {
            if (myColor == 1) {
                ++whiteCaptures;
            } else {
                ++blackCaptures;
            }
        }
    }

    private void resetAbstractBoard() {
        whiteCaptures = 0;
        blackCaptures = 0;
        for (int i = 0; i < 19; i++) {
            for (int j = 0; j < 19; j++) {
                abstractBoard[i][j] = 0;
            }
        }
    }


}
