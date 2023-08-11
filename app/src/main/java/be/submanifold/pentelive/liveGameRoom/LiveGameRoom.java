package be.submanifold.pentelive.liveGameRoom;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by waliedothman on 06/01/2017.
 */

public class LiveGameRoom implements Parcelable {
    private String name;
    private int port;
    private List<LivePlayer> players;

    public LiveGameRoom(String name, int port) {
        this.name = name;
        this.port = port;
        players = new ArrayList<>();
    }


    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public void addPlayer(LivePlayer player) {
        this.players.add(player);
    }

    public SpannableStringBuilder getRoomText(int height) {
        SpannableStringBuilder sb = new SpannableStringBuilder(name);
        if (players.size() > 0) {
            SpannableStringBuilder sbPlayers = new SpannableStringBuilder("");
            for (LivePlayer player : players) {
                if (sbPlayers.length() > 0) {
                    sbPlayers.append(", ");
                }
                sbPlayers.append(player.coloredNameString(height));
            }
            sb.append(":\n");
            sb.append(sbPlayers);
        }
        return sb;
    }

    protected LiveGameRoom(Parcel in) {
        name = in.readString();
        port = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(port);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<LiveGameRoom> CREATOR = new Parcelable.Creator<LiveGameRoom>() {
        @Override
        public LiveGameRoom createFromParcel(Parcel in) {
            return new LiveGameRoom(in);
        }

        @Override
        public LiveGameRoom[] newArray(int size) {
            return new LiveGameRoom[size];
        }
    };
}

