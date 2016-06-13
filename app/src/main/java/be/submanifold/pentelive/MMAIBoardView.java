package be.submanifold.pentelive;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by waliedothman on 15/04/16.
 */
public class MMAIBoardView extends View {
    public int blackColor = Color.BLACK, whiteColor = Color.WHITE, penteColor = Color.parseColor("#FDDEA3");
    private Paint blackPaint =  makePaint(blackColor), whitePaint = makePaint(whiteColor), pentePaint = makePaint(penteColor), shadowPaint = makePaint(Color.GRAY);
    public byte abstractBoard[][] = {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
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

    public int whiteCaptures;
    public int blackCaptures;

    public int redDot = -1;

    private boolean active, rated, gameOver;
    private List<Integer> movesList;

    private Ai aiPlayer;

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    private Activity activity;


    private boolean replayed = false;
    private char coordinateLetters[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};

    private Game game;

    public Game getGame() {
        return game;
    }
    public void setGame(Game game) {
        this.game = game;
//        this.game.parseGame(this);
    }
    public void setMyColor(byte myColor) {        this.myColor = myColor;    }
    public void setDifficulty(int difficulty) {        this.difficulty = difficulty;    }

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
        stoneI = (byte) (19*stoneX/size);
        stoneY = y;
        stoneJ = (byte) (19*stoneY/size);
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                scaling = 2;
                translateX = -x/2;
                translateY = -y/2;
                break;
            case MotionEvent.ACTION_MOVE:
                scaling = 2;
                translateX = -x/2;
                translateY = -y/2;
                break;
            case MotionEvent.ACTION_UP:
                scaling = 1;
                translateX = 0;
                translateY = 0;
                break;
        }

