package be.submanifold.pentelive;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.HttpsURLConnection;


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

    private boolean canHide, canUnHide;

    private int untilMove;
    private List<Integer> mMovesList;
    public HashMap<Integer, String> messages;

    private String mLocalizedTime;
    private String mLocalizedRatedNot;

    private String movesString = "";

    private String hideStr = "";

    private String mBoardString;


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

        this.canHide = false;
        this.canUnHide = false;
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
    public boolean isCanHide() { return canHide; }
    public boolean isCanUnHide() { return canUnHide; }


    public void setActive(boolean active) {
        this.mActive = active;
    }
    public void setmGameString(String mGameString) {
        this.mGameString = mGameString;
    }

    public String getMovesString() {
        return movesString;
    }

    public String getHideStr() { return hideStr; }
    public void setHideStr(String hideStr) { this.hideStr = hideStr; }


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
        mLocalizedTime = in.readString();
        mLocalizedRatedNot = in.readString();
        canHide = in.readByte() != 0;
        canUnHide = in.readByte() != 0;
        hideStr = in.readString();
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
        dest.writeByte((byte) (mActive ? 1 : 0));
        dest.writeString(mLocalizedTime);
        dest.writeString(mLocalizedRatedNot);
        dest.writeByte((byte) (canHide ? 1 : 0));
        dest.writeByte((byte) (canUnHide ? 1 : 0));
        dest.writeString(hideStr);
    }

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

    public String getLocalizedRatedNot() {
        if (mRatedNot != null && mLocalizedRatedNot == null) {
            Context ctx = MyApplication.getContext();
            if (mRatedNot.equals("Rated")) {
                mLocalizedRatedNot = ctx.getString(R.string.rated);
            } else if (mRatedNot.equals("Not Rated")) {
                mLocalizedRatedNot = ctx.getString(R.string.notRated);
            } else if (mRatedNot.equals("KotH")) {
                mLocalizedRatedNot = ctx.getString(R.string.koth);
            } else if (mRatedNot.equals("Tournament")) {
                mLocalizedRatedNot = ctx.getString(R.string.tournament);
            } else if (mRatedNot.equals("Rated, beginner")) {
                mLocalizedRatedNot = ctx.getString(R.string.rated_beginner);
            } else if (mRatedNot.equals("Not Rated, beginner")) {
                mLocalizedRatedNot = ctx.getString(R.string.not_rated_beginner);
            }
        }
        return mLocalizedRatedNot;
    }

    public String getLocalizedTime() {
        if (mRemainingTime != null && mLocalizedTime == null) {
            Context ctx = MyApplication.getContext();
            if (mRemainingTime.contains("minutes")) {
                String[] splitTime = mRemainingTime.split(" ");
                String hour = splitTime[0], minutes = splitTime[2];
                if (hour.equals("1") && minutes.equals("1")) {
                    mLocalizedTime = ctx.getString(R.string.hourminute, hour, minutes);
                } else if (hour.equals("1")) {
                    mLocalizedTime = ctx.getString(R.string.hourminutes, hour, minutes);
                } else if (minutes.equals("1")) {
                    mLocalizedTime = ctx.getString(R.string.hoursminute, hour, minutes);
                } else {
                    mLocalizedTime = ctx.getString(R.string.hoursminutes, hour, minutes);
                }
            } else if (mRemainingTime.contains("hours")) {
                String[] splitTime = mRemainingTime.split(" ");
                String day = splitTime[0], hours = splitTime[2];
                if (day.equals("1") && hours.equals("1")) {
                    mLocalizedTime = ctx.getString(R.string.dayhour, day, hours);
                } else if (day.equals("1")) {
                    mLocalizedTime = ctx.getString(R.string.dayhours, day, hours);
                } else if (hours.equals("1")) {
                    mLocalizedTime = ctx.getString(R.string.dayshour, day, hours);
                } else {
                    mLocalizedTime = ctx.getString(R.string.dayshours, day, hours);
                }
            } else {
                String days = mRemainingTime.substring(0, mRemainingTime.indexOf(" days"));
                if (days.equals("1")) {
                    mLocalizedTime = ctx.getString(R.string.daypermove, days);
                } else {
                    mLocalizedTime = ctx.getString(R.string.dayspermove, days);
                }
            }

        }
        return mLocalizedTime;
    }

    public String getBoardString() {
        if (mBoardString == null && mOpponentName != null && mPrivateGame != null) {
            Context ctx = MyApplication.getContext();
            String ratingStr = ctx.getString(R.string.rating),
                    timeStr = ctx.getString(R.string.remainingtime),
                    privateStr = mPrivateGame.contains("non")?ctx.getString(R.string.non_private):ctx.getString(R.string.private_str),
                    thisGame = ctx.getString(R.string.this_is_a_rated_and_private_game, getLocalizedRatedNot(), privateStr);
            if (mOpponentName != null && mOpponentName.contains(" vs ")) {
                String players[] = mOpponentName.split(" vs ");
                mBoardString = "<a href=\"https://www.pente.org/gameServer/profile?viewName=" + players[0]+"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword + "\">" +players[0] + "</a> vs " +
                        "<a href=\"https://www.pente.org/gameServer/profile?viewName=" + players[1]+"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword + "\">" +players[1] + "</a>"
                        + ", " + ratingStr + " " + mOpponentRating + "<br>"+timeStr+" " + getLocalizedTime()
                        + "<br>" + thisGame + "<br><br>";
            } else {
                mBoardString = ctx.getString(R.string.opponent) + ": <a href=\"https://www.pente.org/gameServer/profile?viewName=" + mOpponentName+"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword + "\">" + mOpponentName + "</a>"
                        + ", " + ratingStr + " " + mOpponentRating + "<br>"+timeStr+" " + getLocalizedTime()
                        + "<br>" + thisGame + "<br><br>";
            }
        }
        return mBoardString + getMovesString();
    }

    public String getHideString() {
        Context ctx = MyApplication.getContext();
        if ((canHide && hideStr.length() == 0) || (canUnHide && hideStr.length()>0)) {
            return ctx.getString(R.string.hide_from_public);
        }
        if ((canUnHide && hideStr.length() == 0) || (canHide && hideStr.length()>0)) {
            return ctx.getString(R.string.unhide_from_public);
        }
        return "";
    }
    public void changeHideString() {
        if ((canHide && hideStr.length() == 0) || (canUnHide && hideStr.length()>0)) {
            if (hideStr.length() == 0) {
                hideStr = "&hide=yes";
            } else {
                hideStr = "";
            }
        } else if ((canUnHide && hideStr.length() == 0) || (canHide && hideStr.length()>0)) {
            if (hideStr.length() == 0) {
                hideStr = "&hide=no";
            } else {
                hideStr = "";
            }
        }
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
//                URL url = new URL("https://www.pente.org/gameServer/mobile/game.jsp?gid="+mGameID);
                URL url = new URL("https://www.pente.org/gameServer/mobile/game.jsp?gid="+mGameID
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                if (PentePlayer.development) {
                    url = new URL("https://development.pente.org/gameServer/mobile/game.jsp?gid="+mGameID
                            +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                }
                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item: splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    connection.setRequestProperty("Cookie", cookieStr);
//                    System.out.println("cookieStr: " +cookieStr);
                }
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for loadgame was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output==========" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output);

                mGameString = output.toString();

                if (mGameString.indexOf("moves=") == -1) {
                    url = new URL("https://www.pente.org/gameServer/login.jsp?mobile=&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                    if (PentePlayer.development) {
                        url = new URL("https://development.pente.org/gameServer/login.jsp?mobile=&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                    }
                    connection = (HttpsURLConnection)url.openConnection();
                    responseCode = connection.getResponseCode();
//
//                    output = new StringBuilder();
//                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    System.out.println("output==========" + br);
//                    line = "";
//                    while((line = br.readLine()) != null ) {
//                        output.append(line + System.getProperty("line.separator"));
//                    }
//                    br.close();
//
//                    output.append(System.getProperty("line.separator") + "Response " + System.getProperty("line.separator") + System.getProperty("line.separator"));
//                    System.out.println(output);


                    url = new URL("https://www.pente.org/gameServer/mobile/game.jsp?gid="+mGameID);
                    if (PentePlayer.development) {
                        url = new URL("https://development.pente.org/gameServer/mobile/game.jsp?gid="+mGameID);
                    }
                    connection = (HttpsURLConnection)url.openConnection();
                    responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        System.out.println("response code for loadgame was " + responseCode);
                        return false;
                    }

                    output = new StringBuilder();
                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    System.out.println("output==========" + br);
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
//                URL url = new URL("https://www.pente.org/gameServer/tb/game?command=move&mobile=&gid="+mGameID+"&moves="+move+"&message=" + message);
                URL url = new URL("https://www.pente.org/gameServer/tb/game?command=move"+hideStr+"&mobile=&gid="+mGameID+"&moves="+move+"&message=" + message
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                if (PentePlayer.development) {
                    url = new URL("https://development.pente.org/gameServer/tb/game?command=move"+hideStr+"&mobile=&gid="+mGameID+"&moves="+move+"&message=" + message
                            +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
                }
                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item: splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    connection.setRequestProperty("Cookie", cookieStr);
//                    System.out.println("cookieStr: " +cookieStr);
                }
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for submit was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output==========" + br);
                String line = "";
                while((line = br.readLine()) != null ) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println("submit output: " + output.toString());


//                if (mGameString.indexOf("moves=") == -1) {
//                    url = new URL("https://www.pente.org/gameServer/login.jsp?mobile=&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword);
//                    connection = (HttpsURLConnection)url.openConnection();
//                    responseCode = connection.getResponseCode();
////
////                    output = new StringBuilder();
////                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
////                    System.out.println("output==========" + br);
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
//                    connection = (HttpsURLConnection)url.openConnection();
//                    responseCode = connection.getResponseCode();
//                    if (responseCode != 200) {
//                        System.out.println("response code for loadgame was " + responseCode);
//                        return false;
//                    }
//
//                    output = new StringBuilder();
//                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    System.out.println("output==========" + br);
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
//                String urlParameters  = "sid=" + sid + "&gid=" + gid + "&command=" + reply + "&mobile=";
                String urlParameters  = "sid=" + sid + "&gid=" + gid + "&command=" + reply + "&mobile="
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/tb/cancel";
                if (PentePlayer.development) {
                    request        = "https://development.pente.org/gameServer/tb/cancel";
                }
                    URL url            = new URL( request );
                HttpsURLConnection conn= (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item: splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    conn.setRequestProperty("Cookie", cookieStr);
//                    System.out.println("cookieStr: " +cookieStr);
                }
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
//                System.out.println("output==========" + br);
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

    public class RequestUndoTask extends AsyncTask<Void, Void, Boolean> {

        private String gid;
        private Activity activity;


        RequestUndoTask(String gid, Activity activity) {
            this.gid = gid;
            this.activity = activity;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
//                String urlParameters  = "sid=" + sid + "&gid=" + gid + "&command=" + reply + "&mobile=";
                String urlParameters  = "gid=" + gid + "&command=requestUndo" +  "&mobile="
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/tb/game";
                if (PentePlayer.development) {
                    request        = "https://development.pente.org/gameServer/tb/game";
                }
                URL url            = new URL( request );
                HttpsURLConnection conn= (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item: splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    conn.setRequestProperty("Cookie", cookieStr);
//                    System.out.println("cookieStr: " +cookieStr);
                }
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
//                System.out.println("output==========" + br);
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
    public class ReplyUndoTask extends AsyncTask<Void, Void, Boolean> {

        private String gid;
        private Activity activity;
        private boolean accept;
        private String reply;

        ReplyUndoTask(String gid, boolean accept, Activity activity) {
            this.gid = gid;
            this.activity = activity;
            this.accept = accept;
            reply = accept?"acceptUndo":"declineUndo";
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
//                String urlParameters  = "sid=" + sid + "&gid=" + gid + "&command=" + reply + "&mobile=";
                String urlParameters  = "gid=" + gid + "&command=" + reply + "&mobile="
                        +"&name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword;
                byte[] postData       = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
                }
                int    postDataLength = postData.length;
                String request        = "https://www.pente.org/gameServer/tb/game";
                if (PentePlayer.development) {
                    request        = "https://development.pente.org/gameServer/tb/game";
                }
                URL url            = new URL( request );
                HttpsURLConnection conn= (HttpsURLConnection) url.openConnection();
                String cookies = CookieManager.getInstance().getCookie("https://www.pente.org/");
                if (cookies != null) {
                    String[] splitCookie = cookies.split(";");
                    String cookieStr = "";
                    for (String item: splitCookie) {
                        if (item.contains("name2") || item.contains("password2")) {
                            cookieStr += item + ";";
                        }
                    }
                    conn.setRequestProperty("Cookie", cookieStr);
//                    System.out.println("cookieStr: " +cookieStr);
                }
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
//                System.out.println("output==========" + br);
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
                if (accept) {
                    activity.finish();
                } else {
                    mGameString = null;
                    parseGame((BoardView) activity.findViewById(R.id.boardView));
                }
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
        return !(mRatedNot.contains("Not"));
    }
    public boolean isConnect6() {
        if (getGameType() == null) {
            return false;
        }
        return getGameType().contains("Connect6");
    }
    public boolean isGomoku() {
        if (getGameType() == null) {
            return false;
        }
        return getGameType().contains("Gomoku");
    }
    public boolean isDPente() {
        if (getGameType() == null) {
            return false;
        }
        return (getGameType().contains("D-Pente") || getGameType().contains("DK-Pente"));
    }

    public void parseGame(BoardView boardView) {
        if (mGameString == null) {
            RetrieveGame getGameTask = new RetrieveGame(getGameID(), boardView);
            getGameTask.execute((Void) null);
            return;
        }

//        System.out.println(mGameString);

        boolean amIPlaying = false, undoRequested = false;

        String messageNums[] = null;
        String messagesArray[] = null;

        canHide = mGameString.contains("can_hide=yes");
        canUnHide = mGameString.contains("can_unhide=yes");

        go = false; goMarkStones = false; goEvaluateDeadStones = false;

        String[] dashLines = mGameString.split("\n");
        String dashLine;
        int idx = 0;
        String p1Name = "", p2Name = "";
        while (idx < dashLines.length) {
            dashLine = dashLines[idx];
            if (dashLine.indexOf("Go=MARK_DEAD_STONES") == 0) {
                goMarkStones = true;
            }
            if (dashLine.indexOf("Go=EVALUATE_DEAD_STONES") == 0) {
                goEvaluateDeadStones = true;
            }
            if (dashLine.indexOf("player1=") == 0) {
                p1Name = dashLine.substring(8).split(",")[0];
                if (!p1Name.toLowerCase().equals(PentePlayer.mPlayerName.toLowerCase())) {
                    this.mOpponentName = p1Name;
                    this.mOpponentRating = dashLine.substring(8).split(",")[1];
                }
            }
            if (dashLine.indexOf("current_player=") == 0) {
                mActive = dashLine.equals("current_player="+PentePlayer.mPlayerName.toLowerCase());
                amIPlaying = mActive;
            }
            if (dashLine.contains("undo=requested")) {
                undoRequested = true;
            }
            if (dashLine.indexOf("player2=") == 0) {
                p2Name = dashLine.substring(8).split(",")[0];
                if (!p2Name.toLowerCase().equals(PentePlayer.mPlayerName.toLowerCase())) {
                    this.mOpponentName = p2Name;
                    this.mOpponentRating = dashLine.substring(8).split(",")[1];
                }
            }
            if (dashLine.indexOf("sid=") == 0) {
                this.mSetID = dashLine.substring(4);
            }
            if (dashLine.indexOf("gameName=") == 0) {
                this.mGameType = dashLine.substring(9);
                if (mGameType.equals("Go") || mGameType.equals("Speed Go") ||
                        mGameType.equals("Go (9x9)") || mGameType.equals("Speed Go (9x9)") ||
                        mGameType.equals("Go (13x13)") || mGameType.equals("Speed Go (13x13)")) {
                    if (mGameType.contains("(9x9)")) {
                        gridSize = 9;
                    } else if (mGameType.contains("(13x13)")) {
                        gridSize = 13;
                    } else {
                        gridSize = 19;
                    }

                    boardView.gridSize = gridSize;
                    passMove = gridSize*gridSize;
                    go = true;
                }
            }
            if (dashLine.indexOf("moves=") == 0) {
                String movesString[] = dashLine.substring(6).split(",");
                this.mMovesList = new ArrayList<Integer>();
                for ( int i = 0; i < movesString.length; i++ ) {
                    if ("".equals(movesString[i])) {
                        continue;
                    }
                    this.mMovesList.add(Integer.parseInt(movesString[i]));
                }
            }
            if (dashLine.contains("dPenteState=2")) {
                this.dPenteChoice = true;
            }
            if (dashLine.indexOf("cancel="+getOpponentName()) == 0) {
                final Activity host = (Activity) boardView.getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(host);
                builder.setTitle(host.getString(R.string.cancellation_requested));

                String msg = "";
                if (!dashLine.substring(dashLine.indexOf(",")+1).equals("")) {
                    msg = "\n(" + dashLine.substring(dashLine.indexOf(",")+1) + ")";
                }
                builder.setMessage(host.getString(R.string.requests_cancellation, getOpponentName()) + msg);
                builder.setPositiveButton(host.getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ReplyCancelTask task = new ReplyCancelTask(getSetID(), getGameID(), "Yes", host);
                        task.execute((Void) null);
                    } });
                builder.setNegativeButton(host.getString(R.string.decline), new DialogInterface.OnClickListener() {
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
            if (dashLine.contains("messages=")) {
                messagesArray = dashLine.substring(9).split(",");
            }
            if (dashLine.contains("rated=")) {
                mRatedNot = dashLine.substring(6);
            }
            if (dashLine.contains("private=")) {
                mPrivateGame = dashLine.substring(8);
            }
            if (dashLine.contains("messageNums=")) {
                messageNums = dashLine.substring(12).split(",");
            }
            idx += 1;
        }
        if (!p1Name.equals(PentePlayer.mPlayerName.toLowerCase()) && !p2Name.equals(PentePlayer.mPlayerName.toLowerCase())) {
            this.mOpponentName = p1Name + " vs " + p2Name;
        }
        if (!mGameString.contains("state=active")) {
            mActive = false;
            if (PentePlayer.mSubscriber) {
                final BoardActivity host = (BoardActivity) boardView.getContext();
                ((Button) host.findViewById(R.id.submitButton)).setVisibility(View.GONE);
                Button dbBtn = (Button) host.findViewById(R.id.searchDBbutton);
                dbBtn.setVisibility(View.VISIBLE);
                dbBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(host.getApplicationContext(), DatabaseActivity.class);
                        intent.putIntegerArrayListExtra("moves", new ArrayList<Integer>(getMovesList().subList(0, untilMove)));
                        intent.putExtra("game", getGameType());
                        host.startActivity(intent);
                    }
                });
            }
        }

        if (!mActive && amIPlaying && !undoRequested) {
            final BoardActivity host = (BoardActivity) boardView.getContext();
            Button undoBtn = ((Button) host.findViewById(R.id.submitButton));
            undoBtn.setText(host.getString(R.string.request_undo));
            undoBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (PentePlayer.mSubscriber) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(host);
                        builder.setTitle(host.getString(R.string.rusure));
                        builder.setPositiveButton(host.getString(R.string.yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                RequestUndoTask task = new RequestUndoTask(getGameID(), host);
                                task.execute((Void) null);
                                dialog.dismiss();
                            } });
                        builder.setNegativeButton(host.getString(R.string.no), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            } });
                        final AlertDialog dialog = builder.show();
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(host);
                        builder.setTitle(host.getString(R.string.feature_not_available));

                        builder.setMessage(host.getString(R.string.undo_subscribers_only));
                        builder.setPositiveButton(host.getString(R.string.subscribe_now), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String url = "https://www.pente.org/gameServer/subscriptions?name2="+PentePlayer.mPlayerName+"&password2="+ PentePlayer.mPassword; // missing 'http://' will cause crashed
                                Intent intent = new Intent(host, WebViewActivity.class);
                                intent.putExtra("url", url);
                                host.startActivity(intent);
                                dialog.dismiss();
                            } });
                        builder.setNegativeButton(host.getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            } });
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(final DialogInterface arg0) {
                                    host.finish();
                                }
                            });
                        }
                        final AlertDialog dialog = builder.show();
                    }
                }
            });
        } else if (!mActive && amIPlaying && undoRequested) {
            final BoardActivity host = (BoardActivity) boardView.getContext();
            Button undoBtn = ((Button) host.findViewById(R.id.submitButton));
            undoBtn.setText(host.getString(R.string.undo_requested));
            undoBtn.setOnClickListener(null);
        } else if (mActive && amIPlaying && undoRequested) {
            final BoardActivity host = (BoardActivity) boardView.getContext();
            final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(host);
            builder.setTitle(host.getString(R.string.requests_undo, getOpponentName()));
            String options[] = {host.getString(R.string.accept), host.getString(R.string.decline)};
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ReplyUndoTask task;
                    switch (which) {
                        case 0:
                            task = new ReplyUndoTask(getGameID(), true, host);
                            task.execute((Void) null);
                            break;
                        case 1:
                            task = new ReplyUndoTask(getGameID(), false, host);
                            task.execute((Void) null);
                            break;
                    }
                    // the user clicked on colors[which]
                }
            });
            android.support.v7.app.AlertDialog dlg = builder.create();
            dlg.setCanceledOnTouchOutside(false);
            Window window = dlg.getWindow();
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.BOTTOM;
            dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setAttributes(wlp);
            dlg.show();
        } else {
            BoardActivity host = (BoardActivity) boardView.getContext();
            host.setRegularSubmitListener();
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
            replayGame(boardView);
//            boardView.invalidate();
        }

        if (mActive && goEvaluateDeadStones) {
            getTerritories();
            boardView.invalidate();
            final BoardActivity host = (BoardActivity) boardView.getContext();
            final AlertDialog.Builder builder = new AlertDialog.Builder(host);
            builder.setTitle(host.getString(R.string.score));
            int p1Territory = getGoTerritoryByPlayer().get(1).size(),
                    p2Territory = getGoTerritoryByPlayer().get(2).size(),
                    p1Stones = getMovesForValue(2).size(),
                    p2Stones = getMovesForValue(1).size();
            builder.setMessage(host.getString(R.string.scorestring, p1Territory, p1Stones, p1Stones+p1Territory, p2Territory, p2Stones, p2Territory+p2Stones+7));
            builder.setPositiveButton(host.getString(R.string.accept), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    submitMove("1","");
                    host.finish();
                }
            });
            builder.setNegativeButton(host.getString(R.string.reject), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    submitMove("0", "");
                    host.finish();
                }
            });
            AlertDialog dlg = builder.create();
            dlg.setCanceledOnTouchOutside(false);
            Window window = dlg.getWindow();
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.BOTTOM;
            dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setAttributes(wlp);
            dlg.show();
        }

        if (mActive && goMarkStones) {
            getTerritories();
            boardView.invalidate();

            final BoardActivity host = (BoardActivity) boardView.getContext();
            if (PrefUtils.getBooleanFromPrefs(host, PrefUtils.PREFS_DOUBLEPASSREMINDER_KEY, true)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(host);
                builder.setTitle(host.getString(R.string.score));
                int p1Territory = getGoTerritoryByPlayer().get(1).size(),
                        p2Territory = getGoTerritoryByPlayer().get(2).size(),
                        p1Stones = getMovesForValue(2).size(),
                        p2Stones = getMovesForValue(1).size();
                builder.setMessage(host.getString(R.string.double_pass));
                builder.setPositiveButton(host.getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                builder.setNegativeButton(host.getString(R.string.no_reminder), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PrefUtils.saveBooleanToPrefs(host, PrefUtils.PREFS_DOUBLEPASSREMINDER_KEY, false);
                    }
                });
                AlertDialog dlg = builder.create();
                dlg.setCanceledOnTouchOutside(true);
                Window window = dlg.getWindow();
                WindowManager.LayoutParams wlp = window.getAttributes();
                wlp.gravity = Gravity.BOTTOM;
//                dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setAttributes(wlp);
                dlg.show();
            }
        }
        ((BoardActivity)boardView.getContext()).invalidateOptionsMenu();
    }

    private String filterMessage(String msgStr) {

        String tmpStrComma = msgStr.replace("\\1", ",");
        String tmpStrQuote = tmpStrComma.replace("\\2", "\"");
        String tmpStrAmpersand = tmpStrQuote.replace("&amp;#38;", "&");
        String tmpStrSmiley = tmpStrAmpersand.replace("<img border='0' src='http://[host]/gameServer/forums/images/emoticons/happy.gif' alt=''>", ":)");
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

    public void replayGame(BoardView boardView) {
        if (mMovesList == null) {
            return;
        }

        this.untilMove = this.mMovesList.size();
        replayGameUntilMove(abstractBoard, boardView);

        if (boardView != null) {
            boardView.invalidate();
            if (mGameType.equals("Pente") && mOpponentName.equals("computer")) {
//                System.out.println("what white: " + whiteCaptures + " black: " + blackCaptures + " pente? " + detectPente(abstractBoard, (byte) (2 - (mMovesList.size()%2)), mMovesList.get(mMovesList.size() - 1)));
                if (whiteCaptures == 10 || blackCaptures == 10 || detectPente((byte) (2 - (mMovesList.size()%2)), mMovesList.get(mMovesList.size() - 1))) {
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

    private char coordinateLetters[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};
    public void replayGameUntilMove(byte[][] abstractBoard, BoardView boardView) {
        if (mMovesList == null) {
            return;
        }
        if (getGameType().equals("Gomoku") || getGameType().equals("Speed Gomoku")) {
            boardView.setBackgroundColor(boardView.gomokuColor);
            replayGomokuGame(untilMove);
        } else if (getGameType().equals("Pente") || getGameType().equals("Speed Pente")) {
            boardView.setBackgroundColor(boardView.penteColor);
            replayPenteGame(untilMove);
        } else if (getGameType().equals("Boat-Pente") || getGameType().equals("Speed Boat-Pente")) {
            boardView.setBackgroundColor(boardView.boatPenteColor);
            replayPenteGame(untilMove);
        } else if (getGameType().equals("Keryo-Pente") || getGameType().equals("Speed Keryo-Pente")) {
            boardView.setBackgroundColor(boardView.keryoPenteColor);
            replayKeryoPenteGame(untilMove);
        } else if (getGameType().equals("Connect6") || getGameType().equals("Speed Connect6")) {
            boardView.setBackgroundColor(boardView.connect6Color);
            replayConnect6Game(untilMove);
        } else if (getGameType().equals("G-Pente") || getGameType().equals("Speed G-Pente")) {
            boardView.setBackgroundColor(boardView.gPenteColor);
            replayGPenteGame(untilMove);
        } else if (getGameType().equals("Poof-Pente") || getGameType().equals("Speed Poof-Pente")) {
            boardView.setBackgroundColor(boardView.poofPenteColor);
            replayPoofPenteGame(untilMove);
        } else if (getGameType().equals("D-Pente") || getGameType().equals("Speed D-Pente")) {
            boardView.setBackgroundColor(boardView.dPenteColor);
            replayPenteGame(untilMove);
        } else if (getGameType().equals("DK-Pente") || getGameType().equals("Speed DK-Pente")) {
            boardView.setBackgroundColor(boardView.dkeryoColor);
            replayKeryoPenteGame(untilMove);
        } else if (isGo()) {
            boardView.setBackgroundColor(boardView.goColor);
            replayGoGame(untilMove);
        } else if (getGameType().equals("O-Pente") || getGameType().equals("Speed O-Pente")) {
            boardView.setBackgroundColor(boardView.oPenteColor);
            replayOPenteGame(untilMove);
        }

        movesString = "";
        if (isConnect6()) {
            for (int i = 0; i < Math.min(mMovesList.size(), untilMove); i++) {
                if (i == 0) {
                    movesString += "<b>1.</b> ";
                } else {
                    if (((i - 3) % 4) == 0) {
                        movesString += " <b>" + ((i >> 2) + 2) + ".</b> ";
                    } else if (((i - 3) % 4) == 2 || i == 1) {
                        movesString += " - ";
                    } else {
                        movesString += "-";
                    }
                }
                int move = mMovesList.get(i), moveI = move/19, moveJ = move%19;
                movesString += coordinateLetters[moveJ] + "" + (19 - (moveI));
            }
        } else {
            for (int i = 0; i < Math.min(mMovesList.size(), untilMove); i++) {
                if (i%2 == 0) {
                    movesString += "<b>" + (i/2 + 1) + ".</b> ";
                }
                int move = mMovesList.get(i), moveI = move/gridSize, moveJ = move%gridSize;
                if (move == gridSize*gridSize) {
                    movesString += "PASS ";
                } else {
                    movesString += coordinateLetters[moveJ] + "" + (gridSize - (moveI)) + " ";
                }
                if (i%2 == 0) {
                    movesString += "- ";
                } else {
                    movesString += "  ";
                }
            }
        }


        if (boardView != null && mMovesList.size()>=untilMove && untilMove > 0) {
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

    public void replayGame(byte moveI, byte moveJ, BoardView boardView) {
        if (mMovesList == null) {
            return;
        }

        if (getGameType().equals("Pente") || getGameType().equals("Speed Pente")) {
            boardView.setBackgroundColor(boardView.penteColor);
            replayPenteGame(moveI, moveJ);
        } else if (getGameType().equals("Boat-Pente") || getGameType().equals("Speed Boat-Pente")) {
            boardView.setBackgroundColor(boardView.boatPenteColor);
            replayPenteGame(moveI, moveJ);
        } else if (getGameType().equals("Keryo-Pente") || getGameType().equals("Speed Keryo-Pente")) {
            boardView.setBackgroundColor(boardView.keryoPenteColor);
            replayKeryoPenteGame(moveI, moveJ);
        } else if (getGameType().equals("G-Pente") || getGameType().equals("Speed G-Pente")) {
            boardView.setBackgroundColor(boardView.gPenteColor);
            replayPenteGame(moveI, moveJ);
        } else if (getGameType().equals("Poof-Pente") || getGameType().equals("Speed Poof-Pente")) {
            boardView.setBackgroundColor(boardView.poofPenteColor);
            replayPoofPenteGame(moveI, moveJ);
        } else if (getGameType().equals("D-Pente") || getGameType().equals("Speed D-Pente")) {
            boardView.setBackgroundColor(boardView.dPenteColor);
            replayPenteGame(moveI, moveJ);
        } else if (getGameType().equals("DK-Pente") || getGameType().equals("Speed DK-Pente")) {
            boardView.setBackgroundColor(boardView.dkeryoColor);
            replayKeryoPenteGame(moveI, moveJ);
        } else if (isGo()) {
            boardView.setBackgroundColor(boardView.goColor);
            replayGoGame(moveI, moveJ);
        } else if (getGameType().equals("O-Pente") || getGameType().equals("Speed O-Pente")) {
            boardView.setBackgroundColor(boardView.oPenteColor);
            replayOPenteGame(moveI, moveJ);
        }

        if (boardView != null) {
            boardView.invalidate();
        }

    }



    private void resetAbstractBoard() {
        whiteCaptures = 0;
        blackCaptures = 0;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                abstractBoard[i][j] = 0;
            }
        }
    }

    private void replayGomokuGame(int until) {
        resetAbstractBoard();
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            int move = mMovesList.get(i), moveI = move/19, moveJ = move%19;
            abstractBoard[moveI][moveJ] = color;
        }
    }

    private void replayPenteGame(int until) {
        resetAbstractBoard();
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            int move = mMovesList.get(i), moveI = move/19, moveJ = move%19;
            abstractBoard[moveI][moveJ] = color;
            detectPenteCapture(moveI, moveJ, color);
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
            if (whiteCaptures == 10 || blackCaptures == 10 || detectPente((byte) (2 - (mMovesList.size()%2)), mMovesList.get(mMovesList.size() - 1))) {
//                for (int i = 0; i < 19; i++) {
//                    for (int j = 0; j < 19; j++) {
//                        System.out.print(abstractBoard[i][j]);
//                    }
//                    System.out.println();
//                }
                mActive = false;
            }
        }
    }
    private void replayPenteGame(byte moveI, byte moveJ) {
        resetAbstractBoard();
        for (int i = 0; i < mMovesList.size(); i++) {
            byte color = (byte) (1 + (i%2));
            int move = mMovesList.get(i), mvI = move/19, mvJ = move%19;
            abstractBoard[mvI][mvJ] = color;
            detectPenteCapture(mvI, mvJ, color);
        }
        byte color = (byte) (1 + (mMovesList.size()%2));
        abstractBoard[moveI][moveJ] = color;
//        System.out.println(" kitty heeeelp " + moveI + " and " + moveJ + " and " + color);
        detectPenteCapture(moveI, moveJ, color);
    }

    private void replayKeryoPenteGame(int until) {
        resetAbstractBoard();
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            int move = mMovesList.get(i), moveI = move/19, moveJ = move%19;
            abstractBoard[moveI][moveJ] = color;
            detectPenteCapture(moveI, moveJ, color);
            detectKeryoPenteCapture(moveI, moveJ, color);
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
    private void replayOPenteGame(int until) {
        resetAbstractBoard();
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            int move = mMovesList.get(i), moveI = move/19, moveJ = move%19;
            abstractBoard[moveI][moveJ] = color;
            detectPoof(moveI, moveJ, color);
            detectKeryoPoof(moveI, moveJ, color);
            detectPenteCapture(moveI, moveJ, color);
            detectKeryoPenteCapture(moveI, moveJ, color);
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
    private void replayOPenteGame(byte moveI, byte moveJ) {
        resetAbstractBoard();
        for (int i = 0; i < mMovesList.size(); i++) {
            byte color = (byte) (1 + (i%2));
            int move = mMovesList.get(i), mvI = move/19, mvJ = move%19;
            abstractBoard[mvI][mvJ] = color;
            detectPoof(mvI, mvJ, color);
            detectKeryoPoof(mvI, mvJ, color);
            detectPenteCapture(mvI, mvJ, color);
            detectKeryoPenteCapture(mvI, mvJ, color);
        }
        byte color = (byte) (1 + (mMovesList.size()%2));
        abstractBoard[moveI][moveJ] = color;
        detectPoof(moveI, moveJ, color);
        detectKeryoPoof(moveI, moveJ, color);
        detectPenteCapture(moveI, moveJ, color);
        detectKeryoPenteCapture(moveI, moveJ, color);
    }

    private void replayKeryoPenteGame(byte moveI, byte moveJ) {
        resetAbstractBoard();
        for (int i = 0; i < mMovesList.size(); i++) {
            byte color = (byte) (1 + (i%2));
            int move = mMovesList.get(i), mvI = move/19, mvJ = move%19;
            abstractBoard[mvI][mvJ] = color;
            detectPenteCapture(mvI, mvJ, color);
            detectKeryoPenteCapture(mvI, mvJ, color);
        }
        byte color = (byte) (1 + (mMovesList.size()%2));
        abstractBoard[moveI][moveJ] = color;
        detectPenteCapture(moveI, moveJ, color);
        detectKeryoPenteCapture(moveI, moveJ, color);
    }

    private void replayConnect6Game(int until) {
        resetAbstractBoard();
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) ((((i % 4) == 0) || ((i % 4) == 3)) ? 1 : 2);
            int move = mMovesList.get(i), moveI = move/19, moveJ = move%19;
            abstractBoard[moveI][moveJ] = color;
        }
    }

    private void replayGPenteGame(int until) {
        resetAbstractBoard();
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            int move = mMovesList.get(i), moveI = move/19, moveJ = move%19;
            abstractBoard[moveI][moveJ] = color;
            detectPenteCapture(moveI, moveJ, color);
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

    private void replayPoofPenteGame(int until) {
        resetAbstractBoard();
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (1 + (i%2));
            int move = mMovesList.get(i), moveI = move/19, moveJ = move%19;
            abstractBoard[moveI][moveJ] = color;
            detectPoof(moveI, moveJ, color);
            detectPenteCapture(moveI, moveJ, color);
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
    private void replayPoofPenteGame(byte moveI, byte moveJ) {
        resetAbstractBoard();
        for (int i = 0; i < mMovesList.size(); i++) {
            byte color = (byte) (1 + (i%2));
            int move = mMovesList.get(i), mvI = move/19, mvJ = move%19;
            abstractBoard[mvI][mvJ] = color;
            detectPoof(mvI, mvJ, color);
            detectPenteCapture(mvI, mvJ, color);
        }
        byte color = (byte) (1 + (mMovesList.size()%2));
        abstractBoard[moveI][moveJ] = color;
        detectPoof(moveI, moveJ, color);
        detectPenteCapture(moveI, moveJ, color);
    }


    private void detectPenteCapture(int i, int j, byte myColor) {
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
    private void detectKeryoPenteCapture(int i, int j, byte myColor) {
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
    private void detectPoof(int i, int j, byte myColor) {
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

    private void detectKeryoPoof(int i, int j, byte myColor) {
        byte opponentColor = (byte) (1 + (myColor % 2));
        boolean poofed = false;
        if (((i-3) > -1) && ((i+1) < 19)) { // left
            if (abstractBoard[i-1][j] == myColor && abstractBoard[i-2][j] == myColor) {
                if ((abstractBoard[i-3][j] == opponentColor) && (abstractBoard[i+1][j] == opponentColor)) {
                    abstractBoard[i-2][j] = 0;
                    abstractBoard[i-1][j] = 0;
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
        if (((i-3) > -1) && ((j-3) > -1) && ((i+1) < 19) && ((j+1) < 19)) { // up left
            if (abstractBoard[i-1][j-1] == myColor && abstractBoard[i-2][j-2] == myColor) {
                if ((abstractBoard[i-3][j-3] == opponentColor) && (abstractBoard[i+1][j+1] == opponentColor)) {
                    abstractBoard[i-2][j-2] = 0;
                    abstractBoard[i-1][j-1] = 0;
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
        if (((j-3) > -1) && ((j+1) < 19)) { // up
            if (abstractBoard[i][j-1] == myColor && abstractBoard[i][j-2] == myColor) {
                if ((abstractBoard[i][j-3] == opponentColor) && (abstractBoard[i][j+1] == opponentColor)) {
                    abstractBoard[i][j-2] = 0;
                    abstractBoard[i][j-1] = 0;
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
        if (((i-1) > -1) && ((j-3) > -1) && ((i+3) < 19) && ((j+1) < 19)) { // up right
            if (abstractBoard[i+1][j-1] == myColor && abstractBoard[i+2][j-2] == myColor) {
                if ((abstractBoard[i-1][j+1] == opponentColor) && (abstractBoard[i+3][j-3] == opponentColor)) {
                    abstractBoard[i+2][j-2] = 0;
                    abstractBoard[i+1][j-1] = 0;
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
        if (((i+3) < 19) && ((i-1) > -1)) { // right
            if (abstractBoard[i+1][j] == myColor && abstractBoard[i+2][j] == myColor) {
                if ((abstractBoard[i+3][j] == opponentColor) && (abstractBoard[i-1][j] == opponentColor)) {
                    abstractBoard[i+2][j] = 0;
                    abstractBoard[i+1][j] = 0;
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
        if (((i-1) > -1) && ((j-1) > -1) && ((i+3) < 19) && ((j+3) < 19)) { // down right
            if (abstractBoard[i+1][j+1] == myColor && abstractBoard[i+2][j+2] == myColor) {
                if ((abstractBoard[i-1][j-1] == opponentColor) && (abstractBoard[i+3][j+3] == opponentColor)) {
                    abstractBoard[i+2][j+2] = 0;
                    abstractBoard[i+1][j+1] = 0;
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
        if (((j+2) < 19) && ((j-1) > -1)) { // down
            if (abstractBoard[i][j+1] == myColor && abstractBoard[i][j+2] == myColor) {
                if ((abstractBoard[i][j-1] == opponentColor) && (abstractBoard[i][j+3] == opponentColor)) {
                    abstractBoard[i][j+1] = 0;
                    abstractBoard[i][j+2] = 0;
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
        if (((i-3) > -1) && ((j-1) > -1) && ((i+1) < 19) && ((j+3) < 19)) { // down left
            if (abstractBoard[i-1][j+1] == myColor && abstractBoard[i-2][j+2] == myColor) {
                if ((abstractBoard[i+1][j-1] == opponentColor) && (abstractBoard[i-3][j+3] == opponentColor)) {
                    abstractBoard[i-2][j+2] = 0;
                    abstractBoard[i-1][j+1] = 0;
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
        if (((i-2) > -1) && ((i+2) < 19)) { // horizontal
            if (abstractBoard[i-1][j] == myColor && abstractBoard[i+1][j] == myColor) {
                if ((abstractBoard[i-2][j] == opponentColor) && (abstractBoard[i+2][j] == opponentColor)) {
                    abstractBoard[i+1][j] = 0;
                    abstractBoard[i-1][j] = 0;
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
        if (((i-2) > -1) && ((j-2) > -1) && ((i+2) < 19) && ((j+2) < 19)) { // up left
            if (abstractBoard[i-1][j-1] == myColor && abstractBoard[i+1][j+1] == myColor) {
                if ((abstractBoard[i-2][j-2] == opponentColor) && (abstractBoard[i+2][j+2] == opponentColor)) {
                    abstractBoard[i+1][j+1] = 0;
                    abstractBoard[i-1][j-1] = 0;
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
        if (((j-2) > -1) && ((j+2) < 19)) { // vertical
            if (abstractBoard[i][j-1] == myColor && abstractBoard[i][j+1] == myColor) {
                if ((abstractBoard[i][j-2] == opponentColor) && (abstractBoard[i][j+2] == opponentColor)) {
                    abstractBoard[i][j+1] = 0;
                    abstractBoard[i][j-1] = 0;
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
        if (((i-2) > -1) && ((j-2) > -1) && ((i+2) < 19) && ((j+2) < 19)) { // up right
            if (abstractBoard[i+1][j-1] == myColor && abstractBoard[i+1][j-1] == myColor) {
                if ((abstractBoard[i-2][j+2] == opponentColor) && (abstractBoard[i+2][j-2] == opponentColor)) {
                    abstractBoard[i+1][j-1] = 0;
                    abstractBoard[i-1][j+1] = 0;
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

        private boolean detectPente(byte color, int rowCol) {
        boolean pente = false;
        int penteCounter = 1;
        int row = rowCol / 19, col = rowCol % 19, i, j;
//        System.out.println("row: " + row + " column: " + col + " color: " + color);
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
//            System.out.println("rowPente");
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
//            System.out.println("columnPente");
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
//            System.out.println("rightDiagonalPente");
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
//        System.out.println("leftDiagonalPente");

        return pente;
    }



    private int getPosition(int move) {
        int i = move/gridSize, j = move%gridSize;
        return abstractBoard[i][j];
    }

    private void setPosition(int move, int val) {
        int i = move/gridSize, j = move%gridSize;
        abstractBoard[i][j] = (byte) val;
    }
    private int getPosition(int i, int j) {
        return abstractBoard[i][j];
    }

    private void setPosition(int i, int j, int val) {
        abstractBoard[i][j] = (byte) val;
    }





    private HashMap<Integer,HashMap<Integer, List<Integer>>> groupsByPlayerAndID;
    private HashMap<Integer,HashMap<Integer, Integer>> stoneGroupIDsByPlayer;
    public int koMove = -1;
    private List<Integer> deadStones;
    private HashMap<Integer, List<Integer>> goTerritoryByPlayer;
    private HashMap<Integer, List<Integer>> goDeadStonesByPlayer;

    public HashMap<Integer, HashMap<Integer, List<Integer>>> getGroupsByPlayerAndID() { return groupsByPlayerAndID; }
    public HashMap<Integer, HashMap<Integer, Integer>> getStoneGroupIDsByPlayer() { return stoneGroupIDsByPlayer; }
    public HashMap<Integer, List<Integer>> getGoDeadStonesByPlayer() { return goDeadStonesByPlayer; }
    public HashMap<Integer, List<Integer>> getGoTerritoryByPlayer() { return goTerritoryByPlayer; }

    public int getGridSize() { return gridSize; }

    private int gridSize = 19;
    private int passMove = 361;

    private boolean go, goMarkStones, goEvaluateDeadStones;

    public boolean isGo() { return go; }
    public boolean isGoMarkStones() { return goMarkStones; }
    public boolean isGoEvaluateDeadStones() { return goEvaluateDeadStones; }


    private void initGo() {
        whiteCaptures = 0;
        blackCaptures = 0;
        this.groupsByPlayerAndID = new HashMap<>();
        this.groupsByPlayerAndID.put(1, new HashMap<Integer, List<Integer>>());
        this.groupsByPlayerAndID.put(2, new HashMap<Integer, List<Integer>>());
        this.stoneGroupIDsByPlayer = new HashMap<>();
        this.stoneGroupIDsByPlayer.put(1, new HashMap<Integer, Integer>());
        this.stoneGroupIDsByPlayer.put(2, new HashMap<Integer, Integer>());
        this.koMove = -1;
        this.goDeadStonesByPlayer = new HashMap<>();
        this.goDeadStonesByPlayer.put(1, new ArrayList<Integer>());
        this.goDeadStonesByPlayer.put(2, new ArrayList<Integer>());
        goTerritoryByPlayer = new HashMap<Integer, List<Integer>>();
        goTerritoryByPlayer.put(1, new ArrayList<Integer>());
        goTerritoryByPlayer.put(2, new ArrayList<Integer>());
    }

    private void replayGoGame(int until) {
        resetAbstractBoard();
        initGo();
        boolean suicideAllowed = false, hasPass = false, doublePass = false;
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
//            int move = mMovesList.get(i), moveI = move / gridSize, moveJ = move % gridSize;
            int move = mMovesList.get(i);
            if (move == passMove) {
                if (hasPass) {
                    doublePass = true;
                } else {
                    hasPass = true;
                }
            } else {
                hasPass = false;
            }

            if (doublePass) {
                if (0 <= move && move < passMove) {
                    if (getPosition(move) == 1) {
                        goDeadStonesByPlayer.get(2).add(move);
                    } else if (getPosition(move) == 2) {
                        goDeadStonesByPlayer.get(1).add(move);
                    }
                    setPosition(move, 0);
                }
            } else {
                int currentPlayer = 1 + i%2;
                int color = 3 - currentPlayer;
                if (0 <= move && move < passMove) {
                    setPosition(move, color);

                    Map<Integer, List<Integer>> groupsByID = getGroupsByPlayerAndID().get(currentPlayer);
                    Map<Integer, Integer> stoneGroupIDs = getStoneGroupIDsByPlayer().get(currentPlayer);
                    settleGroups(move, groupsByID, stoneGroupIDs);

                    int opponent = 3 - currentPlayer;
                    groupsByID = getGroupsByPlayerAndID().get(opponent);
                    stoneGroupIDs = getStoneGroupIDsByPlayer().get(opponent);
                    makeCaptures(move, groupsByID, stoneGroupIDs);

                    if (suicideAllowed) {
                        groupsByID = getGroupsByPlayerAndID().get(currentPlayer);
                        stoneGroupIDs = getStoneGroupIDsByPlayer().get(currentPlayer);
                        int moveGroupID = stoneGroupIDs.get(move);
                        List<Integer> moveGroup = groupsByID.get(moveGroupID);
                        if (!groupHasLiberties(moveGroup)) {
                            captureGroup(moveGroupID, groupsByID, stoneGroupIDs);
                        }
                    }
                }
            }
        }
    }
    private void replayGoGame(byte moveI, byte moveJ) {
        resetAbstractBoard();
        initGo();
        boolean suicideAllowed = false, hasPass = false, doublePass = false;
        for (int i = 0; i < mMovesList.size(); i++) {
            int move = mMovesList.get(i);
            if (move == passMove) {
                if (hasPass) {
                    doublePass = true;
                } else {
                    hasPass = true;
                }
            } else {
                hasPass = false;
            }

            if (doublePass) {
                if (0 <= move && move < passMove) {
                    if (getPosition(move) == 1) {
                        goDeadStonesByPlayer.get(2).add(move);
                    } else if (getPosition(move) == 2) {
                        goDeadStonesByPlayer.get(1).add(move);
                    }
                    setPosition(move, 0);
                }
            } else {
                int currentPlayer = 1 + i%2;
                int color = 3 - currentPlayer;
                if (0 <= move && move < passMove) {
                    setPosition(move, color);

                    Map<Integer, List<Integer>> groupsByID = getGroupsByPlayerAndID().get(currentPlayer);
                    Map<Integer, Integer> stoneGroupIDs = getStoneGroupIDsByPlayer().get(currentPlayer);
                    settleGroups(move, groupsByID, stoneGroupIDs);

                    int opponent = 3 - currentPlayer;
                    groupsByID = getGroupsByPlayerAndID().get(opponent);
                    stoneGroupIDs = getStoneGroupIDsByPlayer().get(opponent);
                    makeCaptures(move, groupsByID, stoneGroupIDs);

                    if (suicideAllowed) {
                        groupsByID = getGroupsByPlayerAndID().get(currentPlayer);
                        stoneGroupIDs = getStoneGroupIDsByPlayer().get(currentPlayer);
                        int moveGroupID = stoneGroupIDs.get(move);
                        List<Integer> moveGroup = groupsByID.get(moveGroupID);
                        if (!groupHasLiberties(moveGroup)) {
                            captureGroup(moveGroupID, groupsByID, stoneGroupIDs);
                        }
                    }
                }
            }
        }
        int move = moveI*gridSize+moveJ;
        if (doublePass) {
            if (0 <= move && move < passMove) {
                if (getPosition(move) == 1) {
                    goDeadStonesByPlayer.get(2).add(move);
                } else if (getPosition(move) == 2) {
                    goDeadStonesByPlayer.get(1).add(move);
                }
                setPosition(move, 0);
            }
        } else {
            int currentPlayer = 1 + getMovesList().size()%2;
            int color = 3 - currentPlayer;
            if (0 <= move && move < passMove) {
                setPosition(move, color);

                Map<Integer, List<Integer>> groupsByID = getGroupsByPlayerAndID().get(currentPlayer);
                Map<Integer, Integer> stoneGroupIDs = getStoneGroupIDsByPlayer().get(currentPlayer);
                settleGroups(move, groupsByID, stoneGroupIDs);

                int opponent = 3 - currentPlayer;
                groupsByID = getGroupsByPlayerAndID().get(opponent);
                stoneGroupIDs = getStoneGroupIDsByPlayer().get(opponent);
                makeCaptures(move, groupsByID, stoneGroupIDs);

                if (suicideAllowed) {
                    groupsByID = getGroupsByPlayerAndID().get(currentPlayer);
                    stoneGroupIDs = getStoneGroupIDsByPlayer().get(currentPlayer);
                    int moveGroupID = stoneGroupIDs.get(move);
                    List<Integer> moveGroup = groupsByID.get(moveGroupID);
                    if (!groupHasLiberties(moveGroup)) {
                        captureGroup(moveGroupID, groupsByID, stoneGroupIDs);
                    }
                }
            }
        }
    }

//    public void addGoMove(int move) {
//
//        boolean suicideAllowed = false;
//
////        if (containsDoublePass() > -1) {
////
////            i
////            deadStones.add(move);
////
////        } else {
//
//            int currentPlayer = 1 + mMovesList.size()%2;
//            int color = 3 - currentPlayer;
//            mMovesList.add(move);
//            setPosition(move, color);
//
//            if (0 <= move && move < passMove) {
//
//                Map<Integer, List<Integer>> groupsByID = getGroupsByPlayerAndID().get(currentPlayer);
//                Map<Integer, Integer> stoneGroupIDs = getStoneGroupIDsByPlayer().get(currentPlayer);
//                settleGroups(move, groupsByID, stoneGroupIDs);
//
//                int opponent = 3 - currentPlayer;
//                groupsByID = getGroupsByPlayerAndID().get(opponent);
//                stoneGroupIDs = getStoneGroupIDsByPlayer().get(opponent);
//                makeCaptures(move, groupsByID, stoneGroupIDs);
//
//                if (suicideAllowed) {
//                    groupsByID = getGroupsByPlayerAndID().get(currentPlayer);
//                    stoneGroupIDs = getStoneGroupIDsByPlayer().get(currentPlayer);
//                    int moveGroupID = stoneGroupIDs.get(move);
//                    List<Integer> moveGroup = groupsByID.get(moveGroupID);
//                    if (!groupHasLiberties(moveGroup)) {
//                        captureGroup(moveGroupID, groupsByID, stoneGroupIDs);
//                    }
//                }
//            }
//
////        }
//    }
//


    private void makeCaptures(int move, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs) {
        int captures = 0;
        if (move%gridSize != 0) {
            int neighborStone = move - 1;
            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
            captures = getCaptures(move, groupsByID, stoneGroupIDs, captures, neighborStone, neighborStoneGroupID);
        }
        if (move%gridSize != gridSize - 1) {
            int neighborStone = move + 1;
            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
            captures = getCaptures(move, groupsByID, stoneGroupIDs, captures, neighborStone, neighborStoneGroupID);
        }
        if (move/gridSize != 0) {
            int neighborStone = move - gridSize;
            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
            captures = getCaptures(move, groupsByID, stoneGroupIDs, captures, neighborStone, neighborStoneGroupID);
        }
        if (move/gridSize != gridSize - 1) {
            int neighborStone = move + gridSize;
            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
            captures = getCaptures(move, groupsByID, stoneGroupIDs, captures, neighborStone, neighborStoneGroupID);
        }
        if (captures != 1) {
            koMove = -1;
        }
//        this.captures[capturer] += captures;
    }

    private int getCaptures(int move, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs, int captures, int neighborStone, Integer neighborStoneGroupID) {
        if (neighborStoneGroupID != null) {
            List<Integer> neighborStoneGroup = groupsByID.get(neighborStoneGroupID);
            if (!groupHasLiberties(neighborStoneGroup)) {
                if (koMove < 0 && neighborStoneGroup.size() == 1 && checkKo(move)) {
                    koMove = neighborStone;
                } else {
                    koMove = -1;
                }
                captures += neighborStoneGroup.size();
                captureGroup(neighborStoneGroupID, groupsByID, stoneGroupIDs);
            }
        }
        return captures;
    }

    private boolean checkKo(int move) {
        int position = getPosition(move);
        if (move%gridSize != 0) {
            int neighborStone = move - 1;
            int neighborPosition = getPosition(neighborStone);
            if (position != 3 - neighborPosition) {
                return false;
            }
        }
        if (move%gridSize != gridSize - 1) {
            int neighborStone = move + 1;
            int neighborPosition = getPosition(neighborStone);
            if (position != 3 - neighborPosition) {
                return false;
            }
        }
        if (move/gridSize != 0) {
            int neighborStone = move - gridSize;
            int neighborPosition = getPosition(neighborStone);
            if (position != 3 - neighborPosition) {
                return false;
            }
        }
        if (move/gridSize != gridSize - 1) {
            int neighborStone = move + gridSize;
            int neighborPosition = getPosition(neighborStone);
            if (position != 3 - neighborPosition) {
                return false;
            }
        }
        return true;
    }


    private void captureGroup(int groupID, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs) {
        List<Integer> group = groupsByID.get(groupID);
//        int capturer = 0;
        if (group.size() > 0) {
//            capturer = 3 - getPosition(group.get(0));
            int pos = getPosition(group.get(0));
            if (pos == 1) {
                whiteCaptures += group.size();
            } else if (pos == 2) {
                blackCaptures += group.size();
            }
        }
        for (int stone: group) {
//            captureMove(stone, capturer);
            setPosition(stone, 0);
            stoneGroupIDs.remove(stone);
        }
        groupsByID.remove(groupID);
    }

    private boolean groupHasLiberties(List<Integer> group) {
        for (int stone: group) {
            if (stoneHasLiberties(stone)) {
                return true;
            }
        }
        return false;
    }
    private boolean stoneHasLiberties(int stone) {
        if (stone%gridSize != 0) {
            int neighborStone = stone - 1;
            int position = getPosition(neighborStone);
            if (position != 1 && position != 2) {
                return true;
            }
        }
        if (stone%gridSize != gridSize - 1) {
            int neighborStone = stone + 1;
            int position = getPosition(neighborStone);
            if (position != 1 && position != 2) {
                return true;
            }
        }
        if (stone/gridSize != 0) {
            int neighborStone = stone - gridSize;
            int position = getPosition(neighborStone);
            if (position != 1 && position != 2) {
                return true;
            }
        }
        if (stone/gridSize != gridSize - 1) {
            int neighborStone = stone + gridSize;
            int position = getPosition(neighborStone);
            if (position != 1 && position != 2) {
                return true;
            }
        }
        return false;
    }

    private void settleGroups(int move, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs) {
        List<Integer> newGroup = new ArrayList<>();
        newGroup.add(move);
        groupsByID.put(move, newGroup);
        stoneGroupIDs.put(move, move);
        if (move%gridSize != 0) {
            int neighborStone = move - 1;
            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
            if (neighborStoneGroupID != null) {
                mergeGroups(move, neighborStoneGroupID, groupsByID, stoneGroupIDs);
            }
        }
        if (move%gridSize != gridSize - 1) {
            int neighborStone = move + 1;
            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
            if (neighborStoneGroupID != null) {
                mergeGroups(stoneGroupIDs.get(move), neighborStoneGroupID, groupsByID, stoneGroupIDs);
            }
        }
        if (move/gridSize != 0) {
            int neighborStone = move - gridSize;
            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
            if (neighborStoneGroupID != null) {
                mergeGroups(stoneGroupIDs.get(move), neighborStoneGroupID, groupsByID, stoneGroupIDs);
            }
        }
        if (move/gridSize != gridSize - 1) {
            int neighborStone = move + gridSize;
            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
            if (neighborStoneGroupID != null) {
                mergeGroups(stoneGroupIDs.get(move), neighborStoneGroupID, groupsByID, stoneGroupIDs);
            }
        }
    }

    private void mergeGroups(int group1, int group2, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs) {
        if (group1 == group2) {
            return;
        }
        List<Integer> oldGroup, newGroup;
        int oldGroupID, newGroupID;
        if (group1 < group2) {
            oldGroup = groupsByID.get(group1);
            newGroup = groupsByID.get(group2);
            oldGroupID = group1;
            newGroupID = group2;
        } else {
            oldGroup = groupsByID.get(group2);
            newGroup = groupsByID.get(group1);
            oldGroupID = group2;
            newGroupID = group1;
        }
        groupsByID.remove(oldGroupID);
        newGroup.addAll(oldGroup);
        for (int oldStone: oldGroup) {
            stoneGroupIDs.put(oldStone, newGroupID);
        }
    }

//    private List<Integer> getGroupWithoutMerge(int move, Map<Integer, List<Integer>> groupsByID, Map<Integer, Integer> stoneGroupIDs) {
//        List<Integer> newGroup = new ArrayList<>();
//        newGroup.add(move);
//        int gridSize = gridSize;
//        if (move%gridSize != 0) {
//            int neighborStone = move - 1;
//            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
//            if (neighborStoneGroupID != null) {
//                newGroup.addAll(groupsByID.get(neighborStoneGroupID));
//            }
//        }
//        if (move%gridSize != gridSize - 1) {
//            int neighborStone = move + 1;
//            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
//            if (neighborStoneGroupID != null) {
//                newGroup.addAll(groupsByID.get(neighborStoneGroupID));
//            }
//        }
//        if (move/gridSize != 0) {
//            int neighborStone = move - gridSize;
//            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
//            if (neighborStoneGroupID != null) {
//                newGroup.addAll(groupsByID.get(neighborStoneGroupID));
//            }
//        }
//        if (move/gridSize != gridSize - 1) {
//            int neighborStone = move + gridSize;
//            Integer neighborStoneGroupID = stoneGroupIDs.get(neighborStone);
//            if (neighborStoneGroupID != null) {
//                newGroup.addAll(groupsByID.get(neighborStoneGroupID));
//            }
//        }
//        return newGroup;
//    }

    private void captureMove(int move, int capturePlayer) {
        setPosition(move, 0);
    }
//    public void undoMove() {
//        gridState.clear();
//
//        init();
//        for (int move: gridState.getMoves()) {
//            addMove(move);
//        }
//    }

    private int getEmptyNeighbour(int move) {
        if (move%gridSize != 0) {
            int neighborStone = move - 1;
            if (getPosition(neighborStone) == 0) {
                return neighborStone;
            }
        }
        if (move%gridSize != gridSize - 1) {
            int neighborStone = move + 1;
            if (getPosition(neighborStone) == 0) {
                return neighborStone;
            }
        }
        if (move/gridSize != 0) {
            int neighborStone = move - gridSize;
            if (getPosition(neighborStone) == 0) {
                return neighborStone;
            }
        }
        if (move/gridSize != gridSize - 1) {
            int neighborStone = move + gridSize;
            if (getPosition(neighborStone) == 0) {
                return neighborStone;
            }
        }
        return -1;
    }
    private void floodFillWorker(int move, int value) {
        setPosition(move, value);
        int neighbourStone = getEmptyNeighbour(move);
        while (neighbourStone != -1) {
            floodFillWorker(neighbourStone, value);
            neighbourStone = getEmptyNeighbour(move);
        }
    }
    private List<Integer> floodFill(int player) {
        for (int move = 0; move < passMove; move++) {
            int pos = getPosition(move);
            if (pos == 3-player) {
                int neighbourStone = getEmptyNeighbour(move);
                while (neighbourStone != -1) {
                    floodFillWorker(neighbourStone, player + 2);
                    neighbourStone = getEmptyNeighbour(move);
                }
            }
        }
        List<Integer> floodedTerritory = new ArrayList<>();
        for (int i = 0; i < passMove; i++) {
            int val = getPosition(i);
            if (val == player + 2) {
                floodedTerritory.add(i);
            }
        }
        return floodedTerritory;
    }
    private void resetGoBeforeFlood() {
        for (int i = 0; i < gridSize; i++ ) {
            for (int j = 0; j < gridSize; j++ ) {
                int pos = getPosition(i,j);
                if (pos != 1 && pos != 2) {
                    setPosition(i,j,0);
                }
            }
        }
    }
    public List<Integer> getMovesForValue(int val) {
        List<Integer> results = new ArrayList<>();
        for (int j = 0; j < gridSize; j++) {
            for (int i = 0; i < gridSize; i++) {
                if (getPosition(i,j) == val) {
                    results.add(j*gridSize+i);
                }
            }
        }
        return results;
    }
    public void getTerritories() {
        goTerritoryByPlayer = new HashMap<Integer, List<Integer>>();
        floodFill(1);
        List<Integer> p1Territory = getMovesForValue(3);
        resetGoBeforeFlood();
        floodFill(2);
        List<Integer> p2Territory = getMovesForValue(4);
        resetGoBeforeFlood();

        int i = p1Territory.size()-1, j = p2Territory.size()-1;

        while (i>-1 && j>-1) {
            int p1Stone = p1Territory.get(i), p2Stone = p2Territory.get(j);
            if (p1Stone == p2Stone) {
                p1Territory.remove(i);
                p2Territory.remove(j);
                --i;
                --j;
            } else if (p1Stone>p2Stone) {
                --i;
            } else {
                --j;
            }
        }

        goTerritoryByPlayer.put(1, p1Territory);
        goTerritoryByPlayer.put(2, p2Territory);
    }
    private int containsDoublePass() {
        boolean hasPass = false;
        for (int i = 0; i < mMovesList.size() ; i++) {
            int move = mMovesList.get(i);
            if (move == passMove) {
                if (hasPass) {
                    return i;
                } else {
                    hasPass = true;
                }
            } else {
                hasPass = false;
            }
        }
        return -1;
    }

    public void processDeadStone(int move) {
        int i = move/gridSize, j = move%gridSize;
        int pos = abstractBoard[i][j];
        if (pos == 0) {
            if (goDeadStonesByPlayer.get(1).remove(Integer.valueOf(move))) {
                abstractBoard[i][j] = 2;
            }
            if (goDeadStonesByPlayer.get(2).remove(Integer.valueOf(move))) {
                abstractBoard[i][j] = 1;
            }
        } else if (pos == 1) {
            getGoDeadStonesByPlayer().get(2).add(move);
            abstractBoard[i][j] = 0;
        } else if (pos == 2) {
            getGoDeadStonesByPlayer().get(1).add(move);
            abstractBoard[i][j] = 0;
        }
        getTerritories();
    }

}

