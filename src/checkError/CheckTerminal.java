package checkError;

import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import processtimer.ProcessCompiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckTerminal {

    List<String> llvmOList = new ArrayList<>();
    List<String> gccOList = new ArrayList<>();

    public CheckTerminal(){
        initialLlvmOList();
        initialGccOList();
    }

    public void checkErrorFile(String muIndexPath, String compilerType){
        List<String> transErrorNoMet = new ArrayList<>();
        File resultFile = new File(muIndexPath + "/" + compilerType + "-inconsistent.txt");
        List<String> initialFileList = CommonOperation.genInitialList(resultFile);
        for(String s: initialFileList){
            System.out.println(s.trim());
            File muFile = new File(muIndexPath + s.trim());
            File compilerTxt = new File(muFile.getAbsolutePath() + "/" +compilerType + ".txt");
            List<String> errorFilenameList = getErrorFiles(compilerTxt);
            for(String filename: errorFilenameList){
                File transFile = new File(muFile.getAbsolutePath() + "/" + getRandomNumber(s) + "_" + filename + ".c");
                boolean isMet = false;
                if(compilerType.equals("llvm")){
                    isMet = execLlvm(transFile);
                }
                else if(compilerType.equals("gcc")){
                    isMet = execGcc(transFile);
                }
                if(!isMet){
                    transErrorNoMet.add(s);
                    break;
                }
            }
        }
        genNewResultFile(muIndexPath + "/" + compilerType + "-errorNotMet.txt", transErrorNoMet);
    }

    public void checkInconFile(String muIndexPath, String compilerType){
        List<String> transInconList = new ArrayList<>();
        File resultFile = new File(muIndexPath + "/" + compilerType + "-inconsistent.txt");
        List<String> initialFileList = CommonOperation.genInitialList(resultFile);
        for(String s: initialFileList){
            System.out.println(s.trim());
            File muFile = new File(muIndexPath + s.trim());
            File compilerTxt = new File(muFile.getAbsolutePath() + "/" +compilerType + ".txt");
            List<String> inConFilenameList = getInConFiles(compilerTxt);
            if(!inConFilenameList.isEmpty()){
                transInconList.add(s);
            }
        }
        genNewResultFile(muIndexPath + "/" + compilerType + "-transIncon.txt", transInconList);
    }

    public void initialLlvmOList(){
        llvmOList.add("-O0");
        llvmOList.add("-O1");
        llvmOList.add("-O2");
        llvmOList.add("-O3");
        llvmOList.add("-Os");
        llvmOList.add("-Ofast");
        llvmOList.add("-Oz");
    }

    public void initialGccOList(){
        gccOList.add("-O0");
        gccOList.add("-O1");
        gccOList.add("-O2");
        gccOList.add("-O3");
        gccOList.add("-Os");
        gccOList.add("-Ofast");
        gccOList.add("-Og");
    }

    public boolean execLlvm(File file){
        for(String os: llvmOList){
            String aoutFilename = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/random",
                                    file.getAbsolutePath().lastIndexOf("/block")),
                            file.getAbsolutePath().lastIndexOf("/"))
                    .replaceAll("[a-zA-Z/]", "");

            String command = "cd " + file.getParent() + " && clang " + file.getName() + " " + os
                    + " -lm -I $CSMITH_HOME/include -o " + aoutFilename
                    + " && " + "./" + aoutFilename;

            List<String> execLines = ProcessCompiler.processNotKillCompiler(command, 5, "sh", aoutFilename);

            deleteAoutFile(file, aoutFilename);

            if(!analyseUnswitchingExecLinesLlvm(execLines)){
                return false;
            }
        }
        return true;
    }

    public boolean execGcc(File file){
        for(String os: gccOList){
            String aoutFilename = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/random",
                                    file.getAbsolutePath().lastIndexOf("/block")),
                            file.getAbsolutePath().lastIndexOf("/"))
                    .replaceAll("[a-zA-Z/]", "");

            String command = "cd " + file.getParent() + " && gcc " + file.getName() + " " + os
                    + " -lm -I $CSMITH_HOME/include -o " + aoutFilename
                    + " && " + "./" + aoutFilename;

            List<String> execLines = ProcessCompiler.processNotKillCompiler(command, 5, "sh", aoutFilename);

            deleteAoutFile(file, aoutFilename);
            if(!analyseUnswitchingExecLinesGcc(execLines)){
                return false;
            }
        }
        return true;
    }

    public boolean analyseUnswitchingExecLinesGcc(List<String> execLines){
        for(int i=0; i<execLines.size(); i++){
            if(execLines.get(i).contains("error:")){
                if(!execLines.get(i).matches(".*error:\\s*redeclaration of ‘\\w+’ with no linkage")) {
                    System.out.println(execLines.get(i));
                    System.out.println(execLines.get(i + 1));
                    return false;
                }
            }
        }
        return true;
    }

    public boolean analyseUnswitchingExecLinesLlvm(List<String> execLines){
        for(int i=0; i<execLines.size(); i++){
            if(execLines.get(i).contains("error:")){
                if(!execLines.get(i).matches(".*error:\\s*redeclaration of '\\w+' with no linkage")) {
                    System.out.println(execLines.get(i));
                    System.out.println(execLines.get(i + 1));
                    return false;
                }
            }
        }
        return true;
    }

    public boolean analyseUnrollingExecLinesGcc(List<String> execLines){
        String regex = "\\b[a-zA-Z0-9_]+(\\[[0-9]+\\])+\\s*=\\s*(\\{)+";
        Pattern p =  Pattern.compile(regex);
        Matcher m;
        for(int i=0; i<execLines.size(); i++){
            if(execLines.get(i).contains("error:")){
                if(execLines.get(i).contains("read-only") //const var只写一次
                        || execLines.get(i).contains("expected expression before ‘;’ token") //remainder时字符串匹配error
                        || execLines.get(i).contains("unary ‘&’ operand") //&var 不能写成&(var+1)
                        || execLines.get(i).contains("expected expression before ‘{’ token")//结构体declare之后赋值error
                ){
                }
                else {
                    m = p.matcher(execLines.get(i + 1));
                    if (!m.find()) {
                        System.out.println(execLines.get(i));
                        System.out.println(execLines.get(i + 1));
                        return false;
                    }
                }
                i++;
            }
        }
        return true;
    }

    public boolean analyseUnrollingExecLinesLlvm(List<String> execLines){
        String regex = "\\b[a-zA-Z0-9_]+(\\[[0-9]+\\])+\\s*=\\s*(\\{)+";
        Pattern p =  Pattern.compile(regex);
        Matcher m;
        for(int i=0; i<execLines.size(); i++){
            if(execLines.get(i).contains("error:")){
                if(execLines.get(i).contains("const-qualified type") //const var只写一次
                        || execLines.get(i).contains("cannot take the address of an rvalue") //&var 不能写成&(var+1)
                        || execLines.get(i).contains("fatal error: too many errors emitted, stopping now [-ferror-limit=]")
                ){
                }
                else {
                    m = p.matcher(execLines.get(i + 1));
                    if (!m.find()) {
                        if(!execLines.get(i).contains("expected expression")){
                            System.out.println(execLines.get(i));
                            System.out.println(execLines.get(i + 1));
                            return false;
                        }
                    }
                }
                i++;
            }
        }
        return true;
    }

    public String getRandomNumber(String dirs){
        String[] dir = dirs.split("/");
        return dir[1];
    }

    public List<String> getErrorFiles(File compilerTxt){
        FileModify fm = new FileModify();
        List<String> initialFileList = fm.readFile(compilerTxt);
        List<String> errorFilenameList = new ArrayList<String>();
        for(int i=2; i<initialFileList.size(); i++){
            List<String> sList = new ArrayList<>();
            List<String> errorList = new ArrayList<>();
            String regex = "[\\S]+";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(initialFileList.get(i));

            while(m.find()){
                sList.add(m.group());
            }

            for(int j=1; j<sList.size(); j++){
                if(sList.get(j).trim().equals("error")){
                    errorList.add(sList.get(j).trim());
                }
            }

            if(errorList.size() == 7){
                errorFilenameList.add(sList.get(0).trim());
            }
        }
        return errorFilenameList;
    }

    public List<String> getInConFiles(File compilerTxt){
        FileModify fm = new FileModify();
        List<String> initialFileList = fm.readFile(compilerTxt);
        List<String> inConFilenameList = new ArrayList<String>();
        for(int i=2; i<initialFileList.size(); i++){
            List<String> sList = new ArrayList<>();
            List<String> notErrorList = new ArrayList<>();
            String regex = "[\\S]+";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(initialFileList.get(i));

            while(m.find()){
                sList.add(m.group());
            }

            for(int j=1; j<sList.size(); j++){
                if(!sList.get(j).trim().equals("error")){
                    notErrorList.add(sList.get(j).trim());
                }
            }

            if(!notErrorList.isEmpty()){
                inConFilenameList.add(sList.get(0).trim());
            }
        }
        return inConFilenameList;
    }


    public void genNewResultFile(String filePath, List<String> errorTransList){
        File newFile = new File(filePath);
        try {
            if(newFile.exists()){
                newFile.delete();
            }
            newFile.createNewFile();
            FileWriter fw = new FileWriter(newFile, true);
            PrintWriter pw = new PrintWriter(fw);

            errorTransList.forEach(pw::println);

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAoutFile(File file, String aoutFilename){
        File outFile = new File(file.getParent() + "/" + aoutFilename);
        if(outFile.exists()){
            outFile.delete();
        }
    }
}
