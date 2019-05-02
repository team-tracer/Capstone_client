package tracer.whereiam;

public class Scan_Req_Format {
    String imageUrl;
    Integer positionX;
    Integer positionY;

    Scan_Req_Format(String imageUrl, Integer positionX, Integer positionY) {
        this.imageUrl = imageUrl;
        this.positionX = positionX;
        this.positionY = positionY;
    }
    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getPositionX() {
        return positionX;
    }

    public Integer getPositionY() {
        return positionY;
    }
}
