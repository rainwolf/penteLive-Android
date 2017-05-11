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

    private int gameId;

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

    public int getGameId() { return gameId; }

    public void setGameId(int gameId) { this.gameId = gameId; }



    public RatingStat(String game, String rating, String lastGame, String totalGames, String crown, String gameId) {
        this.game = game;
        this.rating = rating;
        this.lastGame = lastGame;
        this.totalGames = totalGames;
        if (crown != null) {
            this.crown = Integer.parseInt(crown);
        }
        if (gameId != null) {
            this.gameId = Integer.parseInt(gameId);
        }
        String gameStr;
        int gameInt = this.gameId;
        if (gameInt > 50) {
            gameInt -= 50;
        }
        if (gameInt < 3) {
            gameStr = "Pente";
        } else if (gameInt < 5) {
            gameStr = "Keryo-Pente";
        } else if (gameInt < 7) {
            gameStr = "Gomoku";
        } else if (gameInt < 9) {
            gameStr = "D-Pente";
        } else if (gameInt < 11) {
            gameStr = "G-Pente";
        } else if (gameInt < 13) {
            gameStr = "Poof-Pente";
        } else if (gameInt < 15) {
            gameStr = "Connect6";
        } else if (gameInt < 17) {
            gameStr = "Boat-Pente";
        } else {
            gameStr = "DK-Pente";
        }
        if (this.gameId > 50) {
            this.game = "tb-" + gameStr;
        } else if (this.gameId % 2 == 0) {
            this.game = "Speed " + gameStr;
        } else {
            this.game = gameStr;
        }
    }

    protected RatingStat(Parcel in) {
        game = in.readString();
        rating = in.readString();
        lastGame = in.readString();
        totalGames = in.readString();
        crown = in.readInt();
        gameId = in.readInt();
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
        dest.writeInt(gameId);
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

