package genAndTest;

import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.model.LoopStatement;
import mutations.invariant.Invariant;
import identicalTestResult.DirIterator;
import common.AllBlockChange;
import common.FinalOperation;
import common.Preparation;
import ObjectOperation.file.getAllFileList;
import ObjectOperation.list.CommonOperation;
import processtimer.LoopExecValues;
import mutations.unswitching.UnswitchingOneInvariant;
import mutations.unswitching.UnswitchingTwoInvariant;
import utity.FixedStuff;
import utity.LoopAllInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenAndTestTwoMutations {
    File sourceDir;

    String invariantPath;
    String unswitchingOnePath;
    String unswitchingTwoPath;


    GenAndTestTwoMutations(File sourceDir, String muIndexPath){
        this.sourceDir = sourceDir;
        this.invariantPath = muIndexPath + "/Invariant";
        this.unswitchingOnePath = muIndexPath + "/Unswitching/OneVar";
        this.unswitchingTwoPath = muIndexPath + "/Unswitching/TwoVar";
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
        File inMuFile = new File(invariantPath + "/" + randomFilename);
        File uwMuFile1 = new File(unswitchingOnePath + "/" + randomFilename);
        File uwMuFile2 = new File(unswitchingTwoPath + "/" + randomFilename);
        boolean isExistInMuFile = inMuFile.exists();
        boolean isExistInUwFile1 = uwMuFile1.exists();
        boolean isExistInUwFile2 = uwMuFile2.exists();

        if(isExistInMuFile && isExistInUwFile1 && isExistInUwFile2){
            System.out.println("mutations have all completed....");
            testExistedMu(invariantPath, file);
            testExistedMu(unswitchingOnePath, file);
            testExistedMu(unswitchingTwoPath, file);
            return;
        }

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
        }

        //mutations.invariant
        System.out.println("start mutations.invariant......");
        if(!isExistInMuFile) {
            for (LoopAllInfo lai : loopInfoListInvariant) {
                System.out.println("mutations.invariant: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
                FixedStuff fs = lai.getFs();
                LoopStatement loop = lai.getLoop();
                FixedStuff newFs = new FixedStuff(fs);

                Invariant inv = new Invariant();
                inv.invariant(newFs);
                if (inv.isTrans) {
                    fsList.add(newFs);
                    correspondingLoopList.add(loop);
                }
            }
            genFiles(fsList, correspondingLoopList, invariantPath);
        }

        System.out.println("start to generate inconsistence check in mutations.invariant: ");
        if(isExistInMuFile){
            System.out.println(file.getName() + " has completed mutations.invariant mutation generation......");
            testExistedMu(invariantPath, file);
        }
        else {
            testMu(invariantPath, file);
        }

        fsList.clear();
        correspondingLoopList.clear();

        //mutations.unswitching one mutations.invariant
        System.out.println("start mutations.unswitching one mutations.invariant......");
        if(!isExistInUwFile1) {
            for (LoopAllInfo lai : loopInfoListUnswitching1) {
                System.out.println("mutations.unswitching one mutations.invariant: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
                FixedStuff fs = lai.getFs();
                LoopStatement loop = lai.getLoop();
                FixedStuff newFs = new FixedStuff(fs);

                UnswitchingOneInvariant uoi = new UnswitchingOneInvariant();
                uoi.unswitching(newFs);
                if (uoi.isTrans) {
                    fsList.add(newFs);
                    correspondingLoopList.add(loop);
                }
            }
            genFiles(fsList, correspondingLoopList, unswitchingOnePath);
        }

        System.out.println("start to generate inconsistence check in mutations.unswitching one mutations.invariant: ");
        if(isExistInUwFile1){
            System.out.println(file.getName() + " has completed mutations.unswitching one mutations.invariant mutation generation......");
            testExistedMu(unswitchingOnePath, file);
        }
        else {
            testMu(unswitchingOnePath, file);
        }

        fsList.clear();
        correspondingLoopList.clear();

        //mutations.unswitching two mutations.invariant
        System.out.println("start mutations.unswitching two mutations.invariant......");
        if(!isExistInUwFile2) {
            for (LoopAllInfo lai : loopInfoListUnswitching2) {
                System.out.println("mutations.unswitching two mutations.invariant: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
                FixedStuff fs = lai.getFs();
                LoopStatement loop = lai.getLoop();
                FixedStuff newFs = new FixedStuff(fs);

                UnswitchingTwoInvariant uti = new UnswitchingTwoInvariant();
                uti.unswitching(newFs);
                if (uti.isTrans) {
                    fsList.add(newFs);
                    correspondingLoopList.add(loop);
                }
            }
            genFiles(fsList, correspondingLoopList, unswitchingTwoPath);
        }

        System.out.println("start to generate inconsistence check in mutations.unswitching two mutations.invariant: ");
        if(isExistInUwFile2){
            System.out.println(file.getName() + " has completed mutations.unswitching two mutations.invariant mutation generation......");
            testExistedMu(unswitchingTwoPath, file);
        }
        else {
            testMu(unswitchingTwoPath, file);
        }

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
