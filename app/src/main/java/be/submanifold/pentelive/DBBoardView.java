package be.submanifold.pentelive;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import androidx.core.view.ViewCompat;
import android.view.ViewConfiguration;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import be.submanifold.pentelive.db.BoardReplay;
import be.submanifold.pentelive.db.DbBoardGeometry;
import be.submanifold.pentelive.db.DbGames;
import be.submanifold.pentelive.db.MoveLabel;
import be.submanifold.pentelive.db.SelectionFlash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by waliedothman on 15/04/16.
 */
public class DBBoardView extends View {
    public int blackColor = Color.BLACK, whiteColor = Color.WHITE, penteColor = Color.parseColor("#FDDEA3"),
            keryoPenteColor = Color.parseColor("#BAFDA3"), gomokuColor = Color.parseColor("#A3FDEB"),
            dPenteColor = Color.parseColor("#A3CDFD"), gPenteColor = Color.parseColor("#AEA3FD"),
            poofPenteColor = Color.parseColor("#EDA3FD"), connect6Color = Color.parseColor("#EDA3FD"),
            boatPenteColor = Color.parseColor("#25BAFF"), dkeryoColor = Color.parseColor("#FFA500"),
            oPenteColor = Color.parseColor("#52be80"), swap2PenteColor = Color.parseColor("#E5AA70"),
            swap2KeryoColor = Color.parseColor("#50C878");
    /** Live BoardView goColor / renjuColor; literal so they hold in JVM tests too. */
    public final int goColor = 0xFFFAC832, renjuColor = 0xFFD98880;
    private final Paint blackPaint = makePaint(blackColor);
    private final Paint whitePaint = makePaint(whiteColor);
    private final Paint pentePaint = makePaint(penteColor);
    private final Paint keryoPentePaint = makePaint(keryoPenteColor);
    private final Paint gomokuPaint = makePaint(gomokuColor);
    private final Paint dPentePaint = makePaint(dPenteColor);
    private final Paint gPentePaint = makePaint(gPenteColor);
    private final Paint poofPentePaint = makePaint(poofPenteColor);
    private final Paint connect6Paint = makePaint(connect6Color);
    private final Paint boatPentePaint = makePaint(boatPenteColor);
    private final Paint dkeryoPaint = makePaint(dkeryoColor);
    private final Paint oPentePaint = makePaint(oPenteColor);
    private final Paint shadowPaint = makePaint(Color.BLACK);
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
    private float size;
    private float scaling = 1;
    private float translateX = 0, translateY = 0, stoneX, stoneY;


    private byte myColor = 2;
    public int playedMove = -1;

    public int difficulty;

    private String game;

    public int whiteCaptures;
    public int blackCaptures;

    public int redDot = -1;


    private List<Integer> movesList = new ArrayList<>();
    private Map<Integer, Integer> candidateDots;
    private boolean dotsVisible = true;
    /** DB game id for {@link #game}, resolved once per setGame; null for an unknown game (no replay). */
    private Integer dbGameId;

    /** Board grid size: 9/13/19 for Go, 15 for Renju, 19 otherwise (and for an unknown game). */
    int grid() {
        return dbGameId == null ? 19 : DbGames.gridSize(dbGameId);
    }

    /** Reports user placements and AI moves; the controller owns the position. */
    public interface BoardListener {
        void onMovePlayed(int cell);

        void onAiMove(int cell);
    }

    private BoardListener boardListener;

    public void setBoardListener(BoardListener listener) {
        boardListener = listener;
    }

    public List<Integer> getMovesList() {
        return movesList;
    }

    public void setMovesList(List<Integer> movesList) {
        showPosition(movesList);
    }

    /**
     * Shows a position. The list is copied (callers pass RedoHistory's unmodifiable list).
     * Without a game the list is only stored; the replay runs on the next setGame-backed call.
     */
    public void showPosition(List<Integer> moves) {
        this.movesList = new ArrayList<>(moves);
        if (game != null) {
            replayGame(abstractBoard);
        }
        invalidate();
    }

    public Ai getAiPlayer() {
        return aiPlayer;
    }

