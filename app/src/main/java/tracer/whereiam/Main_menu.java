package tracer.whereiam;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.kakao.kakaolink.v2.KakaoLinkResponse;
import com.kakao.kakaolink.v2.KakaoLinkService;
import com.kakao.message.template.LinkObject;
import com.kakao.message.template.TextTemplate;
import com.kakao.network.ErrorResult;
import com.kakao.network.callback.ResponseCallback;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.callback.UnLinkResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.helper.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Main_menu extends AppCompatActivity implements ListViewBtnAdapter.ListBtnClickListener {
    private ImageButton btn_drawer, btn_dot, btn_friend_add, btn_refresh;
    private TextView text_name;
    private CircleImageView profile_image;
    private DrawerLayout drawer_layout;
    private Button btn_trace;
    private ListView friend_list;
    ListViewBtnAdapter adapter;
    ArrayList<ListViewItem> items;

    String Nickname = "";
    String pImage = "";
    long userID = 0;
    @Override
    public void onListBtnClick(int position) {
        final int idx = position;
        new AlertDialog.Builder(this)
                .setMessage("정말 삭제하시겠습니까?")
                .setPositiveButton(getString(R.string.com_kakao_ok_button),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Retrofit retrofit = new Retrofit.Builder()
                                        .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
                                RetroApi apiService = retrofit.create(RetroApi.class);
                                Call<Void> res = apiService.delFrd(Long.toString(userID), idx);
                                res.enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        if(response.isSuccessful()){
                                            Toast.makeText(Main_menu.this, "친구삭제 완료", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {

                                    }
                                });
                                items.remove(idx);
                                adapter.notifyDataSetChanged();
                            }
                        })
                .setNegativeButton(getString(R.string.com_kakao_cancel_button),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        requestMe();

        text_name = (TextView)findViewById(R.id.textName);
        profile_image = (CircleImageView)findViewById(R.id.profileImage);

        btn_drawer = (ImageButton)findViewById(R.id.btn_drawer);
        drawer_layout = (DrawerLayout) findViewById(R.id.drawer) ;
        DrawerLayout.DrawerListener myDrawerListener = new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View view, float v) {  }

            @Override
            public void onDrawerOpened(@NonNull View view) {
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onDrawerClosed(@NonNull View view) { }

            @Override
            public void onDrawerStateChanged(int i) { }
        };
        drawer_layout.setDrawerListener(myDrawerListener);

        btn_drawer.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (!drawer_layout.isDrawerOpen(Gravity.LEFT)) {
                    drawer_layout.openDrawer(Gravity.LEFT);
                }
                else {
                    drawer_layout.closeDrawer(Gravity.LEFT);
                }
            }
        });
        friend_list = (ListView) findViewById(R.id.friend_list);
        btn_dot = (ImageButton)findViewById(R.id.btn_dot);
        btn_dot.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                show();
            }
        });

        btn_friend_add = (ImageButton)findViewById(R.id.friend_add);
        btn_friend_add.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                send_message();
            }
        });
        btn_refresh = (ImageButton)findViewById(R.id.refresh);
        btn_refresh.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                refresh_friendlist();
                Toast.makeText(Main_menu.this, "친구 목록 갱신!", Toast.LENGTH_SHORT).show();
            }
        });
        btn_trace = (Button)findViewById(R.id.btn_trace);
        btn_trace.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for(int i =0; i < items.size();i++) {
                        Logger.e("닉네임1: " + items.get(i).getNickname());
                    }
                    Intent intent2 = new Intent(getApplicationContext(), tracer.whereiam.Map.class);
                    intent2.putExtra("friend_list", items);
                    startActivity(intent2);
                }
        });
    }
    private void refresh_friendlist(){
        items.clear();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
        RetroApi apiService = retrofit.create(RetroApi.class);
        Call<List<ListViewItem>> res = apiService.loadFrd(Long.toString(userID));
        res.enqueue(new Callback<List<ListViewItem>>() {
            @Override
            public void onResponse(Call<List<ListViewItem>> call, Response<List<ListViewItem>> response) {
                List<ListViewItem> friend_list = response.body();
                for(int i = 0; i < friend_list.size(); i++){
                    ListViewItem item = new ListViewItem();
                    item.setProfile_image(friend_list.get(i).getProfile_image());
                    item.setNickname(friend_list.get(i).getNickname());
                    item.setUserID(friend_list.get(i).getUserID());
                    items.add(item);
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(Call<List<ListViewItem>> call, Throwable t) {
                Logger.e("friend data receive error");
            }
        });
    }
    private void set_friendlist(){
        items = new ArrayList<ListViewItem>() ;
        loadItemsFromDB();
        adapter = new ListViewBtnAdapter(this, R.layout.listview_item, items, this) ;
        friend_list.setAdapter(adapter);
    }
    private void send_message(){
        TextTemplate params = TextTemplate.newBuilder(
                "Where I am에서 "+ Nickname +"님이 친구 요청을 보냈습니다. 친구를 맺고 "+ Nickname +"님의 위치를 확인해보세요!",
                LinkObject.newBuilder()
                .setAndroidExecutionParams("key1=" + userID)
                .build()).setButtonTitle("친구 요청 수락").build();

        Map<String, String> serverCallbackArgs = new HashMap<String, String>();
        serverCallbackArgs.put("user_id", "${current_user_id}");
        serverCallbackArgs.put("product_id", "${shared_product_id}");

        KakaoLinkService.getInstance().sendDefault(this, params, serverCallbackArgs, new ResponseCallback<KakaoLinkResponse>() {
            @Override
            public void onFailure(ErrorResult errorResult) {
                Logger.e(errorResult.toString());
            }
            @Override
            public void onSuccess(KakaoLinkResponse result) {
                // 템플릿 밸리데이션과 쿼터 체크가 성공적으로 끝남. 톡에서 정상적으로 보내졌는지 보장은 할 수 없다. 전송 성공 유무는 서버콜백 기능을 이용하여야 한다.
            }
        });
    }
    public boolean loadItemsFromDB() {
        if (items.isEmpty()) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
            RetroApi apiService = retrofit.create(RetroApi.class);
            Call<List<ListViewItem>> res = apiService.loadFrd(Long.toString(userID));
            res.enqueue(new Callback<List<ListViewItem>>() {
                @Override
                public void onResponse(Call<List<ListViewItem>> call, Response<List<ListViewItem>> response) {
                    List<ListViewItem> friend_list = response.body();
                    for(int i = 0; i < friend_list.size(); i++){
                        ListViewItem item = new ListViewItem();
                        item.setProfile_image(friend_list.get(i).getProfile_image());
                        item.setNickname(friend_list.get(i).getNickname());
                        item.setUserID(friend_list.get(i).getUserID());
                        items.add(item);
                    }
                }
                @Override
                public void onFailure(Call<List<ListViewItem>> call, Throwable t) {
                    Logger.e("friend data receive error");
                }
            });
        }
        return true;
    }
    private void show()
    {
        final List<String> ListItems = new ArrayList<>();
        ListItems.add("로그아웃");
        ListItems.add("앱 연결 해제");
        final CharSequence[] dotitems =  ListItems.toArray(new String[ListItems.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(dotitems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
                if(dotitems[pos] == "로그아웃"){
                    onClickLogout();
                }
                else {
                    onClickUnlink();
                }
            }
        });
        builder.show();
    }
    private void onClickUnlink() {
        final String appendMessage = getString(R.string.com_kakao_confirm_unlink);
        new AlertDialog.Builder(this)
                .setMessage(appendMessage)
                .setPositiveButton(getString(R.string.com_kakao_ok_button),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                UserManagement.getInstance().requestUnlink(new UnLinkResponseCallback() {
                                    @Override
                                    public void onFailure(ErrorResult errorResult) {
                                        Logger.e(errorResult.toString());
                                    }

                                    @Override
                                    public void onSessionClosed(ErrorResult errorResult) {
                                        redirectLoginActivity();
                                    }

                                    @Override
                                    public void onNotSignedUp() {
                                        redirectLoginActivity();
                                    }

                                    @Override
                                    public void onSuccess(Long userId) {
                                        Retrofit retrofit = new Retrofit.Builder()
                                                .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
                                        RetroApi apiService = retrofit.create(RetroApi.class);
                                        Call<Void> res = apiService.requestDelUser(Long.toString(userID));
                                        res.enqueue(new Callback<Void>() {
                                            @Override
                                            public void onResponse(Call<Void> call, Response<Void> response) {
                                                Toast.makeText(Main_menu.this, "회원탈퇴 되었습니다.", Toast.LENGTH_SHORT).show();
                                            }

                                            @Override
                                            public void onFailure(Call<Void> call, Throwable t) {
                                                Toast.makeText(Main_menu.this, t.toString(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        redirectLoginActivity();
                                    }
                                });
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(getString(R.string.com_kakao_cancel_button),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
    }

    private void onClickLogout() {
        UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {
                redirectLoginActivity();
            }
        });
    }

    public void LinkImage(){
        if(pImage == null){
            profile_image.setImageResource(R.drawable.thumb_talk);
        }
        else {
            Glide.with(getApplicationContext()).load(pImage).into(new SimpleTarget<GlideDrawable>() {
                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    profile_image.setImageDrawable(resource);
                }
            });
        }
    }

    private void requestMe() {
        List<String> keys = new ArrayList<>();
        keys.add("properties.nickname");
        keys.add("properties.profile_image");

        Intent intent=getIntent();
        final String oppoID = intent.getStringExtra("oppoID");

        UserManagement.getInstance().me(keys, new MeV2ResponseCallback() {
            @Override
            public void onFailure(ErrorResult errorResult) {
                String message = "failed to get user info. msg=" + errorResult;
                Logger.d(message);
            }

            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                redirectLoginActivity();
            }

            @Override
            public void onSuccess(MeV2Response response) {
                userID = response.getId();
                Nickname = response.getNickname();
                pImage = response.getProfileImagePath();
                text_name.setText(Nickname);
                HashMap<String, String> request_obj = new HashMap<>();
                request_obj.put("userID", Long.toString(userID));
                request_obj.put("nickName", Nickname);
                request_obj.put("imgSrc", pImage);
                if(oppoID!=null){
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
                    RetroApi apiService = retrofit.create(RetroApi.class);
                    Call<Frd_Res> res = apiService.acceptFrd(oppoID,Long.toString(userID));
                    res.enqueue(new Callback<Frd_Res>() {
                        @Override
                        public void onResponse(Call<Frd_Res> call, Response<Frd_Res> response) {
                            if(response.isSuccessful()) {
                                if(response.body().getType()==200) {
                                    Toast.makeText(Main_menu.this, response.body().getFromName() + "와 친구가 되었습니다.", Toast.LENGTH_SHORT).show();
                                } else if(response.body().getType()==404){
                                    Toast.makeText(Main_menu.this, "이미 추가된 사용자입니다.", Toast.LENGTH_SHORT).show();
                                } else{
                                    Toast.makeText(Main_menu.this, "같은 사용자를 친구할 수 없습니다.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        @Override
                        public void onFailure(Call<Frd_Res> call, Throwable t) {
                            Toast.makeText(Main_menu.this, t.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
                RetroApi apiService = retrofit.create(RetroApi.class);
                Call<Void> res = apiService.isMember(request_obj);
                res.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        //Toast.makeText(Main_menu.this, "로그인 되었습니다.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(Main_menu.this, t.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
                LinkImage();
                set_friendlist();
            }
        });
    }

    protected void redirectLoginActivity() {
        final Intent intent = new Intent(this, login.class);
        startActivity(intent);
        finish();
    }
}
