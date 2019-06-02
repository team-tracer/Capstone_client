package tracer.whereiam;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.kakao.util.helper.log.Logger;

import java.util.ArrayList;

public class Popup extends Activity {
    ArrayList<ListViewItem> items;
    Button btn_list_close;
    ListView share_list;
    ShareListViewAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_popup);

        Intent intent= getIntent();
        items = (ArrayList<ListViewItem>) intent.getSerializableExtra("friend_list");
        Logger.e("닉네임3: "+items.get(0).getNickname());
        adapter = new ShareListViewAdapter();
        share_list = (ListView) findViewById(R.id.share_list);
        share_list.setAdapter(adapter);

        for(int i = 0; i < items.size();i++){
            adapter.addItem(items.get(i).getProfile_image(),items.get(i).getNickname(),items.get(i).getUserID());
        }

        share_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                // get item
                ListViewItem item = (ListViewItem) parent.getItemAtPosition(position);
                String userID = item.getUserID();
                Toast.makeText(Popup.this,"click: " + userID,Toast.LENGTH_LONG).show();
            }
        });

        btn_list_close = (Button)findViewById(R.id.btn_list_close);
        btn_list_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(0, 0);
            }
        });
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

}
