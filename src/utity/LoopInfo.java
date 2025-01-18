package utity;

import AST_Information.model.AstVariable;
import AST_Information.model.LoopStatement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LoopInfo {
    private List<InitialAndTransBlock> iatList;
    private List<AvailableVariable> validVarList;//所有可用的var
    private List<AstVariable> validArrayList;
    private File initialFile;
    private LoopStatement loop;
    private int loopExecTimes;

    public LoopInfo(List<InitialAndTransBlock> iatList, List<AvailableVariable> validVarList, List<AstVariable> validArrayList, File initialFile, LoopStatement loop, int loopExecTimes) {
        this.iatList = iatList;
        this.validVarList = validVarList;
        this.validArrayList = validArrayList;
        this.initialFile = initialFile;
        this.loop = loop;
        this.loopExecTimes = loopExecTimes;
    }

    public LoopInfo(LoopInfo oldFs){
        List<InitialAndTransBlock> itaList = new ArrayList<>();
        for(InitialAndTransBlock ita: oldFs.getIatList()) {
            InitialAndTransBlock newIta = new InitialAndTransBlock(ita);
            itaList.add(newIta);
        }

        if(oldFs.isAvailableInValidVarList()) {
            List<AvailableVariable> validVarList = new ArrayList<>();
            validVarList.addAll(oldFs.getValidVarList());
            this.validVarList = validVarList;
        }

        if(oldFs.isAvailableInValidArrayList()) {
            List<AstVariable> validArrayList = new ArrayList<>();
            validArrayList.addAll(oldFs.getValidArrayList());
            this.validArrayList = validArrayList;
        }

        this.iatList = itaList;
        this.initialFile = oldFs.getInitialFile();
        this.loop = oldFs.getLoop();
        this.loopExecTimes = oldFs.getLoopExecTimes();
    }

    public boolean isAvailableInValidVarList(){
        return (this.validVarList != null) && !this.validVarList.isEmpty();
    }

    public boolean isAvailableInValidArrayList(){
        return (this.validArrayList != null) && !this.validArrayList.isEmpty();
    }

    public List<InitialAndTransBlock> getIatList() {
        return iatList;
    }

    public void setIatList(List<InitialAndTransBlock> iatList) {
        this.iatList = iatList;
    }

    public List<AvailableVariable> getValidVarList() {
        return validVarList;
    }

    public void setValidVarList(List<AvailableVariable> validVarList) {
        this.validVarList = validVarList;
    }

    public List<AstVariable> getValidArrayList() {
        return validArrayList;
    }

    public void setValidArrayList(List<AstVariable> validArrayList) {
        this.validArrayList = validArrayList;
    }

    public File getInitialFile() {
        return initialFile;
    }

    public void setInitialFile(File initialFile) {
        this.initialFile = initialFile;
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
