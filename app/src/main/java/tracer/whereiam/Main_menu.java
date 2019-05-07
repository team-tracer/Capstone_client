package tracer.whereiam;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

public class Main_menu extends AppCompatActivity {
    private ImageButton btn_drawer, btn_dot, btn_friend_add;
    private TextView text_name;
    private CircleImageView profile_image;
    private DrawerLayout drawer_layout;
    private Button btn_scan;
    private IntentIntegrator qrScan;
    private ListView friend_list;

    String Nickname = "";
    String pImage = "";
    long userID = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        requestMe();

        text_name = (TextView)findViewById(R.id.textName);
        profile_image = (CircleImageView)findViewById(R.id.profileImage);

        btn_drawer = (ImageButton)findViewById(R.id.btn_drawer);
        btn_drawer.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                drawer_layout = (DrawerLayout) findViewById(R.id.drawer) ;
                if (!drawer_layout.isDrawerOpen(Gravity.LEFT)) {
                    drawer_layout.openDrawer(Gravity.LEFT);
                }
                else {
                    drawer_layout.closeDrawer(Gravity.LEFT);
                }
            }
        });

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

        btn_scan = (Button)findViewById(R.id.btn_scan);
        qrScan = new IntentIntegrator(this);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrScan.setPrompt("Scanning");
                qrScan.initiateScan();
            }
        });
        set_friendlist();
    }
    private void set_friendlist(){
        final ArrayList<String> items = new ArrayList<>();
        items.add("임태현");
        items.add("류동현");
        items.add("홍길동");
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);

        friend_list = (ListView) findViewById(R.id.friend_list);
        friend_list.setAdapter(adapter);
    }
    private void send_message(){
        TextTemplate params = TextTemplate.newBuilder(
                "Where I am에서 "+ Nickname +"님이 친구 요청을 보냈습니다. 친구를 맺고 "+ Nickname +"님의 위치를 확인해보세요!",
                LinkObject.newBuilder()
                .setAndroidExecutionParams("key1=" + Nickname)
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
    private void show()
    {
        final List<String> ListItems = new ArrayList<>();
        ListItems.add("로그아웃");
        ListItems.add("앱 연결 해제");
        final CharSequence[] items =  ListItems.toArray(new String[ListItems.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
                if(items[pos] == "로그아웃"){
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

    //pImgBtn부분//
    Handler handler = new Handler();    //카카오톡 이미지 연동 시 사용할 핸들러입니다!
    //이미지 연동
    public void LinkImage(){
        if(pImage == null){
            profile_image.setImageResource(R.drawable.thumb_talk);
        }
        else {
            new ImageDownload().execute(pImage);
            // 인터넷 상의 이미지 보여주기

            // 1. 권한을 획득한다 (인터넷에 접근할수 있는 권한을 획득한다)  - 메니페스트 파일
            // 2. Thread 에서 웹의 이미지를 가져오기
            // 3. 외부쓰레드에서 메인 UI에 접근위해 Handler 사용

            //Thread t = new Thread(Runnable 객체 생성);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {    // 오래 걸릴 작업 구현
                    // TODO Auto-generated method stub
                    try {
                        URL url = new URL(pImage);
                        InputStream is = url.openStream();
                        final Bitmap bm = BitmapFactory.decodeStream(is);
                        handler.post(new Runnable() {

                            @Override
                            public void run() {  // 화면에 그려줄 작업
                                profile_image.setImageBitmap(bm);
                            }
                        });
                        profile_image.setImageBitmap(bm); //비트맵 객체로 보여주기
                    } catch (Exception e) {
                    }
                }
            });
            t.start();
        }
    }
    //url 이미지 다운로드 (카카오프로필 이미지)
    private class ImageDownload extends AsyncTask<String, Void, Void> {
        private String fileName;/* 파일명 */
        private final String SAVE_FOLDER = "/Goal_Profile";/* 저장할 폴더 */

        @Override
        protected Void doInBackground(String... params) {
            //다운로드 경로를 지정
            String savePath = Environment.getExternalStorageDirectory().toString() + SAVE_FOLDER;

            File dir = new File(savePath);
            //상위 디렉토리가 존재하지 않을 경우 생성
            if (!dir.exists()) {
                dir.mkdirs();
            }

            fileName = String.valueOf(userID);

            //웹 서버 쪽 파일이 있는 경로
            String fileUrl = params[0];

            //다운로드 폴더에 동일한 파일명이 존재하는지 확인
            /*if (new File(savePath + "/" + fileName).exists() == false) {
            } else {}*/

            String localPath = savePath + "/" + fileName + ".jpg";

            try {
                URL imgUrl = new URL(fileUrl);
                //서버와 접속하는 클라이언트 객체 생성
                HttpURLConnection conn = (HttpURLConnection)imgUrl.openConnection();
                int len = conn.getContentLength();
                byte[] tmpByte = new byte[len];
                //입력 스트림을 구한다
                InputStream is = conn.getInputStream();
                File file = new File(localPath);
                //파일 저장 스트림 생성
                FileOutputStream fos = new FileOutputStream(file);
                int read;
                //입력 스트림을 파일로 저장
                for (;;) {
                    read = is.read(tmpByte);
                    if (read <= 0) {
                        break;
                    }
                    fos.write(tmpByte, 0, read); //file 생성
                }

                is.close();
                fos.close();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private void requestMe() {
        List<String> keys = new ArrayList<>();
        keys.add("properties.nickname");
        keys.add("properties.profile_image");

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
                LinkImage();
            }
        });
    }

    protected void redirectLoginActivity() {
        final Intent intent = new Intent(this, login.class);
        startActivity(intent);
        finish();
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        IntentResult result=IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
        if(result!=null){
            if(result.getContents()==null){
                Toast.makeText(Main_menu.this, "취소!", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(Main_menu.this,"스캔완료!", Toast.LENGTH_SHORT).show();
                try{
                    JSONObject obj=new JSONObject(result.getContents());
                    req_map(obj.getString("imageUrl"),obj.getInt("posX"),obj.getInt("posY"));
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }

        }else{
            super.onActivityResult(requestCode,resultCode,data);
        }
    }
    public void req_map(final String url,final Integer pos_x, final Integer pos_y) {
        Retrofit retrofit= new Retrofit.Builder()
                .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
        RetroApi apiService=retrofit.create(RetroApi.class);
        Call<Map_Res> res=apiService.loadMap("highTech_1st.jpg",10,20);
        res.enqueue(new Callback<Map_Res>() {
            @Override
            public void onResponse(Call<Map_Res> call, final Response<Map_Res> response) {
                if(response.isSuccessful()){
                    Intent intent=new Intent(getApplicationContext(), Navi_activity.class);
                    intent.putExtra("imgPath",response.body().getPath());
                    intent.putExtra("posX",response.body().getPosX());
                    intent.putExtra("posY",response.body().getPosY());
                    startActivity(intent);
                }
            }
            @Override
            public void onFailure(Call<Map_Res> call, Throwable t) {
                Log.e("networking err",t.toString());
                Toast.makeText(getApplicationContext(),"fuck",Toast.LENGTH_LONG);
            }
        });
    }
}
