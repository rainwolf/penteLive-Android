package be.submanifold.pentelive.liveGameRoom;

/**
 * Created by waliedothman on 11/01/2017.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.submanifold.pente.rules.RenjuLiveState;
import be.submanifold.pente.rules.RenjuSymmetry;

public class LiveBoardView extends View {
    Table table;
    LiveTableFragment fragment;
    private float size;
    private float scaling = 1;
    private float translateX = 0, translateY = 0, stoneX, stoneY;
    public int blackColor = Color.BLACK, whiteColor = Color.WHITE;
    private final Paint blackPaint = makePaint(blackColor);
    private final Paint whitePaint = makePaint(whiteColor);
    private final Paint shadowPaint = makePaint(Color.BLACK);
    private String me;

    private byte myColor = 2, stoneI, stoneJ;
    public int playedMove = -1, redDot = -1;

    private final float zoomedScale = 3;
    float offSetX = 0, offSetY = 0;

    private int gridSize = 19;

    // ----- Renju (Taraguchi-10) opening interaction -----
    public static final int RENJU_IDLE = 0;
    public static final int RENJU_PLACE = 1;       // arm a single box-constrained decline / Branch-A stone
    public static final int RENJU_OFFER = 2;       // collect up to 10 symmetry-distinct picks
    public static final int RENJU_SELECTION = 3;   // accept one tap on a rendered offer
    public static final int RENJU_PENDING = 4;     // inert until the next server echo
    private int renjuMode = RENJU_IDLE;
    private final List<Integer> renjuPicks = new ArrayList<>();
    private int[] renjuOffers = new int[0];
    private static final int RENJU_BG = Color.parseColor("#D98880");

    /** Enter PLACE mode: arm a single box-constrained decline / Branch-A stone. */
    public void beginRenjuPlace() {
        renjuMode = RENJU_PLACE;
        renjuPicks.clear();
        renjuOffers = new int[0];
        invalidate();
    }

    /** Enter OFFER mode: collect up to 10 symmetry-distinct picks (the 10th auto-sends). */
    public void beginRenjuOffer() {
        renjuMode = RENJU_OFFER;
        renjuPicks.clear();
        renjuOffers = new int[0];
        invalidate();
    }

    /** Enter SELECTION mode: render the ten offers and accept one tap. */
    public void beginRenjuSelection(int[] offers) {
        renjuMode = RENJU_SELECTION;
        renjuPicks.clear();
        renjuOffers = (offers != null) ? offers : new int[0];
        invalidate();
    }

    /** Return to IDLE. */
    public void clearRenjuArming() {
        renjuMode = RENJU_IDLE;
        renjuPicks.clear();
        renjuOffers = new int[0];
        invalidate();
    }

    /** Enter PENDING mode: the board is inert until the next server echo. */
    public void markRenjuPending() {
        renjuMode = RENJU_PENDING;
        renjuPicks.clear();
        renjuOffers = new int[0];
        invalidate();
    }

    /** True when mode != IDLE (PLACE/OFFER/SELECTION/PENDING). */
    public boolean isRenjuArmed() {
        return renjuMode != RENJU_IDLE;
    }

    /** Read-only: number of fifth-move offers picked so far while in OFFER mode. */
    public int renjuPickCount() {
        return renjuPicks.size();
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        // Renju is always 15x15; ignore the table's (19-based) default so a later
        // refresh from the fragment cannot override the renju grid back to 19.
        if (table != null && table.isRenju()) {
            this.gridSize = 15;
        } else {
            this.gridSize = gridSize;
        }
    }

    private Map<Integer, List<Integer>> goDeadStonesByPlayer, goTerritoryByPlayer;

    public void setGoDeadStonesByPlayer(Map<Integer, List<Integer>> goDeadStonesByPlayer) {
        this.goDeadStonesByPlayer = goDeadStonesByPlayer;
    }

    public void setGoTerritoryByPlayer(Map<Integer, List<Integer>> goTerritoryByPlayer) {
        this.goTerritoryByPlayer = goTerritoryByPlayer;
    }

    public int getRedDot() {
        return redDot;
    }

    public void setRedDot(int redDot) {
        this.redDot = redDot;
    }

    public LiveBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaling = 1;
        translateX = 0;
        translateY = 0;
    }

    public void setTable(Table table, String me) {
        this.me = me;
        this.table = table;
        if (table != null && table.isRenju()) {
            this.gridSize = 15;
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        size = getWidth();
        canvas.scale(scaling, scaling);
        canvas.translate(translateX, translateY);
        if (table != null && !table.getMoves().isEmpty()) {
            redDot = table.getMoves().get(table.getMoves().size() - 1);
        } else {
            redDot = -1;
        }
        drawBoard(canvas);
        if (playedMove == -1) {
            return;
        }
        if (scaling == zoomedScale) {
            drawZoomedLine(canvas, stoneX, stoneY);
            drawZoomedStone(canvas, stoneX, stoneY, myColor);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!table.currentPlayerName().equals(me) || table.getGameState().state != State.STARTED) {
            return false;
        }
        myColor = (byte) table.currentColor();
        float x, y;
        x = event.getX();
        y = event.getY();
        float realSize = getWidth();
        if (x > realSize || y > realSize || x < 0 || y < 0) {
            playedMove = -1;
            scaling = 1;
            translateX = 0;
            translateY = 0;
            invalidate();
            return false;
        }
        // Renju: while the board is inert (PENDING, or an unanswered SWAP/BRANCH dialog) the
        // board owns nothing — consume the touch without zooming or placing.
        if (table != null && table.isRenju() && isRenjuBoardInert()) {
            playedMove = -1;
            scaling = 1;
            translateX = 0;
            translateY = 0;
            invalidate();
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                offSetX = x;
                offSetY = y;
                redDot = -1;
                scaling = zoomedScale;
                translateX = -(zoomedScale - 1) * offSetX / zoomedScale;
                translateY = -(zoomedScale - 1) * offSetY / zoomedScale;
                break;
            case MotionEvent.ACTION_MOVE:
                scaling = zoomedScale;
                translateX = -(zoomedScale - 1) * offSetX / zoomedScale - (x - offSetX) / zoomedScale;
                translateY = -(zoomedScale - 1) * offSetY / zoomedScale - (y - offSetY) / zoomedScale;
                break;
            case MotionEvent.ACTION_UP:
                scaling = 1;
                translateX = 0;
                translateY = 0;
                break;
        }
        playedMove = -1;
        stoneX = offSetX + 2 * (x - offSetX) / zoomedScale;
        stoneJ = (byte) (gridSize * stoneX / size);
        stoneY = offSetY + 2 * (y - offSetY) / zoomedScale;
        stoneI = (byte) (gridSize * stoneY / size);
        if (table != null && table.isRenju()) {
            handleRenjuActiveTouch(event);
            invalidate();
            return true;
        } else if (table != null) {
            boolean filled = table.abstractBoard[stoneI][stoneJ] != 0;
            if (table.isGo()) {
                if ((filled && table.getGameState().goState == GoState.MARKSTONES) || (!filled && table.getGameState().goState == GoState.PLAY)) {
                    playedMove = gridSize * stoneI + stoneJ;
                }
            } else if (!filled) {
                playedMove = gridSize * stoneI + stoneJ;
            }
        } else {
            playedMove = -1;
        }
        if (playedMove > -1 && event.getAction() == MotionEvent.ACTION_UP) {
            fragment.getListener().sendEvent("{\"dsgMoveTableEvent\":{\"move\":" + playedMove + ",\"moves\":[" + playedMove + "],\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
        }


        invalidate();
        return true;
    }

    /** True when a renju touch must be swallowed (PENDING, or an open SWAP/BRANCH decision dialog). */
    private boolean isRenjuBoardInert() {
        if (renjuMode == RENJU_PENDING) {
            return true;
        }
        if (renjuMode == RENJU_IDLE) {
            int n = table.getMoves().size();
            RenjuLiveState.Phase phase = table.getGameState().renjuState.phase(n);
            return phase == RenjuLiveState.Phase.SWAP || phase == RenjuLiveState.Phase.BRANCH || phase == RenjuLiveState.Phase.SELECTION;
        }
        return false;
    }

    /**
     * Active-mode renju touch FSM (PLACE / OFFER / SELECTION / IDLE-MOVE). stoneI/stoneJ are already
     * resolved. Sets {@code playedMove} for the in-drag preview and, on ACTION_UP, performs the move.
     */
    private void handleRenjuActiveTouch(MotionEvent event) {
        RenjuLiveState rs = table.getGameState().renjuState;
        int n = table.getMoves().size();
        int r = rs.boxRadius(n);
        int move = gridSize * stoneI + stoneJ;                 // 15-based (col = m%15, row = m/15)
        boolean empty = table.abstractBoard[stoneI][stoneJ] == 0;
        boolean inBox = (r == 0) || (Math.abs(stoneI - 7) <= r && Math.abs(stoneJ - 7) <= r);
        boolean up = event.getAction() == MotionEvent.ACTION_UP;
        playedMove = -1;
        switch (renjuMode) {
            case RENJU_PLACE:
                if (empty && inBox) {
                    playedMove = move;
                    if (up) {
                        fragment.sendRenjuSwap(false, move);
                        markRenjuPending();
                    }
                }
                break;
            case RENJU_OFFER:
                if (renjuPicks.contains(move)) {
                    if (up) {
                        renjuPicks.remove((Integer) move);   // re-tap deselects
                    }
                } else if (empty && !RenjuSymmetry.isSymmetricDup(move, renjuPicksArray(), renjuBoardSnapshot())) {
                    playedMove = move;                        // preview the candidate
                    if (up) {
                        if (renjuPicks.size() >= 9) {
                            int[] ten = new int[renjuPicks.size() + 1];
                            for (int k = 0; k < renjuPicks.size(); k++) ten[k] = renjuPicks.get(k);
                            ten[renjuPicks.size()] = move;
                            fragment.sendRenjuOffer10(ten);
                            markRenjuPending();               // the 10th auto-sends
                        } else {
                            renjuPicks.add(move);
                        }
                    }
                }
                break;
            case RENJU_SELECTION:
                boolean isOffer = false;
                for (int o : renjuOffers) {
                    if (o == move) {
                        isOffer = true;
                        break;
                    }
                }
                if (isOffer) {
                    playedMove = move;
                    if (up) {
                        fragment.sendRenjuSelect1(move);
                        markRenjuPending();
                    }
                }
                break;
            case RENJU_IDLE:
            default:
                // phase MOVE or COMPLETE: an ordinary stone (box-gated for safety).
                if (empty && inBox) {
                    playedMove = move;
                    if (up) {
                        fragment.getListener().sendEvent("{\"dsgMoveTableEvent\":{\"move\":" + move + ",\"moves\":[" + move + "],\"player\":\"" + me + "\",\"table\":" + table.getId() + ",\"time\":0}}");
                    }
                }
                break;
        }
    }

    private int[] renjuPicksArray() {
        int[] a = new int[renjuPicks.size()];
        for (int k = 0; k < renjuPicks.size(); k++) {
            a[k] = renjuPicks.get(k);
        }
        return a;
    }

    /** byte[225] snapshot of the renju board (index 15*i+j; 0=empty, 2=black, 1=white). */
    private byte[] renjuBoardSnapshot() {
        byte[] b = new byte[225];
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                byte v = table.abstractBoard[i][j];
                b[15 * i + j] = (v > 0) ? v : 0;
            }
        }
        return b;
    }

    public void clearGoStructures() {
        goTerritoryByPlayer = null;
        goDeadStonesByPlayer = null;
        invalidate();
    }


    private void drawBoard(Canvas canvas) {
        float step = size / gridSize, margin = step / 2;
        Paint linePaint = blackPaint;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        for (int i = 0; i < gridSize; i++) {
            canvas.drawLine(margin + step * i, margin, margin + step * i, size - margin, linePaint);
            canvas.drawLine(margin, margin + step * i, size - margin, margin + step * i, linePaint);
        }
        if (table != null && table.isGo()) {
            linePaint.setStyle(Paint.Style.FILL);
            float radius = margin / 3;
            int l = 3;
            if (gridSize == 9) {
                l = 2;
            }
            canvas.drawCircle(margin + l * step, margin + l * step, radius, linePaint);
            canvas.drawCircle(margin + l * step, size - (margin + l * step), radius, linePaint);
            canvas.drawCircle(size - (margin + l * step), margin + l * step, radius, linePaint);
            canvas.drawCircle(size - (margin + l * step), size - (margin + l * step), radius, linePaint);
            canvas.drawCircle(size / 2, size / 2, radius, linePaint);

            if (l == 3) {
                canvas.drawCircle(margin + 3 * step, size / 2, radius, linePaint);
                canvas.drawCircle(size / 2, margin + 3 * step, radius, linePaint);
                canvas.drawCircle(size / 2, size - (margin + 3 * step), radius, linePaint);
                canvas.drawCircle(size - (margin + 3 * step), size / 2, radius, linePaint);
            }
        } else if (table != null && table.isRenju()) {
            // 9 star points at {3,7,11}^2 (indices {48,52,56,108,112,116,168,172,176}).
            linePaint.setStyle(Paint.Style.FILL);
            float dot = margin / 2;
            int[] pts = {3, 7, 11};
            for (int pi : pts) {
                for (int pj : pts) {
                    canvas.drawCircle(margin + pj * step, margin + pi * step, dot, linePaint);
                }
            }
        } else {
            canvas.drawCircle(margin + 6 * step, margin + 6 * step, margin / 2, linePaint);
            canvas.drawCircle(size - (margin + 6 * step), margin + 6 * step, margin / 2, linePaint);
            canvas.drawCircle(margin + 6 * step, size - (margin + 6 * step), margin / 2, linePaint);
            canvas.drawCircle(size - (margin + 6 * step), size - (margin + 6 * step), margin / 2, linePaint);
            canvas.drawCircle(size / 2, size / 2, margin / 2, linePaint);
        }
        if (table != null) {
            for (byte i = 0; i < gridSize; i++) {
                for (byte j = 0; j < gridSize; j++) {
                    drawStone(canvas, i, j, table.abstractBoard[i][j]);
                }
            }
            setBackgroundColor(table.isRenju() ? RENJU_BG : table.getGameColor());
            // Translucent black candidates: in-progress offer picks (offerer, while collecting),
            // or the ten offered fifth moves during the SELECTION phase. The offered set is drawn
            // from the authoritative renjuState.offered so BOTH players see it (the offerer's local
            // renjuMode is PENDING/IDLE after sending, but the offers must stay visible until white
            // selects one).
            if (table.isRenju()) {
                if (renjuMode == RENJU_OFFER) {
                    for (int m : renjuPicks) {
                        drawStone(canvas, (byte) (m / gridSize), (byte) (m % gridSize), (byte) 4);
                    }
                } else if (table.getGameState().renjuState.phase(table.getMoves().size())
                        == RenjuLiveState.Phase.SELECTION) {
                    for (int m : table.getGameState().renjuState.offered) {
                        drawStone(canvas, (byte) (m / gridSize), (byte) (m % gridSize), (byte) 4);
                    }
                }
            }
        }
        if (goDeadStonesByPlayer != null) {
            for (int move : goDeadStonesByPlayer.get(1)) {
                byte movei = (byte) (move / gridSize);
                byte movej = (byte) (move % gridSize);
                drawStone(canvas, movei, movej, (byte) 4);
            }
            for (int move : goDeadStonesByPlayer.get(2)) {
                byte movei = (byte) (move / gridSize);
                byte movej = (byte) (move % gridSize);
                drawStone(canvas, movei, movej, (byte) 3);
            }
        }
        if (goTerritoryByPlayer != null) {
            for (int move : goTerritoryByPlayer.get(1)) {
                byte movei = (byte) (move / gridSize);
                byte movej = (byte) (move % gridSize);
                drawSquare(canvas, movei, movej, 2);
            }
            for (int move : goTerritoryByPlayer.get(2)) {
                byte movei = (byte) (move / gridSize);
                byte movej = (byte) (move % gridSize);
                drawSquare(canvas, movei, movej, 1);
            }
        }
        if (redDot > -1) {
            drawRedDot(canvas);
        }
    }

    private void drawStone(Canvas canvas, float x, float y, byte stoneColor) {
        if (stoneColor < 1) {
            return;
        }
        float radius = size / (gridSize * 2);
        float cx = (float) Math.floor(gridSize * x / size) * size / gridSize + size / (2 * gridSize), cy = (float) Math.floor(gridSize * y / size) * size / gridSize + size / (2 * gridSize);
        float cgx = cx - size / 200, cgy = cy - size / 200;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.BLACK);
        if (stoneColor == 2) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.rgb(125, 125, 125), Color.BLACK, Shader.TileMode.CLAMP));
        } else if (stoneColor == 1) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.WHITE, Color.rgb(210, 210, 210), Shader.TileMode.CLAMP));
        } else if (stoneColor == 4) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.rgb(125, 125, 125), Color.BLACK, Shader.TileMode.CLAMP));
            stonePaint.setAlpha(180);
        } else if (stoneColor == 3) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.WHITE, Color.rgb(210, 210, 210), Shader.TileMode.CLAMP));
            stonePaint.setAlpha(180);
        }

        if (stoneColor < 3) {
            float shadowOffset = radius / 5;
            shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            shadowPaint.setAlpha(110);
            canvas.drawCircle(cx + shadowOffset, cy + shadowOffset, radius, shadowPaint);
        }
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }

    private void drawZoomedStone(Canvas canvas, float x, float y, byte stoneColor) {
        float radius = size * 2 / (3 * gridSize);
        float cx = (float) Math.floor(gridSize * x / size) * size / gridSize + size / (2 * gridSize), cy = (float) Math.floor(gridSize * y / size) * size / gridSize + size / (2 * gridSize);
        float cgx = cx - size / 200, cgy = cy - size / 200;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.BLACK);
        if (stoneColor == 2) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.rgb(125, 125, 125), Color.BLACK, Shader.TileMode.CLAMP));
        } else if (stoneColor == 1) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.WHITE, Color.rgb(210, 210, 210), Shader.TileMode.CLAMP));
        } else if (stoneColor == 3) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.rgb(250, 125, 125), Color.RED, Shader.TileMode.CLAMP));
        }
        float shadowOffset = radius / 5;
        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        shadowPaint.setAlpha(110);
        canvas.drawCircle(cx + shadowOffset, cy + shadowOffset, radius, shadowPaint);
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }

    private void drawZoomedLine(Canvas canvas, float x, float y) {
        float radius = size * 2 / (3 * gridSize);
        float cx = (float) Math.floor(gridSize * x / size) * size / gridSize + size / (2 * gridSize), cy = (float) Math.floor(gridSize * y / size) * size / gridSize + size / (2 * gridSize);
        Paint linePaint;
        linePaint = new Paint();
        linePaint.setStrokeWidth(4);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.WHITE);
        canvas.drawLine(0, cy, size, cy, linePaint);
        canvas.drawLine(cx, 0, cx, size, linePaint);
    }

    private void drawStone(Canvas canvas, byte i, byte j, byte stoneColor) {
        drawStone(canvas, size * j / gridSize + size / (2 * gridSize), size * i / gridSize + size / (2 * gridSize), stoneColor);
    }

    private void drawSquare(Canvas canvas, byte i, byte j, int stoneColor) {
        drawSquare(canvas, size * j / gridSize + size / (2 * gridSize), size * i / gridSize + size / (2 * gridSize), stoneColor);
    }

    private void drawSquare(Canvas canvas, float x, float y, int stoneColor) {
        if (stoneColor < 1) {
            return;
        }
        float width = size / (3 * gridSize);
        float cx = (float) Math.floor(gridSize * x / size) * size / gridSize + size / (2 * gridSize) - width / 2, cy = (float) Math.floor(gridSize * y / size) * size / gridSize + size / (2 * gridSize) - width / 2;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        if (stoneColor == 1) {
            stonePaint.setColor(Color.WHITE);
        } else if (stoneColor == 2) {
            stonePaint.setColor(Color.BLACK);
        }
        canvas.drawRect(cx, cy, cx + width, cy + width, stonePaint);
    }

    private void drawRedDot(Canvas canvas) {
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.RED);
        float radius = size / (5 * gridSize);
        byte i = (byte) (redDot / gridSize);
        byte j = (byte) (redDot % gridSize);
        float cx = size * j / gridSize + size / (2 * gridSize), cy = size * i / gridSize + size / (2 * gridSize);
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }


    private Paint makePaint(int color) {
        Paint p = new Paint();
        p.setColor(color);
        return (p);
    }


    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width);
    }

    public void setFragment(LiveTableFragment fragment) {
        this.fragment = fragment;
    }

