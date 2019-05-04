package tracer.whereiam;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class Scan_Res_Format {
    private Bitmap map_image;
//    private Integer posX;
//    private Integer posY;
//, Integer posX, Integer posY
    public Scan_Res_Format(Bitmap map_image) {
        this.map_image = map_image;
//        this.posX = posX;
//        this.posY = posY;
    }
    public Bitmap getMap_image() {
        return map_image;
    }

//    public Integer getPosX() {
//        return posX;
//    }
//
//    public Integer getPosY() {
//        return posY;
//    }
}
