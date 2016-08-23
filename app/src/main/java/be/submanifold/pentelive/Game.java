package be.submanifold.pentelive;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.Toolbar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by waliedothman on 10/04/16.
 */
public class Game implements Parcelable {
    private String mGameID;
    private String mSetID;
    private String mGameType;
    private String mOpponentName;
    private String mOpponentRating;
    private String mMyColor;
    private String mRemainingTime;
    private String mRatedNot;
    private String mPrivateGame;
    private int mNameColor;
    private int mCrown;
    private String mGameString;
    private boolean mActive;
    public boolean dPenteChoice;

    public int whiteCaptures;
    public int blackCaptures;


    private int untilMove;
    private List<Integer> mMovesList;
    public HashMap<Integer, String> messages;

    public Game(String gameID, String setID, String gameType, String opponentName, String opponentRating, String myColor,
                String remainingTime, String ratedNot, String privateGame, String nameColor, String crown) {
        this.mGameID = gameID;
        this.mSetID = setID;
        this.mGameType = gameType;
        this.mOpponentName = opponentName;
        this.mOpponentRating = opponentRating;
        this.mMyColor = myColor;
        this.mRemainingTime = remainingTime;
        this.mRatedNot = ratedNot;
        this.mPrivateGame = privateGame;
        if (nameColor != null) {
            this.mNameColor = Integer.parseInt(nameColor);
        }
        if (crown != null) {
            this.mCrown = Integer.parseInt(crown);
        }
        this.mGameString = null;
        this.mActive = false;
        this.dPenteChoice = false;

    }

    public String getGameID() {
        return this.mGameID;
    }
    public String getSetID() {
        return this.mSetID;
    }
    public String getGameType() {
        return this.mGameType;
    }
    public String getOpponentName() {
        return this.mOpponentName;
    }
    public String getOpponentRating() {
        return this.mOpponentRating;
    }
    public String getMyColor() {
        return this.mMyColor;
    }
    public String getRemainingTime() {
        return this.mRemainingTime;
    }
    public String getRatedNot() {
        return this.mRatedNot;
    }
    public String getPrivateGame() {
        return this.mPrivateGame;
    }
    public int getNameColor() {
        return this.mNameColor;
    }
    public int getCrown() {
        return this.mCrown;
    }
    public boolean isActive() {
//        return true;
        return mActive;
    }

    public void setActive(boolean active) {
        this.mActive = active;
    }
    public void setmGameString(String mGameString) {
        this.mGameString = mGameString;
    }



