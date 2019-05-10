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
    String msg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        Intent intent= getIntent();
        Uri uri = intent.getData();

        if(intent.getData()!=null){
            msg = uri.getQueryParameter("key1");
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent2 = new Intent(getApplicationContext(),Main_menu.class);
                if(msg!=null){
                    intent2.putExtra("oppoID", msg);
                }
                startActivity(intent2);
                finish();
            }
        },1500);
    }
}