        if (!gameOver && active && abstractBoard[stoneI][stoneJ] == 0) {
            playedMove = 19*stoneJ + stoneI;
        }
        if (scaling == 1) {
            if (playedMove > -1 && !gameOver){
                movesList.add(new Integer(playedMove));
                replayPenteGame(abstractBoard);
                if (!gameOver) {
                    ((MMAIActivity) activity).showThinking();
                    int[] moves = new int[movesList.size()];
                    for (int i = 0; i < movesList.size(); ++i) {
                        moves[i] = movesList.get(i).intValue();
                    }
                    active = false;
                    aiPlayer.getMove(moves);
                }
            }
        }
        invalidate();
        return true;
    }

    public void startGame() {
        gameOver = false;
        aiPlayer.setLevel(difficulty);
        aiPlayer.setSeat(3 - myColor);
        aiPlayer.setBoard(this);
        movesList.clear();
        movesList.add(new Integer(180));
        if (myColor == 1) {
            active = false;
            int[] moves = new int[movesList.size()];
            for (int i = 0; i < movesList.size(); ++i ) {
                moves[i] = movesList.get(i).intValue();
            }
            aiPlayer.getMove(moves);
        } else {
            active = true;
        }
        replayPenteGame(abstractBoard);
        invalidate();
    }

    public void processAImove(final int move) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                active = true;
                movesList.add(new Integer(move));
                replayPenteGame(abstractBoard);
                playedMove = -1;
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                ((MMAIActivity) activity).hideThinking();
            }
        });//        aiPlayer.destroy();
    }

    public void undoMove() {
        if (movesList.size()>1) {
            movesList.remove(movesList.size() - 1);
            active = myColor == (1 + movesList.size()%2);
            replayPenteGame(abstractBoard);
        }
    }


    private void drawBoard(Canvas canvas) {
        float step = (float) size / 19, margin = step/2;
        Paint linePaint = blackPaint;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        for ( int i = 0; i < 19; i++ ) {
            canvas.drawLine(margin + step*i, margin, margin + step*i, size - margin, linePaint);
            canvas.drawLine(margin, margin + step*i, size - margin, margin + step*i, linePaint);
        }
        canvas.drawCircle(margin + 6*step, margin + 6*step, margin / 2, linePaint);
        canvas.drawCircle(size - (margin + 6*step), margin + 6*step, margin / 2, linePaint);
        canvas.drawCircle( margin + 6*step, size - (margin + 6*step), margin / 2, linePaint);
        canvas.drawCircle(size - (margin + 6*step), size - (margin + 6*step), margin / 2, linePaint);
        canvas.drawCircle(size/2, size/2, margin / 2, linePaint);
        for ( byte i = 0; i < 19; i++ ) {
            for ( byte j = 0; j < 19; j++ ) {
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
        float cx = (float) Math.floor(19*x/size)*size/19 + size/38, cy = (float) Math.floor(19*y/size)*size/19 + size/38;
        float cgx = cx - size/200, cgy = cy - size/200;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.BLACK);
        if (stoneColor == 2) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius*5/4, Color.rgb(125,125,125), Color.BLACK, Shader.TileMode.CLAMP));
        } else {
            stonePaint.setShader(new RadialGradient(cgx,cgy,
                    radius*5/4, Color.WHITE, Color.rgb(210,210,210), Shader.TileMode.CLAMP));
        }
        float shadowOffset = radius/7;
        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(cx+shadowOffset, cy+shadowOffset, radius, shadowPaint);
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }
    private void drawZoomedStone(Canvas canvas, float x, float y, byte stoneColor) {
        float radius = size / 30;
        float cx = (float) Math.floor(19*x/size)*size/19 + size/38, cy = (float) Math.floor(19*y/size)*size/19 + size/38;
        float cgx = cx - size/200, cgy = cy - size/200;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.BLACK);
        if (stoneColor == 2) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius*5/4, Color.rgb(125,125,125), Color.BLACK, Shader.TileMode.CLAMP));
        } else {
            stonePaint.setShader(new RadialGradient(cgx,cgy,
                    radius*5/4, Color.WHITE, Color.rgb(210,210,210), Shader.TileMode.CLAMP));
        }
        float shadowOffset = radius/7;
        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(cx+shadowOffset, cy+shadowOffset, radius, shadowPaint);
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }
    private void drawZoomedLine(Canvas canvas, float x, float y) {
        float radius = size / 30;
        float cx = (float) Math.floor(19*x/size)*size/19 + size/38, cy = (float) Math.floor(19*y/size)*size/19 + size/38;
        Paint linePaint;
        linePaint = new Paint();
        linePaint.setStrokeWidth(4);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.WHITE);
        canvas.drawLine(0,cy,size,cy, linePaint);
        canvas.drawLine(cx,0,cx,size, linePaint);
    }
    private void drawStone(Canvas canvas, byte i, byte j, byte stoneColor) {
        drawStone(canvas, size*i/19 + size/38, size*j/19 + size/38, stoneColor);
    }

    private void drawRedDot(Canvas canvas) {
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.RED);
        float radius = size / 100;
        byte j = (byte) (redDot/19);
        byte i = (byte) (redDot%19);
        float cx = size*i/19 + size/38, cy = size*j/19 + size/38;
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }


    private Paint makePaint(int color) {
        Paint p = new Paint();
        p.setColor(color);
        return(p);
    }

    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span)
    {
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

    protected void setTextViewHTML(TextView text, String html)
    {
        CharSequence sequence = Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for(URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
        text.setMovementMethod(new ScrollingMovementMethod());
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void replayPenteGame(byte[][] abstractBoard) {
        resetAbstractBoard(abstractBoard);
        for (int i = 0; i < movesList.size(); i++) {
            byte color = (byte) (1 + (i%2));
            abstractBoard[movesList.get(i) % 19][(int) (movesList.get(i) / 19)] = color;
            detectPenteCapture(abstractBoard, movesList.get(i) % 19, (int) (movesList.get(i) / 19), color);
        }
        if (rated && (movesList.size() == 2)) {
            for( int i = 7; i < 12; ++i) {
                for (int j = 7; j < 12; ++j) {
                    if (abstractBoard[i][j] == 0) {
                        abstractBoard[i][j] = -1;
                    }
                }
            }
        }
        if (movesList.isEmpty()) {
            return;
        } else {
            String str = "<center><b>Color:</b> " + (myColor == 1?"white":"black") + ", <b>difficulty: </b>" + difficulty + "</center><br>";
            for (int i = 0; i < movesList.size(); i++) {
                if (i%2 == 0) {
                    str = str + " <b>" + (i/2 + 1) + ".</b> ";
                } else {
                    str = str+"-";
                }
                str = str + coordinateLetters[movesList.get(i)%19] + "" + (19 - (movesList.get(i)/19));
            }

            RelativeLayout parentLayout = (RelativeLayout) this.getParent();
            setTextViewHTML(((TextView) parentLayout.findViewById(R.id.playerInfo)), str);
            redDot = movesList.get(movesList.size() - 1);
        }
        RelativeLayout parentLayout = (RelativeLayout) this.getParent();
        ((Toolbar) parentLayout.findViewById(R.id.toolbar)).setSubtitle("\u2B24 x " + blackCaptures + " - \u25EF x " + whiteCaptures);
        ((TextView) parentLayout.findViewById(R.id.capturesView)).setText("\u2B24 x " + blackCaptures + "\n\u25EF x " + whiteCaptures);

        if (whiteCaptures == 10 || blackCaptures == 10 || detectPente(abstractBoard, (byte) (2 - (movesList.size()%2)), movesList.get(movesList.size() - 1))) {
            gameOver = true;
            boolean iWon = false;
            if (whiteCaptures == 10) {
                if (myColor == 2) {
                    iWon = true;
                }
            } else if (blackCaptures == 10) {
                if (myColor == 1) {
                    iWon = true;
                }
            } else if (myColor == (2 - movesList.size()%2)) {
                iWon = true;
            }
            String msg = "You lost";
            if (iWon) {
                msg = "You won";
            }
            Toast toast = Toast.makeText(getContext(), msg, Toast.LENGTH_LONG);
            TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
            if (iWon) {
                v.setTextColor(Color.GREEN);
            } else {
                v.setTextColor(Color.YELLOW);
            }
            toast.show();
        } else {
            gameOver = false;
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

    private void detectPenteCapture(byte[][] abstractBoard, int i, int j, byte myColor) {
        byte opponentColor = (byte) (1 + (myColor % 2));
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



}