    protected Game(Parcel in) {
        mGameID = in.readString();
        mSetID = in.readString();
        mGameType = in.readString();
        mOpponentName = in.readString();
        mOpponentRating = in.readString();
        mMyColor = in.readString();
        mRemainingTime = in.readString();
        mRatedNot = in.readString();
        mPrivateGame = in.readString();
        mNameColor = in.readInt();
        mCrown = in.readInt();
        mGameString = in.readString();
        mMovesList = (ArrayList<Integer>) in.readSerializable();
        mActive = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mGameID);
        dest.writeString(mSetID);
        dest.writeString(mGameType);
        dest.writeString(mOpponentName);
        dest.writeString(mOpponentRating);
        dest.writeString(mMyColor);
        dest.writeString(mRemainingTime);
        dest.writeString(mRatedNot);
        dest.writeString(mPrivateGame);
        dest.writeInt(mNameColor);
        dest.writeInt(mCrown);
        dest.writeString(mGameString);
        dest.writeSerializable((ArrayList<Integer>) mMovesList);
        dest.writeByte((byte) (mActive ? 1 : 0));      }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Game> CREATOR = new Parcelable.Creator<Game>() {
        @Override
        public Game createFromParcel(Parcel in) {
            return new Game(in);
        }

        @Override
        public Game[] newArray(int size) {
            return new Game[size];
        }
    };

    public int getUntilMove() {
        return untilMove;
    }

    public void setUntilMove(int untilMove) {
        this.untilMove = untilMove;
    }

    public class RetrieveGame extends AsyncTask<Void, Void, Boolean> {

        private final String mGameID;
        private BoardView boardView;

        RetrieveGame(String gameID, BoardView boardView) {
            this.mGameID = gameID;
            this.boardView = boardView;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url = new URL("https://www.pente.org/gameServer/mobile/game.jsp?gid="+mGameID + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for loadgame was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output);

                mGameString = output.toString();

                if (mGameString.indexOf("moves=") == -1) {
                    url = new URL("https://www.pente.org/gameServer/login.jsp?mobile=&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                    connection = (HttpURLConnection)url.openConnection();
                    responseCode = connection.getResponseCode();
//
//                    output = new StringBuilder();
//                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    System.out.println("output===============" + br);
//                    line = "";
//                    while((line = br.readLine()) != null ) {
//                        output.append(line + System.getProperty("line.separator"));
//                    }
//                    br.close();
//
//                    output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
//                    System.out.println(output);


                    url = new URL("https://www.pente.org/gameServer/mobile/game.jsp?gid="+mGameID);
                    connection = (HttpURLConnection)url.openConnection();
                    responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        System.out.println("response code for loadgame was " + responseCode);
                        return false;
                    }

                    output = new StringBuilder();
                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    System.out.println("output===============" + br);
                    line = "";
                    while((line = br.readLine()) != null ) {
                        output.append(line + "\n");
                    }
                    br.close();

//                    System.out.println(output);

                    mGameString = output.toString();
                }

            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            parseGame(boardView);
        }

        @Override
        protected void onCancelled() {
        }
    }
    public class SubmitMoveTask extends AsyncTask<Void, Void, Boolean> {

        private String move;
        private String message;

        SubmitMoveTask(String move, String message) {
            try {
                this.message = URLEncoder.encode(message,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                this.message = "";
            }
            this.move = move;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url = new URL("https://www.pente.org/gameServer/tb/game?command=move&mobile=&gid="+mGameID+"&moves="+move+"&message=" + message + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for submit was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + "\n");
                }
                br.close();

                System.out.println("submit output: " + output.toString());


//                if (mGameString.indexOf("moves=") == -1) {
//                    url = new URL("https://www.pente.org/gameServer/login.jsp?mobile=&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
//                    connection = (HttpURLConnection)url.openConnection();
//                    responseCode = connection.getResponseCode();
////
////                    output = new StringBuilder();
////                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
////                    System.out.println("output===============" + br);
////                    line = "";
////                    while((line = br.readLine()) != null ) {
////                        output.append(line + System.getProperty("line.separator"));
////                    }
////                    br.close();
////
////                    output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
////                    System.out.println(output);
//
//
//                    url = new URL("https://www.pente.org/gameServer/mobile/game.jsp?gid="+mGameID);
//                    connection = (HttpURLConnection)url.openConnection();
//                    responseCode = connection.getResponseCode();
//                    if (responseCode != 200) {
//                        System.out.println("response code for loadgame was " + responseCode);
//                        return false;
//                    }
//
//                    output = new StringBuilder();
//                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    System.out.println("output===============" + br);
//                    line = "";
//                    while((line = br.readLine()) != null ) {
//                        output.append(line + "\n");
//                    }
//                    br.close();
//
//                    System.out.println(output);
//
//                    mGameString = output.toString();
//                }

            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
        }

        @Override
        protected void onCancelled() {
        }
    }

    public class ReplyCancelTask extends AsyncTask<Void, Void, Boolean> {

        private String sid, reply, gid;
        private Activity activity;


        ReplyCancelTask(String sid, String gid, String reply, Activity activity) {
            this.gid = gid;
            this.activity = activity;
            this.sid = sid;
            this.reply = reply;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String urlParameters  = "sid=" + sid + "&gid=" + gid + "&command=" + reply + "&mobile=" + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/tb/cancel";
                URL url            = new URL( request );
                HttpURLConnection conn= (HttpURLConnection) url.openConnection();
                conn.setDoOutput( true );
                conn.setInstanceFollowRedirects( false );
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty( "charset", "utf-8");
                conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
                conn.setUseCaches( false );
                try {
                    DataOutputStream wr = new DataOutputStream( conn.getOutputStream());
                    wr.write( postData );
                } catch (Exception e) {
                    e.printStackTrace();
                    return  false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                System.out.println("output===============" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + System.getProperty("line.separator"));
                }
                br.close();

                output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
//                System.out.println(output);


            } catch (IOException e1) {
                e1.printStackTrace();
                return  false;
            }

            return true;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                activity.finish();
            }
        }

        @Override
        protected void onCancelled() {
        }
    }


        public List<Integer> getMovesList() {
        return mMovesList;
    }
