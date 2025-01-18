package csmithgen;

import astinfo.AstStmtOperation;
import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.Inform_Gen.LoopInform_Gen;
import astinfo.model.LoopStatement;
import overall.OverallProcess;
import processmemory.ProcessCompiler;
import processmemory.ProcessTerminal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExceptionCheck {
    final static List<String> oList = new ArrayList<>(List.of("-O0"));
//    final static List<String> oList = new ArrayList<>(Arrays.asList("-O0", "-O1", "-O2", "-O3", "-Os", "-Ofast"));

    public  boolean filterUB(File file){
        return isSanitizerError(file, "undefined");
    }
    public  boolean filterUBAndAddress(File file){
        return isSanitizerError(file, "undefined,address");
    }

    public  boolean filterMemory(File file){
        return isSanitizerError(file, "memory -fPIE -pie");
    }

    public boolean isNotHaveLoop(File file){
        AstInform_Gen astgen = new AstInform_Gen(file);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);
        return loopList.isEmpty();
    }

    public boolean isTimeout(File file){
        String aoutFilename = file.getName().substring(0, file.getName().indexOf(".c"))
                .replaceAll("[a-zA-Z_]", "");

        String command = (OverallProcess.commandType.equals("gcc")? "export LANGUAGE=en && export LANG=en_US.UTF-8 && ": "")
                + "cd " + file.getParent() + " && " + OverallProcess.commandType + " " + file.getName()
                + " -lm -w -I $CSMITH_HOME/include -o " + aoutFilename;
        ProcessTerminal pt = new ProcessTerminal();
        pt.processThreadNotLimitJustExec(command, "sh");

        File aoutFile = new File(file.getParent() + "/" + aoutFilename);
        if(!aoutFile.exists()){
            System.out.println("csmith generation has error in compiler...");
            return true;
        }

        command = "cd " + file.getParent() + " && " + "./" + aoutFilename;

        List<String> execLines = ProcessCompiler.processNotKillCompiler(command, 30, "sh", aoutFilename);

        deleteAoutFile(file, aoutFilename);

        return analysisResult(execLines).equals("timeout");
    }

    public boolean isSanitizerError(File file, String type){
        for(String os: oList) {
            String aoutFilename = file.getName().substring(0, file.getName().indexOf(".c"))
                    .replaceAll("[a-zA-Z_]", "");

            String command = (OverallProcess.commandType.equals("gcc")? "export LANGUAGE=en && export LANG=en_US.UTF-8 && ": "")
                    + "cd " + file.getParent() + " && " + OverallProcess.commandType + " " + file.getName()
                    + " " + os + " -fsanitize=" + type
                    + " -fno-omit-frame-pointer -g -w -lm -I $CSMITH_HOME/include -o " + aoutFilename;
            ProcessTerminal pt = new ProcessTerminal();
            pt.processThreadNotLimitJustExec(command, "sh");

            File aoutFile = new File(file.getParent() + "/" + aoutFilename);
            if(!aoutFile.exists()){
                return true;
            }

            command = "cd " + file.getParent() + " && " + "./" + aoutFilename;

            List<String> execLines = ProcessCompiler.processNotKillCompiler(command, 30, "sh", aoutFilename);

            deleteAoutFile(file, aoutFilename);

            if (analysisResult(execLines).equals("error") || analysisResult(execLines).equals("timeout")) {
                return true;
            }
        }
        return false;
    }


    public  void deleteAoutFile(File file, String aoutFilename){
        File outFile = new File(file.getParent() + "/" + aoutFilename);
        if(outFile.exists()){
            outFile.delete();
        }
    }

    public  String analysisResult(List<String> execLines){
        if(execLines.isEmpty()){
            System.out.println("timeout............");
            return "timeout";
        }
        else{
            for(String s: execLines){
                if(s.contains("error:") || s.contains("ERROR:")){
                    System.out.println("Error: " + s);
                    return "error";
                }
            }
        }
        return "other";
    }
}
