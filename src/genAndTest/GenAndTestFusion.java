package genAndTest;

import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.model.LoopStatement;
import genMutation.GenFusion;
import identicalTestResult.DirIterator;
import common.AllBlockChange;
import common.FinalOperation;
import ObjectOperation.file.getAllFileList;
import mutations.fusion.FusionAdd;
import mutations.fusion.FusionMax;
import mutations.fusion.FusionSameHeader;
import ObjectOperation.list.CommonOperation;
import utity.FixedStuff;
import utity.LoopAllInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenAndTestFusion {
    File sourceDir;

    String fusionSameHeaderPath;
    String fusionAddPath;
    String fusionMaxPath;


    GenAndTestFusion(File sourceDir, String muIndexPath){
        this.sourceDir = sourceDir;
        this.fusionSameHeaderPath = muIndexPath + "/Fusion_CheckUB/SameHeader";
        this.fusionAddPath = muIndexPath + "/Fusion_CheckUB/Add";
        this.fusionMaxPath = muIndexPath + "/Fusion_CheckUB/Max";
    }

    public void genAndTestMutation() {
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

            FusionSameHeader.gIndex = 0;
            FusionAdd.gIndex = 0;
            FusionMax.gIndex = 0;
        }
        System.out.println("end!");
    }

    public void testMu(String singleIndexPath, File file){
        DirIterator dt = new DirIterator(new File(singleIndexPath));
        dt.runSingleRandom(new File(singleIndexPath + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c"))));
    }

    public void testExistedMu(String singleIndexPath, File file){
        String randomName = file.getName().substring(0, file.getName().lastIndexOf(".c"));
        DirIterator dt = new DirIterator(new File(singleIndexPath));
        dt.checkExistedRandom("/" + randomName);
        dt.runSingleRandom(new File(singleIndexPath + "/" + randomName));
    }

    public void SingleFileMutation(File file) {
        List<String> initialFileList = CommonOperation.genInitialList(file);

        String randomFilename = file.getName().substring(0, file.getName().lastIndexOf(".c"));
        File sameHeaderMuFile = new File(fusionSameHeaderPath + "/" + randomFilename);
        File addMuFile = new File(fusionAddPath + "/" + randomFilename);
        File maxMuFile = new File(fusionMaxPath + "/" + randomFilename);
        boolean isExistInSameHeaderMuFile = sameHeaderMuFile.exists();
        boolean isExistAddMuFile = addMuFile.exists();
        boolean isExistMaxMuFile = maxMuFile.exists();

        if(isExistInSameHeaderMuFile && isExistAddMuFile && isExistMaxMuFile){
            System.out.println(file.getName() + "'s mutations have all completed....");
            testExistedMu(fusionSameHeaderPath, file);
            testExistedMu(fusionAddPath, file);
            testExistedMu(fusionMaxPath, file);
            return;
        }

        List<LoopAllInfo> loopInfoListFusion = new ArrayList<>();

        List<FixedStuff> fsList = new ArrayList<>();
        List<LoopStatement> correspondingLoopList = new ArrayList<>();

        AstInform_Gen astgen = new AstInform_Gen(file);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);

        GenFusion.genLoopInfo(file, initialFileList, loopInfoListFusion, astgen, loopList);

        //mutations.fusion in same header
        int loopIndex = 0;
        System.out.println("start mutations.fusion in same header......");
        if(!isExistInSameHeaderMuFile) {
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
        }

        System.out.println("start to generate inconsistence check in mutations.fusion same header: ");
        if(isExistInSameHeaderMuFile){
            System.out.println(file.getName() + " has completed mutations.fusion same header mutation generation......");
            testExistedMu(fusionSameHeaderPath, file);
        }
        else {
            testMu(fusionSameHeaderPath, file);
        }

        fsList.clear();
        correspondingLoopList.clear();

        //mutations.fusion in add execTimes
        System.out.println("start mutations.fusion in add execTimes......");
        loopIndex = 0;
        if(!isExistAddMuFile) {
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
        }

        System.out.println("start to generate inconsistence check in mutations.fusion add: ");
        if(isExistAddMuFile){
            System.out.println(file.getName() + " has completed mutations.fusion add mutation generation......");
            testExistedMu(fusionAddPath, file);
        }
        else {
            testMu(fusionAddPath, file);
        }

        fsList.clear();
        correspondingLoopList.clear();

        //mutations.fusion in max execTimes
        System.out.println("start mutations.fusion in max execTimes......");
        loopIndex = 0;
        if(!isExistMaxMuFile) {
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
        }

        System.out.println("start to generate inconsistence check in mutations.fusion max: ");
        if(isExistMaxMuFile){
            System.out.println(file.getName() + " has completed mutations.fusion max mutation generation......");
            testExistedMu(fusionMaxPath, file);
        }
        else {
            testMu(fusionMaxPath, file);
        }

        fsList.clear();
        correspondingLoopList.clear();
    }

    public static void genFiles(List<FixedStuff> fsList, List<LoopStatement> correspondingLoopList, String indexDir){
        int count = 0;
        for(FixedStuff fs: fsList){
            if(fs.getIatList().isEmpty()){
                continue;
            }
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
