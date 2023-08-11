package be.submanifold.pentelive;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by waliedothman on 28/05/16.
 */
public class Tournament implements Parcelable {
    private String game;
    private String name;
    private String tournamentID;
    private String round;
    private String tournamentState;
    private String date;

    public Tournament(String game, String name, String tournamentID, String round, String tournamentState, String date) {
        this.game = game;
        this.name = name;
        this.tournamentID = tournamentID;
        this.round = round;
        this.tournamentState = tournamentState;
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public String getTournamentState() {
        return tournamentState;
    }

    public String getRound() {
        return round;
    }

    public String getTournamentID() {
        return tournamentID;
    }

    public String getName() {
        return name;
    }

    public String getGame() {
        return game;
    }

    protected Tournament(Parcel in) {
        game = in.readString();
        name = in.readString();
        tournamentID = in.readString();
        round = in.readString();
        tournamentState = in.readString();
        date = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(game);
        dest.writeString(name);
        dest.writeString(tournamentID);
        dest.writeString(round);
        dest.writeString(tournamentState);
        dest.writeString(date);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Tournament> CREATOR = new Parcelable.Creator<Tournament>() {
        @Override
        public Tournament createFromParcel(Parcel in) {
            return new Tournament(in);
        }

        @Override
        public Tournament[] newArray(int size) {
            return new Tournament[size];
        }
    };
}