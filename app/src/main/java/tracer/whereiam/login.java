package tracer.whereiam;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.usermgmt.LoginButton;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;

public class login extends AppCompatActivity {
    private Context mContext;
    private Button btn_custom_login;
    private LoginButton btn_kakao_login;
    private SessionCallback callback;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        callback = new SessionCallback();
        Session.getCurrentSession().addCallback(callback);
        Session.getCurrentSession().checkAndImplicitOpen();

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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)){
            return;
        }
        super.onActivityResult(requestCode,resultCode,data);
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        Session.getCurrentSession().removeCallback(callback);
    }
    private class SessionCallback implements ISessionCallback {

        @Override
        public void onSessionOpened() {
            redirectMainMenuActivity();
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if(exception != null) {
                Logger.e("login error"+exception);
            }
        }
    }
    protected void redirectMainMenuActivity() {
        final Intent intent = new Intent(this, Main_menu.class);
        startActivity(intent);
        finish();
    }
}
