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
    private ImageView myPoint, oppoPoint;
    private IntentIntegrator qrScan;
    private String map_String;
    private String imgPath;
    private String Shared, IsTracking;
    private String userID;
    private String Nickname;
    private String recv_ID;
    private String recv_map_String;
    private String recv_imgPath;
    private Integer myPos_x, myPos_y, oppoPos_x, oppoPos_y, my_len, oppo_len;
    boolean qr_flag = false;
    boolean recv_flag = false;
    ArrayList<ListViewItem> items;
    RelativeLayout map_layout;
    RelativeLayout.LayoutParams layoutParams, layoutParams2;

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
    private double rot_Angle;
    int flag = 1;
    boolean flag1 = false;
    float[] mR = new float[9];
    float[] mI = new float[9];
    float[] mV = new float[9];
    int mOrientCount;
    float[] mGravity = null;
    float[] mGeometric = null;
    final static int FREQ = 1;

    KalmanFilter accL_kal;
    KalmanFilter gyrO_kal;
    KalmanFilter testGyro;
    double[] dx = {Math.cos(Math.toRadians(270.0)),Math.cos(Math.toRadians(300.0)),Math.cos(Math.toRadians(330.0)),
            Math.cos(Math.toRadians(0.0)),Math.cos(Math.toRadians(30.0)),Math.cos(Math.toRadians(60.0)),
            Math.cos(Math.toRadians(90.0)),Math.cos(Math.toRadians(120.0)),Math.cos(Math.toRadians(150.0)),
            Math.cos(Math.toRadians(180.0)),Math.cos(Math.toRadians(210.0)),Math.cos(Math.toRadians(240.0))};

    double[] dy = {Math.sin(Math.toRadians(270.0)),Math.sin(Math.toRadians(300.0)),Math.sin(Math.toRadians(330.0)),
            Math.sin(Math.toRadians(0.0)),Math.sin(Math.toRadians(30.0)),Math.sin(Math.toRadians(60.0)),
            Math.sin(Math.toRadians(90.0)),Math.sin(Math.toRadians(120.0)),Math.sin(Math.toRadians(150.0)),
            Math.sin(Math.toRadians(180.0)),Math.sin(Math.toRadians(210.0)),Math.sin(Math.toRadians(240.0))};
    int state = -1;
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        map_name = (TextView) findViewById(R.id.map_name);
        map_layout = (RelativeLayout) findViewById(R.id.map_layout);

        final Intent intent= getIntent();
        Shared = intent.getStringExtra("shared");
        if(Shared == null){ // 처음에 자기가 위치 추적 서비스를 이용할 때
            items = (ArrayList<ListViewItem>) intent.getSerializableExtra("friend_list");
            userID = intent.getStringExtra("my_id");
            Nickname = intent.getStringExtra("my_nick");
        }
        else{ // 자신은 QR코드를 안 찍은 상태에서 상대방에게 온 알림을 눌렀을 때 ( 상대방의 위치를 가져오기만 할 때)
            recv_flag = true;
            recv_ID = intent.getStringExtra("snd_ID");
            recv_map_String = intent.getStringExtra("snd_map_String");
            recv_imgPath = intent.getStringExtra("snd_imgPath");
            map_name.setText(recv_map_String);

            oppoPoint=new ImageView(this);
            layoutParams2= new RelativeLayout.LayoutParams(30, 30);
            oppoPoint.setImageResource(R.drawable.oppo_point);
            oppoPoint.bringToFront();


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

        accL = new accListener();
        gyroL = new gyroListener();
        deteT = new deteTListener();
        accL_kal = new KalmanFilter();
        gyrO_kal = new KalmanFilter();
        testGyro = new KalmanFilter();

        metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        myPoint=new ImageView(this);
        myPoint.setImageResource(R.drawable.my_point);

        qrScan = new IntentIntegrator(this);
        btn_scan = (Button)findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state == -1) {
                    qrScan.setPrompt("Scanning");
                    qrScan.initiateScan();
                }
                else{
                    Toast.makeText(Map.this, "이미 위치를 추적하고 있습니다.", Toast.LENGTH_LONG).show();
                }
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
        myPos_x=pos_x;
        myPos_y=pos_y;
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
                        socket=IO.socket(RetroApi.BASEURL);
                        socket.on(Socket.EVENT_CONNECT, onConnect);
                        socket.on(Socket.EVENT_ERROR,onError);
                        socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
                        socket.on("oppo_changed",oppoPoint_changed);
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
                    try{
                        JSONObject obj=new JSONObject();
                        obj.accumulate("id",userID);
                        obj.accumulate("posX",pos_x);
                        obj.accumulate("posY",pos_y);
                        socket.emit("stepDetection",obj);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
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
        if(recv_map_String.equals(map_String)){ // 위치 공유를 해야 한다.
            oppoPoint=new ImageView(this);
            layoutParams2= new RelativeLayout.LayoutParams(30, 30);
            oppoPoint.setImageResource(R.drawable.oppo_point);
            oppoPoint.bringToFront();
            map_layout.addView(oppoPoint);
            try {
                JSONObject obj=new JSONObject();
                obj.accumulate("acceptID",userID);
                obj.accumulate("requestID",recv_ID);
                socket.emit("request_full_duplex",obj);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else { // 이렇게 예외처리로 끝
            Toast.makeText(this, "같은 건물에 있어야 합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*      시작시 리스너 호출        */
        sensorManager.registerListener(deteT, stepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(accL, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(gyroL, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
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
    boolean stepFlag=false;
    Handler stepDelay=new Handler();
    int stepCnt=0;

    private class deteTListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {
            if (state !=-1) {
                if (event.values[0] == 1.0f)//      1 step detect시
                {
                    stepCnt++;
                    stepFlag=true;
                    JSONObject obj=new JSONObject();
                    Integer len = 8;
                    myPos_x += (int) (dx[state]*len);
                    myPos_y += (int) (dy[state]*len);

                    layoutParams.leftMargin=(int)(map_layout.getWidth()*((float)myPos_x/1000));
                    layoutParams.topMargin=(int)(map_layout.getHeight()*((float)myPos_y/1000));
                    myPoint.setLayoutParams(layoutParams);
                    try {
                        obj.accumulate("id",userID);
                        obj.accumulate("posX", myPos_x);
                        obj.accumulate("posY",myPos_y);
                        socket.emit("stepDetection",obj);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    stepDelay.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            stepCnt--;
                            if(stepCnt==0) {
                                stepFlag = false;
                            }
                        }
                    }, 1500);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private class accListener implements SensorEventListener {

        private long lastTime=0;
        private float speed;
        private float lastX = 0;
        private float lastY = 0;
        private float lastZ = 0;
        boolean up_thresh=false;
        boolean accStep_flag=false;
        private float x, y, z;
        private static final double SHAKE_THRESHOLD = 60;
        Handler accHandler=new Handler();
        private static final double DOWN_SHAKE_THRESHOLD = 30;

        public void onSensorChanged(SensorEvent event) {

            long currentTime = System.currentTimeMillis();
            long gabOfTime = (currentTime - lastTime);

            if (state!=-1 && gabOfTime > 100) {
                lastTime = currentTime;
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                speed = Math.abs(x + y + z - lastX - lastY - lastZ) / gabOfTime * 3000;
                if(speed>SHAKE_THRESHOLD && !up_thresh){
                    up_thresh=true;
                    accHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            up_thresh=false;
                        }
                    },1000);
                }
                if(up_thresh && speed<DOWN_SHAKE_THRESHOLD){
                    up_thresh=false;
                    accStep_flag=true;
                }

                if (accStep_flag  && !stepFlag) {
                    accStep_flag=false;
                    Integer len = 8;
                    myPos_x += (int) (dx[state]*len);
                    myPos_y += (int) (dy[state]*len);
                    layoutParams.leftMargin=(int)(map_layout.getWidth()*((float)myPos_x/1000));
                    layoutParams.topMargin=(int)(map_layout.getHeight()*((float)myPos_y/1000));
                    myPoint.setLayoutParams(layoutParams);
                }
                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private class gyroListener implements SensorEventListener {
        private String[] dire = {"270","300","330","0","30","60","90","120","150","180","210","240"};
        private long timestamp = 0;
        private double yAngle = 0.0;
        public void onSensorChanged(final SensorEvent event) {
            if(state!=-1) {
                if(timestamp != 0){
                    final float dT = (event.timestamp - timestamp) * 1.0f / 1000000000.0f;
                    double axisY = event.values[1];
                    testGyro.z_Din = axisY;
                    axisY = KalmanValue(testGyro);
                    yAngle = yAngle + (axisY * dT);
                    if(axisY < 0.05 && axisY > -0.05) {
                        final double Angle_value = Math.toDegrees(yAngle);
                        if (Angle_value > 15.0) {
                            int rot_num = (int) Math.round(Angle_value / 30.0);
                            state = (state + 12 - rot_num) % 12;
                            yAngle = 0;
                        } else if (Angle_value < -15.0) {
                            int rot_num = (int) Math.round(Angle_value / 30.0);
                            state = (state - rot_num) % 12;
                            yAngle = 0;
                        }
                    }
                }
                timestamp = event.timestamp;
            }
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
                obj.accumulate("oppo_id",recv_ID);
                socket.emit("registerUser",userID);
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
    private Emitter.Listener oppoPoint_changed=new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                final JSONObject receiveData=(JSONObject)args[0];
                layoutParams2.leftMargin=(int)(map_layout.getWidth() * ((float) receiveData.getInt("pos_x")/ 1000));
                layoutParams2.topMargin=(int)(map_layout.getHeight() * ((float) receiveData.getInt("pos_y")/ 1000));
                Log.e("posX",Integer.toString(layoutParams2.leftMargin));
                Log.e("posY",Integer.toString(layoutParams2.topMargin));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Toast.makeText(Map.this, Integer.toString(receiveData.getInt("pos_y")), Toast.LENGTH_SHORT).show();
                            oppoPoint.setLayoutParams(layoutParams2);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
            }catch(Exception e){
                e.printStackTrace();
            }
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
                try {
//                        socket= IO.socket("http://165.246.242.150:8000");
                    if(socket==null) {
                        socket = IO.socket(RetroApi.BASEURL);
                        socket.on(Socket.EVENT_CONNECT, onConnect);
                        socket.on(Socket.EVENT_ERROR, onError);
                        socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
                        socket.on("oppo_changed",oppoPoint_changed);
                        socket.connect();

                        JSONObject obj = new JSONObject();
                        obj.accumulate("from_id", recv_ID);
                        obj.accumulate("to_id",userID);
                        socket.emit("registerUser",userID);
                        socket.emit("request_oppoPoint", obj);
                        map_layout.addView(oppoPoint,layoutParams2);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    protected void redirectLoginActivity() {
        final Intent intent3 = new Intent(this, login.class);
        startActivity(intent3);
        finish();
    }
}