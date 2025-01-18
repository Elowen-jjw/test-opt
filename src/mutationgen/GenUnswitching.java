package mutationgen;

import astinfo.AstStmtOperation;
import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.Inform_Gen.LoopInform_Gen;
import astinfo.model.LoopStatement;
import objectoperation.structure.FindInfoInLoop;
import common.AllBlockChange;
import common.FinalOperation;
import common.Preparation;
import objectoperation.file.getAllFileList;
import objectoperation.list.CommonOperation;
import mutations.unswitching.Unswitching;
import processmemory.LoopExecValues;
import utity.FixedStuff;
import utity.IfInLoop;
import utity.LoopAllInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenUnswitching {
    File sourceDir;
    public String unswitchingPath;
    String unswitchingType;

    public GenUnswitching(String muIndexPath, String unswitchingType){
        this.unswitchingType = unswitchingType;
        this.unswitchingPath = muIndexPath + "/Unswitching/" + unswitchingType;
    }

    public GenUnswitching(File sourceDir, String muIndexPath, String unswitchingType){
        this.sourceDir = sourceDir;
        this.unswitchingType = unswitchingType;
        this.unswitchingPath = muIndexPath + "/Unswitching/" + unswitchingType;
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

        List<LoopAllInfo> loopInfoListUnswitching = new ArrayList<>();

        List<FixedStuff> fsList = new ArrayList<>();
        List<LoopStatement> correspondingLoopList = new ArrayList<>();

        AstInform_Gen astgen = new AstInform_Gen(file);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);

        for(LoopStatement loop: loopList){
            List<String> initialBlockList = CommonOperation.getListPart(initialFileList, loop.getStartLine(), loop.getEndLine());
            FindInfoInLoop fil = new FindInfoInLoop();
            List<IfInLoop> ifListInitial = fil.findInfoInLoop(initialBlockList);
            if(ifListInitial.isEmpty()){
                System.out.println("this loop don not have if statement............");
                continue;
            }

            int loopExecTimes = LoopExecValues.getTimes(file, initialFileList, loop.getStartLine(), loop.getEndLine());
            if(loopExecTimes == 0){
                System.out.println("this loop don not execute...........");
                continue;
            }

            Preparation pre = new Preparation();
            FixedStuff fs = pre.getBlockInfo(astgen, file, loop);
            System.out.println(loop.getStartLine() + "  " + loop.getEndLine() + "   " + fs.getAvarList().size());

            if(!fs.isAvailableInAvarList()){
                System.out.println("this loop don not have avar...........");
                continue;
            }
            loopInfoListUnswitching.add(new LoopAllInfo(fs, loop));
        }

        System.out.println("start to generate mutations.unswitching ......");
        for (LoopAllInfo lai : loopInfoListUnswitching) {
            System.out.println("mutations.unswitching: " + lai.getLoop().getStartLine() + "  " + lai.getLoop().getEndLine());
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            Unswitching un = new Unswitching();
            un.unswitching(newFs);
            if (un.isTrans) {
                fsList.add(newFs);
                correspondingLoopList.add(loop);
            }
        }

        genFiles(fsList, correspondingLoopList, unswitchingPath);
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
