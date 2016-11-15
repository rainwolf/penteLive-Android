package be.submanifold.pentelive;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by waliedothman on 15/04/16.
 */
public class BoardView extends View {
//    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();
//    boolean dragging = true, swipeLeft = false;
//    static final int SWIPE_THRESHOLD_VELOCITY = 300;
    private BoardActivity boardActivity;

    public int blackColor = Color.BLACK, whiteColor = Color.WHITE, penteColor = Color.parseColor("#FDDEA3"),
            keryoPenteColor = Color.parseColor("#BAFDA3"), gomokuColor = Color.parseColor("#A3FDEB"),
            dPenteColor = Color.parseColor("#A3CDFD"), gPenteColor = Color.parseColor("#AEA3FD"),
            poofPenteColor = Color.parseColor("#EDA3FD"), connect6Color = Color.parseColor("#EDA3FD"),
            boatPenteColor = Color.parseColor("#25BAFF");
    private Paint blackPaint =  makePaint(blackColor), whitePaint = makePaint(whiteColor), pentePaint = makePaint(penteColor),
            keryoPentePaint = makePaint(keryoPenteColor), gomokuPaint = makePaint(gomokuColor),
            dPentePaint = makePaint(dPenteColor), gPentePaint = makePaint(gPenteColor),
            poofPentePaint = makePaint(poofPenteColor), connect6Paint = makePaint(connect6Color),
            boatPentePaint = makePaint(boatPenteColor), shadowPaint = makePaint(Color.BLACK);
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
    private byte myColor, stoneI, stoneJ;
    public int playedMove = -1;


    public int redDot = -1;


    public int c6RedDot = -1;
    public int connect6Move1 = -1;
    public int dPenteMove1 = -1;
    public int dPenteMove2 = -1;
    public int dPenteMove3 = -1;
    public boolean dPenteChosen = false;

    private boolean replayed = false;
    private char coordinateLetters[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};

    private Game game;

    private TextView textView = null;

