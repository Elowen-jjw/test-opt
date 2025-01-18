package utity;

public class AvarExecTimes {
    private AvailableVariable av;
    private int execTimes;

    public AvarExecTimes(AvailableVariable av, int execTimes) {
        this.av = av;
        this.execTimes = execTimes;
    }

    public AvailableVariable getAv() {
        return av;
    }

    public void setAv(AvailableVariable av) {
        this.av = av;
    }

    public int getExecTimes() {
        return execTimes;
    }

    public void setExecTimes(int execTimes) {
        this.execTimes = execTimes;
    }
}
