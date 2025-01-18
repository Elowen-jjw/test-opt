package utity;


import java.util.List;

public class PointerInfo {
    private String name;
    private String type;
    private boolean isLocal;
    private List<String[]> pointsLink;

    public PointerInfo(String name, String type, List<String[]> pointsLink) {
        this.name = name;
        this.isLocal = name.contains("l_");
        this.type = type;
        this.pointsLink = pointsLink;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    public List<String[]> getPointsLink() {
        return pointsLink;
    }

    public void setPointsLink(List<String[]> pointsLink) {
        this.pointsLink = pointsLink;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
