package tracer.whereiam;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ShareListViewAdapter extends BaseAdapter {
    // Adapter에 추가된 데이터를 저장하기 위한 ArrayList
    private ArrayList<ListViewItem> listViewItemList = new ArrayList<ListViewItem>() ;

    @Override
    public int getCount() {
        return listViewItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return listViewItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();

        /* 'listview_custom' Layout을 inflate하여 convertView 참조 획득 */
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.share_list, parent, false);
        }

        /* 'listview_custom'에 정의된 위젯에 대한 참조 획득 */
        final CircleImageView share_friend_image = (CircleImageView) convertView.findViewById(R.id.share_friend_image);
        final TextView share_friend_nick = (TextView) convertView.findViewById(R.id.share_friend_nick);

        /* 각 리스트에 뿌려줄 아이템을 받아오는데 mMyItem 재활용 */
        final ListViewItem listViewItem = listViewItemList.get(position);

        if(listViewItem.getProfile_image() == null){
            share_friend_image.setImageResource(R.drawable.thumb_talk);
        }
        else {
            Glide.with(context).load(listViewItem.getProfile_image()).into(new SimpleTarget<GlideDrawable>() {
                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    share_friend_image.setImageDrawable(resource);
                }
            });
        }
        share_friend_nick.setText(listViewItem.getNickname());
        return convertView;
    }
    public void addItem(String profile_image, String nickname, String userID, String token) {
        ListViewItem item = new ListViewItem();

        item.setUserID(userID);
        item.setNickname(nickname);
        item.setProfile_image(profile_image);
        item.setToken(token);
        listViewItemList.add(item);
    }
}
