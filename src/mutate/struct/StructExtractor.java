package mutate.struct;

import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.VarInform;
import AST_Information.model.*;
import ObjectOperation.list.CommonOperation;
import common.PropertiesInfo;
import processtimer.ProcessTerminal;
import utity.AvailableVariable;
import mutate.minmax.SafeMathInfo;

import java.io.File;
import java.util.*;

public class StructExtractor {
    public File file;
    public List<String> initialFileList;
    public AstInform_Gen astgen;
    public List<FunctionBlock> FuncfunctionBlockList;
    public Map<String, AstVariable> varMap;
    List<String> pointerAnalysisList = new ArrayList<>();

    public Map<Integer, Integer> loopMap = new HashMap<>();
    public List<AvailableVariable> avarList = new ArrayList<>();

    public Map<Integer, List<SafeMathInfo>> safeMathMap = new HashMap<>();

    public Map<Integer, List<String>> mutateMap = new HashMap<>();


    public List<String> replacedMinMaxList = new ArrayList<>();

    public StructExtractor(File file){
        this.file = file;
        this.initialFileList = CommonOperation.genInitialList(file);
        this.astgen = new AstInform_Gen(file);
        this.varMap = astgen.allVarsMap;
//        getLoopMap();
//        getAvarList();
        this.FuncfunctionBlockList = astgen.getAllFunctionBlocks();
//        getPointerAnalysis();
    }

    public void getLoopMap(){
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);
        for (LoopStatement loop : loopList) {
            loopMap.put(loop.getStartLine(), loop.getEndLine());
        }
    }

    public void getAvarList(){
        List<AstVariable> astVarList = new ArrayList<>();
        for(String s: varMap.keySet()){
            astVarList.add(varMap.get(s));
        }
        //修改了varInform，可以得到关于union和结构体本身的变量，而不仅仅是成员变量
        List<AvailableVariable> overallavarList = VarInform.getInitialAvailableVarList(astVarList, astgen);
        avarList = VarInform.removeStructAndUnionOverall(overallavarList);
//        avarList = VarInform.getInitialAvailableVarList(astVarList, astgen);
    }

    public static void main(String[] args) {
        File dir = new File(PropertiesInfo.testSuiteStructDir);
        for(File file: dir.listFiles()){
            if(!file.getName().endsWith(".c")) continue;
            System.out.println(file.getName());
            StructExtractor extractor = new StructExtractor(file);
            extractor.getStructInfo();
        }
    }

    public void getStructInfo(){
        List<AstVariable> astStruct = new ArrayList<>();
        for(String s: varMap.keySet()) {
            AstVariable var = varMap.get(s);
            if (var.getIsStructUnion() && var.getName().contains("g_")) {
                for(int i: new TreeSet<>(var.useLine)){
                    if(i == var.getDeclareLine()) continue;
                    System.out.println(var.getName() + " " + var.getType() + " " + initialFileList.get(i - 1));
                }
//                astStruct.add(var);
            }
        }



        //struct avarList
//        List<AvailableVariable> avarStruct = VarInform.getInitialAvailableVarList(astStruct, astgen);
//        for (AvailableVariable variable : avarStruct) {
//            System.out.println("Value: " + variable.getValue() + ", Type: " + variable.getType());
//        }
//        //定义的有哪些struct
//        for (StructUnionBlock su : astgen.allStructUnionMap.values()) {
//            System.out.println(su.getBlockType() + " " + su.getName());
//            for (FieldVar field : su.getChildField()) {
//                System.out.println(field.getName() + " " + field.getType() + " " + field.getIsBit());
//            }
//            System.out.println();
//        }
    }


    public void getPointerAnalysis(){
        //export LD_LIBRARY_PATH=" + PropertiesInfo.LLVM_HOME + "/lib:$LD_LIBRARY_PATH &&
        String command = "export LD_LIBRARY_PATH=" + PropertiesInfo.pointerAnalysisExport + ":$LD_LIBRARY_PATH" + " && cd " + PropertiesInfo.indexDir + " && " +
                "./pointer_parser " + file.getAbsolutePath();
        pointerAnalysisList = ProcessTerminal.listNotMemCheck(command, "sh");
    }
}
