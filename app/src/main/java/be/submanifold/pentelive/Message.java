package be.submanifold.pentelive;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by waliedothman on 10/04/16.
 */
public class Message implements Parcelable {
    String mMessageID;
    String mAuthor;
    String mSubject;
    String mTimeStamp;
    String mUnread;
    int mNameColor;
    int mCrown;

    public Message(String messageID, String author, String subject, String timeStamp, String unread, String nameColor, String crown) {
        this.mMessageID = messageID;
        this.mAuthor = author;
        this.mSubject = subject;
        this.mTimeStamp = timeStamp;
        this.mUnread = unread;
        this.mNameColor = Integer.parseInt(nameColor);
        this.mCrown = Integer.parseInt(crown);
    }

    public String getMessageID() {
        return this.mMessageID;
    }

    public String getAuthor() {
        return this.mAuthor;
    }

    public String getSubject() {
        return this.mSubject;
    }

    public String getTimeStamp() {
        return this.mTimeStamp;
    }

    public String getUnread() {
        return this.mUnread;
    }

    public int getNameColor() {
        return this.mNameColor;
    }

    public int getCrown() {
        return this.mCrown;
    }


    protected Message(Parcel in) {
        mMessageID = in.readString();
        mAuthor = in.readString();
        mSubject = in.readString();
        mTimeStamp = in.readString();
        mUnread = in.readString();
        mNameColor = in.readInt();
        mCrown = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMessageID);
        dest.writeString(mAuthor);
        dest.writeString(mSubject);
        dest.writeString(mTimeStamp);
        dest.writeString(mUnread);
        dest.writeInt(mNameColor);
        dest.writeInt(mCrown);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public boolean unread() {
        return this.mUnread.indexOf("unread") > -1;
    }
}