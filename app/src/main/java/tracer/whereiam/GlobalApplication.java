package tracer.whereiam;

import android.app.Activity;
import android.app.Application;

import com.kakao.auth.KakaoSDK;

public class GlobalApplication extends Application {
    private static volatile GlobalApplication instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // Kakao Sdk 초기화
        KakaoSDK.init(new KakaoSDKAdapter());
    }
    public static GlobalApplication getGlobalApplicationContext() {
        return instance;
    }
    @Override
    public void onTerminate() {
        super.onTerminate();
        instance = null;
    }
}
