package utity;

public class AvailableVariable {
    private String value;
    private String type;
    private boolean isConst;

    public AvailableVariable() {
    }

    public AvailableVariable(String value, String type) {
        this.value = value;
        this.type = type;
    }

    public AvailableVariable(String value, String type, boolean isConst) {
        this.value = value;
        this.type = type;
        this.isConst = isConst;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getIsConst() {
        return isConst;
    }

    public void setConst(boolean aConst) {
        isConst = aConst;
    }
}
