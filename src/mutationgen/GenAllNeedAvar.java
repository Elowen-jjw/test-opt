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
import mutations.invariant.Invariant;
import objectoperation.list.CommonOperation;
import processmemory.LoopExecValues;
import mutations.strengthreduction.StrengthReduction;
import mutations.unswitching.UnswitchingOneInvariant;
import mutations.unswitching.UnswitchingTwoInvariant;
import utity.AvailableVariable;
import utity.FixedStuff;
import utity.LoopAllInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenAllNeedAvar {
    File sourceDir;

    String fusionSameHeaderPath;
    String fusionAddPath;
    String fusionMaxPath;
    String srPath;
    String invariantPath;
    String unswitchingOnePath;
    String unswitchingTwoPath;


    public GenAllNeedAvar(File sourceDir, String muIndexPath){
        this.sourceDir = sourceDir;
        this.fusionSameHeaderPath = muIndexPath + "/Fusion/SameHeader";
        this.fusionAddPath = muIndexPath + "/Fusion/Add";
        this.fusionMaxPath = muIndexPath + "/Fusion/Max";
        this.srPath = muIndexPath + "/StrengthReduction";
        this.invariantPath = muIndexPath + "/Invariant";
        this.unswitchingOnePath = muIndexPath + "/Unswitching/OneVar";
        this.unswitchingTwoPath = muIndexPath + "/Unswitching/TwoVar";
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
            file.delete();
            StrengthReduction.gIndex = 0;
            FusionSameHeader.gIndex = 0;
            FusionAdd.gIndex = 0;
            FusionMax.gIndex = 0;
        }
        System.out.println("end!");

    }

    public void SingleFileMutation(File file) {
        List<String> initialFileList = CommonOperation.genInitialList(file);

        List<LoopAllInfo> loopInfoListFusion = new ArrayList<>();
        List<LoopAllInfo> loopInfoListSR = new ArrayList<>();
        List<LoopAllInfo> loopInfoListInvariant = new ArrayList<>();
        List<LoopAllInfo> loopInfoListUnswitching1 = new ArrayList<>();
        List<LoopAllInfo> loopInfoListUnswitching2 = new ArrayList<>();

        List<FixedStuff> fsList = new ArrayList<>();
        List<LoopStatement> correspondingLoopList = new ArrayList<>();

        AstInform_Gen astgen = new AstInform_Gen(file);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);

        for(LoopStatement loop: loopList){
            int loopExecTimes = LoopExecValues.getTimes(file, initialFileList, loop.getStartLine(), loop.getEndLine());
            if(loopExecTimes == 0){
                continue;
            }

            Preparation pre = new Preparation();
            FixedStuff fs = pre.getBlockInfo(astgen, file, loop);
            System.out.println(loop.getStartLine() + "  " + loop.getEndLine() + "   " + fs.getAvarList().size());

            if(!fs.isAvailableInAvarList()){
                continue;
            }
            loopInfoListUnswitching1.add(new LoopAllInfo(fs, loop));

            if(fs.getAvarList().size() < 2){
                continue;
            }

            loopInfoListUnswitching2.add(new LoopAllInfo(fs, loop));
            loopInfoListInvariant.add(new LoopAllInfo(fs, loop));
            loopInfoListSR.add(new LoopAllInfo(fs, loop, loopExecTimes));

            List<AvailableVariable> avarUseList = FusionCommon.getAVarUseList(astgen, file, loop);
            if(avarUseList.isEmpty()){
                continue;
            }
            fs.setAuseVarList(avarUseList);

            if(!LoopExecValues.checkConsistency(file, initialFileList, loop.getStartLine(), loop.getEndLine())){
                continue;
            }
            loopInfoListFusion.add(new LoopAllInfo(fs, loop, loopExecTimes));
        }

        //mutations.fusion in same header
        int loopIndex = 0;
        System.out.println("start mutations.fusion in same header......");
        for (LoopAllInfo lai : loopInfoListFusion) {
            System.out.println("mutations.fusion in same header: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            FusionSameHeader tf = new FusionSameHeader(fs.getAuseVarList(), loop, loopIndex, lai.getLoopExecTimes());
            tf.fusion(newFs);
            if (tf.isTrans) {
                fsList.add(newFs);
                correspondingLoopList.add(loop);
                loopIndex++;
            }
        }

        genFiles(fsList, correspondingLoopList, fusionSameHeaderPath);

        fsList.clear();
        correspondingLoopList.clear();

        //mutations.fusion in add execTimes
        System.out.println("start mutations.fusion in add execTimes......");
        loopIndex = 0;
        for (LoopAllInfo lai : loopInfoListFusion) {
            System.out.println("mutations.fusion in add: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            FusionAdd tf = new FusionAdd(fs.getAuseVarList(), loop, loopIndex, lai.getLoopExecTimes());
            tf.fusion(newFs);
            if (tf.isTrans) {
                fsList.add(newFs);
                correspondingLoopList.add(loop);
                loopIndex++;
            }
        }

        genFiles(fsList, correspondingLoopList, fusionAddPath);

        fsList.clear();
        correspondingLoopList.clear();

        //mutations.fusion in max execTimes
        System.out.println("start mutations.fusion in max execTimes......");
        loopIndex = 0;
        for (LoopAllInfo lai : loopInfoListFusion) {
            System.out.println("mutations.fusion in max: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            FusionMax tf = new FusionMax(fs.getAuseVarList(), loop, loopIndex, lai.getLoopExecTimes());
            tf.fusion(newFs);
            if (tf.isTrans) {
                fsList.add(newFs);
                correspondingLoopList.add(loop);
                loopIndex++;
            }
        }

        genFiles(fsList, correspondingLoopList, fusionMaxPath);

        fsList.clear();
        correspondingLoopList.clear();

        //sr
        System.out.println("start sr......");
        loopIndex = 0;
        for (LoopAllInfo lai : loopInfoListSR) {
            System.out.println("sr: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            StrengthReduction sr = new StrengthReduction(loop, loopIndex, lai.getLoopExecTimes());
            sr.strengthReduction(newFs);
            if (sr.isTrans) {
                fsList.add(newFs);
                correspondingLoopList.add(loop);
                loopIndex++;
            }
        }

        genFiles(fsList, correspondingLoopList, srPath);

        fsList.clear();
        correspondingLoopList.clear();

        //mutations.invariant
        System.out.println("start mutations.invariant......");
        loopIndex = 0;
        for(LoopAllInfo lai: loopInfoListInvariant){
            System.out.println("mutations.invariant: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            Invariant inv = new Invariant();
            inv.invariant(newFs);
            if(inv.isTrans) {
                fsList.add(newFs);
                correspondingLoopList.add(loop);
                loopIndex++;
            }
        }

        genFiles(fsList, correspondingLoopList, invariantPath);

        fsList.clear();
        correspondingLoopList.clear();

        //mutations.unswitching one mutations.invariant
        System.out.println("start mutations.unswitching one mutations.invariant......");
        for(LoopAllInfo lai: loopInfoListUnswitching1){
            System.out.println("mutations.unswitching one mutations.invariant: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            UnswitchingOneInvariant uoi = new UnswitchingOneInvariant();
            uoi.unswitching(newFs);
            if(uoi.isTrans) {
                fsList.add(newFs);
                correspondingLoopList.add(loop);
            }
        }

        genFiles(fsList, correspondingLoopList, unswitchingOnePath);

        fsList.clear();
        correspondingLoopList.clear();

        //mutations.unswitching two mutations.invariant
        System.out.println("start mutations.unswitching two mutations.invariant......");
        for(LoopAllInfo lai: loopInfoListUnswitching2){
            System.out.println("mutations.unswitching two mutations.invariant: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            UnswitchingTwoInvariant uti = new UnswitchingTwoInvariant();
            uti.unswitching(newFs);
            if(uti.isTrans) {
                fsList.add(newFs);
                correspondingLoopList.add(loop);
            }
        }

        genFiles(fsList, correspondingLoopList, unswitchingTwoPath);

        fsList.clear();
        correspondingLoopList.clear();

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
