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

import java.util.List;
import java.util.Map;

import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.PrefUtils;

public class LiveBoardView extends View {
    Table table;
    LiveTableFragment fragment;
    private float size;
    private float scaling = 1;
    private float translateX = 0, translateY = 0, stoneX, stoneY;
    public int blackColor = Color.BLACK, whiteColor = Color.WHITE;
    private Paint blackPaint =  makePaint(blackColor), whitePaint = makePaint(whiteColor),
            shadowPaint = makePaint(Color.BLACK);
    private String me;

    private byte myColor = 2, stoneI, stoneJ;
    public int playedMove = -1, redDot = -1;

    private float zoomedScale = 3;
    float offSetX = 0, offSetY = 0;

    private int gridSize = 19;

    public int getGridSize() { return gridSize; }
    public void setGridSize(int gridSize) { this.gridSize = gridSize; }

    private Map<Integer, List<Integer>> goDeadStonesByPlayer, goTerritoryByPlayer;

    public void setGoDeadStonesByPlayer(Map<Integer, List<Integer>> goDeadStonesByPlayer) { this.goDeadStonesByPlayer = goDeadStonesByPlayer; }
    public void setGoTerritoryByPlayer(Map<Integer, List<Integer>> goTerritoryByPlayer) { this.goTerritoryByPlayer = goTerritoryByPlayer; }
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

    public void setTable(Table table) {
        me = (String) PrefUtils.getFromPrefs(MyApplication.getContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, "").toLowerCase();
        this.table = table;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        size = getWidth();
        canvas.scale(scaling, scaling);
        canvas.translate(translateX, translateY);
        if (table != null && table.getMoves().size() > 0) {
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
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                offSetX = x;
                offSetY = y;
                redDot = -1;
                scaling = zoomedScale;
                translateX = -(zoomedScale-1)*offSetX/zoomedScale;
                translateY = -(zoomedScale-1)*offSetY/zoomedScale;
                break;
            case MotionEvent.ACTION_MOVE:
                scaling = zoomedScale;
                translateX = -(zoomedScale-1)*offSetX/zoomedScale - (x-offSetX)/zoomedScale;
                translateY = -(zoomedScale-1)*offSetY/zoomedScale - (y-offSetY)/zoomedScale;
                break;
            case MotionEvent.ACTION_UP:
                scaling = 1;
                translateX = 0;
                translateY = 0;
                break;
        }
        playedMove = -1;
        stoneX = offSetX + 2*(x-offSetX)/zoomedScale;
        stoneJ = (byte) (gridSize*stoneX/size);
        stoneY = offSetY + 2*(y-offSetY)/zoomedScale;
        stoneI = (byte) (gridSize*stoneY/size);
        if (table != null) {
            boolean filled = table.abstractBoard[stoneI][stoneJ] != 0;
            if (table.isGo()) {
                if ((filled && table.getGameState().goState == GoState.MARKSTONES) || (!filled && table.getGameState().goState == GoState.PLAY)) {
                    playedMove = gridSize*stoneI + stoneJ;
                }
            } else if (!filled) {
                playedMove = gridSize*stoneI + stoneJ;
            }
        } else {
            playedMove = -1;
        }
        if (playedMove > -1 && event.getAction() == MotionEvent.ACTION_UP) {
            fragment.getListener().sendEvent("{\"dsgMoveTableEvent\":{\"move\":"+playedMove+",\"moves\":["+playedMove+"],\"player\":\""+me+"\",\"table\":"+table.getId()+",\"time\":0}}");
        }


        invalidate();
        return true;
    }

    public void clearGoStructures() {
        goTerritoryByPlayer = null;
        goDeadStonesByPlayer = null;
        invalidate();
    }


