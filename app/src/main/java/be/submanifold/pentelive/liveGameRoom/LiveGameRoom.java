package be.submanifold.pentelive.liveGameRoom;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by waliedothman on 06/01/2017.
 */

public class LiveGameRoom implements Parcelable {
    private String name;
    private int port;

    public LiveGameRoom(String name, int port) {
        this.name = name;
        this.port = port;
    }


    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
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