//    private void drawStone(Canvas canvas, int move, int stoneColor) {
//        int i = move % 19, j = move / 19;
//        double x = size*i/19 + size/38, y = size*j/19 + size/38;
//
//        float radius = size / 39;
//        float cx = (float) Math.floor(19*x/size)*size/19 + size/38, cy = (float) Math.floor(19*y/size)*size/19 + size/38;
//        float cgx = cx - size/200, cgy = cy - size/200;
//        Paint stonePaint;
//        stonePaint = new Paint();
//        stonePaint.setStrokeWidth(1);
//        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
////        stonePaint.setColor(Color.BLACK);
////        if (stoneColor == 2) {
////            stonePaint.setShader(new RadialGradient(cgx, cgy,
////                    radius*5/4, Color.rgb(125,125,125), Color.BLACK, Shader.TileMode.CLAMP));
////        } else {
////            stonePaint.setShader(new RadialGradient(cgx,cgy,
////                    radius*5/4, Color.WHITE, Color.rgb(210,210,210), Shader.TileMode.CLAMP));
////        }
//        stonePaint.setColor(stoneColor);
////        float shadowOffset = radius/7;
////        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
////        shadowPaint.setAlpha(110);
////        canvas.drawCircle(cx+shadowOffset, cy+shadowOffset, radius, shadowPaint);
//        canvas.drawCircle(cx, cy, radius, stonePaint);
//        stonePaint = new Paint();
//        stonePaint.setStrokeWidth(1);
//        stonePaint.setStyle(Paint.Style.STROKE);
//        stonePaint.setColor(Color.BLACK);
//        canvas.drawCircle(cx, cy, radius, stonePaint);
//    }


}
