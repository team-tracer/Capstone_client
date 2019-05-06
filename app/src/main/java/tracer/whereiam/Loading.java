package tracer.whereiam;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.kakao.util.helper.log.Logger;

public class Loading extends AppCompatActivity {
    Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        Intent intent   = getIntent();
        Uri uri = intent.getData();
        if(uri == null){
            Logger.e("인텐트 없음");
        }
        else{
            String msg = uri.getQueryParameter("key1");
            if(uri != null) {
                Toast toastView = Toast.makeText(getApplicationContext(), msg + "님의 친구 요청을 수락했습니다.", Toast.LENGTH_LONG);
                toastView.show();
            }
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(),login.class);
                startActivity(intent);
                finish();
            }
        },1500);
    }
}
