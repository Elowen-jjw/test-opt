package utity;

import astinfo.model.LoopStatement;

public class LoopAllInfo {
    private FixedStuff fs;
    private LoopStatement loop;
    private int loopExecTimes;

    public LoopAllInfo(FixedStuff fs, LoopStatement loop) {
        this.fs = fs;
        this.loop = loop;
    }

	public LoopAllInfo(FixedStuff fs, LoopStatement loop, int loopExecTimes) {
        this.fs = fs;
        this.loop = loop;
        this.loopExecTimes = loopExecTimes;
    }

    public FixedStuff getFs() {
        return fs;
    }

    public void setFs(FixedStuff fs) {
        this.fs = fs;
    }

    public LoopStatement getLoop() {
        return loop;
    }

    public void setLoop(LoopStatement loop) {
        this.loop = loop;
    }

    public int getLoopExecTimes() {
        return loopExecTimes;
    }

    public void setLoopExecTimes(int loopExecTimes) {
        this.loopExecTimes = loopExecTimes;
    }

}
