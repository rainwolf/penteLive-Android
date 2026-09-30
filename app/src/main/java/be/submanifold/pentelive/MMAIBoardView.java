package be.submanifold.pentelive;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
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
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import be.submanifold.pente.rules.VariantReferee;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by waliedothman on 15/04/16.
 */
public class MMAIBoardView extends View {
    public int blackColor = Color.BLACK, whiteColor = Color.WHITE, penteColor = Color.parseColor("#FDDEA3"),
            keryoPenteColor = Color.parseColor("#BAFDA3"),
            poofColor = Color.parseColor("#EDA3FD"),
            boatColor = Color.parseColor("#25BAFF"),
            openteColor = Color.parseColor("#52be80"),
            connect6Color = Color.parseColor("#EDA3FD");
    private final Paint blackPaint = makePaint(blackColor);
    private final Paint whitePaint = makePaint(whiteColor);
    private final Paint pentePaint = makePaint(penteColor);
    private final Paint shadowPaint = makePaint(Color.GRAY);
    private final Paint keryoPentePaint = makePaint(keryoPenteColor);
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


    private byte myColor = 2, stoneI, stoneJ;
    public int playedMove = -1;

    public int difficulty;
    public int game;

    public int whiteCaptures;
    public int blackCaptures;

    public int redDot = -1;

    private boolean active, rated, gameOver, aiThinking = false;
    private final List<Integer> movesList;

    private Ai aiPlayer;

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    private Activity activity;

    private final Context ctx = MyApplication.getContext();