    public void setAiPlayer(Ai aiPlayer) {
        this.aiPlayer = aiPlayer;
    }

    private Ai aiPlayer;

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    private Activity activity;


    private boolean replayed = false;


    public String getGame() {
        if (game == null) {
            setGame("Pente");
            invalidate();
        }
        return game;
    }

    public void setGame(String game) {
        this.game = game;
        this.dbGameId = DbGames.fromGameType(game);
        if (game.contains("Gomoku")) {
            setBackgroundColor(gomokuColor);
        } else {
            if (game.contains("Swap2-Pente")) {
                setBackgroundColor(swap2PenteColor);
            } else if (game.contains("Swap2-Keryo")) {
                setBackgroundColor(swap2KeryoColor);
            } else if (game.contains("Boat-Pente")) {
                setBackgroundColor(boatPenteColor);
            } else if (game.contains("Keryo-Pente")) {
                setBackgroundColor(keryoPenteColor);
            } else if (game.contains("G-Pente")) {
                setBackgroundColor(gPenteColor);
            } else if (game.contains("Poof-Pente")) {
                setBackgroundColor(poofPenteColor);
            } else if (game.contains("Connect6")) {
                setBackgroundColor(connect6Color);
            } else if (game.contains("D-Pente")) {
                setBackgroundColor(dPenteColor);
            } else if (game.contains("DK-Pente")) {
                setBackgroundColor(dkeryoColor);
            } else if (game.equals("Pente")) {
                setBackgroundColor(penteColor);
            } else if (game.equals("O-Pente")) {
                setBackgroundColor(oPenteColor);
            } else if (dbGameId != null && DbGames.isGo(dbGameId)) {
                setBackgroundColor(goColor);
            } else if (dbGameId != null && DbGames.blackMovesFirst(dbGameId)) {
                setBackgroundColor(renjuColor);
            }

        }
    }

    /** Candidate heat dots, cell to ARGB; null or empty clears them. */
    public void setCandidateDots(Map<Integer, Integer> cellArgb) {
        this.candidateDots = cellArgb;
        invalidate();
    }

    // ---- selected-move flash ----

    private static final long FLASH_HALF_PERIOD_MS = 450;
    private int selectedCell = -1;
    private int selectedStoneArgb;
    private int selectedHeatArgb;
    private float flashPhase;
    private boolean flashAnimated;
    private android.animation.ValueAnimator flashAnimator;
    /** Test seam; null = read Settings.Global.ANIMATOR_DURATION_SCALE. */
    private java.util.function.BooleanSupplier animationsEnabledOverride;

    /** The selected Moves row's cell, or -1. */
    int selectedCell() {
        return selectedCell;
    }

    /** True while the selection pulses; false for the static ring (animations off) or no selection. */
    boolean isFlashing() {
        return flashAnimated && selectedCell >= 0;
    }

    void setAnimationsEnabledForTest(java.util.function.BooleanSupplier s) {
        animationsEnabledOverride = s;
    }

