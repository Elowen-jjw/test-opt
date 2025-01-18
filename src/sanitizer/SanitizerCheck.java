package sanitizer;

import overall.OverallProcess;
import processmemory.ProcessCompiler;
import processmemory.ProcessTerminal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SanitizerCheck {
//    final static List<String> oList = new ArrayList<>(Arrays.asList("-O0", "-O1", "-O2", "-O3", "-Os", "-Ofast"));
    String compilerCommand;
    List<String> specificUBError;

    public SanitizerCheck(String compilerCommand){
        this.compilerCommand = compilerCommand;
        this.specificUBError = new ArrayList<>();
    }

    public boolean filterUB(File file, List<String> specificUBError){
        this.specificUBError = specificUBError;
        return isHaveSantinizerError(file, "undefined");
    }

    public String filterUBAndAddress(File file){
        return isNotEquivalentTrans(file, "undefined,address");
    }

    public String filterMemory(File file){
        return isNotEquivalentTrans(file, "memory -fPIE -pie");
    }

    public  boolean isHaveSantinizerError(File file, String type){
        List<String> execLines = runSanitizer(file, type);
        return analysisResult(execLines).equals("error") || analysisResult(execLines).equals("timeout");
    }

    public String isNotEquivalentTrans(File file, String type){
        List<String> execLines = runSanitizer(file, type);
        return analysisResult(execLines);
    }

    private List<String> runSanitizer(File file, String type) {
        List<String> execLines = new ArrayList<>();
        String aoutFilename = file.getName().substring(0, file.getName().indexOf(".c"))
                .replaceAll("[a-zA-Z_]", "");

        String command = (OverallProcess.commandType.equals("gcc")? "export LANGUAGE=en && export LANG=en_US.UTF-8 && ": "")
                + "cd " + file.getParent() + " && " + OverallProcess.commandType + " " + file.getName()
                + " -O0 -fsanitize=" + type
                + " -w -lm -I $CSMITH_HOME/include -o " + aoutFilename;
        ProcessTerminal pt = new ProcessTerminal();
        pt.processThreadNotLimitJustExec(command, "sh");

        File aoutFile = new File(file.getParent() + "/" + aoutFilename);
        if(aoutFile.exists()){
            command = "cd " + file.getParent() + " && " + "./" + aoutFilename;
            execLines = ProcessCompiler.processNotKillCompiler(command, 30, "sh", aoutFilename);
            deleteAoutFile(file, aoutFilename);
        }
        return execLines;
    }

    public void deleteAoutFile(File file, String aoutFilename){
        File outFile = new File(file.getParent() + "/" + aoutFilename);
        if(outFile.exists()){
            outFile.delete();
        }
    }

    public String analysisResult(List<String> execLines){
        if(execLines.isEmpty()){
            return "timeout";
        }
        else{
            for(String s: execLines){
                if((s.contains("error:") && containsListElement(specificUBError, s)) || s.contains("ERROR:")){
                    return "error";
                }
                if(s.trim().matches("checksum\\s*=\\s*[0-9a-zA-Z]+")){//s.replace("checksum", "").replace("=", "").trim();
                    return s.trim();
                }
            }
        }
        return "other";
    }

    public boolean containsListElement(List<String> specificUBError, String line){
        if(specificUBError.isEmpty())
            return true;
        for(String s: specificUBError){
            if(line.contains(s))
                return true;
        }
        return false;
    }
}