    private boolean replayed = false;
    private final char[] coordinateLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};


    public void setGame(int game) {
        this.game = game;
//        this.game.parseGame(this);
    }

    // Owner (1 = white, 2 = black) of the move at index i. Connect6 (game 13/14)
    // plays TWO stones per turn: owner is 1 iff i % 4 in {0, 3}, else 2 (matches
    // GameClass.currentPlayer / VariantReferee.colorForMove). Every other game keeps
    // strict alternation 1 + (i % 2), so owner() is behaviour-identical for them.
    // Callers pass movesList.size() to ask "who owns the NEXT move to be placed".
    private int owner(int i) {
        if (game == 13 || game == 14) {
            int m = i % 4;
            return (m == 0 || m == 3) ? 1 : 2;
        }
        return 1 + (i % 2);
    }

    // Append one decoded Connect6 AI stone (a raw 0..360 cell index) to the move
    // list. An out-of-range or already-occupied cell means the native engine and
    // this client have desynced (stale/duplicate move, packing bug...) -- there is
    // no sane way to keep playing on a board the two sides disagree about, so this
    // is loud-fatal: log an error and freeze the game (gameOver = true) rather than
    // silently dropping the stone, which would permanently desync turn ownership
    // (movesList / owner() would no longer match what the engine actually played)
    // while still looking like a normal in-progress game. Replay is the caller's
    // responsibility so a two-stone turn draws in a single frame.
    private void appendAiStone(int cell) {
        if (cell < 0 || cell >= 361) {
            android.util.Log.e("MMAIBoardView", "connect6: AI stone out of range " + cell + " -- engine/client desync, freezing game");
            gameOver = true;
            return;
        }
        int x = cell % 19, y = cell / 19;
        if (abstractBoard[x][y] != 0) {
            android.util.Log.e("MMAIBoardView", "connect6: AI stone on occupied cell " + cell + " -- engine/client desync, freezing game");
            gameOver = true;
            return;
        }
        movesList.add(Integer.valueOf(cell));
    }

    public void setMyColor(byte myColor) {
        this.myColor = myColor;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
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

    //    public BoardView(Context context) {
//        super(context);
//        scaling = 1;
//        translateX = 0;
//        translateY = 0;
//    }
    public MMAIBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(penteColor);
        movesList = new ArrayList<Integer>();
        scaling = 1;
        translateX = 0;
        translateY = 0;
    }

    public void setAiPlayer(Ai aiPlayer) {
        RelativeLayout parentLayout = (RelativeLayout) this.getParent();
        TextView capturesTextView = parentLayout.findViewById(R.id.capturesView);
        capturesTextView.setText(getCapturesText(capturesTextView.getLineHeight()));
        this.aiPlayer = aiPlayer;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        size = getWidth();
        canvas.scale(scaling, scaling);
        canvas.translate(translateX, translateY);
        drawBoard(canvas);
        if (gameOver) {
            return;
        }
        if (!active) {
            return;
        }
        if (playedMove == -1) {
//            playedMove = -1;
            return;
        }

//        if (movesList != null) {
//            myColor = (byte) (movesList.size()%2 + 1);
//        } else {
//            myColor = 1;
//        }
        if (scaling == 2) {
            drawZoomedLine(canvas, stoneX, stoneY);
            drawZoomedStone(canvas, stoneX, stoneY, myColor);
        }
//        else {
//            drawStone(canvas, stoneX, stoneY, myColor);
//        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub

//        active = myColor == (1 + movesList.size()%2);
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
        stoneI = (byte) (19 * stoneX / size);
        stoneY = y;
        stoneJ = (byte) (19 * stoneY / size);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
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

        // A human placement is allowed iff the next move belongs to myColor. For
        // one-stone games this is exactly the old `active` gate; for Connect6 it also
        // allows the SECOND stone of the human's two-stone turn (owner unchanged).
        if (!gameOver && owner(movesList.size()) == myColor && abstractBoard[stoneI][stoneJ] == 0) {
            playedMove = 19 * stoneJ + stoneI;
        }
        if (scaling == 1) {
            if (playedMove > -1 && !gameOver) {
                movesList.add(Integer.valueOf(playedMove));
                replayGame(abstractBoard);
                // Trigger the AI only once the turn has actually passed to it. For
                // one-stone games this fires after every human move (owner flips each
                // move); for Connect6 it does NOT fire between the human's two stones
                // (owner still == myColor) and fires once after the second.
                if (!gameOver && owner(movesList.size()) != myColor) {
                    ((MMAIActivity) activity).showThinking();
                    int[] moves = new int[movesList.size()];
                    for (int i = 0; i < movesList.size(); ++i) {
                        moves[i] = movesList.get(i).intValue();
                    }
                    active = false;
                    aiThinking = true;
                    aiPlayer.getMove(moves);
                }
            }
        }
        invalidate();
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void startGame() {
        gameOver = false;
        aiThinking = false;
        aiPlayer.setLevel(difficulty);
        aiPlayer.setSeat(3 - myColor);
        aiPlayer.setGame(game);
        aiPlayer.setBoard(this);
        movesList.clear();
        movesList.add(Integer.valueOf(180)); // centre seed is move 0 (owner 1)
        // "AI is first" == the move after the seed does not belong to myColor. For
        // one-stone games owner(1) != myColor is exactly myColor == 1; for Connect6
        // it stays correct (owner(1) == 2, so the AI opens iff the human is white).
        if (owner(movesList.size()) != myColor) {
            active = false;
            int[] moves = new int[movesList.size()];
            for (int i = 0; i < movesList.size(); ++i) {
                moves[i] = movesList.get(i).intValue();
            }
            aiThinking = true;
            aiPlayer.getMove(moves);
        } else {
            active = true;
        }
        replayGame(abstractBoard);
        invalidate();
    }

    public void processAImove(final int move) {
        activity.runOnUiThread(() -> {
            if (game == 13 || game == 14) {
                // Connect6: the native move packs the whole two-stone turn base-362
                // (m1 = move / 362, m2 = move % 362; m2 == 361 is the single-stone
                // opening sentinel). Replay after EACH stone, not once at the end:
                // VariantReferee.replay only examines the row through the LAST move
                // in movesList, so a six completed by m1 (with m2 landing elsewhere)
                // would never be detected if both stones were appended before the
                // first replay -- the game would hang "in progress" forever. This
                // mirrors the react authority (two separate ADD_MOVE dispatches, one
                // isGameOver check per stone) and the human path here (two separate
                // onTouchEvent placements). If m1 already wins, m2 is skipped: the
                // red dot / winner banner stay on the stone that actually finished
                // the game, matching "dot ends on the last actually-applied stone".
                int m1 = move / 362, m2 = move % 362;
                appendAiStone(m1);
                // appendAiStone sets gameOver = true itself on an engine/client
                // desync (see there), but the replayGame() call right below always
                // recomputes gameOver from the replay outcome and -- since a bailed
                // append leaves movesList unchanged, so the replay is a no-op with
                // winner 0 -- would silently clear that freeze again. Snapshot it
                // here (before replay) so we can both gate m2 on it and reassert it
                // afterward.
                boolean desynced = gameOver;
                replayGame(abstractBoard);
                if (!desynced && !gameOver && m2 != 361) {
                    appendAiStone(m2);
                    desynced = gameOver;
                    replayGame(abstractBoard);
                }
                if (desynced) {
                    gameOver = true;
                }
            } else {
                movesList.add(Integer.valueOf(move));
                replayGame(abstractBoard);
            }
            // Turn has passed back to the human (owner(size) == myColor) for every
            // game after the AI's move(s); identical to the old unconditional `true`.
            active = owner(movesList.size()) == myColor;
            playedMove = -1;
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            aiThinking = false;
            ((MMAIActivity) activity).hideThinking();
        });//        aiPlayer.destroy();
    }

    public void undoMove() {
        if (aiThinking) {
            return;
        }
        int size = movesList.size();
        // Find the LARGEST t in [1, size-1] whose owner is myColor -- the human's
        // own most-recently-placed move -- and truncate back to it. This replaces
        // the old "pop one unconditionally, then keep popping while it still isn't
        // the human's turn, floored at size 1" loop, which could floor on a state
        // where owner(1) != myColor and get permanently stuck there (Undo would
        // keep landing on the same inert size-1 state -- frozen). The lookahead
        // instead recognises up front when there is nothing of the human's to
        // retract and refuses (no-op) rather than flooring blind. For every state
        // the old loop DIDN'T freeze on, it stopped at exactly this same t (it
        // walks size-1, size-2, ... one at a time, stopping at the first index
        // whose owner is myColor -- i.e. the largest such t), so behaviour is
        // identical there; the two diverge only in case (c) below.
        //
        // (a) Normal mid-game undo: e.g. size 6, myColor's last move at t = 4 (the
        //     AI played 5). Truncating to t = 4 removes the AI's whole turn AND
        //     the human's own last move, landing back on the human's turn to
        //     replay it -- same result the old pop-loop reached.
        // (b) Human retracting the first stone of their own two-stone Connect6
        //     turn: owner(size-1) is ALREADY myColor (they just placed it), so
        //     t == size-1 and exactly one stone is popped -- the old loop's while
        //     condition never even fired, so this is also unchanged.
        // (c) Boundary right after the auto-seed + the AI's first reply (e.g.
        //     size 2: the seed at index 0 owned by 1, the AI's reply at index 1
        //     owned by 2; with myColor == 1 there is no t in [1, size-1] == [1,1]
        //     with owner(t) == myColor). The human hasn't placed anything of their
        //     own yet -- there is nothing to undo. The old code had no way to
        //     express "give up" here and instead floored at size 1 regardless of
        //     ownership, leaving active permanently false (owner(1) != myColor)
        //     and Undo inert from then on. This lookahead detects that up front
        //     and makes undo a no-op: movesList and active are left exactly as
        //     they were, so the human can still play their move.
        int target = -1;
        for (int t = size - 1; t >= 1; t--) {
            if (owner(t) == myColor) {
                target = t;
                break;
            }
        }
        if (target == -1) {
            return; // no-op: nothing of the human's own turn to retract
        }
        while (movesList.size() > target) {
            movesList.remove(movesList.size() - 1);
        }
        active = owner(movesList.size()) == myColor;
        replayGame(abstractBoard);
    }


    private void drawBoard(Canvas canvas) {
        float step = size / 19, margin = step / 2;
        Paint linePaint = blackPaint;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        for (int i = 0; i < 19; i++) {
            canvas.drawLine(margin + step * i, margin, margin + step * i, size - margin, linePaint);
            canvas.drawLine(margin, margin + step * i, size - margin, margin + step * i, linePaint);
        }
        canvas.drawCircle(margin + 6 * step, margin + 6 * step, margin / 2, linePaint);
        canvas.drawCircle(size - (margin + 6 * step), margin + 6 * step, margin / 2, linePaint);
        canvas.drawCircle(margin + 6 * step, size - (margin + 6 * step), margin / 2, linePaint);
        canvas.drawCircle(size - (margin + 6 * step), size - (margin + 6 * step), margin / 2, linePaint);
        canvas.drawCircle(size / 2, size / 2, margin / 2, linePaint);
        for (byte i = 0; i < 19; i++) {
            for (byte j = 0; j < 19; j++) {
                drawStone(canvas, i, j, abstractBoard[i][j]);
            }
        }
        drawRedDot(canvas);

    }

    private void drawStone(Canvas canvas, float x, float y, byte stoneColor) {
        if (stoneColor < 1) {
            return;
        }
        float radius = size / 39;
        float cx = (float) Math.floor(19 * x / size) * size / 19 + size / 38, cy = (float) Math.floor(19 * y / size) * size / 19 + size / 38;
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

    private void drawZoomedStone(Canvas canvas, float x, float y, byte stoneColor) {
        float radius = size / 30;
        float cx = (float) Math.floor(19 * x / size) * size / 19 + size / 38, cy = (float) Math.floor(19 * y / size) * size / 19 + size / 38;
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
        float radius = size / 30;
        float cx = (float) Math.floor(19 * x / size) * size / 19 + size / 38, cy = (float) Math.floor(19 * y / size) * size / 19 + size / 38;
        Paint linePaint;
        linePaint = new Paint();
        linePaint.setStrokeWidth(4);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.WHITE);
        canvas.drawLine(0, cy, size, cy, linePaint);
        canvas.drawLine(cx, 0, cx, size, linePaint);
    }

    private void drawStone(Canvas canvas, byte i, byte j, byte stoneColor) {
        drawStone(canvas, size * i / 19 + size / 38, size * j / 19 + size / 38, stoneColor);
    }

    private void drawRedDot(Canvas canvas) {
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.RED);
        float radius = size / 100;
        byte j = (byte) (redDot / 19);
        byte i = (byte) (redDot % 19);
        float cx = size * i / 19 + size / 38, cy = size * j / 19 + size / 38;
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
                Intent intent = new Intent(getContext(), WebViewActivity.class);
                intent.putExtra("url", url);
                getContext().startActivity(intent);
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    protected void setTextViewHTML(TextView text, String html) {
        CharSequence sequence = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for (URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
        text.setMovementMethod(new ScrollingMovementMethod());
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // The board/captures/winner referee now lives in the plain-Java :rules module
    // (VariantReferee), tied to the authority react_mmai/src/Classes/GameClass.js
    // by a fixture regression test. This View only presents the result.
    private final VariantReferee referee = new VariantReferee();

    private void replayGame(byte[][] abstractBoard) {
        int winner = referee.replay(game, abstractBoard, movesList, rated);
        whiteCaptures = referee.whiteCaptures;
        blackCaptures = referee.blackCaptures;
        finishReplay(abstractBoard, winner);
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

    // Shared board-info + game-over presentation for the new variants. winner is
    // 0 (game continues), 1 (white) or 2 (black); iWon = (myColor == winner).
    private void finishReplay(byte[][] abstractBoard, int winner) {
        if (movesList.isEmpty()) {
            return;
        }
        String str = "<center><b>" + ctx.getString(R.string.color) + ":</b> "
                + (myColor == 1 ? ctx.getString(R.string.white) : ctx.getString(R.string.black))
                + ", <b>" + ctx.getString(R.string.difficulty) + " </b>" + difficulty + "</center><br>";
        for (int i = 0; i < movesList.size(); i++) {
            if (i % 2 == 0) {
                str = str + " <b>" + (i / 2 + 1) + ".</b> ";
            } else {
                str = str + "-";
            }
            str = str + coordinateLetters[movesList.get(i) % 19] + "" + (19 - (movesList.get(i) / 19));
        }

        RelativeLayout parentLayout = (RelativeLayout) this.getParent();
        setTextViewHTML(parentLayout.findViewById(R.id.playerInfo), str);
        redDot = movesList.get(movesList.size() - 1);
        ((Toolbar) parentLayout.findViewById(R.id.toolbar)).setSubtitle("⬤ x " + blackCaptures + " - ◯ x " + whiteCaptures);
        TextView capturesTextView = parentLayout.findViewById(R.id.capturesView);
        capturesTextView.setText(getCapturesText(capturesTextView.getLineHeight()));

        if (winner != 0) {
            gameOver = true;
            boolean iWon = (myColor == winner);
            String msg = iWon ? ctx.getString(R.string.you_won) : ctx.getString(R.string.you_lost);
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
        } else {
            gameOver = false;
        }
        invalidate();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width);
    }


}
