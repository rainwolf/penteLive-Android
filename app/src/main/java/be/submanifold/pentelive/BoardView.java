package be.submanifold.pentelive;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import androidx.appcompat.widget.Toolbar;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
            boatPenteColor = Color.parseColor("#25BAFF"), dkeryoColor = Color.parseColor("#FFA500"),
            goColor = Color.parseColor("#FAC832"), oPenteColor = Color.parseColor("#52be80"),
            swap2PenteColor = Color.parseColor("#E5AA70"), swap2KeryoColor = Color.parseColor("#50C878"),
            renjuColor = Color.parseColor("#D98880");
    private final Paint blackPaint = makePaint(blackColor);
    private final Paint shadowPaint = makePaint(Color.BLACK);

    private float size;
    private float scaling = 1;
    private float translateX = 0, translateY = 0, stoneX, stoneY;
    private byte myColor, stoneI, stoneJ;
    public int playedMove = -1;
    public int gridSize = 19;
    public int renjuBoxRadius = 0; // 0 = no constraint; >0 limits placement to the central (2r+1)^2 box
    public java.util.List<Integer> renjuCandidates = null; // move indices to render as translucent black (value 4)
    public java.util.List<Integer> renjuPicks = null; // in-progress OFFERS multi-select (whole board)
    public boolean renjuOfferMode = false; // collecting 1 (Branch A) or up to 10 (Branch B) candidate stones -> single `move`
    public java.util.List<Integer> renjuSelection = null; // SELECTION 2-tap: [offered black 5th, white 6th]

    public int redDot = -1;


    public int c6RedDot = -1;
    public int connect6Move1 = -1;
    public int dPenteMove1 = -1;
    public int dPenteMove2 = -1;
    public int dPenteMove3 = -1;
    public int dPenteMove4 = -1;
    public boolean dPenteChosen = false;
    public int swap2Move1 = -1;
    public int swap2Move2 = -1;
    public int swap2Move3 = -1;
    public boolean swap2WillPass = false;
    public boolean swap2Chosen = false;
    public boolean renjuChosen = false; // true once the SWAP prompt has been answered (don't re-show)

    private boolean replayed = false;
    private final char[] coordinateLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};

    private Game game;

    private TextView textView = null;

    private final String submitStr;
    private final String passStr;

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
        submitStr = context.getString(R.string.submit);
        passStr = context.getString(R.string.pass);
