package tracer.whereiam;

import android.os.Parcel;
import android.os.Parcelable;

public class ListViewItem implements Parcelable {
    private String profile_image;
    private String nickname;
    private String userID;
    private String token;

    public ListViewItem() {
        this.profile_image = "";
        this.nickname = "";
        this.userID = "";
    }

    protected ListViewItem(Parcel in) {
        profile_image = in.readString();
        nickname = in.readString();
        userID = in.readString();
        token = in.readString();
    }

    public static final Creator<ListViewItem> CREATOR = new Creator<ListViewItem>() {
        @Override
        public ListViewItem createFromParcel(Parcel in) {
            return new ListViewItem(in);
        }

        @Override
        public ListViewItem[] newArray(int size) {
            return new ListViewItem[size];
        }
    };

    public String getProfile_image() {
        return profile_image;
    }

    public void setProfile_image(String profile_image) {
        this.profile_image = profile_image;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(profile_image);
        dest.writeString(nickname);
        dest.writeString(userID);
        dest.writeString(token);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
