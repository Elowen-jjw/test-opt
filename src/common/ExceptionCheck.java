package common;

import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.model.LoopStatement;
import ObjectOperation.list.CommonOperation;
import common.PropertiesInfo;
import processtimer.ProcessTerminal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExceptionCheck {
    final static List<String> oList = new ArrayList<>(List.of(""));
//    final static List<String> oList = new ArrayList<>(Arrays.asList("-O0", "-O1", "-O2", "-O3", "-Os", "-Ofast"));

    public  boolean filterUB(File file){
        List<File> files = new ArrayList<>();
        files.add(file);
        return isPassedSanitizerError(files, "undefined,address", PropertiesInfo.csmithSanitizer);//&& isPassedSanitizerError(files, "undefined,address", "clang")
    }

    public boolean filterUB2(List<File> files){
        return isPassedSanitizerError(files, "undefined", "gcc") && isPassedSanitizerError(files, "undefined", "clang");
    }
    public  boolean filterAddress(File file){
        List<File> files = new ArrayList<>();
        files.add(file);
        return isPassedSanitizerError(files, "address", "gcc") && isPassedSanitizerError(files, "address", "clang");
    }

    public  boolean filterMemory(File file){
        List<File> files = new ArrayList<>();
        files.add(file);
        return isPassedSanitizerError(files, "memory -fPIE -pie", "gcc") && isPassedSanitizerError(files, "memory -fPIE -pie", "clang");
    }

    public boolean isNotHaveLoop(File file){
        AstInform_Gen astgen = new AstInform_Gen(file);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);
        return loopList.isEmpty();
    }

    public boolean isPassedSanitizerError(List<File> files, String type, String compilerType){
        System.out.println(files.get(0).getAbsolutePath() + " start to check " + compilerType + " sanitizer error.....");
        File file = files.get(0);
        for(String os: oList) {
            String aoutFilename = file.getName().substring(0, file.getName().indexOf(".c"))
                    .replaceAll("[a-zA-Z]", "");
            System.out.println("exception::: " + aoutFilename);

            String command = (compilerType.equals("gcc") ?
                    "export LANGUAGE=en && export LANG=en_US.UTF-8 && " : "")
                    + "cd " + file.getParent() + " && " + compilerType + " " + getFileNames(files)
                    + " " + os
                    + " -fsanitize=" + type
                    + " -fno-omit-frame-pointer -g "
                    + " -w -lm -o " + aoutFilename;
            List<String> compilationList = ProcessTerminal.listNotMemCheck(command, "sh");

            File aoutFile = new File(file.getParent() + "/" + aoutFilename);
            if(!aoutFile.exists()){
                System.out.println(file.getAbsolutePath() + " has compiler errors during exception check.......");
                CommonOperation.printList(compilationList);
                return false;
            }

            command = (compilerType.equals("gcc") ?
                    "export LANGUAGE=en && export LANG=en_US.UTF-8 && " : "")
                    + "cd " + file.getParent() + " && " + "./" + aoutFilename;

            List<String> execLines = ProcessTerminal.listMemCheck(command, 8, "sh", true,
                    true, new ArrayList<>(Arrays.asList(aoutFilename)));
            deleteAoutFile(file, aoutFilename);
            String sanitizerResult = analysisResult(execLines);

            if (sanitizerResult.equals("timeout") || sanitizerResult.equals("error")) {
                return false;
            }
        }
        return true;
    }
    
    

    public String getFileNames(List<File> files){
        String names = "";
        for(File file: files){
            names += (file.getName() + " ");
        }
        return names;
    }

    public  void deleteAoutFile(File file, String aoutFilename){
        File outFile = new File(file.getParent() + "/" + aoutFilename);
        if(outFile.exists()){
            outFile.delete();
        }
    }

    public  String analysisResult(List<String> execLines){
        if(execLines.isEmpty()){
            System.out.println("This file don't have output!!!");
            return "empty";
        }
        else if(execLines.get(0).equals("timeout")){
            System.out.println("This file execute timeout!!!");
            return "timeout";
        }
        else{
            for(String s: execLines){
                if(s.contains("error:") || s.contains("ERROR:") || s.contains("(core dumped)")){
                    System.out.println("Error: " + s);
                    return "error";
                }
            }
        }
        return "other";
    }
}
