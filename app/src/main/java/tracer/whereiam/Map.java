package tracer.whereiam;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
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
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.ViewTarget;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;

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
    private String imgPath;
    private String userID;
    private String Nickname;
    ArrayList<ListViewItem> items;
    Integer myPos_x, myPos_y;
    BitmapDrawable map;
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

    float[] mR = new float[9];
    float[] mI = new float[9];
    float[] mV = new float[9];
    int mOrientCount;
    float[] mGravity = null;
    float[] mGeometric = null;
    final static int FREQ = 1;
    String[] dir_str = {"서", "남", "동", "북"};

    KalmanFilter accL_kal;
    KalmanFilter gyrO_kal;
    int[] dx = {-1, 0, 1, 0};
    int[] dy = {0, 1, 0, -1}; // 0서, 1남, 2동, 3북
    int state;
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent intent= getIntent();
        items = (ArrayList<ListViewItem>) intent.getSerializableExtra("friend_list");
        userID = intent.getStringExtra("my_id");
        Nickname = intent.getStringExtra("my_nick");

        map_name = (TextView) findViewById(R.id.map_name);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        accL = new accListener();
        gyroL = new gyroListener();
        magL = new magListener();
        deteT = new deteTListener();
        accL_kal = new KalmanFilter();
        gyrO_kal = new KalmanFilter();

        metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        map_name = (TextView) findViewById(R.id.map_name);
        map_layout = (RelativeLayout) findViewById(R.id.map_layout);
//        map_image = (ImageView) findViewById(R.id.map_image);

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
                Intent intent2 = new Intent(getApplicationContext(), Popup.class);
                intent2.putExtra("friend_list", items);
                intent2.putExtra("my_id",userID);
                intent2.putExtra("my_nick",Nickname);
                startActivity(intent2);
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
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
        RetroApi apiService = retrofit.create(RetroApi.class);
        Call<Map_Res> res = apiService.loadMap(url, pos_x, pos_y);
        res.enqueue(new Callback<Map_Res>() {
            @Override
            public void onResponse(Call<Map_Res> call, final Response<Map_Res> response) {
                if (response.isSuccessful()) {
                    try {
                        socket= IO.socket("http://165.246.242.150:8000");
//                        socket=IO.socket(RetroApi.BASEURL);
//                        socket.on(Socket.EVENT_CONNECT, onConnect);
//                        socket.on(Socket.EVENT_ERROR,onError);
//                        socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
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
//                    Glide.with(getApplicationContext()).load(imgPath).into(new SimpleTarget<GlideDrawable>() {
//                        @Override
//                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
//                            map_layout.setBackgroundDrawable(resource);
//                            map_layout.requestLayout();
//                        }
//                    });

                    layoutParams = new RelativeLayout.LayoutParams(30, 30);
                    layoutParams.leftMargin = (int) (map_layout.getWidth() * ((float) pos_x / 1000)); // 233
                    layoutParams.topMargin = (int) (map_layout.getHeight() * ((float) pos_y / 1000)); // 255
                    myPoint.bringToFront();
//                    map_layout.removeAllViews();
//                    map_layout.requestLayout();
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
                    Integer length = 7;
                    layoutParams.leftMargin += (dx[state] * length);
                    layoutParams.topMargin += (dy[state] * length);
                    myPoint.setLayoutParams(layoutParams);
                    //mStepDetector++;//      걸음수 증가
                    //StepDetector.setText("Step Detect : " + String.valueOf(mStepDetector));
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    float sum;

    private class accListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {
            double x, y, z;
            //double temp[] = new double[10];
            double energy;
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            energy = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));

            Log.d("SENSOR_value", "Acceleration changed.");

            /*디스플레이가 하늘을 보게 두고 앞으로 나아갈때 속도가 증가*/
            Log.d("SENSOR_values", "" + y);

            Log.d("SENSOR_value", "  Acceleration X: " + x
                    + ", Acceleration Y: " + y
                    + ", Acceleration Z: " + z);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

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
            map_name.setText(dir_str[state]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }


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
//            obj.accumulate("id",)
            socket.emit("test","hi");
        }
    };
}
