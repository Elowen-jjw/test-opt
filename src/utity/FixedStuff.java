package utity;

import astinfo.Inform_Gen.AstInform_Gen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FixedStuff {
    private List<InitialAndTransBlock> iatList;
    private List<AvailableVariable> avarList;
    private List<AvailableVariable> auseVarList;
    private File initialFile;
    private int startLine;
    private int endLine;
    private AstInform_Gen ast;

    public FixedStuff(List<InitialAndTransBlock> iatList, List<AvailableVariable> avarList, List<AvailableVariable> auseVarList, File initialFile, int startLine, int endLine, AstInform_Gen ast) {
        this.iatList = iatList;
        this.avarList = avarList;
        this.auseVarList = auseVarList;
        this.initialFile = initialFile;
        this.startLine = startLine;
        this.endLine = endLine;
        this.ast = ast;
    }

    public FixedStuff(List<InitialAndTransBlock> iatList, List<AvailableVariable> avarList, File initialFile, int startLine, int endLine, AstInform_Gen ast) {
        this.iatList = iatList;
        this.avarList = avarList;
        this.initialFile = initialFile;
        this.startLine = startLine;
        this.endLine = endLine;
        this.ast = ast;
    }

    public FixedStuff(List<InitialAndTransBlock> iatList, File initialFile, int startLine, int endLine, AstInform_Gen ast) {
        this.iatList = iatList;
        this.initialFile = initialFile;
        this.startLine = startLine;
        this.endLine = endLine;
        this.ast = ast;
    }

    public FixedStuff(FixedStuff oldFs){
        List<InitialAndTransBlock> itaList = new ArrayList<>();
        for(InitialAndTransBlock ita: oldFs.getIatList()) {
            InitialAndTransBlock newIta = new InitialAndTransBlock(ita);
            itaList.add(newIta);
        }

        if(oldFs.isAvailableInAvarList()) {
            List<AvailableVariable> avarList = new ArrayList<>();
            avarList.addAll(oldFs.getAvarList());
            this.avarList = avarList;
        }

        if(oldFs.isAvailableInAUsevarList()){
            List<AvailableVariable> auseVarList = new ArrayList<>();
            auseVarList.addAll(oldFs.getAuseVarList());
            this.auseVarList = auseVarList;
        }

        this.iatList = itaList;
        this.initialFile = oldFs.getInitialFile();
        this.startLine = oldFs.getStartLine();
        this.endLine = oldFs.getEndLine();
        this.ast = oldFs.getAst();
    }

    public boolean isAvailableInAvarList(){
        return (this.avarList != null) && !this.avarList.isEmpty();
    }

    public boolean isAvailableInAUsevarList(){
        return (this.auseVarList != null) && !this.auseVarList.isEmpty();
    }

    public List<InitialAndTransBlock> getIatList() {
        return iatList;
    }

    public void setIatList(List<InitialAndTransBlock> iatList) {
        this.iatList = iatList;
    }

    public AstInform_Gen getAst() {
        return ast;
    }

    public void setAst(AstInform_Gen ast) {
        this.ast = ast;
    }

    public List<AvailableVariable> getAvarList() {
        return avarList;
    }

    public void setAvarList(List<AvailableVariable> avarList) {
        this.avarList = avarList;
    }

    public List<AvailableVariable> getAuseVarList() {
        return auseVarList;
    }

    public void setAuseVarList(List<AvailableVariable> auseVarList) {
        this.auseVarList = auseVarList;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public File getInitialFile() {
        return initialFile;
    }

    public void setInitialFile(File initialFile) {
        this.initialFile = initialFile;
    }

}
