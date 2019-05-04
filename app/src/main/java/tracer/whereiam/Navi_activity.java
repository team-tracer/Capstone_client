package tracer.whereiam;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class Navi_activity extends AppCompatActivity {
    ImageView imageView;
    Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navi);
        imageView=(ImageView)findViewById(R.id.imageView);

        if(getIntent().hasExtra("byteArray")) {
            bitmap= BitmapFactory.decodeByteArray(
                    getIntent().getByteArrayExtra("byteArray"),0,getIntent().getByteArrayExtra("byteArray").length
            );
            imageView.setImageBitmap(bitmap);
        }
    }
}
