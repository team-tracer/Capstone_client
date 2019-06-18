package tracer.whereiam;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Popup extends Activity {
    private String userID;
    private String nickname;
    private String map_String;
    private String imgPath;
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
        userID = intent.getStringExtra("my_id");
        nickname = intent.getStringExtra("my_nick");
        map_String = intent.getStringExtra("map_String");
        imgPath = intent.getStringExtra("imgPath");

        adapter = new ShareListViewAdapter();
        share_list = (ListView) findViewById(R.id.share_list);
        share_list.setAdapter(adapter);

        for(int i = 0; i < items.size();i++){
            adapter.addItem(items.get(i).getProfile_image(),items.get(i).getNickname(),items.get(i).getUserID(),items.get(i).getToken());
        }

        share_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                final ListViewItem item = (ListViewItem) parent.getItemAtPosition(position);
                final String Nick = item.getNickname();
                final String friend_token = item.getToken();
                AlertDialog.Builder builder = new AlertDialog.Builder(Popup.this);
                builder.setMessage(Nick + "님에게 내 위치를 공유하시겠습니까?");
                builder.setPositiveButton("예",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread() {
                                    public void run() {
                                        send(friend_token, nickname, userID, map_String, imgPath);
                                    }
                                }.start();
                                Toast.makeText(Popup.this, Nick + "님에게 위치를 공유했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
                builder.setNegativeButton("아니요",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                builder.show();
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

    public String send(String to,  String Nickname, String ID, String map_String, String imgPath) {
        try {
            final String apiKey = getString(R.string.fcm_api_key);
            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "key=" + apiKey);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            JSONObject message = new JSONObject();
            message.put("to", to);
            message.put("priority", "high");
            JSONObject notification = new JSONObject();
            notification.put("title", "위치 공유 알림");
            notification.put("Nickname", Nickname);
            notification.put("id", ID);
            notification.put("map_String", map_String);
            notification.put("imgPath", imgPath);
            message.put("data", notification);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(message.toString());
            writer.flush();
            writer.close();
            os.close();

            conn.connect();

            int responseCode = conn.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }
}
