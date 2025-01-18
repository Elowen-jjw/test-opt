package mutationgen;

import astinfo.AstStmtOperation;
import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.Inform_Gen.LoopInform_Gen;
import astinfo.model.LoopStatement;
import common.AllBlockChange;
import common.FinalOperation;
import common.FusionCommon;
import common.Preparation;
import objectoperation.file.getAllFileList;
import mutations.fusion.FusionAdd;
import mutations.fusion.FusionMax;
import mutations.fusion.FusionSameHeader;
import objectoperation.list.CommonOperation;
import processmemory.LoopExecValues;
import utity.AvailableVariable;
import utity.FixedStuff;
import utity.LoopAllInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenFusion {
    File sourceDir;
    public String fusionPath;
    String fusionType;

    public GenFusion(String muIndexPath, String fusionType){
        this.fusionType = fusionType;
        this.fusionPath = muIndexPath + "/Fusion/" + fusionType;
    }

    public GenFusion(File sourceDir, String muIndexPath, String fusionType){
        this.sourceDir = sourceDir;
        this.fusionType = fusionType;
        this.fusionPath = muIndexPath + "/Fusion/" + fusionType;
    }

    public void genMutation() {
        List<File> allFileList = new ArrayList<File>();
        getAllFileList getFileList = new getAllFileList(sourceDir);
        getFileList.getAllFile(sourceDir, allFileList);
        getFileList.compareFileList(allFileList);

        int fineCnt = 0;
        for(File file: allFileList) {
            if(!file.getName().endsWith(".c")) {
                continue;
            }
            System.out.println(fineCnt ++ + ": " + file.getName());
            SingleFileMutation(file);
//            file.delete();
        }
    }

    public void SingleFileMutation(File file) {
        List<String> initialFileList = CommonOperation.genInitialList(file);

        List<LoopAllInfo> loopInfoListFusion = new ArrayList<>();
        List<FixedStuff> fsList = new ArrayList<>();
        List<LoopStatement> correspondingLoopList = new ArrayList<>();

        AstInform_Gen astgen = new AstInform_Gen(file);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);

        genLoopInfo(file, initialFileList, loopInfoListFusion, astgen, loopList);

        System.out.println("start mutations.fusion......");
        int loopIndex = 0;
        for (LoopAllInfo lai : loopInfoListFusion) {
            System.out.println("mutations.fusion: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            switch (fusionType) {
                case "SameHeader" -> {
                    FusionSameHeader tf = new FusionSameHeader(fs.getAuseVarList(), loop, loopIndex, lai.getLoopExecTimes());
                    tf.fusion(newFs);
                    if (tf.isTrans) {
                        fsList.add(newFs);
                        correspondingLoopList.add(loop);
                        loopIndex++;
                    }
                }
                case "Add" -> {
                    FusionAdd tf = new FusionAdd(fs.getAuseVarList(), loop, loopIndex, lai.getLoopExecTimes());
                    tf.fusion(newFs);
                    if (tf.isTrans) {
                        fsList.add(newFs);
                        correspondingLoopList.add(loop);
                        loopIndex++;
                    }
                }
                case "Max" -> {
                    FusionMax tf = new FusionMax(fs.getAuseVarList(), loop, loopIndex, lai.getLoopExecTimes());
                    tf.fusion(newFs);
                    if (tf.isTrans) {
                        fsList.add(newFs);
                        correspondingLoopList.add(loop);
                        loopIndex++;
                    }
                }
            }
        }

        genFiles(fsList, correspondingLoopList, fusionPath);
        fsList.clear();
        correspondingLoopList.clear();

        FusionSameHeader.gIndex = 0;
        FusionAdd.gIndex = 0;
        FusionMax.gIndex = 0;

    }

    public static void genLoopInfo(File file, List<String> initialFileList, List<LoopAllInfo> loopInfoListFusion, AstInform_Gen astgen, List<LoopStatement> loopList) {
        for(LoopStatement loop: loopList){
            int loopExecTimes = LoopExecValues.getTimes(file, initialFileList, loop.getStartLine(), loop.getEndLine());
            if(loopExecTimes == 0){
                continue;
            }

            if(!LoopExecValues.checkConsistency(file, initialFileList, loop.getStartLine(), loop.getEndLine())){
                System.out.println("checkConsistency failed................");
                continue;
            }

            List<AvailableVariable> avarUseList = FusionCommon.getAVarUseList(astgen, file, loop);
            if(avarUseList.isEmpty()){
                System.out.println("avarUseList is empty..........");
                continue;
            }

            Preparation pre = new Preparation();
            FixedStuff fs = pre.getBlockInfo(astgen, file, loop);
            fs.setAuseVarList(avarUseList);
            System.out.println(loop.getStartLine() + "  " + loop.getEndLine() + "   " + fs.getAvarList().size());

            if(!fs.isAvailableInAvarList()){
                continue;
            }

            loopInfoListFusion.add(new LoopAllInfo(fs, loop, loopExecTimes));
        }
    }

    public static void genFiles(List<FixedStuff> fsList, List<LoopStatement> correspondingLoopList, String indexDir){
        int count = 0;
        for(FixedStuff fs: fsList){
            FinalOperation fo = new FinalOperation();
            fo.genAllFiles(fs, count++, indexDir);
        }

        AllBlockChange abc = new AllBlockChange();
        List<FixedStuff> afsList = abc.getLoopAvailableFsList(fsList, correspondingLoopList);
        if(afsList.size() > 1){
            abc.genAllFiles(afsList, count, indexDir);
        }
    }
}
