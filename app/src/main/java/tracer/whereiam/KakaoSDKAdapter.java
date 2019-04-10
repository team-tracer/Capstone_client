package tracer.whereiam;

import android.app.Activity;
import android.content.Context;

import com.kakao.auth.ApprovalType;
import com.kakao.auth.AuthType;
import com.kakao.auth.IApplicationConfig;
import com.kakao.auth.ISessionConfig;
import com.kakao.auth.KakaoAdapter;

public class KakaoSDKAdapter extends KakaoAdapter{
    @Override
    public ISessionConfig getSessionConfig() {
        return new ISessionConfig() {
            // ISessionConfig: 로그인을 위해 Session을 생성하기 위해 필요한 옵션을 얻기 위한 추상 클래스.
            // 기본설정은 KakaoAdapter에 정의되어 있으며, 설정 변경이 필요한 경우 상속해서 사용할 수 있다.
            @Override
            public AuthType[] getAuthTypes() {
                // 로그인시 인증받을 타입을 지정. 지정하지 않을 시 가능한 모든 옵션이 지정됨.
                return new AuthType[] {AuthType.KAKAO_TALK};
                // Kakao SDK 로그인을 하는 방식에 대한 Enum class
                // 카카오톡으로 로그인
            }
            @Override
            public boolean isUsingWebviewTimer() {
                // SDK 로그인시 사용되는 WebView에서 pause와 resume시에 Timer를 설정하여 CPU소모를 절약한다.
                // true 를 리턴할경우 webview로그인을 사용하는 화면서 모든 webview에 onPause와 onResume 시에 Timer를 설정해 주어야 한다.
                // 지정하지 않을 시 false로 설정된다.
                return false;
            }
            @Override
            public boolean isSecureMode() {
                // 로그인시 access token과 refresh token을 저장할 때의 암호화 여부를 결정한다.
                return false;
            }
            @Override
            public ApprovalType getApprovalType() {
                // 일반 사용자가 아닌 Kakao와 제휴된 앱에서 사용되는 값으로,
                // 값을 채워주지 않을경우 ApprovalType.INDIVIDUAL 값을 사용하게 된다.
                return ApprovalType.INDIVIDUAL;
            }

            @Override
            public boolean isSaveFormData() {
                // Kakao SDK 에서 사용되는 WebView에서 email 입력폼에서 data를 save할지 여부를 결정한다.
                // Default true.
                return true;
            }
        };
    }

    @Override
    public IApplicationConfig getApplicationConfig() {
        return new IApplicationConfig() {
            @Override
            public Context getApplicationContext() {
                // Application이 가지고 있는 정보를 얻기 위한 메소드
                return GlobalApplication.getGlobalApplicationContext();
            }
        };
    }
}
