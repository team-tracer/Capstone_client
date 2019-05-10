package tracer.whereiam;

public class Frd_Res {
    private Integer type;
    private String fromName;
    private String toName;

    public Frd_Res(Integer type, String fromName, String toName) {
        this.type=type;
        this.fromName = fromName;
        this.toName = toName;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToName() {
        return toName;
    }
    public Integer getType(){
        return type;
    }
}