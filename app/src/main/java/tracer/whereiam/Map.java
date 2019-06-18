package tracer.whereiam;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ViewTarget;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.helper.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class Map extends AppCompatActivity implements Serializable {
    private Button btn_share;
    private Button btn_scan;
    private TextView map_name;
    private ImageView myPoint;
    private IntentIntegrator qrScan;
    private String map_String;
    private String imgPath;
    private String Shared;
    private String userID;
    private String Nickname;
    private String recv_ID;
    private String recv_map_String;
    private String recv_imgPath;
    boolean qr_flag = false;
    boolean recv_flag = false;
    ArrayList<ListViewItem> items;
    RelativeLayout map_layout;
    RelativeLayout.LayoutParams layoutParams;

    DisplayMetrics metrics;
    WindowManager windowManager;
    SensorEventListener accL;
    SensorEventListener gyroL;
    SensorEventListener magL;
    SensorEventListener deteT;

    private SensorManager sensorManager;
    private Sensor stepDetectorSensor;
    private Sensor accSensor;
    private Sensor gyroSensor;
    private Sensor magSensor;

    private int mStepDetector = 0;
    private double mStepDistance = 0.0;       //한 보폭의 길이, 단위는 m
    private int cnt1 = 0;
    private double direction;
    int flag = 1;
    boolean flag1 = false;
    float[] mR = new float[9];
    float[] mI = new float[9];
    float[] mV = new float[9];
    int mOrientCount;
    float[] mGravity = null;
    float[] mGeometric = null;
    final static int FREQ = 1;
    String[] dir_str = {"서", "남", "동", "북"};
//    String[] dir_str={"북", "동북", "동", "동남","남","남서","서","북서"};

    KalmanFilter accL_kal;
    KalmanFilter gyrO_kal;
    int[] dx={-1,0,1,0};
    int[] dy={0,1,0,-1};
//    int[] dx = {0,1,1,1,0,-1,-1,-1};
//    int[] dy = {-1,-1,0,1,1,1,0,-1}; // 0서, 1남, 2동, 3북
    int state=0;
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        map_name = (TextView) findViewById(R.id.map_name);
        map_layout = (RelativeLayout) findViewById(R.id.map_layout);

        final Intent intent= getIntent();
        Shared = intent.getStringExtra("shared");
        if(Shared == null){
            // 위치 공유를 받지 않은 경우
            items = (ArrayList<ListViewItem>) intent.getSerializableExtra("friend_list");
            userID = intent.getStringExtra("my_id");
            Nickname = intent.getStringExtra("my_nick");
        }
        else{
            // 위치를 추적하고 있지 않을 때 위치 공유를 받은 경우
            // 위치를 추적하고 있을 때 위치 공유 받은 경우는 onNewIntent()에서 처리
            recv_flag = true;
            recv_ID = intent.getStringExtra("snd_ID");
            recv_map_String = intent.getStringExtra("snd_map_String");
            recv_imgPath = intent.getStringExtra("snd_imgPath");
            map_name.setText(recv_map_String);
            Glide.with(getApplicationContext()).load(recv_imgPath).into(new ViewTarget<RelativeLayout, GlideDrawable>(map_layout) {
                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    map_layout.setBackgroundDrawable(resource);
                    map_layout.requestLayout();
                }
            });
            items = new ArrayList<ListViewItem>();
            requestMe();
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

//        accL = new accListener();
        gyroL = new gyroListener();
//        magL = new magListener();
        deteT = new deteTListener();
        accL_kal = new KalmanFilter();
        gyrO_kal = new KalmanFilter();

        metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        myPoint=new ImageView(this);
        myPoint.setImageResource(R.drawable.point);

        qrScan = new IntentIntegrator(this);
        btn_scan = (Button)findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrScan.setPrompt("Scanning");
                qrScan.initiateScan();
            }
        });
        btn_share = (Button) findViewById(R.id.btn_share);
        btn_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(qr_flag == false){
                    Toast.makeText(Map.this, "위치를 추적하고 있지 않습니다.", Toast.LENGTH_LONG).show();
                }
                else {
                    Intent intent2 = new Intent(getApplicationContext(), Popup.class);
                    intent2.putExtra("friend_list", items);
                    intent2.putExtra("my_id", userID);
                    intent2.putExtra("my_nick", Nickname);
                    intent2.putExtra("map_String", map_String);
                    intent2.putExtra("imgPath", imgPath);
                    startActivity(intent2);
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        IntentResult result=IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
        if(result!=null){
            if(result.getContents()==null){
                Toast.makeText(Map.this, "취소!", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(Map.this,"스캔완료!", Toast.LENGTH_SHORT).show();
                try{
                    JSONObject obj=new JSONObject(result.getContents());
                    map_String = obj.getString("map_name");
                    map_name.setText(map_String);
                    req_map(obj.getString("imageUrl"),obj.getInt("posX"),obj.getInt("posY"));
                    state=obj.getInt("dir");
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        } else{
            super.onActivityResult(requestCode,resultCode,data);
        }
    }
    public void req_map(final String url, final Integer pos_x, final Integer pos_y) {
        layoutParams = new RelativeLayout.LayoutParams(30, 30);
        layoutParams.leftMargin = (int) (map_layout.getWidth() * ((float) pos_x / 1000)); // 233
        layoutParams.topMargin = (int) (map_layout.getHeight() * ((float) pos_y / 1000)); // 255
        myPoint.bringToFront();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
        RetroApi apiService = retrofit.create(RetroApi.class);
        Call<Map_Res> res = apiService.loadMap(url, pos_x, pos_y);
        res.enqueue(new Callback<Map_Res>() {
            @Override
            public void onResponse(Call<Map_Res> call, final Response<Map_Res> response) {
                if (response.isSuccessful()) {
                    try {
//                        socket= IO.socket("http://165.246.242.150:8000");
                        socket=IO.socket(RetroApi.BASEURL);
                        socket.on(Socket.EVENT_CONNECT, onConnect);
                        socket.on(Socket.EVENT_ERROR,onError);
                        socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
                        socket.connect();
                    }catch(URISyntaxException e){
                        e.printStackTrace();
                    }

                    imgPath = response.body().getPath();
                    Glide.with(getApplicationContext()).load(imgPath).into(new ViewTarget<RelativeLayout, GlideDrawable>(map_layout) {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            map_layout.setBackgroundDrawable(resource);
                            map_layout.requestLayout();
                        }
                    });
                    qr_flag = true;
                    map_layout.addView(myPoint, layoutParams);
                }
            }

            @Override
            public void onFailure(Call<Map_Res> call, Throwable t) {
                Log.e("networking err", t.toString());
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        recv_ID = intent.getStringExtra("snd_ID");
        recv_map_String = intent.getStringExtra("snd_map_String");
        recv_imgPath = intent.getStringExtra("snd_imgPath");
        if(recv_map_String.equals(map_String)){
            Toast.makeText(this, "같은 건물에 있습니다.", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "같은 건물에 있어야 합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*      시작시 리스너 호출        */
        sensorManager.registerListener(deteT, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(accL, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(gyroL, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(magL, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        /*      앱 종료시 배터리 절약을 위해 센서들 OFF        */
        sensorManager.unregisterListener(deteT);
        sensorManager.unregisterListener(accL);
        sensorManager.unregisterListener(gyroL);
        sensorManager.unregisterListener(magL);
    }

    double KalmanValue(KalmanFilter kal) {
        /*      시스템 방정식 부분      */
        /*      Step1> Time Update(predict) : 예측업데이트를 수행하는 단계       */
        /*      (1) (예상되는 현재값) 예측하는 센서값은 일단 과거에 측정한 값과 동일한 값이라고 가정      */
        kal.x_Predict = (kal.A * kal.Xk) + kal.B_uk;

        /*      (2) (예상되는 오류공분산 값 : 위에서 예상한 현재값의 불확실성 정도) 예측한 센서 값 정보가 얼마나 불확실 한지       */
        kal.p_Predict = (kal.A * kal.Pk) + kal.Q;

        /*      관측 방정식 부분       */
        /*      Step2> Measurement Update(Correct) : (정정)업데이트를 수행하는 단계      */
        /*      (3) (칼만이득 업데이트 : 보정규모) 위 불확실성 정도를 기반으로 보정해야할 규모 결정      */
        kal.K_gain = kal.p_Predict / (kal.H * kal.p_Predict + kal.R);

        /*      (4) (현재값 업데이트 : 계산을 해본 현재 값) = 예측값 + 보정규모(센서값 - 예측값)       */
        /*      방금 받은 센서값을 토대로 실제 계산을 해보니 예되는 현재값과 차이가 얼마나 되는지      */
        double conV_z_Din = Double.valueOf(kal.z_Din).doubleValue();
        kal.Xk = kal.x_Predict + kal.K_gain * (conV_z_Din - kal.x_Predict);

        /*      (5) (오류공분산 업데이트) 예측한 값의 오차분과 칼만이득 간 서로의 상관정도 (공분산)      */
        kal.Pk = (1 - kal.K_gain) * kal.p_Predict;

        return kal.Xk;
    }

    private class deteTListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                if (event.values[0] == 1.0f)//      1 step detect시
                {
//                    Toast.makeText(Map.this, "hi", Toast.LENGTH_SHORT).show();
                    JSONObject obj=new JSONObject();
                    Integer len=70;
//                    if(대각선 이동일 경우){
//                        len=Math.sqrt(len);
//                }
                    Integer x_len=(int)(map_layout.getWidth()*((float)len/10000));
                    Integer y_len=(int)(map_layout.getHeight()*((float)len/10000));
//                    Integer length = 15;
//                    (int) (map_layout.getWidth() * ((float) pos_x / 1000)); // 233
                    layoutParams.leftMargin += (dx[state] * x_len);
                    layoutParams.topMargin += (dy[state] * y_len);
                    myPoint.setLayoutParams(layoutParams);
                    try {
                        obj.accumulate("id",userID);
                        obj.accumulate("posX", layoutParams.leftMargin);
                        obj.accumulate("posY",layoutParams.topMargin);
                        socket.emit("stepDetection",obj);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

//    int count = values.Step;
//    private class accListener implements SensorEventListener {
//
//        private long lastTime;
//        private float speed;
//        private float lastX = 0;
//        private float lastY = 0;
//        private float lastZ = 0;
//
//        private float x, y, z;
//        private static final double SHAKE_THRESHOLD = 78;
//
//        public void onSensorChanged(SensorEvent event) {
//
//            long currentTime = System.currentTimeMillis();
//            long gabOfTime = (currentTime - lastTime);
//
//            if (gabOfTime > 100) {
//                lastTime = currentTime;
//                x = event.values[0];
//                y = event.values[1];
//                z = event.values[2];
//                speed = Math.abs(x + y + z - lastX - lastY - lastZ) / gabOfTime * 3000;
//
//                if (speed > SHAKE_THRESHOLD && !flag1) {
//
//                    JSONObject obj=new JSONObject();
//                    double len=55;
////                    String[] dir_str={"북", "동북", "동", "동남","남","남서","서","북서"};
////                    if(state==1 || state==3 || state==5 || state==7){
////                        len=len/Math.sqrt(2);
////                    }
//                    Integer x_len=(int)(map_layout.getWidth()*(len/10000));
//                    Integer y_len=(int)(map_layout.getHeight()*(len/10000));
////                    Integer length = 15;
////                    (int) (map_layout.getWidth() * ((float) pos_x / 1000)); // 233
//                    layoutParams.leftMargin += (dx[state] * x_len);
//                    layoutParams.topMargin += (dy[state] * y_len);
//                    myPoint.setLayoutParams(layoutParams);
//                    try {
//                        obj.accumulate("id",userID);
//                        obj.accumulate("posX", layoutParams.leftMargin);
//                        obj.accumulate("posY",layoutParams.topMargin);
//                        socket.emit("stepDetection",obj);
//                    }catch (Exception e){
//                        e.printStackTrace();
//                    }
//
////                    values.Step = count++;
////                    StepDetector2.setText("Step Detect accL: " + String.valueOf(values.Step));
////                    mStepDistance = (0.8 * values.Step);//     걸음수 x 보폭
////                    String cStepDistance = String.format("%.1f", mStepDistance);
////                    StepDistance.setText("Moving Distance : " + String.valueOf(cStepDistance) + "m");
//                }
//                lastX = x;
//                lastY = y;
//                lastZ = z;
//                //flag1 = true;
//                //svm은 신호 벡터 크기
//                //double svm = abs(event.values[0])+abs(event.values[1])+abs(event.values[2]);
//            }
//
//        }
//
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        }
//    }

    Handler delayHandler = new Handler();

    private class gyroListener implements SensorEventListener {
        public void onSensorChanged(final SensorEvent event) {
            direction = event.values[1];
            gyrO_kal.z_Din = direction;
            direction = KalmanValue(gyrO_kal);

            if (direction > 0.65) {
                if (flag == 1) {
                    flag = 0;
                    state = (state + 1) % 4;
                    delayHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            flag = 1;
                        }
                    }, 1000);
                }
            } else if (direction < -0.65) {
                if (flag == 1) {
                    state = (state + 3) % 4;
                    flag = 0;
                    delayHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            flag = 1;
                        }
                    }, 1000);
                }
            } else {
                flag = 1;
            }
//            map_name.setText(dir_str[state]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
    int pre_step = 0;

//    private class gyroListener implements SensorEventListener {
//        public void onSensorChanged(final SensorEvent event) {
////            dir_str[0] = "북";
////            dir_str[1] = "동북";
////            dir_str[2] = "동";
////            dir_str[3] = "동남";
////            dir_str[4] = "남";
////            dir_str[5] = "남서";
////            dir_str[6] = "서";
////            dir_str[7] = "북서";
//            gyrO_kal.z_Din = event.values[1];
//            direction = KalmanValue(gyrO_kal);
//
//            /*남 4*/
//            if (direction > 2.45 && direction < -2.45) {
//                if (flag == 1) {
//                    Toast.makeText(getApplicationContext(), "남", Toast.LENGTH_SHORT).show();
//                    flag = 0;
//                    state = (state + 4) % 8;
//                    delayHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            flag = 1;
//                        }
//                    }, 1000);
//                }
//            }
//            /*서남 5*/
//            else if (direction > 1.95 && direction < 2.45) {
//                if (flag == 1) {
//                    state = (state + 5) % 8;
//                    flag = 0;
//                    Toast.makeText(getApplicationContext(), "서남", Toast.LENGTH_SHORT).show();
//                    delayHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            flag = 1;
//                        }
//                    }, 1000);
//                }
//            }
//            /*동남 3*/
//            else if (direction < -1.95 && direction > -2.45) {
//                if (flag == 1) {
//                    state = (state + 3) % 8;
//                    flag = 0;
//                    Toast.makeText(getApplicationContext(), "동남", Toast.LENGTH_SHORT).show();
//                    delayHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            flag = 1;
//                        }
//                    }, 1000);
//                }
//            }
//            /*서 6*/
//            else if (direction > 0.65 && direction < 1.95) {
//                if (flag == 1) {
//                    state = (state + 6) % 8;
//                    flag = 0;
//                    Toast.makeText(getApplicationContext(), "서", Toast.LENGTH_SHORT).show();
//                    delayHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            flag = 1;
//                        }
//                    }, 1000);
//                }
//            }
//            /*동 2*/
//            else if (direction < -0.65 && direction > -1.95) {
//                if (flag == 1) {
//                    state = (state + 2) % 8;
//                    flag = 0;
//                    Toast.makeText(getApplicationContext(), "동", Toast.LENGTH_SHORT).show();
//                    delayHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            flag = 1;
//                        }
//                    }, 1000);
//                }
//            }
//            /*서북 7*/
//            else if (direction > 0.5 && direction < 0.65) {
//                if (flag == 1) {
//                    Toast.makeText(getApplicationContext(), "서북", Toast.LENGTH_SHORT).show();
//                    flag = 0;
//                    state = (state + 7) % 8;
//                    delayHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            flag = 1;
//                        }
//                    }, 1000);
//                }
//            }
//            /*동북 1*/
//            else if (direction < -0.5 && direction > -0.6) {
//                if (flag == 1) {
//                    state = (state + 1) % 8;
//                    flag = 0;
//                    Toast.makeText(getApplicationContext(), "동북", Toast.LENGTH_SHORT).show();
//                    delayHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            flag = 1;
//                        }
//                    }, 1000);
//                }
//            }
//            else {
//                flag = 1;
//            }
//            /*방향전환시에는 스텝카운터 증가를 막는다*/
//            if (pre_step != state) flag1=true;
//            else flag1 = false;
//            pre_step = state;
////            StepDirection.setText(dir_str[state]);
//        }
//
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        }
//    }


    private class magListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {
            String x, y, z;

            x = String.format("%.2f", event.values[0]);
            y = String.format("%.2f", event.values[1]);
            z = String.format("%.2f", event.values[2]);

            Log.d("SENSOR_value", "magnetic changed.");
            Log.d("SENSOR_value", "  magnetic X: " + x
                    + ", magnetic Y: " + y
                    + ", magnetic Z: " + z);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
    private Emitter.Listener onConnect=new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject obj=new JSONObject();
            try {
                obj.accumulate("id", userID);
                obj.accumulate("posX",layoutParams.leftMargin);
                obj.accumulate("posY",layoutParams.topMargin);
                socket.emit("stepDetection",obj);
            }catch(Exception e){
                e.printStackTrace();
            }

        }
    };
    private Emitter.Listener onDisconnect=new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            socket.emit("disconnect",userID);
        }
    };
    private Emitter.Listener onError=new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            socket.emit("error",userID);
        }
    };

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
                userID = Long.toString(response.getId());
                Nickname = response.getNickname();
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
                RetroApi apiService = retrofit.create(RetroApi.class);
                Call<List<ListViewItem>> res = apiService.loadFrd(userID);
                res.enqueue(new Callback<List<ListViewItem>>() {
                    @Override
                    public void onResponse(Call<List<ListViewItem>> call, Response<List<ListViewItem>> resp) {
                        List<ListViewItem> friend_list = resp.body();
                        for(int i = 0; i < friend_list.size(); i++){
                            ListViewItem item = new ListViewItem();
                            item.setProfile_image(friend_list.get(i).getProfile_image());
                            item.setNickname(friend_list.get(i).getNickname());
                            item.setUserID(friend_list.get(i).getUserID());
                            item.setToken(friend_list.get(i).getToken());
                            items.add(item);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<ListViewItem>> call, Throwable t) {
                        Logger.e("friend data receive error");
                    }
                });
            }
        });
    }

    protected void redirectLoginActivity() {
        final Intent intent3 = new Intent(this, login.class);
        startActivity(intent3);
        finish();
    }
}