    private boolean animationsEnabled() {
        if (animationsEnabledOverride != null) return animationsEnabledOverride.getAsBoolean();
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return false;
        float scale = android.provider.Settings.Global.getFloat(getContext().getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return SelectionFlash.animationsEnabled(scale);
    }

    /**
     * Marks cell as selected: it flashes between the side-to-move stone and heatArgb, or shows a static
     * ring when system animations are off. Replaces any earlier selection.
     */
    public void setSelectedCell(int cell, int heatArgb) {
        stopFlash();
        selectedCell = cell;
        selectedStoneArgb = SelectionFlash.stoneArgb(placementColour());
        selectedHeatArgb = heatArgb;
        flashPhase = 0f;
        flashAnimated = animationsEnabled();
        if (flashAnimated) startFlash();
        invalidate();
    }

    public void clearSelectedCell() {
        if (selectedCell < 0) return;
        stopFlash();
        selectedCell = -1;
        flashAnimated = false;
        invalidate();
    }

    /** Creates and starts the pulse. Overridden in tests (the android.jar stubs have no animator). */
    void startFlash() {
        flashAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        flashAnimator.setDuration(FLASH_HALF_PERIOD_MS);
        flashAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        flashAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        flashAnimator.addUpdateListener(a -> {
            flashPhase = (Float) a.getAnimatedValue();
            invalidate();
        });
        flashAnimator.start();
    }

    /** Cancels the pulse and drops the animator (and with it the listener's reference to this view). */
    void stopFlash() {
        if (flashAnimator != null) {
            flashAnimator.removeAllUpdateListeners();
            flashAnimator.cancel();
            flashAnimator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopFlash();
        selectedCell = -1;
        flashAnimated = false;
        super.onDetachedFromWindow();
    }

    private void drawSelection(Canvas canvas) {
        if (selectedCell < 0) return;
        int n = grid();
        int i = selectedCell % n, j = selectedCell / n;
        float cx = DbBoardGeometry.centre(i, size, n), cy = DbBoardGeometry.centre(j, size, n);
        float radius = DbBoardGeometry.stoneRadius(size, n);
        // Animations switched off mid-flash: draw the static ring.
        boolean animated = flashAnimated
                && (animationsEnabledOverride != null || android.animation.ValueAnimator.areAnimatorsEnabled());
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(animated ? SelectionFlash.colourAt(flashPhase, selectedStoneArgb, selectedHeatArgb)
                : selectedHeatArgb);
        canvas.drawCircle(cx, cy, radius, fill);
        Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        outline.setStyle(Paint.Style.STROKE);
        outline.setColor(Color.BLACK);
        outline.setStrokeWidth(animated ? 1 : Math.max(2f, radius / 4));
        canvas.drawCircle(cx, cy, animated ? radius : radius * 1.35f, outline);
    }

    public void setDotsVisible(boolean visible) {
        this.dotsVisible = visible;
        invalidate();
    }

    boolean drawsDots() {
        return dotsVisible && candidateDots != null && !candidateDots.isEmpty();
    }

    public boolean isReplayed() {
        return replayed;
    }

    public void setReplayed(boolean replayed) {
        this.replayed = replayed;
    }

    public int getRedDot() {
        return redDot;
    }

    public void setRedDot(int redDot) {
        this.redDot = redDot;
    }

    public DBBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(penteColor);
//        resetAbstractBoard(abstractBoard);
        movesList = new ArrayList<>(Collections.singletonList(180));
        abstractBoard[9][9] = 1;
        whiteCaptures = 0;
        blackCaptures = 0;
        scaling = 1;
        translateX = 0;
        translateY = 0;
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        size = getWidth();
        canvas.scale(scaling, scaling);
        canvas.translate(translateX, translateY);
        drawBoard(canvas);
        if (playedMove == -1) {
//            playedMove = -1;
            return;
        }

//        if (movesList != null) {
//            myColor = (byte) (movesList.size()%2 + 1);
//        } else {
//            myColor = 1;
//        }
        if (movesList != null) {
            myColor = (byte) placementColour();
        }
        if (scaling == 2) {
            drawZoomedLine(canvas, stoneX, stoneY);
            drawZoomedStone(canvas, stoneX, stoneY, myColor);
        }
//        else {
//            drawStone(canvas, stoneX, stoneY, myColor);
//        }

    }

    /** Stone colour of the side to move: Connect6 pattern, black first for Go/Renju, else white first. */
    int placementColour() {
        if (dbGameId != null) return SelectionFlash.sideToMove(dbGameId, movesList.size());
        return SelectionFlash.sideToMove(game != null && game.contains("Connect6"), movesList.size());
    }

    /** The empty cell under (x, y) on a board {@code boardSize} wide, or -1 (outside, occupied, blocked). */
    int placementCell(float x, float y, float boardSize) {
        int n = grid();
        int cell = DbBoardGeometry.cellAt(x, y, boardSize, n);
        if (cell < 0) return -1;
        return abstractBoard[cell % n][cell / n] == 0 ? cell : -1;
    }

    /** Horizontal fling on the board; finger moving left = onSwipeLeft, right = onSwipeRight. */
    public interface OnSwipeListener {
        void onSwipeLeft();

        void onSwipeRight();
    }

    private OnSwipeListener swipeListener;
    private VelocityTracker velocityTracker;
    private float swipeDownX, swipeDownY;
    // ACTION_DOWN clears the red dot before we know it is a swipe, so a swipe restores it.
    private int downRedDot;

    public void setOnSwipeListener(OnSwipeListener listener) {
        swipeListener = listener;
    }

    private boolean placementEnabled = true;

    /** False makes taps do nothing (no zoom, no placement); swipes still reach the listener. */
    public void setPlacementEnabled(boolean enabled) {
        placementEnabled = enabled;
    }

    /**
     * Detects a fast, mostly horizontal swipe. A slow drag to aim a stone is not a swipe (too slow),
     * so placement is unaffected. Returns true when the gesture ended as a swipe: the zoom and
     * tracking stone are cleared, nothing is placed, and the listener is called.
     */
    private boolean handleSwipe(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (velocityTracker != null) velocityTracker.recycle();
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
                swipeDownX = event.getX();
                swipeDownY = event.getY();
                downRedDot = redDot;
                return false;
            case MotionEvent.ACTION_MOVE:
                if (velocityTracker != null) velocityTracker.addMovement(event);
                return false;
            case MotionEvent.ACTION_CANCEL:
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                return false;
            case MotionEvent.ACTION_UP:
                if (velocityTracker == null) return false;
                velocityTracker.addMovement(event);
                velocityTracker.computeCurrentVelocity(1000);
                float vx = velocityTracker.getXVelocity();
                velocityTracker.recycle();
                velocityTracker = null;
                ViewConfiguration vc = ViewConfiguration.get(getContext());
                float dx = event.getX() - swipeDownX;
                float dy = event.getY() - swipeDownY;
                if (Math.abs(dx) < vc.getScaledTouchSlop() * 8
                        || Math.abs(dx) < 2 * Math.abs(dy)
                        || Math.abs(vx) < vc.getScaledMinimumFlingVelocity() * 8) {
                    return false;
                }
                playedMove = -1;
                scaling = 1;
                translateX = 0;
                translateY = 0;
                redDot = downRedDot;
                invalidate();
                if (dx < 0) swipeListener.onSwipeLeft();
                else swipeListener.onSwipeRight();
                return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub

//        active = myColor == (1 + movesList.size()%2);
        if (swipeListener != null && handleSwipe(event)) {
            return true;
        }
        // A scrolling parent (the NestedScrollView in activity_database) took over the gesture:
        // drop the aiming zoom and place nothing.
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            playedMove = -1;
            scaling = 1;
            translateX = 0;
            translateY = 0;
            invalidate();
            return false;
        }
        // Read-only board (the game viewer): keep the gesture for swipe detection, no aim or placement.
        if (!placementEnabled) {
            return true;
        }

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
        playedMove = -1;
        stoneX = x;
        stoneY = y;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Keep the scrolling parent from stealing a vertical aim-drag (it would cancel
                // the gesture). The flag resets automatically at the end of the gesture.
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                redDot = -1;
                scaling = 2;
                translateX = -x / 2;
                translateY = -y / 2;
                break;
            case MotionEvent.ACTION_MOVE:
                scaling = 2;
                translateX = -x / 2;
                translateY = -y / 2;
                break;
            case MotionEvent.ACTION_UP:
                scaling = 1;
                translateX = 0;
                translateY = 0;
                // Accessibility: report the release as a click. No OnClickListener is set on
                // this view, so this only sends TYPE_VIEW_CLICKED; the return value is unused.
                performClick();
                break;
        }

        playedMove = placementCell(x, y, realSize);
        if (scaling == 1 && playedMove > -1) {
            reportPlacement(playedMove);
        }
        invalidate();
        return true;
    }

