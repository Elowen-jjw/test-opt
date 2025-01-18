package identicalTestResult;

import ObjectOperation.file.FileModify;
import ObjectOperation.file.getAllFileList;
import processtimer.ProcessCompiler;
import processtimer.ProcessTerminal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompilerProcess {
    List<String> llvmOList = new ArrayList<>();
    List<String> gccOList = new ArrayList<>();

    File muDir;

    public CompilerProcess(){
        initialLlvmOList();
        initialGccOList();
    }

    public Map<String, Boolean> genResult(File mutationDir){
        this.muDir = mutationDir;
        List<File> allFileList = new ArrayList<File>();
        getAllFileList getFileList = new getAllFileList(muDir);
        getFileList.getAllFile(muDir, allFileList);
        getFileList.compareFileList(allFileList);

        Map<String, Map<String, String>> llvmMaps = new TreeMap<>();
        Map<String, Map<String, String>> gccMaps = new TreeMap<>();

        Map<String, Boolean> resultMap = new TreeMap<>();

        File llvmFile = new File(muDir.getAbsolutePath() + "/llvm.txt");
        File gccFile = new File(muDir.getAbsolutePath() + "/gcc.txt");
        boolean isEndLlvm = false;
        boolean isEndGcc = false;

        if(llvmFile.exists()){
            if(isAll(llvmFile)) {
                isEndLlvm = true;
                resultMap.put("llvm", readFileToResult(llvmFile));
            }else{
                addMaps(llvmMaps, llvmFile, llvmOList);
            }
        }else{
            genNewFile(llvmFile, llvmOList);
        }

        if(gccFile.exists()){
            if(isAll(gccFile)) {
                isEndGcc = true;
                resultMap.put("gcc", readFileToResult(gccFile));
            }else{
                addMaps(gccMaps, gccFile, gccOList);
            }
        }else{
            genNewFile(gccFile, gccOList);
        }

        if(isEndLlvm && isEndGcc){
            return resultMap;
        }

        for(File file: allFileList){
            if(!file.getName().endsWith(".c")){
                continue;
            }
            System.out.println(file.getAbsolutePath());

            String splitfilename = file.getName().substring(file.getName().indexOf("_") + 1, file.getName().indexOf(".c"));

            if(!llvmMaps.containsKey(splitfilename)) {
                Map<String, String> llvmResultMap = compilerProcess(file, llvmOList, "clang");
                llvmMaps.put(splitfilename, llvmResultMap);
                genOneResInFile(splitfilename, llvmResultMap, "llvm", llvmOList);
            }

            if(!gccMaps.containsKey(splitfilename)) {
                Map<String, String> gccResultMap = compilerProcess(file, gccOList, "gcc");
                gccMaps.put(splitfilename, gccResultMap);
                genOneResInFile(splitfilename, gccResultMap, "gcc", gccOList);
            }
        }

        resultMap.put("llvm", checkConsistent(llvmMaps));
        resultMap.put("gcc", checkConsistent(gccMaps));

        return resultMap;
    }

    public boolean checkConsistent(Map<String, Map<String, String>> maps){
        Set<String> resultSet = new HashSet<String>();
        for(String filename: maps.keySet()){
            Map<String, String> oMap = maps.get(filename);
            for(String o: oMap.keySet()){
                resultSet.add(oMap.get(o));
            }
        }
        resultSet.remove("skip");
        if(resultSet.size() == 1){
            return true;
        }
        else{
            return false;
        }
    }

    public void addMaps(Map<String, Map<String, String>> maps, File resultFile, List<String> oList){
        FileModify fm = new FileModify();
        List<String> initialFileList = fm.readFile(resultFile);
        for(int i=1; i<initialFileList.size(); i++){
            List<String> sList = new ArrayList<>();
            String regex = "[\\S]+";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(initialFileList.get(i));

            while(m.find()){
                sList.add(m.group());
            }

            Map<String, String> oMap = new TreeMap<>();
            for(int j = 0; j < oList.size(); j++){
                oMap.put(oList.get(j), sList.get(j+1).trim());
            }
            maps.put(sList.get(0), oMap);
        }
    }

    public boolean isAll(File file){
        FileModify fm = new FileModify();
        List<String> initialFileList = fm.readFile(file);
        if((initialFileList.size() - 1) == countCFiles()){
            return true;
        }
        return false;
    }

    public int countCFiles(){
        int count = 0;
        for(File file: muDir.listFiles()){
            if(file.getName().endsWith(".c")){
                count++;
            }
        }
        return count;
    }

    public boolean readFileToResult(File file){
        FileModify fm = new FileModify();
        List<String> initialFileList = fm.readFile(file);
        Set<String> resultSet = new HashSet<String>();
        for(int i=1; i<initialFileList.size(); i++){
            List<String> sList = new ArrayList<>();
            String regex = "[\\S]+";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(initialFileList.get(i));

            while(m.find()){
                sList.add(m.group());
            }

            for(int j=1; j<sList.size(); j++){
                resultSet.add(sList.get(j).trim());
            }
        }

        resultSet.remove("skip");
        if(resultSet.size() == 1){
            return true;
        }
        else{
            return false;
        }
    }

    public void genNewFile(File newFile, List<String> oList){
        try {
            newFile.createNewFile();
            FileWriter fw = new FileWriter(newFile, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println("                  " + printLine(oList));

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void genOneResInFile(String filename, Map<String, String> oneResult, String type, List<String> oList){
        File resultFile = new File(muDir.getAbsolutePath() + "/" + type + ".txt");
        try {
            FileWriter fw = new FileWriter(resultFile, true);
            PrintWriter pw = new PrintWriter(fw);

            String brace = "";
            for(int i=0; i<18-filename.length(); i++){
                brace += " ";
            }

            List<String> resultList = new ArrayList<>();
            for(String os: oList){
                resultList.add(oneResult.get(os));
            }
            pw.println(filename + brace + printLine(resultList));

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void genResultFile(Map<String, Map<String, String>> maps, String type, List<String> oList){
        File resultFile = new File(muDir.getAbsolutePath() + "/" + type + ".txt");
        if(resultFile.exists()){
            resultFile.delete();
        }
        try {
            resultFile.createNewFile();

            FileWriter fw = new FileWriter(resultFile, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println("                  " + printLine(oList));

            for(String filename: maps.keySet()){
                Map<String, String> oresults = maps.get(filename);

                String brace = "";
                for(int i=0; i<18-filename.length(); i++){
                    brace += " ";
                }

                List<String> resultList = new ArrayList<>();
                for(String os: oList){
                    resultList.add(oresults.get(os));
                }
                pw.println(filename + brace + printLine(resultList));
            }

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String printLine(List<String> keyList){
        String temp = "";
        for(String key: keyList){
            temp += ("   " + key + addBrace(key) + "   ");
        }
        return temp;
    }

    public String addBrace(String key){
        String braces = "";
        for(int i=0; i<10-key.length(); i++){
            braces += " ";
        }
        return braces;
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

    public Map<String, String> compilerProcess(File file, List<String> oList, String typeCommand){
        Map<String,String> resultMap = new TreeMap<>();
        for(String os: oList){
            if(os.equals("-Og") || os.equals("-Oz")){
                resultMap.put(os, "skip");
                continue;
            }
            String aoutFilename = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/random",
                                    file.getAbsolutePath().lastIndexOf("/block")),
                            file.getAbsolutePath().lastIndexOf("/"))
                    .replaceAll("/", "")
                    .replaceAll("random", "")
                    .replaceAll("block", "")
                    .replaceAll("mutation", "");

            String command = (typeCommand.equals("gcc")? "export LANGUAGE=en && export LANG=en_US.UTF-8 && ": "") + "cd " + file.getParent() + " && " + typeCommand + " " + file.getName() + " " + os
                    + " -lm -I $CSMITH_HOME/include -o " + aoutFilename;

            ProcessTerminal pt = new ProcessTerminal();
            pt.processThreadNotLimitJustExec(command, "sh");

            File aoutFile = new File(file.getParent() + "/" + aoutFilename);
            if(!aoutFile.exists()){
                resultMap.put(os, "error");
            }
            else {
                command = "cd " + file.getParent() + " && " + "./" + aoutFilename;
                List<String> execLines = ProcessCompiler.processNotKillCompiler(command, 30, "sh", aoutFilename);
                deleteAoutFile(file, aoutFilename);

                resultMap.put(os, analysisResult(execLines));
                System.out.println(os + "   " + analysisResult(execLines));
            }
        }
        return resultMap;
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
                if(s.contains("error:")){
                    return "error";
                }
                if(s.contains("Segmentation fault (core dumped)")){
                    return "seg";
                }
                if(s.contains("checksum")){
                    return s.replace("checksum", "").replace("=", "").trim();
                }
            }
        }
        return "other";
    }
}
