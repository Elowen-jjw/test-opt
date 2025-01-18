package mutate.vect;

import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.model.AstVariable;
import AST_Information.model.FunctionBlock;
import AST_Information.model.LoopStatement;
import AST_Information.VarInform;
import ObjectOperation.list.CommonOperation;
import ObjectOperation.structure.FindIfInfo;
import common.*;
import common.before.AllBlockChange;
import common.before.LoopExecValues;
import common.before.Preparation;
import utity.AvailableVariable;
import utity.LoopInfo;
import utity.IfInfo;

import java.io.File;
import java.util.*;

public class GenLoopMutate {
    String mutatePath;
    String loopMutateType;
    File file;
    List<String> initialFileList = new ArrayList<>();
    AstInform_Gen astgen;
    LoopInform_Gen loopGen;
    List<LoopStatement> allLoopList = new ArrayList<>();
    List<LoopInfo> allLoopInfoList = new ArrayList<>();
    Map<Integer, Integer> scopeMap = new TreeMap<>();//function, if, loop block's startline and endline
    List<VariableScope> varScopes = new ArrayList<>();


    public GenLoopMutate(){}

    //file size at least 4 kb
    public GenLoopMutate(String mutatePath, String loopMutateType, File file){
        this.loopMutateType = loopMutateType;
        this.mutatePath = mutatePath;
        this.file = file;
        this.initialFileList = CommonOperation.genInitialList(file);
        this.astgen = new AstInform_Gen(file);
        this.loopGen = new LoopInform_Gen(astgen);
        allLoopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);
        getScopeMap();
        getVarScopes();
    }

    public static void main(String args[]){
        File file = new File("/home/sdu/Desktop/test/random710.c");
        new GenLoopMutate(PropertiesInfo.mutateVectorDir, "vector", file).genLoopMutate();
    }

    public void genLoopMutate() {
        genLoopInfoList();
//        System.out.println("start mutations.loop......");
//        for (LoopAllInfo lai : loopInfoListFusion) {
//            System.out.println("mutations.fusion: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
//            FixedStuff fs = lai.getFs();
//            LoopStatement loop = lai.getLoop();
//            FixedStuff newFs = new FixedStuff(fs);
//
//            switch (loopMutateType) {
//                case "vectorization" -> {
//                    Vectorization vect = new Vectorization(fs.getAuseVarList(), loop, lai.getLoopExecTimes());
//                    vect.vector(newFs);
//                    if (vect.isTrans) {
//                        fsList.add(newFs);
//                        correspondingLoopList.add(loop);
//                    }
//                }
//            }
//        }
//        genFiles(fsList, correspondingLoopList, mutatePath);
//        fsList.clear();
//        correspondingLoopList.clear();
    }

    public void genLoopInfoList() {
        for(int i = 0; i < allLoopList.size(); i++){
            LoopStatement loop = allLoopList.get(i);
            System.out.println(loop.getStartLine() + "   " + loop.getEndLine());
            int loopExecTimes = LoopExecValues.getTimes(file, initialFileList, loop.getStartLine(), loop.getEndLine());
            if(loopExecTimes == 0){
                continue;
            }

            //若astvar为common或者array(无pointer)且initialized，则不需要check segment fault,否则需要check
            List<AstVariable> notNeedToCheckVar = new ArrayList<>();
            List<AstVariable> needToCheckVar = new ArrayList<>();
            List<AstVariable> validArrayList = new ArrayList<>();
            for(VariableScope vs: varScopes){
                if(vs.validLoopList.contains(i)){
                    if((vs.astVar.getKind().equals("common")
                            || (vs.astVar.getKind().equals("array") && !vs.astVar.getType().contains("*")))
                            && (vs.astVar.getIsIntialized())){
                        notNeedToCheckVar.add(vs.astVar);
                    } else {
                        needToCheckVar.add(vs.astVar);
                    }
                }
                if(vs.astVar.getKind().equals("array"))
                    validArrayList.add(vs.astVar);
            }

            List<AvailableVariable> notNeedToCheckAvarList = getAvListFromAstVar(notNeedToCheckVar);
            List<AvailableVariable> needToCheckAvarList = getAvListFromAstVar(needToCheckVar);

//            RandomAndCheck rc = new RandomAndCheck();
//            List<AvailableVariable> checkedAvarListPart = rc.getAvailableVarList(file, needToCheckAvarList, loop.getStartLine() + 1);
            List<AvailableVariable> validVarList = new ArrayList<>();
            validVarList.addAll(notNeedToCheckAvarList);
//            validVarList.addAll(checkedAvarListPart);

            if(validVarList.isEmpty()) continue;
            for(AvailableVariable astVar: validVarList){
                System.out.print(astVar.getValue() + ", ");
            }

            System.out.println("\n\n");

            allLoopInfoList.add(new LoopInfo(new Preparation().getInitialIatList(initialFileList, loop), validVarList, validArrayList, file, loop, loopExecTimes));
        }
    }

    public List<AvailableVariable> getAvListFromAstVar(List<AstVariable> astVarList){
        List<AvailableVariable> var_value_type = VarInform.getInitialAvailableVarList(astVarList, astgen);
        return VarInform.removeStructAndUnionOverall(var_value_type); //remove struct and union, just adapt the fields of these.
    }

    public static void genFiles(List<LoopInfo> fsList, List<LoopStatement> correspondingLoopList, String indexDir){
        AllBlockChange abc = new AllBlockChange();
        List<LoopInfo> afsList = abc.getLoopAvailableFsList(fsList, correspondingLoopList);//最外层
        if(!afsList.isEmpty()){
            abc.genAllFiles(afsList, fsList.size(), indexDir);
        }
    }

    public void getScopeMap(){
        for(FunctionBlock fb: astgen.getAllFunctionBlocks()){
            scopeMap.put(fb.startline, fb.endline);
        }
        for(IfInfo ii: new FindIfInfo().findAllIfInfo(initialFileList)){
            scopeMap.put(ii.getStartLine(), ii.getEndLine());
        }
        for(LoopStatement loop: allLoopList){
            scopeMap.put(loop.getStartLine(), loop.getEndLine());
        }
//        for(int i: scopeMap.keySet()){
//            System.out.println(i + "   " + scopeMap.get(i));
//        }
    }

    public void getVarScopes(){
        Map<String, AstVariable> varMap = astgen.allVarsMap;
        for(String s: varMap.keySet()){
            if(varMap.get(s).getIsGlobal()){
                varScopes.add(new VariableScope(varMap.get(s), 1, initialFileList.size(), findContainedLoops(1, initialFileList.size())));
            } else if (!varMap.get(s).getIsParmVar()) {
                Map.Entry<Integer, Integer> smallestScope = findMinContainingRange(scopeMap, varMap.get(s).getDeclareLine());
                varScopes.add(new VariableScope(varMap.get(s), varMap.get(s).getDeclareLine(), smallestScope.getValue(), findContainedLoops(varMap.get(s).getDeclareLine(), smallestScope.getValue())));
            } else {
                if(initialFileList.get(varMap.get(s).getDeclareLine() - 1).trim().endsWith("{")){
                    Map.Entry<Integer, Integer> smallestScope = findMinContainingRange(scopeMap, varMap.get(s).getDeclareLine());
                    varScopes.add(new VariableScope(varMap.get(s), varMap.get(s).getDeclareLine(), smallestScope.getValue(), findContainedLoops(varMap.get(s).getDeclareLine(), smallestScope.getValue())));
                }
            }
        }

        for(VariableScope vs: varScopes){
            System.out.println(vs.astVar.getName() + "    " + vs.astVar.getKind() + "   " + vs.astVar.getType() + "   " + vs.endLine);
        }
    }

    public List<Integer> findContainedLoops(int scopeStart, int scopeEnd) {
        List<Integer> containedLoopsIndices = new ArrayList<>();

        for (int i = 0; i < allLoopList.size(); i++) {
            LoopStatement loop = allLoopList.get(i);
            if (loop.getStartLine() >= scopeStart && loop.getEndLine() <= scopeEnd) {
                containedLoopsIndices.add(i);
            }
        }

        return containedLoopsIndices;
    }

    public static Map.Entry<Integer, Integer> findMinContainingRange(Map<Integer, Integer> ranges, int declarationLine) {
        Map.Entry<Integer, Integer> minRange = null;
        int minSize = Integer.MAX_VALUE;

        for (Map.Entry<Integer, Integer> range : ranges.entrySet()) {
            int startLine = range.getKey();
            int endLine = range.getValue();

            if (declarationLine >= startLine && declarationLine <= endLine) {
                int size = endLine - startLine;
                if (minRange == null || size < minSize) {
                    minRange = range;
                    minSize = size;
                }
            }
        }

        return minRange;
    }


    class VariableScope {
        AstVariable astVar;
        int startLine;
        int endLine;
        List<Integer> validLoopList;

        public VariableScope(AstVariable astVar, int startLine, int endLine, List<Integer> validLoopList) {
            this.astVar = astVar;
            this.startLine = startLine;
            this.endLine = endLine;
            this.validLoopList = validLoopList;
        }
    }
}
