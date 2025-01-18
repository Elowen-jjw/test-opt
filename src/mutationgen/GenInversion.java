package mutationgen;

import astinfo.AstStmtOperation;
import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.Inform_Gen.LoopInform_Gen;
import astinfo.model.LoopStatement;
import common.AllBlockChange;
import common.FinalOperation;
import common.Preparation;
import objectoperation.file.getAllFileList;
import csmithgen.ExceptionCheck;
import mutations.inversion.Inversion;
import objectoperation.structure.StructureTransform;
import utity.FixedStuff;
import utity.LoopAllInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenInversion {
    File sourceDir;

    public String inversionPath;

    public GenInversion(String muIndexPath){
        this.inversionPath = muIndexPath + "/Inversion";
    }

    public GenInversion(File sourceDir, String muIndexPath){
        this.sourceDir = sourceDir;
        this.inversionPath = muIndexPath + "/Inversion";
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

    public void SingleFileMutation(File initialFile) {
        File file = StructureTransform.forTransWhile(initialFile, initialFile.getParent());

        ExceptionCheck ec = new ExceptionCheck();
        if(ec.isTimeout(file)){
            return;
        }

        List<LoopAllInfo> loopInfoListInversion = new ArrayList<>();
        List<FixedStuff> fsList = new ArrayList<>();
        List<LoopStatement> correspondingLoopList = new ArrayList<>();

        AstInform_Gen astgen = new AstInform_Gen(file);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);

        for(LoopStatement loop: loopList){
            if(!loop.getStmtType().equals("WhileStmt")){
                continue;
            }
            Preparation pre = new Preparation();
            FixedStuff fs = pre.getBlockInfoNotAvar(astgen, file, loop);
            loopInfoListInversion.add(new LoopAllInfo(fs, loop));
        }

        //mutations.inversion
        System.out.println("Inversion......");
        for(LoopAllInfo lai: loopInfoListInversion){
            FixedStuff fs = lai.getFs();
            LoopStatement loop = lai.getLoop();
            FixedStuff newFs = new FixedStuff(fs);

            System.out.println(loop.getStartLine() + "  " + loop.getEndLine());

            Inversion in = new Inversion();
            in.inversion(newFs);
            if(in.isTrans) {
                fsList.add(newFs);
                correspondingLoopList.add(loop);
            }
        }

        genFiles(fsList, correspondingLoopList, inversionPath);
        fsList.clear();
        correspondingLoopList.clear();

        file.delete();
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
