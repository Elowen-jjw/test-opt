package common;

import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.VarInform;
import astinfo.model.AstVariable;
import astinfo.model.LoopStatement;
import objectoperation.list.CommonOperation;
import objectoperation.list.RandomAndCheck;
import utity.AvailableVariable;
import utity.FixedStuff;
import utity.InitialAndTransBlock;
import utity.ProcessingBlock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class Preparation {
    public FixedStuff getBlockInfo(AstInform_Gen ast, File file, LoopStatement loop){
        List<String> initialFileList = CommonOperation.genInitialList(file);
        List<String> initialBlockList = CommonOperation.getListPart(initialFileList, loop.getStartLine(), loop.getEndLine());
        List<String> transBlockList = CommonOperation.getListPart(initialFileList, loop.getStartLine(), loop.getEndLine());
        List<ProcessingBlock> transBlockLists = new ArrayList<>();

        ProcessingBlock initialBlock = new ProcessingBlock(initialBlockList);
        ProcessingBlock transBlock = new ProcessingBlock(transBlockList);
        transBlockLists.add(transBlock);

        InitialAndTransBlock iat = new InitialAndTransBlock(initialBlock, transBlockLists);//only init block not have transformed block
        List<InitialAndTransBlock> iatList = new ArrayList<>();
        iatList.add(iat);

        List<AstVariable> allGlobalVars = ast.getAllGlobalVars();
        List<AstVariable> invars = VarInform.getBlockGlobalInvars(allGlobalVars, loop.getUseVarList());
        List<AvailableVariable> var_value_type = VarInform.getInitialAvailableVarList(invars, ast);

        RandomAndCheck rc = new RandomAndCheck();
        List<AvailableVariable> avarList = rc.getAvailableVarList(file, var_value_type, loop.getStartLine() + 1);
        return new FixedStuff(iatList, avarList, file, loop.getStartLine(), loop.getEndLine(), ast);
    }

    public FixedStuff getBlockInfoNotAvar(AstInform_Gen ast, File file, LoopStatement loop){
        List<String> initialFileList = CommonOperation.genInitialList(file);
        List<String> initialBlockList = CommonOperation.getListPart(initialFileList, loop.getStartLine(), loop.getEndLine());
        List<String> transBlockList = CommonOperation.getListPart(initialFileList, loop.getStartLine(), loop.getEndLine());
        List<ProcessingBlock> transBlockLists = new ArrayList<>();

        ProcessingBlock initialBlock = new ProcessingBlock(initialBlockList);
        ProcessingBlock transBlock = new ProcessingBlock(transBlockList);
        transBlockLists.add(transBlock);

        InitialAndTransBlock iat = new InitialAndTransBlock(initialBlock, transBlockLists);//only init block not have transformed block
        List<InitialAndTransBlock> iatList = new ArrayList<>();
        iatList.add(iat);

        return new FixedStuff(iatList, file, loop.getStartLine(), loop.getEndLine(), ast);
    }

    public void setFsAvarList(AstInform_Gen ast, File file, LoopStatement loop, FixedStuff fs){
        List<AstVariable> allGlobalVars = ast.getAllGlobalVars();
        List<AstVariable> invars = VarInform.getBlockGlobalInvars(allGlobalVars, loop.getUseVarList());
        List<AvailableVariable> var_value_type = VarInform.getInitialAvailableVarList(invars, ast);

        RandomAndCheck rc = new RandomAndCheck();
        List<AvailableVariable> avarList = rc.getAvailableVarList(file, var_value_type, loop.getStartLine() + 1);
        fs.setAvarList(avarList);
    }

}
