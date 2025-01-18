package common;

import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.model.LoopStatement;
import objectoperation.list.CommonOperation;
import utity.AvailableVariable;
import utity.FixedStuff;
import utity.InitialAndTransBlock;

import java.io.File;
import java.util.*;

public class SpecificOperation_OneToMany {
    AstInform_Gen astgen;
    File initialFile;
    List<String> initialFileList;
    int startLine;
    int endLine;
    LoopStatement currentLoop;

    List<String> initialBlockList;
    List<AvailableVariable> avarList;
    InitialAndTransBlock newIta;
    List<InitialAndTransBlock> newItaList = new ArrayList<>();

    public boolean isTrans;


    public void specific(FixedStuff fs, LoopStatement loop){
        astgen = fs.getAst();
        initialFile = fs.getInitialFile();
        initialFileList = CommonOperation.genInitialList(initialFile);
        startLine = fs.getStartLine();
        endLine = fs.getEndLine();
        currentLoop = loop;

        avarList = fs.getAvarList();
        isTrans = true;

        for(InitialAndTransBlock ita: fs.getIatList()){
            newIta = new InitialAndTransBlock(ita);//backups
            initialBlockList = newIta.getInitialBlock().getBlockList();
            Trans();
            newItaList.add(newIta);
        }
        fs.setIatList(newItaList);
    }

    public void Trans(){
        if(!isAvailableTrans()){
            isTrans = false;
            return;
        }
        modifyTransformed();
    }

    public boolean isAvailableTrans(){
        return true;
    }


    public void modifyTransformed(){

    }

}
