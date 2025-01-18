package mutationgen;

import astinfo.AstStmtOperation;
import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.Inform_Gen.LoopInform_Gen;
import astinfo.model.LoopStatement;
import common.AllBlockChange;
import common.FinalOperation;
import common.Preparation;
import objectoperation.file.getAllFileList;
import objectoperation.list.CommonOperation;
import processmemory.LoopExecValues;
import mutations.strengthreduction.StrengthReduction;
import utity.FixedStuff;
import utity.LoopAllInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenStrengthReduction {
    File sourceDir;
    public String srPath;

    public GenStrengthReduction(String muIndexPath){
        this.srPath = muIndexPath + "/StrengthReduction";
    }

    public GenStrengthReduction(File sourceDir, String muIndexPath){
        this.sourceDir = sourceDir;
        this.srPath = muIndexPath + "/StrengthReduction";
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
        }
    }

    public void SingleFileMutation(File file) {
        List<String> initialFileList = CommonOperation.genInitialList(file);

        List<LoopAllInfo> loopInfoListSR = new ArrayList<>();

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

            if(fs.getAvarList().size() < 2){
                continue;
            }

            loopInfoListSR.add(new LoopAllInfo(fs, loop, loopExecTimes));
        }

        //sr
        System.out.println("start sr......");
        int loopIndex = 0;
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

        StrengthReduction.gIndex = 0;
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