    private void drawBoard(Canvas canvas) {
        float step = (float) size / gridSize, margin = step/2;
        Paint linePaint = blackPaint;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        for ( int i = 0; i < gridSize; i++ ) {
            canvas.drawLine(margin + step*i, margin, margin + step*i, size - margin, linePaint);
            canvas.drawLine(margin, margin + step*i, size - margin, margin + step*i, linePaint);
        }
        if (table != null && table.isGo()) {
            linePaint.setStyle(Paint.Style.FILL);
            float radius = margin/3;
            int l = 3;
            if (gridSize == 9) { l = 2; }
            canvas.drawCircle(margin + l*step, margin + l*step, radius, linePaint);
            canvas.drawCircle( margin + l*step, size - (margin + l*step), radius, linePaint);
            canvas.drawCircle(size - (margin + l*step), margin + l*step, radius, linePaint);
            canvas.drawCircle( size - (margin + l*step), size - (margin + l*step), radius, linePaint);
            canvas.drawCircle( size/2, size/2, radius, linePaint);

            if (l == 3) {
                canvas.drawCircle( margin + 3*step, size/2, radius, linePaint);
                canvas.drawCircle(size/2, margin + 3*step, radius, linePaint);
                canvas.drawCircle( size/2, size - (margin + 3*step), radius, linePaint);
                canvas.drawCircle( size - (margin + 3*step), size/2, radius, linePaint);
            }
        } else {
            canvas.drawCircle(margin + 6*step, margin + 6*step, margin / 2, linePaint);
            canvas.drawCircle(size - (margin + 6*step), margin + 6*step, margin / 2, linePaint);
            canvas.drawCircle( margin + 6*step, size - (margin + 6*step), margin / 2, linePaint);
            canvas.drawCircle(size - (margin + 6*step), size - (margin + 6*step), margin / 2, linePaint);
            canvas.drawCircle(size/2, size/2, margin / 2, linePaint);
        }
        if (table != null) {
            for ( byte i = 0; i < gridSize; i++ ) {
                for ( byte j = 0; j < gridSize; j++ ) {
                    drawStone(canvas, i, j, table.abstractBoard[i][j]);
                }
            }
            setBackgroundColor(table.getGameColor());
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
            for (int move: goTerritoryByPlayer.get(1)) {
                byte movei = (byte) (move/gridSize);
                byte movej = (byte) (move%gridSize);
                drawSquare(canvas, movei, movej, 2);
            }
            for (int move: goTerritoryByPlayer.get(2)) {
                byte movei = (byte) (move/gridSize);
                byte movej = (byte) (move%gridSize);
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
        float radius = size / (gridSize*2);
        float cx = (float) Math.floor(gridSize*x/size)*size/gridSize + size/(2*gridSize), cy = (float) Math.floor(gridSize*y/size)*size/gridSize + size/(2*gridSize);
        float cgx = cx - size/200, cgy = cy - size/200;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.BLACK);
        if (stoneColor == 2) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius*5/4, Color.rgb(125,125,125), Color.BLACK, Shader.TileMode.CLAMP));
        } else if (stoneColor == 1){
            stonePaint.setShader(new RadialGradient(cgx,cgy,
                    radius*5/4, Color.WHITE, Color.rgb(210,210,210), Shader.TileMode.CLAMP));
        } else if (stoneColor == 4) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius*5/4, Color.rgb(125,125,125), Color.BLACK, Shader.TileMode.CLAMP));
            stonePaint.setAlpha(180);
        } else if (stoneColor == 3){
            stonePaint.setShader(new RadialGradient(cgx,cgy,
                    radius*5/4, Color.WHITE, Color.rgb(210,210,210), Shader.TileMode.CLAMP));
            stonePaint.setAlpha(180);
        }

        if (stoneColor < 3) {
            float shadowOffset = radius/5;
            shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            shadowPaint.setAlpha(110);
            canvas.drawCircle(cx+shadowOffset, cy+shadowOffset, radius, shadowPaint);
        }
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }
    private void drawZoomedStone(Canvas canvas, float x, float y, byte stoneColor) {
        float radius = size *2/(3*gridSize);
        float cx = (float) Math.floor(gridSize*x/size)*size/gridSize + size/(2*gridSize), cy = (float) Math.floor(gridSize*y/size)*size/gridSize + size/(2*gridSize);
        float cgx = cx - size/200, cgy = cy - size/200;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.BLACK);
        if (stoneColor == 2) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius*5/4, Color.rgb(125,125,125), Color.BLACK, Shader.TileMode.CLAMP));
        } else if (stoneColor == 1) {
            stonePaint.setShader(new RadialGradient(cgx,cgy,
                    radius*5/4, Color.WHITE, Color.rgb(210,210,210), Shader.TileMode.CLAMP));
        } else if (stoneColor == 3) {
            stonePaint.setShader(new RadialGradient(cgx, cgy,
                    radius*5/4, Color.rgb(250,125,125), Color.RED, Shader.TileMode.CLAMP));
        }
        float shadowOffset = radius/5;
        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        shadowPaint.setAlpha(110);
        canvas.drawCircle(cx+shadowOffset, cy+shadowOffset, radius, shadowPaint);
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }
    private void drawZoomedLine(Canvas canvas, float x, float y) {
        float radius = size*2 / (3*gridSize);
        float cx = (float) Math.floor(gridSize*x/size)*size/gridSize + size/(2*gridSize), cy = (float) Math.floor(gridSize*y/size)*size/gridSize + size/(2*gridSize);
        Paint linePaint;
        linePaint = new Paint();
        linePaint.setStrokeWidth(4);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.WHITE);
        canvas.drawLine(0,cy,size,cy, linePaint);
        canvas.drawLine(cx,0,cx,size, linePaint);
    }
    private void drawStone(Canvas canvas, byte i, byte j, byte stoneColor) {
        drawStone(canvas, size*j/gridSize + size/(2*gridSize), size*i/gridSize + size/(2*gridSize), stoneColor);
    }
    private void drawSquare(Canvas canvas, byte i, byte j, int stoneColor) {
        drawSquare(canvas, size*j/gridSize + size/(2*gridSize), size*i/gridSize + size/(2*gridSize), stoneColor);
    }
    private void drawSquare(Canvas canvas, float x, float y, int stoneColor) {
        if (stoneColor < 1) {
            return;
        }
        float width = size / (3*gridSize);
        float cx = (float) Math.floor(gridSize*x/size)*size/gridSize + size/(2*gridSize) - width/2, cy = (float) Math.floor(gridSize*y/size)*size/gridSize + size/(2*gridSize) - width/2;
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        if (stoneColor == 1) {
            stonePaint.setColor(Color.WHITE);
        } else if (stoneColor == 2) {
            stonePaint.setColor(Color.BLACK);
        }
        canvas.drawRect(cx,cy,cx+width,cy+width, stonePaint);
    }

    private void drawRedDot(Canvas canvas) {
        Paint stonePaint;
        stonePaint = new Paint();
        stonePaint.setStrokeWidth(1);
        stonePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stonePaint.setColor(Color.RED);
        float radius = size / (5*gridSize);
        byte i = (byte) (redDot/gridSize);
        byte j = (byte) (redDot%gridSize);
        float cx = size*j/gridSize + size/(2*gridSize), cy = size*i/gridSize + size/(2*gridSize);
        canvas.drawCircle(cx, cy, radius, stonePaint);
    }


    private Paint makePaint(int color) {
        Paint p = new Paint();
        p.setColor(color);
        return(p);
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
