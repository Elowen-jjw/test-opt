package mutations.fusion;

import astinfo.model.LoopStatement;
import objectoperation.list.CommonOperation;
import objectoperation.list.RandomAndCheck;
import common.FusionCommon;
import common.MuProcessException;
import utity.AvailableVariable;
import utity.FixedStuff;
import utity.InitialAndTransBlock;
import utity.ProcessingBlock;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FusionSameHeader {
    Map<Integer, String[]> fusionOperators = new HashMap<>();
    Integer[] updatedOperatorIndex = {1, 4, 5, 6};
    File file;
    int startLine;
    int endLine;
    public static int gIndex = 0;
    LoopStatement currentLoop;
    int loopExecTimes;
    List<AvailableVariable> avarUseList;
    int currentIndex;
    String index;

    ProcessingBlock initialBlock;
    List<ProcessingBlock> transformedBlockList;
    List<AvailableVariable> avarListA = new ArrayList<>();
    List<AvailableVariable> avarListB = new ArrayList<>();
    List<InitialAndTransBlock> newItaList = new ArrayList<>();

    public boolean isTrans;
    List<AvailableVariable> twoVarA;
    List<AvailableVariable> twoVarB;

    String regexFor = "\\bfor\\s*\\((.*);(.*);(.*?)\\)\\s*\\{";
    Pattern pFor = Pattern.compile(regexFor);
    Matcher mFor;
    String[] expA = new String[3];
    String[] expB = new String[3];

    String typeA;
    String typeB;
    String arrName;
    String brrName;
    List<String> loopBodyA = new ArrayList<>();
    List<String> loopBodyB = new ArrayList<>();

    int genCount = 0;

    public FusionSameHeader(List<AvailableVariable> avarUseList, LoopStatement loop, int loopIndex, int loopExecTimes){
        this.avarUseList = avarUseList;
        this.currentLoop = loop;
        this.currentIndex = loopIndex;
        this.loopExecTimes = loopExecTimes;
    }

    public void fusion(FixedStuff fs){
        this.file = fs.getInitialFile();
        this.startLine = fs.getStartLine();
        this.endLine = fs.getEndLine();

        index = "ii_" + currentIndex;
        CommonOperation.copyAvaiableVarList(avarListA, fs.getAvarList());
        CommonOperation.copyAvaiableVarList(avarListB, fs.getAvarList());
        isTrans = true;
        initialBlock = fs.getIatList().get(0).getInitialBlock();

        if(!isAvailableTrans()){
            isTrans = false;
            return;
        }

        for(InitialAndTransBlock ita: fs.getIatList()){
            while(!avarListA.isEmpty() && !avarListB.isEmpty()) {
                FusionCommon.initFusionOperators(fusionOperators);
                genCount = 0;
                twoVarA = FusionCommon.getRandomAvarList(avarListA);
                twoVarB = FusionCommon.getRandomAvarList(avarListB);
                Trans(ita, fusionOperators);
            }
        }

        if(newItaList.isEmpty()){
            isTrans = false;
            return;
        }

        fs.setIatList(newItaList);
    }

    public void Trans(InitialAndTransBlock ita, Map<Integer, String[]> singleOperators){
        clearAddLoopList();
        if(genCount++ > 10){
            return;
        }
        Random random = new Random();
        RandomAndCheck rc = new RandomAndCheck();
        List<AvailableVariable> useVarList = rc.getRandomAvailableVarNotChange(avarUseList, random.nextInt(1, Integer.min(avarUseList.size(),3) + 1));

        InitialAndTransBlock newIta = new InitialAndTransBlock(ita);//backups
        initialBlock = newIta.getInitialBlock();
        transformedBlockList = newIta.getTransformedBlockList();

        typeA = FusionCommon.getArrType(twoVarA, FusionCommon.getArrType(useVarList, "int8_t"));
        typeB = FusionCommon.getArrType(twoVarB, typeA);

        arrName = "g_a" + gIndex;
        brrName = "g_b" + gIndex;
        gIndex++;

        modifyInitial(useVarList, singleOperators);

        if(MuProcessException.isHaveUB(file, startLine, endLine, initialBlock, new ArrayList<>(List.of("overflow")))){
            System.out.println("generate undefined overflow.....");
            fusionOperators.put(2, fusionOperators.get(updatedOperatorIndex[random.nextInt(updatedOperatorIndex.length)]));
            Trans(ita, fusionOperators);
            return;
        }

        if(MuProcessException.isHaveUB(file, startLine, endLine, initialBlock, new ArrayList<>(List.of("shift")))){
            System.out.println("generate shift error.....");
            fusionOperators.put(3, fusionOperators.get(updatedOperatorIndex[random.nextInt(updatedOperatorIndex.length)]));
            Trans(ita, fusionOperators);
            return;
        }

        for(ProcessingBlock singleTrans: transformedBlockList){
            modifyTransformed(singleTrans);
        }

        newItaList.add(newIta);
    }

    public void clearAddLoopList(){
        loopBodyA.clear();
        loopBodyB.clear();
    }

    public boolean isAvailableTrans(){
        if(loopExecTimes == 0){
            return false;
        }

        mFor = pFor.matcher(initialBlock.getBlockList().get(0));
        if (mFor.find()) {
            expA[0] = mFor.group(1).trim();
            expA[1] = mFor.group(2).trim();
            expA[2] = mFor.group(3).trim();
        }
        else{
            return false;
        }

        if(avarUseList.isEmpty()){
            return false;
        }
        return true;
    }

    public void modifyInitial(List<AvailableVariable> useVarList, Map<Integer, String[]> singleOperators){
        List<String> globalList = new ArrayList<>();
        List<String> beforeHeaderList = new ArrayList<>();
        List<String> intoChecksumList = new ArrayList<>();
        Random random = new Random();
        FusionCommon.addGlobal(globalList, typeA, arrName, loopExecTimes);
        FusionCommon.addGlobal(globalList, typeB, brrName, loopExecTimes);

        FusionCommon.addBeforeHeader(beforeHeaderList, index);
        FusionCommon.addCommentBeforeHeader(beforeHeaderList, "//mutations.fusion in same header");

        FusionCommon.addIntoChecksum(intoChecksumList, typeA, arrName, loopExecTimes);
        FusionCommon.addIntoChecksum(intoChecksumList, typeB, brrName, loopExecTimes);

        initialBlock.setGlobalDeclare(globalList);
        initialBlock.setAddLineBoforeHeader(beforeHeaderList);
        initialBlock.setIntoChecksum(intoChecksumList);

        initialBlock.getBlockList().set(0, initialBlock.getBlockList().get(0)
                .replace(expA[0], expA[0] + ", " + index + " = 0")
                .replace(expA[2], expA[2] + ", " + index + "++"));
        List<String> addBodyA = FusionCommon.addRandomBodyInLoop(arrName, index, FusionCommon.getValues(twoVarA), FusionCommon.getValues(useVarList), singleOperators);
        initialBlock.getBlockList().addAll(random.nextInt(1, initialBlock.getBlockList().size()), addBodyA);
        loopBodyA = CommonOperation.getListPart(initialBlock.getBlockList(), 2, initialBlock.getBlockList().size() - 1);

        //add second loop
        List<String> newLoop = new ArrayList<>();
        expB[0] = index + " = 0";
        expB[1] = index + " < " + loopExecTimes;
        expB[2] = index + "++";
        newLoop.add("for (" + expB[0] + "; " + expB[1] + "; " + expB[2] + ") {");
        List<String> addBodyB = FusionCommon.addRandomBodyInLoop(brrName, index, FusionCommon.getValues(twoVarB), new ArrayList<>(List.of(arrName + "["+index+"]")), singleOperators);
        newLoop.addAll(addBodyB);
        loopBodyB.addAll(addBodyB);
        newLoop.add("}");
        initialBlock.getBlockList().addAll(newLoop);
    }


    public void modifyTransformed(ProcessingBlock singleTrans){
        List<String> transBlock = new ArrayList<>();
        List<String> globalList = new ArrayList<>();
        List<String> beforeHeaderList = new ArrayList<>();
        List<String> intoChecksumList = new ArrayList<>();

        FusionCommon.addGlobal(globalList, typeA, arrName, loopExecTimes);
        FusionCommon.addGlobal(globalList, typeB, brrName, loopExecTimes);

        FusionCommon.addBeforeHeader(beforeHeaderList, index);
        FusionCommon.addCommentBeforeHeader(beforeHeaderList, "//mutations.fusion in same header");

        FusionCommon.addIntoChecksum(intoChecksumList, typeA, arrName, loopExecTimes);
        FusionCommon.addIntoChecksum(intoChecksumList, typeB, brrName, loopExecTimes);

        singleTrans.setGlobalDeclare(globalList);
        singleTrans.setAddLineBoforeHeader(beforeHeaderList);
        singleTrans.setIntoChecksum(intoChecksumList);

        transBlock.add(singleTrans.getBlockList().get(0)
                .replace(expA[0], expA[0] + ", " + expB[0])
                .replace(expA[2], expA[2] + ", " + expB[2]));
        transBlock.addAll(loopBodyA);
        transBlock.addAll(loopBodyB);
        transBlock.add("}");
        singleTrans.setBlockList(transBlock);
    }

}