//    public void setmMovesList(List<Integer> mMovesList) {
//        this.mMovesList = mMovesList;
//    }

    public void submitMove(String move, String message) {
        SubmitMoveTask submitTask = new SubmitMoveTask(move, message);
        submitTask.execute((Void) null);
    }


    public boolean rated() {
        if (mRatedNot == null) {
            return false;
        }
        return !(mRatedNot.indexOf("Not") > -1);
    }
    public boolean isConnect6() {
        if (getGameType() == null) {
            return false;
        }
        return getGameType().indexOf("Connect6") > -1;
    }
    public boolean isGomoku() {
        if (getGameType() == null) {
            return false;
        }
        return getGameType().indexOf("Gomoku") > -1;
    }
    public boolean isDPente() {
        if (getGameType() == null) {
            return false;
        }
        return getGameType().indexOf("D-Pente") > -1;
    }

    public void parseGame(BoardView boardView) {
        if (mGameString == null) {
            RetrieveGame getGameTask = new RetrieveGame(getGameID(), boardView);
            getGameTask.execute((Void) null);
            return;
        }

        String messageNums[] = null;
        String messagesArray[] = null;

        String[] dashLines = mGameString.split("\n");
        String dashLine;
        int idx = 0;
        while (idx < dashLines.length) {
            dashLine = dashLines[idx];
            if (dashLine.indexOf("player1=") == 0) {
                String playerName = dashLine.substring(8).split(",")[0];
                if (playerName.toLowerCase().equals(PentePlayer.mPlayerName.toLowerCase())) {
                    if (this.mMovesList != null) {
//                        if (isDPente() && mMovesList.size() == 1) {
//                            this.mActive = true;
//
//                        } else
                        if (isConnect6()) {
                            if ((((mMovesList.size() % 4) == 0) || ((mMovesList.size() % 4) == 3))) {
                                this.mActive = true;
                            } else {
                                this.mActive = false;
                            }
                        } else if (this.mMovesList.size()%2 == 0) {
                            this.mActive = true;
                        } else {
                            this.mActive = false;
                        }
                    }
                } else {
                    this.mOpponentName = playerName;
                    this.mOpponentRating = dashLine.substring(8).split(",")[1];
                }
            }
            if (dashLine.indexOf("player2=") == 0) {
                String playerName = dashLine.substring(8).split(",")[0];
                if (playerName.toLowerCase().equals(PentePlayer.mPlayerName.toLowerCase())) {
                    if (this.mMovesList != null) {
//                        if (isDPente() && mMovesList.size() == 1) {
//                            this.mActive = false;
//                        } else
                        if (isConnect6()) {
                            if ((((mMovesList.size() % 4) == 1) || ((mMovesList.size() % 4) == 2))) {
                                this.mActive = true;
                            } else {
                                this.mActive = false;
                            }
                        } else if (this.mMovesList.size()%2 == 1) {
                            this.mActive = true;
                        } else {
                            this.mActive = false;
                        }
                    }
                } else {
                    this.mOpponentName = playerName;
                    this.mOpponentRating = dashLine.substring(8).split(",")[1];
                }
            }
            if (dashLine.indexOf("sid=") == 0) {
                this.mSetID = dashLine.substring(4);
            }
            if (dashLine.indexOf("gameName=") == 0) {
                this.mGameType = dashLine.substring(9);
            }
            if (dashLine.indexOf("moves=") == 0) {
                String movesString[] = dashLine.substring(6).split(",");
                this.mMovesList = new ArrayList<Integer>();
                for ( int i = 0; i < movesString.length; i++ ) {
                    this.mMovesList.add(Integer.parseInt(movesString[i]));
                }
            }
            if (isDPente() && (dashLine.indexOf("dPenteState=2") > -1 || dashLine.contains("dPenteState=1"))) {
                this.mActive = !mActive;
            }
            if (dashLine.indexOf("dPenteState=2") > -1) {
                this.dPenteChoice = true;
            }
            if (dashLine.indexOf("cancel="+getOpponentName()) == 0) {
                final Activity host = (Activity) boardView.getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(host);
                builder.setTitle("Cancel request");

                String msg = "";
                if (!dashLine.substring(dashLine.indexOf(",")+1).equals("")) {
                    msg = "\n(" + dashLine.substring(dashLine.indexOf(",")+1) + ")";
                }
                builder.setMessage(getOpponentName() + " requests to cancel this set." + msg);
                builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ReplyCancelTask task = new ReplyCancelTask(getSetID(), getGameID(), "Yes", host);
                        task.execute((Void) null);
                    } });
                builder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ReplyCancelTask task = new ReplyCancelTask(getSetID(), getGameID(), "No", host);
                        task.execute((Void) null);
                    } });
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(final DialogInterface arg0) {
                            host.finish();
                        }
                    });
                }
                builder.show();
            }
            if (dashLine.indexOf("messages=") > -1) {
                messagesArray = dashLine.substring(9).split(",");
            }
            if (dashLine.indexOf("rated=") > -1) {
                mRatedNot = dashLine.substring(6);
            }
            if (dashLine.indexOf("private=") > -1) {
                mPrivateGame = dashLine.substring(8);
            }
            if (dashLine.indexOf("messageNums=") > -1) {
                messageNums = dashLine.substring(12).split(",");
            }
            idx += 1;
        }

        messages = new HashMap<Integer, String>();
        if (messagesArray != null && messagesArray.length > 0) {
            for (int i = 0; i < messagesArray.length; ++i ) {
                Integer msgNum;
                try {
                    msgNum = Integer.parseInt(messageNums[i]);
                } catch (NumberFormatException e) {
                    continue;
                }
                messages.put(msgNum, filterMessage(messagesArray[i]));
            }
        }

        if (mMovesList != null) {
            this.untilMove = mMovesList.size();
        } else {
            this.untilMove = 0;
        }

        if (messages.get(untilMove) != null) {
            BoardActivity host = (BoardActivity) boardView.getContext();
            host.messageIcon.startAnimation(host.rotation);
        }
