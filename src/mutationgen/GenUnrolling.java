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
import mutations.unrolling.Unrolling;
import utity.FixedStuff;
import utity.LoopAllInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenUnrolling {
    File sourceDir;

    public String unrollingPath;

    public GenUnrolling(String muIndexPath){
        this.unrollingPath = muIndexPath + "/Unrolling";
    }

    public GenUnrolling(File sourceDir, String muIndexPath){
        this.sourceDir = sourceDir;
        this.unrollingPath = muIndexPath + "/Unrolling";
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
            System.out.println("start to generate mutations:");

            SingleFileMutation(file);
            file.delete();
        }
    }

    public void SingleFileMutation(File file) {
        List<String> initialFileList = CommonOperation.genInitialList(file);

        List<LoopAllInfo> loopInfoListunrolling = new ArrayList<>();
        List<FixedStuff> fsList = new ArrayList<>();
        List<LoopStatement> correspondingLoopList = new ArrayList<>();

        AstInform_Gen astgen = new AstInform_Gen(file);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);

        for(LoopStatement loop: loopList){
            int loopExecTimes = LoopExecValues.getTimes(file, initialFileList, loop.getStartLine(), loop.getEndLine());
            if(loopExecTimes == 0 || loopExecTimes == 1){
                continue;
            }
            if(!LoopExecValues.checkConsistency(file, initialFileList, loop.getStartLine(), loop.getEndLine())){
                continue;
            }

            Preparation pre = new Preparation();
            FixedStuff fs = pre.getBlockInfoNotAvar(astgen, file, loop);

            loopInfoListunrolling.add(new LoopAllInfo(fs, loop, loopExecTimes));
        }

        //mutations.unrolling
        System.out.println("mutations.unrolling......");
        for(LoopAllInfo lai: loopInfoListunrolling){
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            System.out.println(loop.getStartLine() + "  " + loop.getEndLine());

            Unrolling un = new Unrolling(loop, lai.getLoopExecTimes());
            un.unrolling(newFs);

            if(un.isTrans) {
                fsList.add(newFs);
                correspondingLoopList.add(loop);
            }
        }

        genFiles(fsList, correspondingLoopList, unrollingPath);
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
