package tracer.whereiam;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.kakao.usermgmt.LoginButton;

public class login extends AppCompatActivity {
    private Context mContext;
    private Button btn_custom_login;
    private LoginButton btn_kakao_login;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mContext = getApplicationContext();

        btn_custom_login = (Button) findViewById(R.id.btn_custom_login);
        btn_custom_login.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                btn_kakao_login.performClick();
            }
        });
        btn_kakao_login = (LoginButton) findViewById(R.id.btn_kakao_login);
    }
}