//        this.mActive = true;
        if (boardView != null) {
            replayGame(boardView.abstractBoard, boardView);
//            boardView.invalidate();
        }
    }

    private String filterMessage(String msgStr) {

        String tmpStrComma = msgStr.replace("\\1", ",");
        String tmpStrSmiley = tmpStrComma.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/happy.gif' alt=''>", ":)");
        String tmpStrWink = tmpStrSmiley.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/wink.gif' alt=''>", ";)");
        String tmpStrTongue = tmpStrWink.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/silly.gif' alt=''>", ":p");
        String tmpStrGrin = tmpStrTongue.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/grin.gif' alt=''>", ":D");
        String tmpStrSad = tmpStrGrin.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/sad.gif' alt=''>", ":(");
        String tmpStrLove = tmpStrSad.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/love.gif' alt=''>", "<3");
        String tmpStrMischief = tmpStrLove.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/mischief.gif' alt=''>", ";\\");
        String tmpStrCool = tmpStrMischief.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/cool.gif' alt=''>", "B)");
        String tmpStrDevil = tmpStrCool.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/devil.gif' alt=''>", ">:)");
        String tmpStrAngry = tmpStrDevil.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/angry.gif' alt=''>", "X(");
        String tmpStrLaugh = tmpStrAngry.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/laugh.gif' alt=''>", ":^O");
        String tmpStrBlush = tmpStrLaugh.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/blush.gif' alt=''>", ":8)");
        String tmpStrCry = tmpStrBlush.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/cry.gif' alt=''>", ":'(");
        String tmpStrConfused = tmpStrCry.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/confused.gif' alt=''>", "?:|");
        String tmpStrShocked = tmpStrConfused.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/shocked.gif' alt=''>", ":O");
        String tmpStrPlain = tmpStrShocked.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/plain.gif' alt=''>", ":|");
        return tmpStrPlain;
    }

    public void replayGame(byte[][] abstractBoard, BoardView boardView) {
        if (mMovesList == null) {
            return;
        }
//        if (getGameType().equals("Gomoku")) {
//            boardView.setBackgroundColor(boardView.gomokuColor);
//            replayGomokuGame(abstractBoard, mMovesList.size());
//        } else if (getGameType().equals("Pente")) {
//            boardView.setBackgroundColor(boardView.penteColor);
//            replayPenteGame(abstractBoard, mMovesList.size());
//        } else if (getGameType().equals("Boat-Pente")) {
//            boardView.setBackgroundColor(boardView.boatPenteColor);
//            replayPenteGame(abstractBoard, mMovesList.size());
//        } else if (getGameType().equals("Keryo-Pente")) {
//            boardView.setBackgroundColor(boardView.keryoPenteColor);
//            replayKeryoPenteGame(abstractBoard, mMovesList.size());
//        } else if (getGameType().equals("Connect6")) {
//            boardView.setBackgroundColor(boardView.connect6Color);
//            replayConnect6Game(abstractBoard, mMovesList.size());
//        } else if (getGameType().equals("G-Pente")) {
//            boardView.setBackgroundColor(boardView.gPenteColor);
//            replayGPenteGame(abstractBoard, mMovesList.size());
//        } else if (getGameType().equals("Poof-Pente")) {
//            boardView.setBackgroundColor(boardView.poofPenteColor);
//            replayPoofPenteGame(abstractBoard, mMovesList.size());
//        } else if (getGameType().equals("D-Pente")) {
//            boardView.setBackgroundColor(boardView.dPenteColor);
//            replayPenteGame(abstractBoard, mMovesList.size());
//        }

        this.untilMove = this.mMovesList.size();
        replayGameUntilMove(abstractBoard, boardView);

        if (boardView != null) {
            boardView.invalidate();
            if (mGameType.equals("Pente") && mOpponentName.equals("computer")) {
                if (whiteCaptures == 10 || blackCaptures == 10 || detectPente(abstractBoard, (byte) (2 - (mMovesList.size()%2)), mMovesList.get(mMovesList.size() - 1))) {
                    boolean iWon = false;
                    mActive = false;
                    int myColor = (mMyColor.contains("white")?1:2);
                    if (whiteCaptures == 10) {
                        if (myColor == 2) {
                            iWon = true;
                        }
                    } else if (blackCaptures == 10) {
                        if (myColor == 1) {
                            iWon = true;
                        }
                    } else if (myColor == (2 - mMovesList.size()%2)) {
                        iWon = true;
                    }
                    String msg = "You lost";
                    if (iWon) {
                        msg = "You won";
                    }
                    Toast toast = Toast.makeText(boardView.getContext(), msg, Toast.LENGTH_LONG);
                    TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                    if (iWon) {
                        v.setTextColor(Color.GREEN);
                    } else {
                        v.setTextColor(Color.YELLOW);
                    }
                    toast.show();
                }
            }
        }

    }
    public void replayGameUntilMove(byte[][] abstractBoard, BoardView boardView) {
        if (mMovesList == null) {
            return;
        }
        if (getGameType().equals("Gomoku")) {
            boardView.setBackgroundColor(boardView.gomokuColor);
            replayGomokuGame(abstractBoard, untilMove);
        } else if (getGameType().equals("Pente")) {
            boardView.setBackgroundColor(boardView.penteColor);
            replayPenteGame(abstractBoard, untilMove);
        } else if (getGameType().equals("Boat-Pente")) {
            boardView.setBackgroundColor(boardView.boatPenteColor);
            replayPenteGame(abstractBoard, untilMove);
        } else if (getGameType().equals("Keryo-Pente")) {
            boardView.setBackgroundColor(boardView.keryoPenteColor);
            replayKeryoPenteGame(abstractBoard, untilMove);
        } else if (getGameType().equals("Connect6")) {
            boardView.setBackgroundColor(boardView.connect6Color);
            replayConnect6Game(abstractBoard, untilMove);
        } else if (getGameType().equals("G-Pente")) {
            boardView.setBackgroundColor(boardView.gPenteColor);
            replayGPenteGame(abstractBoard, untilMove);
        } else if (getGameType().equals("Poof-Pente")) {
            boardView.setBackgroundColor(boardView.poofPenteColor);
            replayPoofPenteGame(abstractBoard, untilMove);
        } else if (getGameType().equals("D-Pente")) {
            boardView.setBackgroundColor(boardView.dPenteColor);
            replayPenteGame(abstractBoard, untilMove);
        }

        if (boardView != null) {
            boardView.setRedDot(this.mMovesList.get(untilMove - 1));
            if (isConnect6()) {
                if (untilMove == 1) {
                    boardView.setC6RedDot(-1);
                } else {
                    boardView.setC6RedDot(this.mMovesList.get(untilMove - 2));
                }
            }
            boardView.invalidate();
        }
    }

    public void replayGame(byte[][] abstractBoard, byte moveI, byte moveJ, BoardView boardView) {
        if (mMovesList == null) {
            return;
        }
        if (getGameType().equals("Pente")) {
            boardView.setBackgroundColor(boardView.penteColor);
            replayPenteGame(abstractBoard, moveI, moveJ);
        } else if (getGameType().equals("Boat-Pente")) {
            boardView.setBackgroundColor(boardView.boatPenteColor);
            replayPenteGame(abstractBoard, moveI, moveJ);
        } else if (getGameType().equals("Keryo-Pente")) {
            boardView.setBackgroundColor(boardView.keryoPenteColor);
            replayKeryoPenteGame(abstractBoard, moveI, moveJ);
        } else if (getGameType().equals("G-Pente")) {
            boardView.setBackgroundColor(boardView.gPenteColor);
            replayPenteGame(abstractBoard, moveI, moveJ);
        } else if (getGameType().equals("Poof-Pente")) {
            boardView.setBackgroundColor(boardView.poofPenteColor);
            replayPoofPenteGame(abstractBoard, moveI, moveJ);
        } else if (getGameType().equals("D-Pente")) {
            boardView.setBackgroundColor(boardView.dPenteColor);
            replayPenteGame(abstractBoard, moveI, moveJ);
        }

        if (boardView != null) {
            boardView.invalidate();
        }

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

    private void replayGomokuGame(byte[][] abstractBoard, int until) {
        resetAbstractBoard(abstractBoard);
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            abstractBoard[mMovesList.get(i) % 19][(int) (mMovesList.get(i) / 19)] = color;
        }
    }

    private void replayPenteGame(byte[][] abstractBoard, int until) {
        resetAbstractBoard(abstractBoard);
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            abstractBoard[mMovesList.get(i) % 19][(int) (mMovesList.get(i) / 19)] = color;
            detectPenteCapture(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
        }
        if (rated() && (mMovesList.size() == 2)) {
            for( int i = 7; i < 12; ++i) {
                for (int j = 7; j < 12; ++j) {
                    if (abstractBoard[i][j] == 0) {
                        abstractBoard[i][j] = -1;
                    }
                }
            }
        }
        if (mOpponentName.equals("computer") && mGameType.equals("Pente")) {
            if (whiteCaptures == 10 || blackCaptures == 10 || detectPente(abstractBoard, (byte) (2 - (mMovesList.size()%2)), mMovesList.get(mMovesList.size() - 1))) {
                mActive = false;
            }
        }
    }
    private void replayPenteGame(byte[][] abstractBoard, byte moveI, byte moveJ) {
        resetAbstractBoard(abstractBoard);
        for (int i = 0; i < mMovesList.size(); i++) {
            byte color = (byte) (1 + (i%2));
            abstractBoard[mMovesList.get(i) % 19][(int) (mMovesList.get(i) / 19)] = color;
            detectPenteCapture(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
        }
        byte color = (byte) (1 + (mMovesList.size()%2));
        abstractBoard[moveI][moveJ] = color;
//        System.out.println(" kitty heeeelp " + moveI + " and " + moveJ + " and " + color);
        detectPenteCapture(abstractBoard, moveI, moveJ, color);
    }

    private void replayKeryoPenteGame(byte[][] abstractBoard, int until) {
        resetAbstractBoard(abstractBoard);
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            abstractBoard[mMovesList.get(i) % 19][(int) (mMovesList.get(i) / 19)] = color;
            detectPenteCapture(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
            detectKeryoPenteCapture(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
        }
        if (rated() && (mMovesList.size() == 2)) {
            for( int i = 7; i < 12; ++i) {
                for (int j = 7; j < 12; ++j) {
                    if (abstractBoard[i][j] == 0) {
                        abstractBoard[i][j] = -1;
                    }
                }
            }
        }
    }
    private void replayKeryoPenteGame(byte[][] abstractBoard, byte moveI, byte moveJ) {
        resetAbstractBoard(abstractBoard);
        for (int i = 0; i < mMovesList.size(); i++) {
            byte color = (byte) (1 + (i%2));
            abstractBoard[mMovesList.get(i) % 19][(int) (mMovesList.get(i) / 19)] = color;
            detectPenteCapture(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
            detectKeryoPenteCapture(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
        }
        byte color = (byte) (1 + (mMovesList.size()%2));
        abstractBoard[moveI][moveJ] = color;
        detectPenteCapture(abstractBoard, moveI, moveJ, color);
        detectKeryoPenteCapture(abstractBoard, moveI, moveJ, color);
    }

    private void replayConnect6Game(byte[][] abstractBoard, int until) {
        resetAbstractBoard(abstractBoard);
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) ((((i % 4) == 0) || ((i % 4) == 3)) ? 1 : 2);
            abstractBoard[mMovesList.get(i) % 19][(int) (mMovesList.get(i) / 19)] = color;
        }
    }

    private void replayGPenteGame(byte[][] abstractBoard, int until) {
        resetAbstractBoard(abstractBoard);
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            abstractBoard[mMovesList.get(i) % 19][(int) (mMovesList.get(i) / 19)] = color;
            detectPenteCapture(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
        }
        if (mMovesList.size() == 2) {
            for(byte i = 7; i < 12; i++) {
                for(byte j = 7; j < 12; j++) {
                    if (abstractBoard[i][j] == 0) {
                        abstractBoard[i][j] = -1;
                    }
                }
            }
            for(byte  i = 1; i < 3; i++) {
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
    }

    private void replayPoofPenteGame(byte[][] abstractBoard, int until) {
        resetAbstractBoard(abstractBoard);
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            abstractBoard[mMovesList.get(i) % 19][(int) (mMovesList.get(i) / 19)] = color;
            detectPoof(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
            detectPenteCapture(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
        }
        if (rated() && (mMovesList.size() == 2)) {
            for( int i = 7; i < 12; ++i) {
                for (int j = 7; j < 12; ++j) {
                    if (abstractBoard[i][j] == 0) {
                        abstractBoard[i][j] = -1;
                    }
                }
            }
        }
    }
    private void replayPoofPenteGame(byte[][] abstractBoard, byte moveI, byte moveJ) {
        resetAbstractBoard(abstractBoard);
        for (int i = 0; i < mMovesList.size(); i++) {
            byte color = (byte) (1 + (i%2));
            abstractBoard[mMovesList.get(i) % 19][(int) (mMovesList.get(i) / 19)] = color;
            detectPoof(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
            detectPenteCapture(abstractBoard, mMovesList.get(i) % 19, (int) (mMovesList.get(i) / 19), color);
        }
        byte color = (byte) (1 + (mMovesList.size()%2));
        abstractBoard[moveI][moveJ] = color;
        detectPoof(abstractBoard, moveI, moveJ, color);
        detectPenteCapture(abstractBoard, moveI, moveJ, color);
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
    private void detectKeryoPenteCapture(byte[][] abstractBoard, int i, int j, byte myColor) {
        byte opponentColor = (byte) (1 + (myColor % 2));
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
    private void detectPoof(byte[][] abstractBoard, int i, int j, byte myColor) {
        byte opponentColor = (byte) (1 + (myColor % 2));
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


    private boolean detectPente(byte[][] abstractBoard, byte color, int rowCol) {
        boolean pente = false;
        int penteCounter = 1;
        int row = rowCol / 19, col = rowCol % 19, i, j;
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