//        mDetector = new GestureDetectorCompat(context, new MyGestureListener());
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        size = getWidth();
        canvas.scale(scaling, scaling);
        canvas.translate(translateX, translateY);
        drawBoard(canvas);
        if (game != null && game.isRenju() && "MOVE".equals(game.renjuPhase)) {
            renjuBoxRadius = 4;
        }
        if (game != null) {
            RelativeLayout parentLayout = (RelativeLayout) this.getParent();
            ((Toolbar) parentLayout.findViewById(R.id.toolbar)).setTitle(game.getGameType());
            if (!game.isConnect6() && !game.isGomoku()) {
                ((Toolbar) parentLayout.findViewById(R.id.toolbar)).setSubtitle("\u2B24 x " + game.getState().blackCaptures + " - \u25EF x " + game.getState().whiteCaptures);
            }
            if (scaling == 1) {
                if (textView == null) {
                    textView = parentLayout.findViewById(R.id.playerInfo);
                }
                String str = game.getBoardString();
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

//            System.out.println("kitten here " +game.dPenteChoice + " " + dPenteChosen+ " " + game.isActive());
            if (game.isDPente() && game.dPenteChoice && game.isActive() && !dPenteChosen) {
                parentLayout.findViewById(R.id.dPenteLayout).setVisibility(VISIBLE);
                parentLayout.findViewById(R.id.submitLayout).setVisibility(INVISIBLE);
                return;
            }
            if (game.isSwap2() && game.swap2Choice && game.isActive() && !swap2Chosen) {
                parentLayout.findViewById(R.id.dPenteLayout).setVisibility(VISIBLE);
                if (game.getMovesList().size() == 3) {
                    parentLayout.findViewById(R.id.swap2PassButton).setVisibility(VISIBLE);
                } else {
                    parentLayout.findViewById(R.id.swap2PassButton).setVisibility(GONE);
                }
                parentLayout.findViewById(R.id.submitLayout).setVisibility(INVISIBLE);
                return;
            }
            if (game.isRenju() && game.isActive() && "SWAP".equals(game.renjuPhase) && !renjuChosen) {
                parentLayout.findViewById(R.id.dPenteLayout).setVisibility(VISIBLE);
                parentLayout.findViewById(R.id.submitLayout).setVisibility(INVISIBLE);
                parentLayout.findViewById(R.id.swap2PassButton).setVisibility(GONE);
                // Decision phase: buttons only — drop the descriptive "play as" label so the
                // two choice buttons fill the row (GONE, not INVISIBLE, leaves no gap). The
                // board-lock (padlock) lives in the top toolbar (R.id.action_lock), so this
                // bottom button row is already clear of it; no repositioning needed.
                parentLayout.findViewById(R.id.playAsLabel).setVisibility(GONE);
                ((Button) parentLayout.findViewById(R.id.playAsWhiteButton)).setText(getContext().getString(R.string.renju_swap_take_over));
                ((Button) parentLayout.findViewById(R.id.playAsBlackButton)).setText(getContext().getString(R.string.renju_dont_swap));
                return;
            }
            // NB: renjuOfferMode for the post-take-over BRANCH phase is set in the
            // data-refresh path (Game.parseGame), not here. Writing it from onDraw would
            // revive the offer UI on every invalidate() — e.g. the STAYWITHGAME post-submit
            // re-parse, where the phase still reads "BRANCH" until the server responds,
            // opening a double-submit window. onDraw only READS the flag.
            if (game.isRenju() && game.isActive() && renjuOfferMode) {
                renjuBoxRadius = 0;
                renjuCandidates = renjuPicks;
                parentLayout.findViewById(R.id.dPenteLayout).setVisibility(INVISIBLE);
                parentLayout.findViewById(R.id.submitLayout).setVisibility(VISIBLE);
            }
            if (game.isRenju() && game.isActive() && "SELECTION".equals(game.renjuPhase)) {
                renjuBoxRadius = 0;
                if (renjuSelection == null || renjuSelection.isEmpty()) {
                    // black-5th pick: lock the board to the 10 offers (mask every other empty
                    // cell -1 so only an offer can show a stone) and draw the offers as the
                    // existing translucent dead-stone candidates. Idempotent per frame.
                    applyRenjuSelectionMask();
                    java.util.List<Integer> cands = new java.util.ArrayList<>();
                    if (game.renjuOffers != null) for (int o : game.renjuOffers) cands.add(o);
                    renjuCandidates = cands;
                } else {
                    // 5th chosen (mask already cleared on the pick): drop the 9 other candidates;
                    // the chosen 5th (and the white 6th) render as live stones from the snapshot.
                    renjuCandidates = null;
                }
                parentLayout.findViewById(R.id.dPenteLayout).setVisibility(INVISIBLE);
                parentLayout.findViewById(R.id.submitLayout).setVisibility(VISIBLE);
            }
            // Renju opening: drive the submit button as the greyed-until-valid feedback surface
            // for every active non-decision phase (offer collection, SELECTION, single placement).
            // The SWAP/BRANCH decision block above returns early while the choice buttons are up,
            // so reaching here implies a non-decision phase — styling here never greys a decision.
            if (game.isRenju() && game.isActive()) {
                styleRenjuSubmit();
            }
            if (!game.isActive()) {
                return;
            }
        }
        if (playedMove == -1) {
            return;
        }

        if (game != null && game.getMovesList() != null && game.isConnect6()) {
            int i = game.getMovesList().size();
            myColor = (byte) ((((i % 4) == 0) || ((i % 4) == 3)) ? 1 : 2);
        } else if (game != null && game.isDPente() && game.getMovesList().isEmpty()) {
            if (dPenteMove4 > -1) {
                myColor = (byte) 2;
            } else if (dPenteMove3 > -1) {
                myColor = (byte) 2;
            } else if (dPenteMove2 > -1) {
                myColor = (byte) 1;
            } else if (dPenteMove1 > -1) {
                myColor = (byte) 2;
            } else {
                myColor = (byte) 1;
            }
        } else if (game != null && game.isSwap2()) {
            if (game.getMovesList().isEmpty()) {
                if (swap2Move3 > -1) {
                    myColor = (byte) 1;
                } else if (swap2Move2 > -1) {
                    myColor = (byte) 1;
                } else if (swap2Move1 > -1) {
                    myColor = (byte) 2;
                } else {
                    myColor = (byte) 1;
                }
            } else if (swap2WillPass && game.getMovesList().size() == 3) {
                if (swap2Move2 > -1) {
                    myColor = (byte) 1;
                } else if (swap2Move1 > -1) {
                    myColor = (byte) 1;
                } else {
                    myColor = (byte) 2;
                }
            } else {
                myColor = (byte) (game.getMovesList().size() % 2 + 1);
            }
        } else if (game != null && game.isGo() && game.getMovesList() != null) {
            if (game.isGoMarkStones()) {
                myColor = 3;
            } else {
                myColor = (byte) (2 - game.getMovesList().size() % 2);
            }
        } else if (game != null && game.getMovesList() != null) {
            myColor = (byte) (game.getMovesList().size() % 2 + 1);
        } else {
            myColor = 1;
        }
        if (scaling == 2 && playedMove != -1) {
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
        stoneJ = (byte) (gridSize * stoneX / size);
        stoneY = y;
        stoneI = (byte) (gridSize * stoneY / size);
        if (stoneI >= gridSize || stoneJ >= gridSize) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!replayed && !game.isGoMarkStones()) {
                    if (game.isSwap2() && game.getMovesList().size() == 3 && swap2WillPass && swap2Move1 > -1) {
                        byte swap2Move1i = (byte) (swap2Move1 / 19), swap2Move1j = (byte) (swap2Move1 % 19);
                        game.replayGame(swap2Move1i, swap2Move1j, this, (byte) 255, (byte) 255);
                    } else {
                        game.replayGame(BoardView.this);
                    }
                    replayed = true;
                }
                if (game != null) {
                    if (game.isDPente() && game.getMovesList().isEmpty()) {
                        dPenteMove4 = -1;
                    }
                    if (game.isSwap2()) {
                        if (game.getMovesList().isEmpty()) {
                            swap2Move3 = -1;
                        } else if (game.getMovesList().size() == 3) {
                            swap2Move2 = -1;
                        }
                    }
                }
                scaling = 2;
                translateX = -x / 2;
                translateY = -y / 2;
                break;
            case MotionEvent.ACTION_MOVE:
                scaling = 2;
                translateX = -x / 2;
                translateY = -y / 2;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                scaling = 1;
                translateX = 0;
                translateY = 0;
                break;
        }

        if (game != null && game.isRenju() && game.isActive() && renjuOfferMode) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int m = gridSize * stoneI + stoneJ;
                if (renjuPicks == null) renjuPicks = new java.util.ArrayList<>();
                if (game.getState().cell(stoneI, stoneJ) != 0) {
                    // occupied: ignore
                } else if (renjuPicks.contains(m)) {
                    renjuPicks.remove((Integer) m); // tap again to deselect
                } else {
                    int[] accepted = new int[renjuPicks.size()];
                    for (int k = 0; k < renjuPicks.size(); k++) accepted[k] = renjuPicks.get(k);
                    if (renjuPicks.size() >= 1 && be.submanifold.pente.rules.RenjuSymmetry.isSymmetricDup(m, accepted)) {
                        // symmetric duplicate: reject (ignore)
                    } else if (renjuPicks.size() < 10) {
                        renjuPicks.add(m);
                    }
                }
            }
            playedMove = -1; // not a single placement
            renjuCandidates = renjuPicks; // alias now so the first pick renders this frame (no one-frame lag)
            invalidate();
            return true; // consume; skip normal placement
        }
        if (game != null && game.isRenju() && game.isActive() && "SELECTION".equals(game.renjuPhase)) {
            // Two taps: 1st = the offered black 5th move; 2nd = an empty, distinct white 6th.
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int m = gridSize * stoneI + stoneJ;
                if (renjuSelection == null) renjuSelection = new java.util.ArrayList<>();
                boolean isOffer = false;
                if (game.renjuOffers != null) for (int o : game.renjuOffers) if (o == m) { isOffer = true; break; }
                if (renjuSelection.size() >= 2) {
                    // third tap resets the pair: undo the provisional 5th/6th stones; the next
                    // onDraw re-masks to the offers and re-shows them for a fresh black-5th pick.
                    for (int p : renjuSelection) game.getState().board[p / gridSize][p % gridSize] = 0;
                    renjuSelection.clear();
                } else if (renjuSelection.isEmpty()) {
                    if (isOffer) {
                        // black 5th: unmask the board (so the 6th may land on ANY empty cell) and
                        // render the chosen offer as a live black stone (2 = black in BoardState).
                        clearRenjuSelectionMask();
                        renjuSelection.add(m);
                        game.getState().board[stoneI][stoneJ] = 2;
                    }
                } else if (game.getState().cell(stoneI, stoneJ) == 0 && m != renjuSelection.get(0)) {
                    // white 6th: empty & distinct from the 5th; render as a live white stone.
                    renjuSelection.add(m);
                    game.getState().board[stoneI][stoneJ] = 1;
                }
            }
            playedMove = -1; // not a single placement
            renjuBoxRadius = 0;
            invalidate();
            return true; // consume; skip normal placement
        }

        String str;
        playedMove = gridSize * stoneI + stoneJ;
        if (game.isActive()) {
            if (game.isGoMarkStones()) {
                if (!(game.getState().cell(stoneI, stoneJ) != 0 || game.getGoDeadStonesByPlayer().get(1).contains(playedMove) || game.getGoDeadStonesByPlayer().get(2).contains(playedMove))) {
                    playedMove = -1;
                }
            } else if (game.getState().cell(stoneI, stoneJ) != 0 || playedMove == game.koMove ||
                    playedMove == dPenteMove1 || playedMove == dPenteMove2 ||
                    playedMove == dPenteMove3 ||
                    playedMove == swap2Move1 ||
                    (playedMove == swap2Move2 && game.getMovesList().isEmpty())) {
                playedMove = -1;
            }
        } else {
            playedMove = -1;
        }
        if (game.isRenju() && renjuBoxRadius > 0 && playedMove != -1 &&
                (Math.abs(stoneI - 7) > renjuBoxRadius || Math.abs(stoneJ - 7) > renjuBoxRadius)) {
            playedMove = -1;
        }
        if (scaling == 1) {
            if (game.isConnect6() && connect6Move1 == -1) {
                connect6Move1 = playedMove;
            }
            if (game.isDPente() && game.getMovesList().isEmpty()) {
                if (dPenteMove1 == -1) {
                    dPenteMove1 = playedMove;
                } else if (dPenteMove2 == -1 && playedMove != dPenteMove1) {
                    dPenteMove2 = playedMove;
                } else if (dPenteMove3 == -1 && playedMove != dPenteMove1 && playedMove != dPenteMove2) {
                    dPenteMove3 = playedMove;
                } else if (playedMove != dPenteMove1 && playedMove != dPenteMove2 && playedMove != dPenteMove3) {
                    dPenteMove4 = playedMove;
                }
            } else if (game.isSwap2() && game.getMovesList().isEmpty()) {
                if (swap2Move1 == -1) {
                    swap2Move1 = playedMove;
                } else if (swap2Move2 == -1 && playedMove != swap2Move1) {
                    swap2Move2 = playedMove;
                } else if (playedMove != swap2Move1 && playedMove != swap2Move2) {
                    swap2Move3 = playedMove;
                }
            } else if (game.isSwap2() && game.getMovesList().size() == 3 && swap2WillPass) {
                if (swap2Move1 == -1) {
                    swap2Move1 = playedMove;
                } else if (playedMove != swap2Move1) {
                    swap2Move2 = playedMove;
                }
            }
            RelativeLayout parentLayout = (RelativeLayout) this.getParent();
            if (!game.isGoMarkStones() && (playedMove == -1 || game.getState().cell(stoneI, stoneJ) != 0)) {
                if (game.isGo()) {
                    ((Button) parentLayout.findViewById(R.id.submitButton)).setText(passStr);
                } else {
                    ((Button) parentLayout.findViewById(R.id.submitButton)).setText(submitStr);
                }
                if (!replayed && game.getMovesList() != null) {
//                    System.out.println(" fuck kitty");
                    game.replayGame(this);
                    replayed = true;
                }
                if (game.isConnect6() && connect6Move1 > -1) {
                    str = submitStr + ": " + coordinateLetters[connect6Move1 % gridSize] + "" + (gridSize - (connect6Move1 / gridSize)) +
                            "-...";
                    ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                }
            } else if (!game.isGoMarkStones() && playedMove > -1) {
                if (game.isConnect6()) {
                    if (connect6Move1 > -1) {
                        if (playedMove > -1 && playedMove != connect6Move1) {
                            str = submitStr + ": " + coordinateLetters[connect6Move1 % gridSize] + "" + (gridSize - (connect6Move1 / gridSize)) +
                                    "-" + coordinateLetters[stoneJ] + "" + (gridSize - stoneI);
                            ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                        } else {
                            str = submitStr + ": " + coordinateLetters[stoneJ] + "" + (gridSize - stoneI) +
                                    "-...";
                            ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                        }
                    }
                } else if (game.isDPente() && game.getMovesList().isEmpty()) {
                    if (dPenteMove4 > -1) {
                        str = submitStr + ": " + coordinateLetters[dPenteMove1 % gridSize] + "" + (gridSize - (dPenteMove1 / gridSize)) +
                                "-" + coordinateLetters[dPenteMove2 % gridSize] + "" + (gridSize - (dPenteMove2 / gridSize)) +
                                "-" + coordinateLetters[dPenteMove3 % gridSize] + "" + (gridSize - (dPenteMove3 / gridSize)) +
                                "-" + coordinateLetters[dPenteMove4 % gridSize] + "" + (gridSize - (dPenteMove4 / gridSize));
                        ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                    } else if (dPenteMove3 > -1) {
                        str = submitStr + ": " + coordinateLetters[dPenteMove1 % gridSize] + "" + (gridSize - (dPenteMove1 / gridSize)) +
                                "-" + coordinateLetters[dPenteMove2 % gridSize] + "" + (gridSize - (dPenteMove2 / gridSize)) +
                                "-" + coordinateLetters[dPenteMove3 % gridSize] + "" + (gridSize - (dPenteMove3 / gridSize)) +
                                "-...";
                        ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                    } else if (dPenteMove2 > -1) {
                        str = submitStr + ": " + coordinateLetters[dPenteMove1 % gridSize] + "" + (gridSize - (dPenteMove1 / gridSize)) +
                                "-" + coordinateLetters[dPenteMove2 % gridSize] + "" + (gridSize - (dPenteMove2 / gridSize)) +
                                "-...";
                        ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                    } else {
                        str = submitStr + ": " + coordinateLetters[dPenteMove1 % gridSize] + "" + (gridSize - (dPenteMove1 / gridSize)) +
                                "-...";
                        ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                    }
                } else if (game.isSwap2() && game.getMovesList().isEmpty()) {
                    if (swap2Move3 > -1) {
                        str = submitStr + ": " + coordinateLetters[swap2Move1 % gridSize] + "" + (gridSize - (swap2Move1 / gridSize)) +
                                "-" + coordinateLetters[swap2Move2 % gridSize] + "" + (gridSize - (swap2Move2 / gridSize)) +
                                "-" + coordinateLetters[swap2Move3 % gridSize] + "" + (gridSize - (swap2Move3 / gridSize));
                        ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                    } else if (swap2Move2 > -1) {
                        str = submitStr + ": " + coordinateLetters[swap2Move1 % gridSize] + "" + (gridSize - (swap2Move1 / gridSize)) +
                                "-" + coordinateLetters[swap2Move2 % gridSize] + "" + (gridSize - (swap2Move2 / gridSize)) +
                                "-...";
                        ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                    } else {
                        str = submitStr + ": " + coordinateLetters[swap2Move1 % gridSize] + "" + (gridSize - (swap2Move1 / gridSize)) +
                                "-...";
                        ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                    }
                } else if (game.isSwap2() && game.getMovesList().size() == 3 && swap2WillPass) {
                    if (swap2Move2 > -1) {
                        str = submitStr + ": " + coordinateLetters[swap2Move1 % gridSize] + "" + (gridSize - (swap2Move1 / gridSize)) +
                                "-" + coordinateLetters[swap2Move2 % gridSize] + "" + (gridSize - (swap2Move2 / gridSize));
                        byte swap2Move1i = (byte) (swap2Move1 / 19), swap2Move1j = (byte) (swap2Move1 % 19);
                        game.replayGame(stoneI, stoneJ, this, swap2Move1i, swap2Move1j);
                    } else {
                        str = submitStr + ": " + coordinateLetters[swap2Move1 % gridSize] + "" + (gridSize - (swap2Move1 / gridSize)) +
                                "-...";
                        game.replayGame(stoneI, stoneJ, this, (byte) 255, (byte) 255);
                    }
                    ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                    replayed = false;
                } else if (game.isDPente() && game.dPenteChoice && parentLayout.findViewById(R.id.dPenteLayout).getVisibility() == VISIBLE) {
                    playedMove = -1;
                } else if (game.isSwap2() && game.swap2Choice && parentLayout.findViewById(R.id.dPenteLayout).getVisibility() == VISIBLE) {
                    playedMove = -1;
                } else {
                    game.replayGame(stoneI, stoneJ, this, (byte) 255, (byte) 255);
                    replayed = false;
                    str = submitStr + ": " + coordinateLetters[stoneJ] + "" + (gridSize - stoneI);
                    ((Button) parentLayout.findViewById(R.id.submitButton)).setText(str);
                }
            } else if (game.isGoMarkStones() && playedMove > -1) {
                game.processDeadStone(playedMove);
            }
        }

        invalidate();
        return true;
    }


    // SELECTION: lock the board to the 10 offered cells (black 5th). Mark every EMPTY non-offer
    // cell as -1 ("forbidden" in BoardState's cell encoding; drawStone renders nothing for <1),
    // the same -1 masking idiom used elsewhere in the app (Table/DBBoardView). The 10 offers stay
    // 0 and are drawn as translucent candidates via renjuCandidates. No-op when there are no
    // offers, so we never lock the whole board. Writes only the rendered snapshot
    // (game.getState().board), not the engine's abstractBoard, and is rebuilt on the next replay.
    private void applyRenjuSelectionMask() {
        if (game == null || game.renjuOffers == null || game.renjuOffers.length == 0) {
            return;
        }
        java.util.Set<Integer> offers = new java.util.HashSet<>();
        for (int o : game.renjuOffers) offers.add(o);
        byte[][] b = game.getState().board;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (b[i][j] == 0 && !offers.contains(gridSize * i + j)) {
                    b[i][j] = -1;
                }
            }
        }
    }

    // Restore every masked (-1) cell to empty (0). The engine never writes -1 into the main
    // snapshot, so the only -1 marks come from applyRenjuSelectionMask — clearing them all is
    // safe. Called once the black 5th is picked so the white 6th may land on ANY empty cell.
    private void clearRenjuSelectionMask() {
        if (game == null) {
            return;
        }
        byte[][] b = game.getState().board;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (b[i][j] == -1) {
                    b[i][j] = 0;
                }
            }
        }
    }

    // Renju opening: drive the submit button as a greyed-until-valid feedback surface, matching
    // iOS. Called from onDraw for every active non-decision renju phase (the SWAP/BRANCH decision
    // block returns earlier while the choice buttons are up, so we never grey those). Submit stays
    // VISIBLE but disabled (alpha 0.5) until the current input is a complete, valid move, then
    // enabled (alpha 1); its text reflects exactly what will be submitted.
    private void styleRenjuSubmit() {
        RelativeLayout parentLayout = (RelativeLayout) this.getParent();
        Button submit = parentLayout.findViewById(R.id.submitButton);
        if (submit == null) {
            return;
        }
        if (renjuOfferMode) {
            // move-4 decline / post-take-over BRANCH: collect 1 (Branch A) or 10 (Branch B) stones,
            // submitted as one `move`; the server infers the branch from the count. Branch A is a
            // single centre stone, so it is enabled at n==1 (otherwise it would be unsubmittable on
            // Android, which unlike iOS has no separate Branch-A/B buttons).
            int n = (renjuPicks == null) ? 0 : renjuPicks.size();
            int c = gridSize / 2;
            if (n == 1) {
                int m = renjuPicks.get(0);
                boolean inBox = Math.abs(m % gridSize - c) <= 4 && Math.abs(m / gridSize - c) <= 4;
                if (inBox) {
                    setSubmitEnabled(submit, true);
                    submit.setText(submitStr + ": " + renjuCoord(m));
                    return;
                }
            } else if (n == 10) {
                int[] arr = new int[n];
                for (int k = 0; k < n; k++) arr[k] = renjuPicks.get(k);
                if (be.submanifold.pente.rules.RenjuSymmetry.isValidOfferSet(arr)) {
                    setSubmitEnabled(submit, true);
                    submit.setText(submitStr + " 10/10");
                    return;
                }
            }
            // still building toward ten (or an as-yet-incomplete count): greyed running count.
            setSubmitEnabled(submit, false);
            submit.setText(submitStr + " " + n + "/10");
            return;
        }
        if ("SELECTION".equals(game.renjuPhase)) {
            // 2-tap: black 5th then white 6th. Enabled only once both are chosen.
            int n = (renjuSelection == null) ? 0 : renjuSelection.size();
            if (n >= 2) {
                setSubmitEnabled(submit, true);
                submit.setText(submitStr + ": " + renjuCoord(renjuSelection.get(0)) + "-" + renjuCoord(renjuSelection.get(1)));
            } else if (n == 1) {
                setSubmitEnabled(submit, false);
                submit.setText(submitStr + ": " + renjuCoord(renjuSelection.get(0)) + "-");
            } else {
                setSubmitEnabled(submit, false);
                submit.setText(submitStr);
            }
            return;
        }
        // single-stone placement (SWAP windows 1-3 decline+place, MOVE): greyed until a legal stone
        // is placed (playedMove is clamped to the legal box in onTouchEvent), then enabled.
        if (playedMove > -1) {
            setSubmitEnabled(submit, true);
            submit.setText(submitStr + ": " + renjuCoord(playedMove));
        } else {
            setSubmitEnabled(submit, false);
            submit.setText(submitStr);
        }
    }

    // Greyed-submit idiom, introduced for the Renju opening only: dim + disable, or restore + enable.
    private void setSubmitEnabled(Button submit, boolean enabled) {
        submit.setEnabled(enabled);
        submit.setAlpha(enabled ? 1f : 0.5f);
    }

    // Move index -> board coordinate (e.g. "H8"). Column letter from coordinateLetters (which skips
    // 'I') indexed by move % gridSize; row = gridSize - move/gridSize. Matches the move-list
    // formatter in Game.getBoardString (column = move % gridSize, row = gridSize - move/gridSize).
    private String renjuCoord(int move) {
        return "" + coordinateLetters[move % gridSize] + (gridSize - (move / gridSize));
    }

    private void drawBoard(Canvas canvas) {
        float step = size / gridSize, margin = step / 2;
//        canvas.drawPaint(pentePaint);
        Paint linePaint = blackPaint;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        for (int i = 0; i < gridSize; i++) {
            canvas.drawLine(margin + step * i, margin, margin + step * i, size - margin, linePaint);
            canvas.drawLine(margin, margin + step * i, size - margin, margin + step * i, linePaint);
        }
        if (game != null && game.isGo()) {
            linePaint.setStyle(Paint.Style.FILL);
            float radius = margin / 3;
            int l = 3;
            if (game.getGridSize() == 9) {
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
        } else if (game != null && game.isRenju()) {
            linePaint.setStyle(Paint.Style.FILL);
            float r = margin / 2;
            canvas.drawCircle(margin + 3 * step, margin + 3 * step, r, linePaint);
            canvas.drawCircle(size - (margin + 3 * step), margin + 3 * step, r, linePaint);
            canvas.drawCircle(margin + 3 * step, size - (margin + 3 * step), r, linePaint);
            canvas.drawCircle(size - (margin + 3 * step), size - (margin + 3 * step), r, linePaint);
            canvas.drawCircle(size / 2, size / 2, r, linePaint);
        } else {
            canvas.drawCircle(margin + 6 * step, margin + 6 * step, margin / 2, linePaint);
            canvas.drawCircle(size - (margin + 6 * step), margin + 6 * step, margin / 2, linePaint);
            canvas.drawCircle(margin + 6 * step, size - (margin + 6 * step), margin / 2, linePaint);
            canvas.drawCircle(size - (margin + 6 * step), size - (margin + 6 * step), margin / 2, linePaint);
            canvas.drawCircle(size / 2, size / 2, margin / 2, linePaint);
        }
        // Renju central-box highlight: shade the legal (2r+1)x(2r+1) cells under the stones.
        if (game != null && game.isRenju() && renjuBoxRadius > 0) {
            Paint boxPaint = new Paint();
            boxPaint.setColor(Color.parseColor("#3300FF00")); // translucent green
            boxPaint.setStyle(Paint.Style.FILL);
            int lo = 7 - renjuBoxRadius, hi = 7 + renjuBoxRadius;
            // Cell center = margin + index*step (margin = step/2); box spans half a step beyond lo/hi.
            float left = margin + (lo - 0.5f) * step;
            float top = margin + (lo - 0.5f) * step;
            float right = margin + (hi + 0.5f) * step;
            float bottom = margin + (hi + 0.5f) * step;
            canvas.drawRect(left, top, right, bottom, boxPaint);
        }
        byte[][] board = game.getState().board;
        for (byte i = 0; i < gridSize; i++) {
            for (byte j = 0; j < gridSize; j++) {
                drawStone(canvas, i, j, board[i][j]);
            }
        }
        // Renju candidate moves: translucent black preview stones drawn on top of the board.
        if (game != null && game.isRenju() && renjuCandidates != null) {
            for (int m : renjuCandidates) {
                int ci = m / gridSize, cj = m % gridSize; // ci = row, cj = col
                // Mirror the stone-draw mapping: x uses col, y uses row, center = margin + index*step.
                float cx = margin + cj * step;
                float cy = margin + ci * step;
                drawStone(canvas, cx, cy, (byte) 4); // translucent black
            }
        }
        drawRedDot(canvas);
        if (game != null) {
            if (game.isGo()) {
                for (int move : game.getGoDeadStonesByPlayer().get(1)) {
                    byte movei = (byte) (move / gridSize);
                    byte movej = (byte) (move % gridSize);
                    drawStone(canvas, movei, movej, (byte) 4);
                }
                for (int move : game.getGoDeadStonesByPlayer().get(2)) {
                    byte movei = (byte) (move / gridSize);
                    byte movej = (byte) (move % gridSize);
                    drawStone(canvas, movei, movej, (byte) 3);
                }
                for (int move : game.getGoTerritoryByPlayer().get(1)) {
                    byte movei = (byte) (move / gridSize);
                    byte movej = (byte) (move % gridSize);
                    drawSquare(canvas, movei, movej, 2);
                }
                for (int move : game.getGoTerritoryByPlayer().get(2)) {
                    byte movei = (byte) (move / gridSize);
                    byte movej = (byte) (move % gridSize);
                    drawSquare(canvas, movei, movej, 1);
                }
            }
            if (game.getMovesList() != null && game.isConnect6()) {
                int i = game.getMovesList().size();
                myColor = (byte) ((((i % 4) == 0) || ((i % 4) == 3)) ? 1 : 2);
                if (connect6Move1 > -1) {
                    byte movei = (byte) (connect6Move1 / 19);
                    byte movej = (byte) (connect6Move1 % 19);
                    drawStone(canvas, movei, movej, myColor);
                }
                if (playedMove > -1 && playedMove != connect6Move1) {
                    byte movei = (byte) (playedMove / 19);
                    byte movej = (byte) (playedMove % 19);
                    drawStone(canvas, movei, movej, myColor);
                }
            }
            if (game.getMovesList() != null && game.isGomoku()) {
                int i = game.getMovesList().size();
                myColor = (byte) (1 + (i % 2));
                if (playedMove > -1) {
                    byte movei = (byte) (playedMove / 19);
                    byte movej = (byte) (playedMove % 19);
                    drawStone(canvas, movei, movej, myColor);
                }
            }
            if (game.isDPente()) {
                if (dPenteMove2 > -1) {
                    byte movei = (byte) (dPenteMove2 / 19);
                    byte movej = (byte) (dPenteMove2 % 19);
                    drawStone(canvas, movei, movej, (byte) 2);
                }
                if (scaling == 1 && dPenteMove4 > -1) {
                    byte movei = (byte) (dPenteMove4 / 19);
                    byte movej = (byte) (dPenteMove4 % 19);
                    drawStone(canvas, movei, movej, (byte) 2);
                    if (!checkDPenteCapture()) {
                        if (dPenteMove1 > -1) {
                            movei = (byte) (dPenteMove1 / 19);
                            movej = (byte) (dPenteMove1 % 19);
                            drawStone(canvas, movei, movej, (byte) 1);
                        }
                        if (dPenteMove3 > -1) {
                            movei = (byte) (dPenteMove3 / 19);
                            movej = (byte) (dPenteMove3 % 19);
                            drawStone(canvas, movei, movej, (byte) 1);
                        }
                    }
                } else {
                    if (dPenteMove1 > -1) {
                        byte movei = (byte) (dPenteMove1 / 19);
                        byte movej = (byte) (dPenteMove1 % 19);
                        drawStone(canvas, movei, movej, (byte) 1);
                    }
                    if (dPenteMove3 > -1) {
                        byte movei = (byte) (dPenteMove3 / 19);
                        byte movej = (byte) (dPenteMove3 % 19);
                        drawStone(canvas, movei, movej, (byte) 1);
                    }
                }
            }
            if (game.isSwap2()) {
                if (game.getMovesList() != null && game.getMovesList().isEmpty()) {
                    if (swap2Move2 > -1) {
                        byte movei = (byte) (swap2Move2 / 19);
                        byte movej = (byte) (swap2Move2 % 19);
                        drawStone(canvas, movei, movej, (byte) 2);
                    }
                    if (swap2Move1 > -1) {
                        byte movei = (byte) (swap2Move1 / 19);
                        byte movej = (byte) (swap2Move1 % 19);
                        drawStone(canvas, movei, movej, (byte) 1);
                    }
                    if (swap2Move3 > -1) {
                        byte movei = (byte) (swap2Move3 / 19);
                        byte movej = (byte) (swap2Move3 % 19);
                        drawStone(canvas, movei, movej, (byte) 1);
                    }
//                } else if (game.getMovesList() != null && game.getMovesList().size() == 3) {
//                    if (swap2Move1 > -1) {
//                        byte movei = (byte) (swap2Move1/19);
//                        byte movej = (byte) (swap2Move1%19);
//                        drawStone(canvas, movei, movej, (byte) 2);
//                    }
//                    if (swap2Move2 > -1) {
//                        byte movei = (byte) (swap2Move2/19);
//                        byte movej = (byte) (swap2Move2%19);
//                        drawStone(canvas, movei, movej, (byte) 1);
//                    }
                }
            }
        }

    }

    private void drawStone(Canvas canvas, float x, float y, byte stoneColor) {
        if (stoneColor < 1) {
            return;
        }
        float radius = size / (2 * gridSize + 1);
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
        float radius = size / (2 * gridSize - 5);
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
//        float radius = size / 30;
        float cx = (float) Math.floor(gridSize * x / size) * size / gridSize + size / (2 * gridSize), cy = (float) Math.floor(gridSize * y / size) * size / gridSize + size / (2 * gridSize);
        Paint linePaint;
        linePaint = new Paint();
        linePaint.setStrokeWidth(4);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.WHITE);
        canvas.drawLine(0, cy, size, cy, linePaint);
        canvas.drawLine(cx, 0, cx, size, linePaint);
    }

    private void drawStone(Canvas canvas, byte j, byte i, byte stoneColor) {
        drawStone(canvas, size * i / gridSize + size / (2 * gridSize), size * j / gridSize + size / (2 * gridSize), stoneColor);
    }

    private void drawSquare(Canvas canvas, byte i, byte j, int stoneColor) {
        drawSquare(canvas, size * i / gridSize + size / (2 * gridSize), size * j / gridSize + size / (2 * gridSize), stoneColor);
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
        byte j = (byte) (redDot / gridSize);
        byte i = (byte) (redDot % gridSize);
        float cx = size * i / gridSize + size / (2 * gridSize), cy = size * j / gridSize + size / (2 * gridSize);
        canvas.drawCircle(cx, cy, radius, stonePaint);
        if (c6RedDot > -1) {
            j = (byte) (c6RedDot / gridSize);
            i = (byte) (c6RedDot % gridSize);
            cx = size * i / gridSize + size / (2 * gridSize);
            cy = size * j / gridSize + size / (2 * gridSize);
            canvas.drawCircle(cx, cy, radius, stonePaint);
        }
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
        CharSequence sequence = Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for (URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
        text.setMovementMethod(new ScrollingMovementMethod());
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }


    public void setBoardActivity(BoardActivity boardActivity) {
        this.boardActivity = boardActivity;
    }

    private SpannableStringBuilder getCapturesString() {
//        String str = "\u2B24 x " + game.getState().blackCaptures + " - \u25EF x " + game.getState().whiteCaptures;
        SpannableStringBuilder sb = new SpannableStringBuilder("\u2B24");
        float size = 1.5f;
        sb.setSpan(new RelativeSizeSpan(size), 0, 1, 0);
//        Drawable icon = null;
//        icon = ContextCompat.getDrawable(boardActivity, R.drawable.black_nobg);
//        sb.setSpan(icon, 0, 1, 0);
        sb.append(" x " + game.getState().blackCaptures + " -  \u25EF");
//        icon = ContextCompat.getDrawable(boardActivity, R.drawable.white_nobg);
        sb.setSpan(new RelativeSizeSpan(size), sb.length() - 1, sb.length(), 0);
        sb.append(" x " + game.getState().whiteCaptures);
        return sb;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width);
    }


    private boolean checkDPenteCapture() {
        if (dPenteMove2 - 3 == dPenteMove4 && dPenteMove2 % 19 > 2) {
            return (dPenteMove1 == dPenteMove2 - 2 && dPenteMove3 == dPenteMove2 - 1) ||
                    (dPenteMove1 == dPenteMove2 - 1 && dPenteMove3 == dPenteMove2 - 2);
        } else if (dPenteMove2 + 3 == dPenteMove4 && dPenteMove4 % 19 > 2) {
            return (dPenteMove1 == dPenteMove2 + 2 && dPenteMove3 == dPenteMove2 + 1) ||
                    (dPenteMove1 == dPenteMove2 + 1 && dPenteMove3 == dPenteMove2 + 2);
        } else if (dPenteMove2 - 57 == dPenteMove4) {
            return (dPenteMove1 == dPenteMove2 - 38 && dPenteMove3 == dPenteMove2 - 19) ||
                    (dPenteMove1 == dPenteMove2 - 19 && dPenteMove3 == dPenteMove2 - 38);
        } else if (dPenteMove2 + 57 == dPenteMove4) {
            return (dPenteMove1 == dPenteMove2 + 38 && dPenteMove3 == dPenteMove2 + 19) ||
                    (dPenteMove1 == dPenteMove2 + 19 && dPenteMove3 == dPenteMove2 + 38);
        } else if (dPenteMove2 - 60 == dPenteMove4 && dPenteMove2 % 19 > 2) {
            return (dPenteMove1 == dPenteMove2 - 40 && dPenteMove3 == dPenteMove2 - 20) ||
                    (dPenteMove1 == dPenteMove2 - 20 && dPenteMove3 == dPenteMove2 - 40);
        } else if (dPenteMove2 + 60 == dPenteMove4 && dPenteMove4 % 19 > 2) {
            return (dPenteMove1 == dPenteMove2 + 40 && dPenteMove3 == dPenteMove2 + 20) ||
                    (dPenteMove1 == dPenteMove2 + 20 && dPenteMove3 == dPenteMove2 + 40);
        } else if (dPenteMove2 - 54 == dPenteMove4 && dPenteMove4 % 19 > 2) {
            return (dPenteMove1 == dPenteMove2 - 36 && dPenteMove3 == dPenteMove2 - 18) ||
                    (dPenteMove1 == dPenteMove2 - 18 && dPenteMove3 == dPenteMove2 - 36);
        } else if (dPenteMove2 + 54 == dPenteMove4 && dPenteMove2 % 19 > 2) {
            return (dPenteMove1 == dPenteMove2 + 36 && dPenteMove3 == dPenteMove2 + 18) ||
                    (dPenteMove1 == dPenteMove2 + 18 && dPenteMove3 == dPenteMove2 + 36);
        }
        return false;
    }

}
