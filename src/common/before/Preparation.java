package common.before;

import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.VarInform;
import AST_Information.model.AstVariable;
import AST_Information.model.LoopStatement;
import ObjectOperation.list.CommonOperation;
import ObjectOperation.list.RandomAndCheck;
import utity.AvailableVariable;
import utity.LoopInfo;
import utity.InitialAndTransBlock;
import utity.ProcessingBlock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class Preparation {
    public List<InitialAndTransBlock> getInitialIatList(List<String> initialFileList, LoopStatement loop){
        List<String> loopPart = CommonOperation.getListPart(initialFileList, loop.getStartLine(), loop.getEndLine());
        List<String> initialBlockList = new ArrayList<>(loopPart);
        List<String> transBlockList = new ArrayList<>(loopPart);

        ProcessingBlock initialBlock = new ProcessingBlock(initialBlockList);
        List<ProcessingBlock> transBlockLists = new ArrayList<>();
        ProcessingBlock transBlock = new ProcessingBlock(transBlockList);
        transBlockLists.add(transBlock);

        InitialAndTransBlock iat = new InitialAndTransBlock(initialBlock, transBlockLists);//only init block not have transformed block
        List<InitialAndTransBlock> iatList = new ArrayList<>();
        iatList.add(iat);
        return iatList;
    }

}
