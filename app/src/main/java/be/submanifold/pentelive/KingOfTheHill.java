package be.submanifold.pentelive;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by waliedothman on 05/07/16.
 */
public class KingOfTheHill implements Parcelable {
    private String game;
    private String numPlayers;
    private String currentKing;
    private boolean member;
    private boolean king;
    private boolean canIchallenge;
    private int gameId;

    public KingOfTheHill(String game, String numPlayers, String currentKing, boolean member, boolean king, boolean canIchallenge, String gameId) {
        this.game = game;
        this.numPlayers = numPlayers;
        this.currentKing = currentKing;
        this.member = member;
        this.king = king;
        this.canIchallenge = canIchallenge;
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
        } else if (gameInt < 19) {
            gameStr = "DK-Pente";
        } else if (gameInt < 21) {
            gameStr = "Go";
        } else if (gameInt < 23) {
            gameStr = "Go (9x9)";
        } else if (gameInt < 25) {
            gameStr = "Go (13x13)";
        } else {
            gameStr = "O-Pente";
        }
        if (this.gameId > 50) {
            this.game = "tb-" + gameStr;
        } else if (this.gameId % 2 == 0) {
            this.game = "Speed " + gameStr;
        } else {
            this.game = gameStr;
        }
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getNumPlayers() {
        return numPlayers;
    }

    public void setNumPlayers(String numPlayers) {
        this.numPlayers = numPlayers;
    }

    public String getCurrentKing() {
        return currentKing;
    }

    public void setCurrentKing(String currentKing) {
        this.currentKing = currentKing;
    }

    public boolean isMember() {
        return member;
    }

    public void setMember(boolean member) {
        this.member = member;
    }

    public boolean isKing() {
        return king;
    }

    public void setKing(boolean king) {
        this.king = king;
    }


    public boolean canIchallenge() {
        return canIchallenge;
    }

    public void setCanIchallenge(boolean canIchallenge) {
        this.canIchallenge = canIchallenge;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    protected KingOfTheHill(Parcel in) {
        game = in.readString();
        numPlayers = in.readString();
        currentKing = in.readString();
        member = in.readByte() != 0x00;
        king = in.readByte() != 0x00;
        canIchallenge = in.readByte() != 0x00;
        gameId = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(game);
        dest.writeString(numPlayers);
        dest.writeString(currentKing);
        dest.writeByte((byte) (member ? 0x01 : 0x00));
        dest.writeByte((byte) (king ? 0x01 : 0x00));
        dest.writeByte((byte) (canIchallenge ? 0x01 : 0x00));
        dest.writeInt(gameId);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<KingOfTheHill> CREATOR = new Parcelable.Creator<KingOfTheHill>() {
        @Override
        public KingOfTheHill createFromParcel(Parcel in) {
            return new KingOfTheHill(in);
        }

        @Override
        public KingOfTheHill[] newArray(int size) {
            return new KingOfTheHill[size];
        }
    };
}