package tracer.whereiam;

import android.content.Context;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ImageView;


public class Map extends AppCompatActivity {
    private ImageView map_image;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        map_image = (ImageView) findViewById(R.id.map_image);
        map_image.getLayoutParams().height = metrics.widthPixels;
        map_image.getLayoutParams().width = metrics.widthPixels;
        map_image.requestLayout();
    }
}