    public Game getGame() {
        return game;
    }
    public void setGame(Game game) {
        this.game = game;
//        this.game.parseGame(this);
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
    public int getC6RedDot() {
        return c6RedDot;
    }
    public void setC6RedDot(int c6RedDot) {
        this.c6RedDot = c6RedDot;
    }

//    public BoardView(Context context) {
//        super(context);
//        scaling = 1;
//        translateX = 0;
//        translateY = 0;
//    }
    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaling = 1;
        translateX = 0;
        translateY = 0;
//        mDetector = new GestureDetectorCompat(context, new MyGestureListener());
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        size = getWidth();
        canvas.scale(scaling, scaling);
        canvas.translate(translateX, translateY);
        drawBoard(canvas);
        if (game != null) {
            RelativeLayout parentLayout = (RelativeLayout) this.getParent();
            ((Toolbar) parentLayout.findViewById(R.id.toolbar)).setTitle(game.getGameType());
            if (!game.isConnect6() && !game.isGomoku()) {
                ((Toolbar) parentLayout.findViewById(R.id.toolbar)).setSubtitle("\u2B24 x " + game.blackCaptures + " - \u25EF x " + game.whiteCaptures);
            }
//            parentLayout.setSupportActionBar(toolbar);
//            if (!game.isConnect6()) {
//                ((TextView) parentLayout.findViewById(R.id.capturesLabel)).setText("\u2B24 x " + game.blackCaptures + "\n\u25EF x " + game.whiteCaptures);
//            } else {
//                ((TextView) parentLayout.findViewById(R.id.capturesLabel)).setVisibility(GONE);
//            }
            if (scaling == 1) {
                if (textView == null) {
                    textView = ((TextView) parentLayout.findViewById(R.id.playerInfo));
                }
                String str;
                if (game.getOpponentName() != null && game.getOpponentName().contains(" vs ")) {
                    String players[] = game.getOpponentName().split(" vs ");
                    str = "<a href=\"https://www.pente.org/gameServer/profile?viewName=" + players[0] + "\">" +players[0] + "</a> vs " +
                            "<a href=\"https://www.pente.org/gameServer/profile?viewName=" + players[1] + "\">" +players[1] + "</a>"
                            + ", rating: " + game.getOpponentRating() + "<br>Remaining Time: " + game.getRemainingTime()
                            + "<br>" + game.getRatedNot() + " and " + game.getPrivateGame() + " game <br><br>" + game.getMovesString();
                } else {
                    str = "Opponent: <a href=\"https://www.pente.org/gameServer/profile?viewName=" + game.getOpponentName() + "\">" + game.getOpponentName() + "</a>"
                            + ", rating: " + game.getOpponentRating() + "<br>Remaining Time: " + game.getRemainingTime()
                            + "<br>" + game.getRatedNot() + " and " + game.getPrivateGame() + " game <br><br>" + game.getMovesString();
                }
                int textHeight = textView.getLineCount() * textView.getLineHeight();
                if (textHeight > textView.getHeight()) {
                    //Text is truncated because text height is taller than TextView height
                    textView.setGravity(Gravity.CENTER | Gravity.BOTTOM);
                } else {
                    //Text not truncated because text height not taller than TextView height
                    textView.setGravity(Gravity.FILL_VERTICAL | Gravity.CENTER);
                }
                setTextViewHTML(textView, str);
            }

//            if (game.dPenteChoice && !game.isActive()) {
            if (game.dPenteChoice && game.isActive() && !dPenteChosen) {
//                System.out.println("kitten here");
                ((LinearLayout) parentLayout.findViewById(R.id.dPenteLayout)).setVisibility(VISIBLE);
                ((LinearLayout) parentLayout.findViewById(R.id.submitLayout)).setVisibility(INVISIBLE);
//                ((TextView) parentLayout.findViewById(R.id.capturesLabel)).setVisibility(GONE);
                return;
            }
            if (!game.isActive()) {
                return;
            }
        }
        if (playedMove == -1) {
//            playedMove = -1;
            return;
        }

        if (game != null && game.getMovesList() != null && game.isConnect6()) {
            int i = game.getMovesList().size();
            myColor = (byte) ((((i % 4) == 0) || ((i % 4) == 3)) ? 1 : 2);
        } else if (game != null && game.isDPente() && game.getMovesList().size() == 1) {
            if (dPenteMove3 > -1) {
                myColor = (byte) 2;
            } else if (dPenteMove2 > -1) {
                myColor = (byte) 2;
            } else if (dPenteMove1 > -1) {
                myColor = (byte) 1;
            } else {
                myColor = (byte) 2;
            }
        } else if (game != null && game.getMovesList() != null) {
            myColor = (byte) (game.getMovesList().size()%2 + 1);
        } else {
            myColor = 1;
        }
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
        int index = event.getActionIndex();
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(index);

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
        dPenteMove3 = -1;
        stoneX = x;
        stoneI = (byte) (19*stoneX/size);
        stoneY = y;
        stoneJ = (byte) (19*stoneY/size);
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (!replayed) {
                    game.replayGame(abstractBoard, BoardView.this);
                    replayed = true;
                }
//                mVelocityTracker.clear();
//                // Add a user's movement to the tracker.
//                mVelocityTracker.addMovement(event);
//
//                dragging = true;
                scaling = 2;
                translateX = -x/2;
                translateY = -y/2;
                break;
            case MotionEvent.ACTION_MOVE:
//                mVelocityTracker.addMovement(event);
//                // When you want to determine the velocity, call
//                // computeCurrentVelocity(). Then call getXVelocity()
//                // and getYVelocity() to retrieve the velocity for each pointer ID.
//                mVelocityTracker.computeCurrentVelocity(1000);
//                // Log velocity of pixels per second
//                // Best practice to use VelocityTrackerCompat where possible.
//                double velocityX = VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId);
//
//                if (Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
//                    if (velocityX < 0) {
//                        swipeLeft = true;
////                        System.out.println("swipe RightToLeft moving ");
//
//                    } else {
//                        swipeLeft = false;
////                        System.out.println("swipe LeftToright moving ");
//                    }
//                    playedMove = -1;
//                    dragging = false;
//                    scaling = 1;
//                    translateX = 0;
//                    translateY = 0;
//                    return true;
//                }
////                System.out.println("dragging");
//                dragging = true;
                scaling = 2;
                translateX = -x/2;
                translateY = -y/2;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                scaling = 1;
                translateX = 0;
                translateY = 0;
//                if (!dragging) {
//                    if (swipeLeft) {
//                        boardActivity.goBack();
////                        System.out.println("swipe RightToLeft ");
//                    } else {
//                        boardActivity.goForward();
////                        System.out.println("swipe LeftToright ");
//                    }
//                    return true;
//                }
//                mVelocityTracker.recycle();
                break;
        }