    /** A placement is reported, never applied: the controller answers with showPosition. */
    void reportPlacement(int cell) {
        playedMove = -1;
        if (boardListener != null) {
            boardListener.onMovePlayed(cell);
        }
    }

    void reportAiMove(int cell) {
        if (boardListener != null) {
            boardListener.onAiMove(cell);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void processAImove(final int move) {
        activity.runOnUiThread(() -> {
            playedMove = -1;
            reportAiMove(move);
        });
    }



    private void drawBoard(Canvas canvas) {
        int n = grid();
        float step = size / n, margin = step / 2;
        Paint linePaint = blackPaint;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        for (int i = 0; i < n; i++) {
            canvas.drawLine(margin + step * i, margin, margin + step * i, size - margin, linePaint);
            canvas.drawLine(margin, margin + step * i, size - margin, margin + step * i, linePaint);
        }
        if (dbGameId != null && DbBoardGeometry.filledStarPoints(dbGameId)) {
            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(Color.BLACK);
            float dot = DbGames.isGo(dbGameId) ? margin / 3 : margin / 2;
            for (int p : DbBoardGeometry.starPoints(dbGameId)) {
                canvas.drawCircle(DbBoardGeometry.centre(p % n, size, n), DbBoardGeometry.centre(p / n, size, n), dot, dotPaint);
            }
        } else {
            canvas.drawCircle(margin + 6 * step, margin + 6 * step, margin / 2, linePaint);
            canvas.drawCircle(size - (margin + 6 * step), margin + 6 * step, margin / 2, linePaint);
            canvas.drawCircle(margin + 6 * step, size - (margin + 6 * step), margin / 2, linePaint);
            canvas.drawCircle(size - (margin + 6 * step), size - (margin + 6 * step), margin / 2, linePaint);
            canvas.drawCircle(size / 2, size / 2, margin / 2, linePaint);
        }
        for (byte i = 0; i < n; i++) {
            for (byte j = 0; j < n; j++) {
                drawStone(canvas, i, j, abstractBoard[i][j]);
            }
        }
        if (drawsDots()) {
            for (Map.Entry<Integer, Integer> dot : candidateDots.entrySet()) {
                drawStone(canvas, dot.getKey(), dot.getValue());
            }
        }
        drawSelection(canvas);
        if (redDot > -1) {
            drawRedDot(canvas);
        }
    }

    private void drawStone(Canvas canvas, float x, float y, byte stoneColor) {
        if (stoneColor < 1) {
            return;
        }
        int n = grid();
        float radius = DbBoardGeometry.stoneRadius(size, n);
        float cx = (float) Math.floor(n * x / size) * size / n + size / (2f * n), cy = (float) Math.floor(n * y / size) * size / n + size / (2f * n);
        float cgx = cx - size / 200, cgy = cy - size / 200;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.BLACK);
        if (stoneColor == 2) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.rgb(125, 125, 125), Color.BLACK, Shader.TileMode.CLAMP));
        } else {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.WHITE, Color.rgb(210, 210, 210), Shader.TileMode.CLAMP));
        }
        float shadowOffset = radius / 7;
        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        shadowPaint.setAlpha(110);
        canvas.drawCircle(cx + shadowOffset, cy + shadowOffset, radius, shadowPaint);
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }

    private void drawStone(Canvas canvas, int move, int stoneColor) {
        int n = grid();
        if (move < 0 || move >= n * n) return;
        int i = move % n, j = move / n;
        float radius = DbBoardGeometry.stoneRadius(size, n);
        float cx = DbBoardGeometry.centre(i, size, n), cy = DbBoardGeometry.centre(j, size, n);
        float cgx = cx - size / 200, cgy = cy - size / 200;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
//        stonePaint.setColor(Color.BLACK);
//        if (stoneColor == 2) {
//            stonePaint.setShader(new RadialGradient(cgx, cgy,
//                    radius*5/4, Color.rgb(125,125,125), Color.BLACK, Shader.TileMode.CLAMP));
//        } else {
//            stonePaint.setShader(new RadialGradient(cgx,cgy,
//                    radius*5/4, Color.WHITE, Color.rgb(210,210,210), Shader.TileMode.CLAMP));
//        }
        stonePaint.setColor(stoneColor);
//        float shadowOffset = radius/7;
//        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
//        shadowPaint.setAlpha(110);
//        canvas.drawCircle(cx+shadowOffset, cy+shadowOffset, radius, shadowPaint);
        canvas.drawCircle(cx, cy, radius, stonePaint);
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.STROKE);
        stonePaint.setColor(Color.BLACK);
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }

    private void drawZoomedStone(Canvas canvas, float x, float y, byte stoneColor) {
        int n = grid();
        float radius = DbBoardGeometry.zoomedStoneRadius(size, n);
        float cx = (float) Math.floor(n * x / size) * size / n + size / (2f * n), cy = (float) Math.floor(n * y / size) * size / n + size / (2f * n);
        float cgx = cx - size / 200, cgy = cy - size / 200;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.BLACK);
        if (stoneColor == 2) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.rgb(125, 125, 125), Color.BLACK, Shader.TileMode.CLAMP));
        } else {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius * 5 / 4, Color.WHITE, Color.rgb(210, 210, 210), Shader.TileMode.CLAMP));
        }
        float shadowOffset = radius / 7;
        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        shadowPaint.setAlpha(110);
        canvas.drawCircle(cx + shadowOffset, cy + shadowOffset, radius, shadowPaint);
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }

    private void drawZoomedLine(Canvas canvas, float x, float y) {
        int n = grid();
        float cx = (float) Math.floor(n * x / size) * size / n + size / (2f * n), cy = (float) Math.floor(n * y / size) * size / n + size / (2f * n);
        Paint linePaint;
        linePaint = new Paint();
        linePaint.setStrokeWidth(4);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.WHITE);
        canvas.drawLine(0, cy, size, cy, linePaint);
        canvas.drawLine(cx, 0, cx, size, linePaint);
    }

    private void drawStone(Canvas canvas, byte i, byte j, byte stoneColor) {
        int n = grid();
        drawStone(canvas, DbBoardGeometry.centre(i, size, n), DbBoardGeometry.centre(j, size, n), stoneColor);
    }

    private void drawRedDot(Canvas canvas) {
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.RED);
        float radius = size / 100;
        int n = grid();
        if (redDot >= n * n) return;
        int j = redDot / n;
        int i = redDot % n;
        float cx = DbBoardGeometry.centre(i, size, n), cy = DbBoardGeometry.centre(j, size, n);
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }


    private Paint makePaint(int color) {
        Paint p = new Paint();
        p.setColor(color);
        return (p);
    }

    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {

                String url = span.getURL();
                LinkOpener.open(getContext(), url);
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    public void setTextViewHTML(TextView text, String html) {
        String str = moveListHtml();

        CharSequence sequence = Html.fromHtml(str + html, Html.FROM_HTML_MODE_LEGACY);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for (URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
        text.setMovementMethod(new ScrollingMovementMethod());
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /** Numbered move list ("1. K10-L10 2. ..."), labels via MoveLabel (a Go pass reads PASS). */
    String moveListHtml() {
        int n = grid();
        String str = "";
        if (game.contains("Connect6")) {
            for (int i = 0; i < movesList.size(); i++) {
                if (i == 0) {
                    str += "<b>1.</b> ";
                } else {
                    if (((i - 3) % 4) == 0) {
                        str += " <b>" + ((i >> 2) + 2) + ".</b> ";
                    } else if (((i - 3) % 4) == 2 || i == 1) {
                        str += " - ";
                    } else {
                        str += "-";
                    }
                }
                str += MoveLabel.label(movesList.get(i), n);
            }
        } else {
            for (int i = 0; i < movesList.size(); i++) {
                if (i % 2 == 0) {
                    str = str + " <b>" + (i / 2 + 1) + ".</b> ";
                } else {
                    str = str + "-";
                }
                str = str + MoveLabel.label(movesList.get(i), n);
            }
        }
        return str;
    }

    private void replayGame(byte[][] abstractBoard) {
        resetAbstractBoard(abstractBoard);
        if (dbGameId == null) {
            return;
        }
        BoardReplay.Result r = BoardReplay.replay(dbGameId, movesList);
        int n = r.boardXY.length;
        for (int x = 0; x < n; x++) {
            System.arraycopy(r.boardXY[x], 0, abstractBoard[x], 0, n);
        }
        whiteCaptures = r.whiteCaptures;
        blackCaptures = r.blackCaptures;
        // Go ko: the retake cell is blocked (not drawn, not tappable) until another stone is played.
        if (r.koMove >= 0) {
            abstractBoard[r.koMove % n][r.koMove / n] = -1;
        }
        // G-Pente tournament-rule restriction: block taps in the centre zone for the third stone.
        if (game.contains("G-Pente") && movesList.size() == 2) {
            for (int i = 7; i < 12; ++i) {
                for (int j = 7; j < 12; ++j) {
                    if (abstractBoard[i][j] == 0) {
                        abstractBoard[i][j] = -1;
                    }
                }
            }
            for (int i = 1; i < 3; ++i) {
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
        } else if (game.contains("G-Pente") && movesList.size() == 3) {
            for (int i = 7; i < 12; ++i) {
                for (int j = 7; j < 12; ++j) {
                    if (abstractBoard[i][j] == -1) {
                        abstractBoard[i][j] = 0;
                    }
                }
            }
            for (int i = 1; i < 3; ++i) {
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
        invalidate();
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

    private SpannableStringBuilder getCapturesText(int lineHeight) {
        SpannableStringBuilder sb = new SpannableStringBuilder("");
        Drawable icon;
        icon = ContextCompat.getDrawable(MyApplication.getContext(), R.drawable.white_nobg);
        icon.setBounds(0, 0, lineHeight * 4 / 5, lineHeight * 4 / 5);
        sb.append(" ").setSpan(new ImageSpan(icon, ImageSpan.ALIGN_BASELINE), sb.length() - 1, sb.length(), 0);
        sb.append(" ").append("x " + whiteCaptures + "\n");
        icon = ContextCompat.getDrawable(MyApplication.getContext(), R.drawable.black_nobg);
        icon.setBounds(0, 0, lineHeight * 4 / 5, lineHeight * 4 / 5);
        sb.append(" ").setSpan(new ImageSpan(icon, ImageSpan.ALIGN_BASELINE), sb.length() - 1, sb.length(), 0);
        sb.append(" ").append("x " + blackCaptures);
        return sb;
    }


    /**
     * Games whose first stone may be played off-center (and therefore taken back again)
     * instead of being seeded at the fixed center point K10.
     */
    public static boolean allowsOffCenterFirstStone(String game) {
        return game.contains("D-Pente") || game.contains("DK-Pente") || game.contains("Swap2-");
    }

    public void resetState() {
        if (dbGameId != null) {
            showPosition(DbGames.seed(dbGameId)); // Go starts empty, Renju at 112
            return;
        }
        List<Integer> seed = new ArrayList<>();
        if (!allowsOffCenterFirstStone(game)) {
            seed.add(180);
        }
        showPosition(seed);
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

    private void detectPoof(byte[][] abstractBoard, int i, int j, byte myColor) {
        byte opponentColor = (byte) (1 + (myColor % 2));
        boolean poofed = false;
        if (((i - 2) > -1) && ((i + 1) < 19)) {
            if (abstractBoard[i - 1][j] == myColor) {
                if ((abstractBoard[i - 2][j] == opponentColor) && (abstractBoard[i + 1][j] == opponentColor)) {
                    abstractBoard[i - 1][j] = 0;
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
        if (((i - 2) > -1) && ((j - 2) > -1) && ((i + 1) < 19) && ((j + 1) < 19)) {
            if (abstractBoard[i - 1][j - 1] == myColor) {
                if ((abstractBoard[i - 2][j - 2] == opponentColor) && (abstractBoard[i + 1][j + 1] == opponentColor)) {
                    abstractBoard[i - 1][j - 1] = 0;
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
        if (((j - 2) > -1) && ((j + 1) < 19)) {
            if (abstractBoard[i][j - 1] == myColor) {
                if ((abstractBoard[i][j - 2] == opponentColor) && (abstractBoard[i][j + 1] == opponentColor)) {
                    abstractBoard[i][j - 1] = 0;
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
        if (((i - 1) > -1) && ((j - 2) > -1) && ((i + 2) < 19) && ((j + 1) < 19)) {
            if (abstractBoard[i + 1][j - 1] == myColor) {
                if ((abstractBoard[i - 1][j + 1] == opponentColor) && (abstractBoard[i + 2][j - 2] == opponentColor)) {
                    abstractBoard[i + 1][j - 1] = 0;
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
        if (((i + 2) < 19) && ((i - 1) > -1)) {
            if (abstractBoard[i + 1][j] == myColor) {
                if ((abstractBoard[i + 2][j] == opponentColor) && (abstractBoard[i - 1][j] == opponentColor)) {
                    abstractBoard[i + 1][j] = 0;
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
        if (((i - 1) > -1) && ((j - 1) > -1) && ((i + 2) < 19) && ((j + 2) < 19)) {
            if (abstractBoard[i + 1][j + 1] == myColor) {
                if ((abstractBoard[i - 1][j - 1] == opponentColor) && (abstractBoard[i + 2][j + 2] == opponentColor)) {
                    abstractBoard[i + 1][j + 1] = 0;
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
        if (((j + 2) < 19) && ((j - 1) > -1)) {
            if (abstractBoard[i][j + 1] == myColor) {
                if ((abstractBoard[i][j - 1] == opponentColor) && (abstractBoard[i][j + 2] == opponentColor)) {
                    abstractBoard[i][j + 1] = 0;
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
        if (((i - 2) > -1) && ((j - 1) > -1) && ((i + 1) < 19) && ((j + 2) < 19)) {
            if (abstractBoard[i - 1][j + 1] == myColor) {
                if ((abstractBoard[i + 1][j - 1] == opponentColor) && (abstractBoard[i - 2][j + 2] == opponentColor)) {
                    abstractBoard[i - 1][j + 1] = 0;
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

    private void detectKeryoPoof(byte[][] abstractBoard, int i, int j, byte myColor) {
        byte opponentColor = (byte) (1 + (myColor % 2));
        boolean poofed = false;
        if (((i - 3) > -1) && ((i + 1) < 19)) { // left
            if (abstractBoard[i - 1][j] == myColor && abstractBoard[i - 2][j] == myColor) {
                if ((abstractBoard[i - 3][j] == opponentColor) && (abstractBoard[i + 1][j] == opponentColor)) {
                    abstractBoard[i - 2][j] = 0;
                    abstractBoard[i - 1][j] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
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
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
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
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
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
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
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
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
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
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                    poofed = true;
                }
            }
        }
        if (((j + 2) < 19) && ((j - 1) > -1)) { // down
            if (abstractBoard[i][j + 1] == myColor && abstractBoard[i][j + 2] == myColor) {
                if ((abstractBoard[i][j - 1] == opponentColor) && (abstractBoard[i][j + 3] == opponentColor)) {
                    abstractBoard[i][j + 1] = 0;
                    abstractBoard[i][j + 2] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
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
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
                    poofed = true;
                }
            }
        }

        // 4 directions with center of 3 stones placed to poof
        if (((i - 2) > -1) && ((i + 2) < 19)) { // horizontal
            if (abstractBoard[i - 1][j] == myColor && abstractBoard[i + 1][j] == myColor) {
                if ((abstractBoard[i - 2][j] == opponentColor) && (abstractBoard[i + 2][j] == opponentColor)) {
                    abstractBoard[i + 1][j] = 0;
                    abstractBoard[i - 1][j] = 0;
                    abstractBoard[i][j] = 0;
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
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
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
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
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
                    }
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
                    if (myColor == 1) {
                        whiteCaptures += 2;
                    } else {
                        blackCaptures += 2;
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


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Keep the system Back gesture from stealing board swipes that start near the screen edge.
        int[] r = SwipeGeometry.exclusionBand(w, h, getResources().getDisplayMetrics().density);
        ViewCompat.setSystemGestureExclusionRects(this,
                java.util.Collections.singletonList(new Rect(r[0], r[1], r[2], r[3])));
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width);
    }


}
