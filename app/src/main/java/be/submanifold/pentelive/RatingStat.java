package be.submanifold.pentelive;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by waliedothman on 23/05/16.
 */
public class RatingStat implements Parcelable {
    private String game;
    private String rating;
    private String lastGame;
    private String totalGames;
    private int crown;

    public int getCrown() {
        return crown;
    }

    public void setCrown(int crown) {
        this.crown = crown;
    }

    public String getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(String totalGames) {
        this.totalGames = totalGames;
    }

    public String getLastGame() {
        return lastGame;
    }

    public void setLastGame(String lastGame) {
        this.lastGame = lastGame;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }


    public RatingStat(String game, String rating, String lastGame, String totalGames, String crown) {
        this.game = game;
        this.rating = rating;
        this.lastGame = lastGame;
        this.totalGames = totalGames;
        if (crown != null) {
            this.crown = Integer.parseInt(crown);
        }
    }

    protected RatingStat(Parcel in) {
        game = in.readString();
        rating = in.readString();
        lastGame = in.readString();
        totalGames = in.readString();
        crown = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(game);
        dest.writeString(rating);
        dest.writeString(lastGame);
        dest.writeString(totalGames);
        dest.writeInt(crown);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<RatingStat> CREATOR = new Parcelable.Creator<RatingStat>() {
        @Override
        public RatingStat createFromParcel(Parcel in) {
            return new RatingStat(in);
        }

        @Override
        public RatingStat[] newArray(int size) {
            return new RatingStat[size];
        }
    };
}

