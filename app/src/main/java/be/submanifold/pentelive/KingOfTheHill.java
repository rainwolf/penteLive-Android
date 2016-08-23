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

    public KingOfTheHill(String game, String numPlayers, String currentKing, boolean member, boolean king, boolean canIchallenge) {
        this.game = game;
        this.numPlayers = numPlayers;
        this.currentKing = currentKing;
        this.member = member;
        this.king = king;
        this.canIchallenge = canIchallenge;
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


    public boolean canIchallenge() { return canIchallenge; }

    public void setCanIchallenge(boolean canIchallenge) { this.canIchallenge = canIchallenge; }

    protected KingOfTheHill(Parcel in) {
        game = in.readString();
        numPlayers = in.readString();
        currentKing = in.readString();
        member = in.readByte() != 0x00;
        king = in.readByte() != 0x00;
        canIchallenge = in.readByte() != 0x00;
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