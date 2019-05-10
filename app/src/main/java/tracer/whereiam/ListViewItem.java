package tracer.whereiam;

import android.graphics.Bitmap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ListViewItem {
    private Bitmap profile_image;
    private String nickname;

    public void setProfile_image(Bitmap civ){
        profile_image = civ;
    }
    public void setNickname(String text){
        nickname = text;
    }
    public Bitmap getProfile_image(){
        return this.profile_image;
    }
    public String getNickname(){
        return this.nickname;
    }
}
