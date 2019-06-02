package tracer.whereiam;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.kakao.util.helper.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
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
    private ImageView map_image;
    private IntentIntegrator qrScan;
    private String imgPath;
    ArrayList<ListViewItem> items;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        Intent intent= getIntent();
        items = (ArrayList<ListViewItem>) intent.getSerializableExtra("friend_list");

        map_name = (TextView)findViewById(R.id.map_name);

        map_image = (ImageView) findViewById(R.id.map_image);
        map_image.getLayoutParams().height = metrics.widthPixels;
        map_image.getLayoutParams().width = metrics.widthPixels;
        map_image.requestLayout();

        qrScan = new IntentIntegrator(this);
        btn_scan = (Button)findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrScan.setPrompt("Scanning");
                qrScan.initiateScan();
            }
        });
        Logger.e("닉네임: "+items.get(0).getNickname());
        btn_share = (Button) findViewById(R.id.btn_share);
        btn_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(getApplicationContext(), Popup.class);
                intent2.putExtra("friend_list", items);
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
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        } else{
            super.onActivityResult(requestCode,resultCode,data);
        }
    }
    public void req_map(final String url,final Integer pos_x, final Integer pos_y) {
        Retrofit retrofit= new Retrofit.Builder()
                .baseUrl(RetroApi.BASEURL).addConverterFactory(GsonConverterFactory.create()).build();
        RetroApi apiService=retrofit.create(RetroApi.class);
        Call<Map_Res> res=apiService.loadMap("highTech_1st.jpg",10,20);
        res.enqueue(new Callback<Map_Res>() {
            @Override
            public void onResponse(Call<Map_Res> call, final Response<Map_Res> response) {
                if(response.isSuccessful()){
                    imgPath = response.body().getPath();
                    Glide.with(getApplicationContext()).load(imgPath).into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            map_image.setImageDrawable(resource);
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call<Map_Res> call, Throwable t) {
                Log.e("networking err",t.toString());
                Toast.makeText(getApplicationContext(),"fuck",Toast.LENGTH_LONG);
            }
        });
    }
}
