package mutations.fusion_backups;

import AST_Information.model.LoopStatement;
import ObjectOperation.list.CommonOperation;
import ObjectOperation.list.RandomAndCheck;
import common.FusionCommon;
import common.MuProcessException;
import utity.AvailableVariable;
import utity.FixedStuff;
import utity.InitialAndTransBlock;
import utity.ProcessingBlock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FusionMax {
    File file;
    int startLine;
    int endLine;
    public static int gIndex = 0;
    LoopStatement currentLoop;
    int loopExecTimes;
    int randomExecTimes;
    int maxExecTimes;
    List<AvailableVariable> avarUseList;
    int currentIndex;
    String index_i;
    String index_j;
    String index_ij;

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
    String dOpA = "";
    String dOpB = "";
    List<String> loopBodyA = new ArrayList<>();
    List<String> loopBodyB = new ArrayList<>();

    public FusionMax(List<AvailableVariable> avarUseList, LoopStatement loop, int loopIndex, int loopExecTimes){
        this.avarUseList = avarUseList;
        this.currentLoop = loop;
        this.currentIndex = loopIndex;
        this.loopExecTimes = loopExecTimes;
    }

    public void fusion(FixedStuff fs){
        this.file = fs.getInitialFile();
        this.startLine = fs.getStartLine();
        this.endLine = fs.getEndLine();

        index_i = "ii_" + currentIndex;
        index_j = "jj_" + currentIndex;
        index_ij = "ij_" + currentIndex;

        CommonOperation.copyAvaiableVarList(avarListA, fs.getAvarList());
        CommonOperation.copyAvaiableVarList(avarListB, fs.getAvarList());
        isTrans = true;
        initialBlock = fs.getIatList().get(0).getInitialBlock();

        if(!isAvailableTrans()){
            isTrans = false;
            return;
        }

        for(InitialAndTransBlock ita: fs.getIatList()){
            while(avarListA.size() >= 2 && avarListB.size() >= 2) {
                RandomAndCheck rc = new RandomAndCheck();
                twoVarA = rc.getRandomAvailableVarChange(avarListA, 2);
                twoVarB = rc.getRandomAvailableVarChange(avarListB, 2);
                loopBodyA.clear();
                loopBodyB.clear();
                Trans(ita);
            }
        }

        if(newItaList.isEmpty()){
            isTrans = false;
            return;
        }

        fs.setIatList(newItaList);
    }

    public void Trans(InitialAndTransBlock ita){
        Random random = new Random();
        int useVarIndex = random.nextInt(avarUseList.size());
        AvailableVariable useVar = avarUseList.get(useVarIndex);
        String useVarType = useVar.getType();
        String useVarName = useVar.getValue();

        InitialAndTransBlock newIta = new InitialAndTransBlock(ita);//backups
        initialBlock = newIta.getInitialBlock();
        transformedBlockList = newIta.getTransformedBlockList();

        typeA = FusionCommon.getArrType(twoVarA, useVarType);
        typeB = FusionCommon.getArrType(twoVarB, typeA);

        arrName = "g_a" + gIndex;
        brrName = "g_b" + gIndex;
        gIndex++;

        dOpA = random.nextBoolean() ? " + " : " - ";
        dOpB = random.nextBoolean() ? " + " : " - ";

        randomExecTimes = random.nextInt(1, 2 * loopExecTimes + 1);
        maxExecTimes = Integer.max(loopExecTimes, randomExecTimes);

        modifyInitial(useVarName);

        List<String> ubList = new ArrayList<>();
        ubList.add("overflow");
        if(MuProcessException.isHaveUB(file, startLine, endLine, initialBlock, ubList)){
            System.out.println("generate undefined overflow.....");
            avarListA.add(twoVarA.get(1));
            avarListB.add(twoVarB.get(1));
            return;
        }

        for(ProcessingBlock singleTrans: transformedBlockList){
            modifyTransformed(singleTrans);
        }

        newItaList.add(newIta);
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

    public void modifyInitial(String useVarName){
        List<String> globalList = new ArrayList<>();
        List<String> beforeHeaderList = new ArrayList<>();
        List<String> intoChecksumList = new ArrayList<>();
        Random random = new Random();
        FusionCommon.addGlobal(globalList, typeA, arrName, maxExecTimes);
        FusionCommon.addGlobal(globalList, typeB, brrName, maxExecTimes);

        FusionCommon.addBeforeHeader(beforeHeaderList, index_i);
        FusionCommon.addCommentBeforeHeader(beforeHeaderList, "//mutations.fusion in max execTimes");

        FusionCommon.addIntoChecksum(intoChecksumList, typeA, arrName, maxExecTimes);
        FusionCommon.addIntoChecksum(intoChecksumList, typeB, brrName, maxExecTimes);

        initialBlock.setGlobalDeclare(globalList);
        initialBlock.setAddLineBoforeHeader(beforeHeaderList);
        initialBlock.setIntoChecksum(intoChecksumList);

        initialBlock.getBlockList().set(0, initialBlock.getBlockList().get(0)
                .replace(expA[0], expA[0] + ", " + index_i + " = 0")
                .replace(expA[2], expA[2] + ", " + index_i + "++"));
        List<String> addBodyA = FusionCommon.addBodyInLoop(arrName, index_i, twoVarA.get(0).getValue(), useVarName, dOpA, twoVarA.get(1).getValue());
        initialBlock.getBlockList().addAll(random.nextInt(1, initialBlock.getBlockList().size()), addBodyA);
        loopBodyA = CommonOperation.getListPart(initialBlock.getBlockList(), 2, initialBlock.getBlockList().size() - 1);

        //add second loop
        List<String> newLoop = new ArrayList<>();
        FusionCommon.addBeforeHeader(newLoop, index_j);
        expB[0] = index_j + " = 0";
        expB[1] = index_j + " < " + randomExecTimes;
        expB[2] = index_j + "++";
        newLoop.add("for (" + expB[0] + "; " + expB[1] + "; " + expB[2] + ") {");
        List<String> addBodyB = FusionCommon.addBodyInLoop(brrName, index_j, twoVarB.get(0).getValue(), arrName+"["+ index_j +"]", dOpB, twoVarB.get(1).getValue());
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

        FusionCommon.addGlobal(globalList, typeA, arrName, maxExecTimes);
        FusionCommon.addGlobal(globalList, typeB, brrName, maxExecTimes);

        FusionCommon.addBeforeHeader(beforeHeaderList, index_i);
        FusionCommon.addBeforeHeader(beforeHeaderList, index_j);
        FusionCommon.addBeforeHeader(beforeHeaderList, index_ij);
        FusionCommon.addCommentBeforeHeader(beforeHeaderList, "//mutations.fusion in max execTimes");

        FusionCommon.addIntoChecksum(intoChecksumList, typeA, arrName, maxExecTimes);
        FusionCommon.addIntoChecksum(intoChecksumList, typeB, brrName, maxExecTimes);

        singleTrans.setGlobalDeclare(globalList);
        singleTrans.setAddLineBoforeHeader(beforeHeaderList);
        singleTrans.setIntoChecksum(intoChecksumList);

        transBlock.add(singleTrans.getBlockList().get(0)
                .replace(expA[0], expA[0] + ", " + index_i + " = 0, " + expB[0] + ", " + index_ij + " = 0")
                .replace(expA[1], index_ij + " <= " + maxExecTimes)
                .replace(expA[2], index_ij + "++"));

        transBlock.add("if (" + index_ij + " <= " + loopExecTimes + " && " + expA[1] + ") {");
        transBlock.addAll(loopBodyA);
        transBlock.add(expA[2] + ";\n" + index_i + "++;");
        transBlock.add("}");

        transBlock.add("if (" + index_ij + " <= " + randomExecTimes + " && " + expB[1] + ") {");
        transBlock.addAll(loopBodyB);
        transBlock.add(index_j + "++;");
        transBlock.add("}\n}");
        singleTrans.setBlockList(transBlock);
    }

}
