package tracer.whereiam;

public class Map_Res {
    private String path;
    private Integer posX;
    private Integer posY;

    public Map_Res(String path, Integer posX, Integer posY) {
        this.path = path;
        this.posX = posX;
        this.posY = posY;
    }

    public String getPath() {
        return path;
    }

    public Integer getPosX() {
        return posX;
    }

    public Integer getPosY() {
        return posY;
    }
}