package tracer.whereiam;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.kakao.network.ErrorResult;
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
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Main_menu extends AppCompatActivity {
    private Button btn_logout, btn_disconnect;
    private TextView text_name;
    private CircleImageView profile_image;
    private Button scanButton;
    private IntentIntegrator qrScan;

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

        btn_logout = (Button)findViewById(R.id.btn_logout);
        btn_logout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                onClickLogout();
            }
        });
        btn_disconnect = (Button)findViewById(R.id.btn_disconnect);
        btn_disconnect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                onClickUnlink();
            }
        });
        scanButton=(Button)findViewById(R.id.scanButton);
        qrScan=new IntentIntegrator(this);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrScan.setPrompt("Scanning");
                qrScan.initiateScan();
            }
        });
        Logger.e("after Nickname : " + Nickname);

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
        if(pImage.equals(""));
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
                Logger.e("user id : " + userID);
                Logger.e("Nickname : " + Nickname);
                Logger.e("thumbnail image : " + response.getThumbnailImagePath());
                Logger.e("profile image: " + pImage);
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
                Toast.makeText(Main_menu.this,"스캔완료!",Toast.LENGTH_SHORT).show();
                try{
                    JSONObject obj=new JSONObject(result.getContents());
                    req_map(obj.getString("imageUrl"),obj.getInt("x"),obj.getInt("y"));
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }

        }else{
            super.onActivityResult(requestCode,resultCode,data);
        }
    }
    public void req_map(String url, Integer pos_x, Integer pos_y){
        Scan_Req_Format req_format= new Scan_Req_Format(url,pos_x,pos_y);
        Retrofit retrofit=new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
                .baseUrl(RetroApi.BASEURL).build();
        RetroApi apiService=retrofit.create(RetroApi.class);
        JSONObject req_obj=new JSONObject();
        try {
            req_obj.put("imageUrl", url);
            req_obj.put("posX",pos_x);
            req_obj.put("posY",pos_y);
        }catch (Exception e){
            e.printStackTrace();
        }

        Call<Scan_Res_Format> res=apiService.request(req_obj);
        res.enqueue(new Callback<Scan_Res_Format>() {
            @Override
            public void onResponse(Call<Scan_Res_Format> call, Response<Scan_Res_Format> response) {
                if(response.isSuccessful()){
                    // 서버와의 통신 형식을 맞추어 이미지파일을 어떻게 불러올 것인지 정해야함
                    // 그래서 이 부분 로직과 Scan_Res_Format을 작성해야함
                    // QR코드를 스캔한 뒤, 액티비티를 만들고 이미지와 현재위치 정보를 인텐트로 넘겨줘야함
//                    if(response.body()!=""){
//                        //Scan_Res_Format result=response.body();
//                    }
                }
            }
            @Override
            public void onFailure(Call<Scan_Res_Format> call, Throwable t) {
                Toast.makeText(Main_menu.this, "fuck", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
