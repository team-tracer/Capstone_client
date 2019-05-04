package tracer.whereiam;

public class Scan_Req_Format {
    String imageUrl;
//    Integer posX;
//    Integer posY;
// , Integer positionX, Integer positionY
    Scan_Req_Format(String imageUrl) {
        this.imageUrl = imageUrl;
//        this.posX = positionX;
//        this.posY = positionY;
    }
    public String getImageUrl() {
        return imageUrl;
    }

//    public Integer getPositionX() {
//        return posX;
//    }
//
//    public Integer getPositionY() {
//        return posY;
//    }
}
