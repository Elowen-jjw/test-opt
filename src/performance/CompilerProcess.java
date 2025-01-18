package performance;

import common.EquivalenceCheck;
import ObjectOperation.file.getAllFileList;
import ObjectOperation.list.CommonOperation;
import processtimer.ProcessCompilerPerformace;
import processtimer.ProcessTerminal;
import utity.CompilationInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class CompilerProcess {
    final static List<String> oList = new ArrayList<>(Arrays.asList("-O0", "-O1", "-O2", "-O3", "-Os", "-Ofast"));

    File muDir;
    String compilerType;
    String compilerCommand = "";

    public void genAllResult(File mutationDir){
        this.muDir = mutationDir;
        if(isChecked()){
            return;
        }
        genResultInOneCompiler("llvm");
        genResultInOneCompiler("gcc");
    }

    public boolean isChecked(){
        File llvmPerformanceFile = new File(muDir.getAbsolutePath() + "/llvm_performance.txt");
        File llvmOutputFile = new File(muDir.getAbsolutePath() + "/llvm_output.txt");
        File memoryErrorFile = new File(muDir.getAbsolutePath() + "/memory_error.txt");
        File gccPerformanceFile = new File(muDir.getAbsolutePath() + "/gcc_performance.txt");
        File gccOutputFile = new File(muDir.getAbsolutePath() + "/gcc_output.txt");
        return (llvmPerformanceFile.exists() && llvmOutputFile.exists() && gccPerformanceFile.exists() && gccOutputFile.exists())
                || (memoryErrorFile.exists() && gccPerformanceFile.exists() && gccOutputFile.exists());
    }

    public void genResultInOneCompiler(String compilerType){
        this.compilerType = compilerType;
        if(compilerType.equals("llvm")){
            compilerCommand = "clang";
            EquivalenceCheck ec = new EquivalenceCheck();
            if(ec.memoryNotPassed(muDir)){
                createFile(new File(muDir.getAbsolutePath() + "/memory_error.txt"));
                return;
            }
        }
        else if(compilerType.equals("gcc")){
            compilerCommand = "gcc";
        }

        CommentChecksum.AddComment(muDir);

        List<File> allFileList = new ArrayList<File>();
        getAllFileList getFileList = new getAllFileList(muDir);
        getFileList.getAllFile(muDir, allFileList);
        getFileList.compareFileList(allFileList);

        Map<String, CompilationInfo> infos = new TreeMap<>();

        for(File file: allFileList){
            if(!file.getName().endsWith(".c")){
                continue;
            }
            System.out.println(file.getAbsolutePath());

            String splitfilename = file.getName().substring(file.getName().indexOf("_") + 1, file.getName().indexOf(".c"))
                    .replace("initial", "init")
                    .replace("transformed", "trans");
            CompilationInfo info = compilerProcess(file);
            infos.put(splitfilename, info);
        }
        genFiles(infos);
    }

    public void genFiles(Map<String, CompilationInfo> infos){
        File performanceFile = new File(muDir.getAbsolutePath() + "/" + compilerType + "_performance.txt");
        File outputFile = new File(muDir.getAbsolutePath() + "/" + compilerType + "_output.txt");
        createFile(performanceFile);
        createFile(outputFile);

        Map<String, Map<String, String>> performanceMaps = new HashMap<>();
        Map<String, Map<String, String>> outputMaps = new HashMap<>();

        for(String filename: infos.keySet()){
            performanceMaps.put(filename, infos.get(filename).getPerformanceMap());
            outputMaps.put(filename, infos.get(filename).getOutputChecksumMap());
        }
        genSpecificFile(performanceMaps, performanceFile);
        genSpecificFile(outputMaps, outputFile);
    }

    public void genSpecificFile(Map<String, Map<String, String>> maps, File resultFile){
        try{
            FileWriter fw = new FileWriter(resultFile, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println("               " + printLine(oList));
            for(String filename: maps.keySet()){
                Map<String, String> oresults = maps.get(filename);
                String brace = "";
                for(int i=0; i<15-filename.length(); i++){
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
            temp += ("  " + key + addBrace(key) + "  ");
        }
        return temp;
    }

    public String addBrace(String key){
        String braces = "";
        for(int i=0; i<15-key.length(); i++){
            braces += " ";
        }
        return braces;
    }

    public CompilationInfo compilerProcess(File file){
        Map<String,String> performanceMap = new TreeMap<>();
        Map<String,String> outputMap = new TreeMap<>();
        for(String os: oList){
            String aoutFilename = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/random",
                    file.getAbsolutePath().lastIndexOf("/block")),
                    file.getAbsolutePath().lastIndexOf("/"))
                    .replaceAll("/", "")
                    .replaceAll("random", "")
                    .replaceAll("block", "")
                    .replaceAll("mutation", "");

            long compilationTime = 0;
            long executionTime = 0;
            long startTime;

            String command = "cd " + file.getParent() + " && " + compilerCommand + " " + file.getName() + " " + os
                    + " -w -lm -I $CSMITH_HOME/include -o " + aoutFilename;
            ProcessTerminal pt = new ProcessTerminal();
            startTime = System.currentTimeMillis();
            pt.processThreadNotLimitJustExec(command, "sh");
            compilationTime = System.currentTimeMillis() - startTime;

            command = "cd " + file.getParent() + " && " + "./" + aoutFilename;

            File aoutFile = new File(file.getParent() + "/" + aoutFilename);
            if(!aoutFile.exists()) {
                outputMap.put(os, "error");//没有生成a.out的标记成error
            }
            else{
                startTime = System.currentTimeMillis();
                List<String> execLines = ProcessCompilerPerformace.process(command, 30, "sh", aoutFilename);
                executionTime = System.currentTimeMillis() - startTime;
                CommonOperation.printList(execLines);
                deleteAoutFile(file, aoutFilename);
                outputMap.put(os, analysisResult(execLines));
            }
            performanceMap.put(os, compilationTime + "+" + executionTime);

            System.out.println(os + "   " + outputMap.get(os) + "   " + compilationTime + "+" + executionTime);
        }
        CompilationInfo info = new CompilationInfo();
        info.setOutputChecksumMap(outputMap);
        info.setPerformanceMap(performanceMap);
        return info;
    }

    public void deleteAoutFile(File file, String aoutFilename){
        File outFile = new File(file.getParent() + "/" + aoutFilename);
        if(outFile.exists()){
            outFile.delete();
        }
    }

    public void createFile(File file){
        if(file.exists()){
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String analysisResult(List<String> execLines){
        if(!execLines.isEmpty()){
            if(execLines.get(0).trim().equals("timeout")) {
                return "timeout";
            }
            return "exception";
        }
        return "black";
    }
}
