package testcompareresults;

import objectoperation.file.getAllFileList;
import overall.OverallProcess;
import processmemory.ProcessCompiler;
import processmemory.ProcessTerminal;
import utity.CompilationInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class TestCompiler {
    final static List<String> oList = new ArrayList<>(Arrays.asList("-O0", "-O1", "-O2", "-O3", "-Os"));
    final static List<String> llvmConfigList = new ArrayList<>(Arrays.asList("-finline-functions",
            "-finline-hint-functions",
            "-fno-integrated-as",
            "-fkeep-static-consts",
            "-fno-merge-all-constants",
            "-fno-common",
            "-fno-reroll-loops",
            "-fno-unroll-loops",
            "-fvectorize",
            "-fno-complete-member-pointers",
            "-fno-coverage-mapping",
            "-fno-discard-value-names",
            "-fno-fine-grained-bitfield-accesses",
            "-fno-fixed-point",
            "-fno-gnu-inline-asm",
            "-fno-lax-vector-conversions",
            "-fno-use-init-array"));

    final static List<String> gccConfigList = new ArrayList<>(Arrays.asList("-fno-dce",
            "-fno-dse",
            "-fno-defer-pop",
            "-fno-forward-propagate",
            "-fno-rerun-cse-after-loop",
            "-fno-omit-frame-pointer",
            "-foptimize-sibling-calls",
            "-foptimize-strlen",
            "-fno-inline-small-functions",
            "-fno-inline-functions-called-once",
            "-fno-inline-small-functions",
            "-fno-thread-jumps",
            "-fno-branch-count-reg",
            "-fno-merge-constants",
            "-fno-split-wide-types",
            "-fno-gcse",
            "-fno-if-conversion",
            "-fno-code-hoisting",
            "-fno-tree-pre",
            "-fno-tree-partial-pre",
            "-fno-tree-loop-distribute-patterns",
            "-fno-tree-dominator-opts",
            "-fno-tree-dce",
            "-fno-tree-dse",
            "-fno-tree-loop-optimize"));
    String config;
    File muDir;
    String compilerType;

    public TestCompiler(File muDir, String compilerType){
        this.muDir = muDir;
        this.compilerType = compilerType;
        this.config = getRandomConfig();
    }

    public Map<String, Boolean> run() {
        List<CompilationInfo> sanitizerInfoList = genSanitizerResult();
        ParseResult pr = new ParseResult();
        Map<String, Boolean> allResultMap = new HashMap<>(pr.parseSanitizerInfoList(sanitizerInfoList));

        if (allResultMap.get("sanitizer_pass")) {
//            CommentChecksum cc = new CommentChecksum();
//            cc.AddComment(muDir);
            List<CompilationInfo> infoList = genOutputResult();
            allResultMap.putAll(pr.parseOutputInfoList(infoList));
        }

        return allResultMap;
    }

    public List<CompilationInfo> genSanitizerResult(){
        List<File> allFileList = new ArrayList<File>();
        getAllFileList getFileList = new getAllFileList(muDir);
        getFileList.getAllFile(muDir, allFileList);
        getFileList.compareFileList(allFileList);

        List<CompilationInfo> sanitizerInfoList = new ArrayList<>();

        for(File file: allFileList){
            if(!file.getName().endsWith(".c")){
                continue;
            }
            System.out.println(file.getAbsolutePath());

            String splitfilename = file.getName().substring(file.getName().indexOf("_") + 1, file.getName().indexOf(".c"))
                    .replace("initial", "init")
                    .replace("transformed", "trans");

            CompilationInfo info = runSanitize(file);
            info.setSimpliedFilename(splitfilename);
            sanitizerInfoList.add(info);
        }
        genSpecificFile(sanitizerInfoList, "sanitizer");
        return sanitizerInfoList;
    }

    public List<CompilationInfo> genOutputResult(){
        List<File> allFileList = new ArrayList<File>();
        getAllFileList getFileList = new getAllFileList(muDir);
        getFileList.getAllFile(muDir, allFileList);
        getFileList.compareFileList(allFileList);

        List<CompilationInfo> infoList = new ArrayList<>();

//        if(compilerType.equals("llvm")) {
//            EquivalenceCheck ec = new EquivalenceCheck();
//            if (ec.memoryNotPassed(muDir)) {
//                createFile(new File(muDir.getAbsolutePath() + "/memory_error.txt"));
//                return infoList;
//            }
//        }

        for(File file: allFileList){
            if(!file.getName().endsWith(".c")){
                continue;
            }
            System.out.println(file.getAbsolutePath());

            String splitfilename = file.getName().substring(file.getName().indexOf("_") + 1, file.getName().indexOf(".c"))
                    .replace("initial", "init")
                    .replace("transformed", "trans");

            CompilationInfo info = null;
            if(compilerType.equals("gcc")){
                info = runGcc(file);
            }
            else if(compilerType.equals("llvm")){
                info = runLlvm(file);
            }
            Objects.requireNonNull(info).setSimpliedFilename(splitfilename);
            infoList.add(info);
        }
        genSpecificFile(infoList, "output");
        genSpecificFile(infoList, "performance");
        return infoList;
    }

    public String getRandomConfig(){
        List<String> configList = new ArrayList<>();
        if(compilerType.equals("gcc")){
            configList.addAll(gccConfigList);
        }else if(compilerType.equals("llvm")){
            configList.addAll(llvmConfigList);
        }
        StringBuilder result = new StringBuilder();
        Set<String> configSet = new TreeSet<>();
        Random random = new Random();
        int number = random.nextInt(1, configList.size() + 1);
        for(int i=0; i<number; i++){
            int randomIndex = random.nextInt(configList.size());
            configSet.add(configList.get(randomIndex));
            configList.remove(randomIndex);
        }
        for(String s: configSet){
            result.append(s).append(" ");
        }
        return result.toString();
    }

    public void genSpecificFile(List<CompilationInfo> infoList, String resultType){
        File resultFile = new File(muDir.getAbsolutePath() + "/" + compilerType + "_" + resultType + ".txt" );
        try{
            FileWriter fw = new FileWriter(resultFile);
            PrintWriter pw = new PrintWriter(fw);

            if(!resultType.equals("sanitizer"))
                pw.println("// config: " + config);
            pw.println(addBrace("") + printLine(infoList.get(0).getOutputListMap().keySet().stream().toList()));//write header
            for(CompilationInfo info: infoList){
                String filename = info.getSimpliedFilename();
                Map<String, List<String>> resultMap = new TreeMap<>();
                Map<String, String> performanceMap = new TreeMap<>();

                if(resultType.equals("output") || resultType.equals("sanitizer")){
                    resultMap = info.getOutputListMap();
                    List<String> resultList = new ArrayList<>();
                    for(String os: resultMap.keySet()){
                        if(resultMap.get(os).get(0).length() <= 8){
                            resultList.add(resultMap.get(os).get(0));
                        }else{
                            resultList.add(resultMap.get(os).get(0).substring(0, 8));
                        }
                    }
                    pw.println(filename + addBrace(filename) + printLine(resultList));
                }
                else if(resultType.equals("performance")){
                    performanceMap = info.getPerformanceMap();
                    List<String> resultList = new ArrayList<>();
                    for(String os: performanceMap.keySet()){
                        resultList.add(performanceMap.get(os));
                    }
                    pw.println(filename + addBrace(filename) + printLine(resultList));
                }
            }

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CompilationInfo runSanitize(File file){
        Map<String,List<String>> outputListMap = new TreeMap<>();
        String aoutFilename = simplifyFilename(file);
        for(String os: oList){
            String compileCommand = "export LANGUAGE=en && export LANG=en_US.UTF-8 && " + "cd " + file.getParent() + " && "
                    + OverallProcess.commandType + " " + file.getName() + " " + os
                    + " -fsanitize=undefined,address -w -lm -I $CSMITH_HOME/include -o " + aoutFilename;
            justProcess(compileCommand);

            String execCommand = "cd " + file.getParent() + " && " + "./" + aoutFilename;
            File aoutFile = new File(file.getParent() + "/" + aoutFilename);
            if(!aoutFile.exists()) {
                outputListMap.put(os, new ArrayList<>(List.of("error")));//没有生成a.out的标记成error
            }
            else{
                List<String> execLines = ProcessCompiler.processNotKillCompiler(execCommand, 30, "sh", aoutFilename);
                deleteAoutFile(file, aoutFilename);
                outputListMap.put(os, analysisSanitizerResult(execLines));
            }
            System.out.println("sanitizer: " + os + "   " + outputListMap.get(os));
        }
        return new CompilationInfo(outputListMap);
    }

    public CompilationInfo runGcc(File file){
        Map<String,String> performanceMap = new TreeMap<>();
        Map<String,List<String>> outputListMap = new TreeMap<>();
        String aoutFilename = simplifyFilename(file);
        for(String os: oList){
            long executionTime = 0;
            //
            String compileCommand = "export LANGUAGE=en && export LANG=en_US.UTF-8 && " + "cd " + file.getParent() + " && " + "gcc" + " " + file.getName() + " " + os
                    + " -w " + (os.equals("-O0")?"": config) + " -lm -I $CSMITH_HOME/include -o " + aoutFilename;
            justProcess(compileCommand);
            System.out.println(compileCommand);

            String execCommand = "cd " + file.getParent() + " && " + "./" + aoutFilename;
            File aoutFile = new File(file.getParent() + "/" + aoutFilename);
            if(!aoutFile.exists()) {
                outputListMap.put(os, new ArrayList<>(List.of("error")));//没有生成a.out的标记成error
            }
            else{
                long startTime = System.currentTimeMillis();
                List<String> execLines = ProcessCompiler.processNotKillCompiler(execCommand, 30, "sh", aoutFilename);
                executionTime = System.currentTimeMillis() - startTime;
                deleteAoutFile(file, aoutFilename);
                outputListMap.put(os, analysisOutputResult(execLines));
            }
            performanceMap.put(os, String.valueOf(executionTime));
            System.out.println(os + "   " + executionTime + "   " + outputListMap.get(os));
        }
        return new CompilationInfo(outputListMap, performanceMap);
    }

    public CompilationInfo runLlvm(File file){
        Map<String,String> performanceMap = new TreeMap<>();
        Map<String,List<String>> outputListMap = new TreeMap<>();
        String aoutFilename = "r" + simplifyFilename(file);

        for(String oClang: oList){
            String commandFront = "cd " + file.getParent() + " && clang " + file.getName() + " " + oClang
                    + " " + (oClang.equals("-O0")?"": config) + " -w -lm -I $CSMITH_HOME/include -o " + aoutFilename;
            System.out.println(commandFront);
            justProcess(commandFront);

            String comandExec = "cd " + file.getParent() + " && ./" + aoutFilename;
            long executionTime = 0;
            File aoutFile = new File(file.getParent() + "/" + aoutFilename);
            if(!aoutFile.exists()) {
                outputListMap.put(oClang, new ArrayList<>(List.of("error")));//没有生成a.out的标记成error
            }
            else{
                long startTime = System.currentTimeMillis();
                List<String> execLines = ProcessCompiler.processNotKillCompiler(comandExec, 30, "sh", aoutFilename);
                executionTime = System.currentTimeMillis() - startTime;
                deleteAoutFile(file, aoutFilename);
                outputListMap.put(oClang, analysisOutputResult(execLines));
            }
            performanceMap.put(oClang, String.valueOf(executionTime));
            System.out.println(oClang + "   " + executionTime + "   " + outputListMap.get(oClang));
        }
        return new CompilationInfo(outputListMap, performanceMap);
    }

    public void justProcess(String command){
        ProcessTerminal pt = new ProcessTerminal();
        pt.processThreadNotLimitJustExec(command, "sh");
    }

    public String simplifyFilename(File file){
        return file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/random",
                                file.getAbsolutePath().lastIndexOf("/block")),
                        file.getAbsolutePath().lastIndexOf("/"))
                .replaceAll("[a-zA-Z/_]", "");
    }

    public String printLine(List<String> keyList){
        String temp = "";
        for(String key: keyList){
            temp += (" " + key + addBrace(key) + " ");
        }
        return temp;
    }

    public String addBrace(String key){
        String braces = "";
        for(int i=0; i<8-key.length(); i++){
            braces += " ";
        }
        return braces;
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

    public List<String> analysisOutputResult(List<String> execLines){
        List<String> resultList = new ArrayList<>();
        if(execLines.isEmpty()){
            return new ArrayList<>(List.of("timeout"));
        }
        for(String s: execLines){
            resultList.add(s.trim());
        }
        return resultList;
    }

    public List<String> analysisSanitizerResult(List<String> execLines){
        List<String> resultList = new ArrayList<>();
        if(execLines.isEmpty()){
            return new ArrayList<>(List.of("timeout"));
        }
        for(String s: execLines){
            if(s.contains("error:") || s.contains("ERROR:")){
                return new ArrayList<>(List.of("sanitize"));
            }else{
                resultList.add(s.trim());
            }
        }
        return resultList;
    }
}