        if (game.isActive() && abstractBoard[stoneI][stoneJ] == 0) {
            playedMove = 19*stoneJ + stoneI;
        }
        if (scaling == 1) {
            if (game.isConnect6() && connect6Move1 == -1) {
                connect6Move1 = playedMove;
            }
            if (game.isDPente() && game.getMovesList().size() == 1) {
                if (dPenteMove1 == -1) {
                    dPenteMove1 = playedMove;
                } else if (dPenteMove2 == -1 && playedMove != dPenteMove1) {
                    dPenteMove2 = playedMove;
                } else if (playedMove != dPenteMove1 && playedMove != dPenteMove2){
                    dPenteMove3 = playedMove;
                }
            }
            RelativeLayout parentLayout = (RelativeLayout) this.getParent();
            if (playedMove == -1 || abstractBoard[stoneI][stoneJ] != 0) {
                if (!replayed && game.getMovesList() != null) {
                    System.out.println(" fuck kitty");
                    game.replayGame(abstractBoard, this);
                    ((Button) parentLayout.findViewById(R.id.submitButton)).setText("Submit");
                    replayed = true;
                }
                if (game.isConnect6() && connect6Move1 > -1) {
                            ((Button) parentLayout.findViewById(R.id.submitButton)).setText("Submit: " + coordinateLetters[connect6Move1%19] + "" + (19 - (connect6Move1/19)) +
                                    "-...");
                }
            } else if (playedMove > -1){
                if (game.isConnect6()) {
                    if (connect6Move1 > -1) {
                        if (playedMove > -1 && playedMove != connect6Move1) {
                            ((Button) parentLayout.findViewById(R.id.submitButton)).setText("Submit: " + coordinateLetters[connect6Move1%19] + "" + (19 - (connect6Move1/19)) +
                                    "-" + coordinateLetters[stoneI] + "" + (19 - stoneJ));
                        } else {
                            ((Button) parentLayout.findViewById(R.id.submitButton)).setText("Submit: " + coordinateLetters[stoneI] + "" + (19 - stoneJ) +
                                    "-...");
                        }
                    }
                } else if (game.isDPente() && game.getMovesList().size() == 1) {
                    if (dPenteMove3 > -1) {
                        ((Button) parentLayout.findViewById(R.id.submitButton)).setText("Submit: " + coordinateLetters[dPenteMove1%19] + "" + (19 - (dPenteMove1/19)) +
                                "-" + coordinateLetters[dPenteMove2%19] + "" + (19 - (dPenteMove2/19)) +
                                "-" + coordinateLetters[dPenteMove3%19] + "" + (19 - (dPenteMove3/19)));
                    } else if (dPenteMove2 > -1) {
                        ((Button) parentLayout.findViewById(R.id.submitButton)).setText("Submit: " + coordinateLetters[dPenteMove1%19] + "" + (19 - (dPenteMove1/19)) +
                                "-" + coordinateLetters[dPenteMove2%19] + "" + (19 - (dPenteMove2/19)) +
                                "-...");
                    } else {
                        ((Button) parentLayout.findViewById(R.id.submitButton)).setText("Submit: " + coordinateLetters[dPenteMove1%19] + "" + (19 - (dPenteMove1/19)) +
                                "-...");
                    }
                } else if (game.isDPente() && game.dPenteChoice && ((LinearLayout) parentLayout.findViewById(R.id.dPenteLayout)).getVisibility()==VISIBLE) {
                    playedMove = -1;
                } else {
                    game.replayGame(abstractBoard, stoneI, stoneJ, this);
                    replayed = false;
                    ((Button) parentLayout.findViewById(R.id.submitButton)).setText("Submit: " + coordinateLetters[stoneI] + "" + (19 - stoneJ));
                }
            }
        }

        invalidate();
        return true;
    }



    private void drawBoard(Canvas canvas) {
        float step = (float) size / 19, margin = step/2;
//        canvas.drawPaint(pentePaint);
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
        if (game!= null) {
            if (game.getMovesList() != null && game.isConnect6()) {
                int i = game.getMovesList().size();
                myColor = (byte) ((((i % 4) == 0) || ((i % 4) == 3)) ? 1 : 2);
                if (connect6Move1 > -1) {
                    byte movej = (byte) (connect6Move1/19);
                    byte movei = (byte) (connect6Move1%19);
                    drawStone(canvas, movei, movej, myColor);
                }
                if (playedMove > -1 && playedMove != connect6Move1) {
                    byte movej = (byte) (playedMove/19);
                    byte movei = (byte) (playedMove%19);
                    drawStone(canvas, movei, movej, myColor);
                }
            }
            if (game.getMovesList() != null && game.isGomoku()) {
                int i = game.getMovesList().size();
                myColor = (byte) (1 + (i%2));
                if (playedMove > -1) {
                    byte movej = (byte) (playedMove/19);
                    byte movei = (byte) (playedMove%19);
                    drawStone(canvas, movei, movej, myColor);
                }
            }
            if (game.isDPente()) {
                if (dPenteMove1 > -1) {
                    byte movej = (byte) (dPenteMove1/19);
                    byte movei = (byte) (dPenteMove1%19);
                    drawStone(canvas, movei, movej, (byte) 2);
                }
                if (dPenteMove2 > -1) {
                    byte movej = (byte) (dPenteMove2/19);
                    byte movei = (byte) (dPenteMove2%19);
                    drawStone(canvas, movei, movej, (byte) 1);
                }
                if (dPenteMove3 > -1) {
                    byte movej = (byte) (dPenteMove3/19);
                    byte movei = (byte) (dPenteMove3%19);
                    drawStone(canvas, movei, movej, (byte) 2);
                }
            }
        }

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
        float shadowOffset = radius/5;
        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        shadowPaint.setAlpha(110);
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
        float shadowOffset = radius/5;
        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        shadowPaint.setAlpha(110);
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
        if (c6RedDot > -1) {
            j = (byte) (c6RedDot/19);
            i = (byte) (c6RedDot%19);
            cx = size*i/19 + size/38;
            cy = size*j/19 + size/38;
            canvas.drawCircle(cx, cy, radius, stonePaint);
        }
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


    public void setBoardActivity(BoardActivity boardActivity) {
        this.boardActivity = boardActivity;
    }



}
