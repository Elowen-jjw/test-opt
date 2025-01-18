package TestResult;

import ObjectOperation.file.FileInfo;
import ObjectOperation.list.CommonOperation;
import processtimer.ProcessTerminal;
import utity.CompilationInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class TestCompiler {
    final static List<String> oList = new ArrayList<>(Arrays.asList("-O0", "-O1", "-O2", "-O3", "-Os"));

    File muDir;
    String compilerType;

    File initialOutputFile;
    CompilationInfo initialInfo;
    List<String> inconInfoString = new ArrayList<>();

    public TestCompiler(File muDir, String compilerType){
        this.muDir = muDir;
        this.compilerType = compilerType;
        initialOutputFile = new File(muDir.getAbsolutePath() + "/" + compilerType + "_initial.txt");
        if(initialOutputFile.exists()){
            initialOutputFile.delete();
        }
        genInitialOutputFile();
    }

    public void genInitialOutputFile(){
        File initialFile = new File(muDir.getAbsolutePath() + "/" + muDir.getName() + ".c");
        List<CompilationInfo> initialInfoList = new ArrayList<>();
        CompilationInfo info = runCompiler(initialFile, new ArrayList<>(Arrays.asList("", "", "", "", "")));
        info.setSimpliedFilename("default");
        initialInfoList.add(info);
        genSpecificFile(initialInfoList, "initial");
    }

    public Map<String, Boolean> run() {
        List<CompilationInfo> infoList = new ArrayList<>();
        if(!initialOutputFile.exists()){
            System.out.println("initial output file does not exist....");
        }
        initialInfo = getInitialInfo();
        infoList.add(initialInfo);
        infoList.addAll(genOutputResult());

        Map<String, Boolean> resultMap = analyzeCompilationInfo(infoList);
        genSpecificFile(infoList, "output");

        return resultMap;
    }

    public CompilationInfo getInitialInfo(){
        System.out.println("initial compiler output file: " + initialOutputFile.getAbsolutePath());
        List<String> outputFileLists = CommonOperation.genInitialList(initialOutputFile);
        for(int i = 1; i < outputFileLists.size(); i++){
            if(outputFileLists.get(i).startsWith("//")) continue;
            String[] parts = outputFileLists.get(i).split("@");
            if(parts[0].trim().equals("default")){
                System.out.println("yes find inintial result----------");
                List<String> partList = Arrays.asList(parts);

                CompilationInfo info = new CompilationInfo();
                info.setSimpliedFilename("initial");

                Map<String, List<String>> outputMap = new TreeMap<>();
                for(int j = 0; j < oList.size(); j++){
                    int finalJ = j;
                    outputMap.put(oList.get(j), new ArrayList<>(){{
                        add(partList.get(finalJ + 1).trim());
                    }});
                }
                info.setOutputListMap(outputMap);
                return info;
            }
        }
        return null;
    }

    public List<CompilationInfo> genOutputResult(){
        FileInfo fi = new FileInfo();
        List<File> allFileList = fi.getSortedOverallFiles(muDir);

        List<CompilationInfo> infoList = new ArrayList<>();

        for(File file: allFileList){
            if(!file.getName().endsWith(".c") || file.getName().matches("random\\d+\\.c") || file.getName().contains("printf")){
                continue;
            }
            System.out.println(file.getAbsolutePath());
            //每一个mutate都随机产生5个config 分别对应5个优先级
            List<String> randomConfigs = compilerType.equals("gcc") ? ConfigureOp.getGccConfigs() : ConfigureOp.getClangConfigs();
            CompilationInfo info = runCompiler(file, randomConfigs);
            Objects.requireNonNull(info).setSimpliedFilename(file.getName().replaceAll("(random\\d+)|_|(\\.c)", ""));
            infoList.add(info);
        }
        return infoList;
    }


    public void genSpecificFile(List<CompilationInfo> infoList, String resultType){
        File resultFile = null;
        if(resultType.equals("output"))
            resultFile = new File(muDir.getAbsolutePath() + "/" + compilerType + "_" + resultType + ".txt" );
        else if(resultType.equals("initial"))
            resultFile = new File(muDir.getAbsolutePath() + "/" + compilerType + "_" + resultType + ".txt" );
        try{
            FileWriter fw = new FileWriter(resultFile);
            PrintWriter pw = new PrintWriter(fw);
            if(resultType.equals("output")) {
                pw.println(addBrace("") + printLine(infoList.get(0).getOutputListMap().keySet().stream().sorted().toList()));//write header
                for (CompilationInfo info : infoList) {
                    String filename = info.getSimpliedFilename();
                    Map<String, List<String>> resultMap = info.getOutputListMap();
                    List<String> resultList = new ArrayList<>();
                    for (String os : resultMap.keySet()) {
                        if (resultMap.get(os).get(0).trim().matches("checksum\\s*=\\s*[0-9A-Za-z]+")) {
                            resultList.add(resultMap.get(os).get(0).trim().replaceAll("checksum\\s*=\\s*", "").trim());
                        } else if (resultMap.get(os).size() == 1 && resultMap.get(os).get(0).length() <= 8) {
                            resultList.add(resultMap.get(os).get(0));
                        } else {
                            resultList.add(resultMap.get(os).get(0).trim().substring(0, 8));
                        }
                    }
                    pw.println(filename + addBrace(filename) + printLine(resultList));
                }
                for(String ss: inconInfoString){
                    if(ss.isEmpty()){
                        pw.println("");
                    } else {
                        pw.println("//" + ss);
                    }
                }
            }

            if(resultType.equals("initial")) {
                pw.println(addBrace("") + printLine(infoList.get(0).getOutputListMap().keySet().stream().sorted().toList()));//write header
                for (CompilationInfo info : infoList) {
                    String filename = info.getSimpliedFilename() + "@";
                    Map<String, List<String>> resultMap = info.getOutputListMap();
                    List<String> resultList = new ArrayList<>();
                    for (String os : resultMap.keySet()) {
                        if(resultMap.get(os).size() == 1) resultList.add(resultMap.get(os).get(0) + "@");
                        else {
                            resultList.add(getHashedString(resultMap.get(os)) + "@");
                        }
                    }
                    pw.println(filename + addBrace(filename) + printLine(resultList));
                }
            }
            pw.println("//file: " + muDir.getName());

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //clang-command: simulate risc-v environment:
    //clang -O3 test.c --target=riscv64-linux-gnu
    //qemu-riscv64 -L /usr/riscv64-linux-gnu/ ./a.out

    public CompilationInfo runCompiler(File file, List<String> configList){
        Map<String,List<String>> outputListMap = new TreeMap<>();
        Map<String,String> configMap = new TreeMap<>();
        Map<String,String> commandMap = new TreeMap<>();
        String aoutFilename = simplifyFilename(file);
        String fullCommand = "";
        int count = 0;
        for(String os: oList){
            String specificConfig = configList.get(count++);
            String compileCommand = (compilerType.equals("gcc") ? "export LANGUAGE=en && export LANG=en_US.UTF-8 && " : "") + "cd " + file.getParent() + " && "
                    + compilerType + " " + file.getName() + " " + os + " " + specificConfig
                    + " -w -lm -I $CSMITH_HOME/include -o " + aoutFilename;
            ProcessTerminal.voidNotMemCheck(compileCommand, "sh");
//            System.out.println(compileCommand);

            String runExe = compilerType.equals("clang") && specificConfig.startsWith("--target=riscv64-linux-gnu") ?
                    "qemu-riscv64 -L /usr/riscv64-linux-gnu/ " : "";
            String execCommand = (compilerType.equals("gcc") ? "export LANGUAGE=en && export LANG=en_US.UTF-8 && " : "")
                    + "cd " + file.getParent() + " && " + runExe + " ./" + aoutFilename;
            fullCommand = compileCommand + " && " + execCommand;
            System.out.println(fullCommand);
            configMap.put(os, specificConfig);
            configMap.put(os, fullCommand);

            File aoutFile = new File(file.getParent() + "/" + aoutFilename);
            if(!aoutFile.exists()) {
                System.out.println(aoutFile.getAbsolutePath() + " does not exist....");
                outputListMap.put(os, new ArrayList<>(List.of("error")));//没有生成a.out的标记成error
            }
            else{
                List<String> execLines = ProcessTerminal.listMemCheck(execCommand, 8, "sh", true,
                        true, new ArrayList<>(Arrays.asList(aoutFilename)));
                deleteAoutFile(file, aoutFilename);
                outputListMap.put(os, analysisOutputResult(execLines));
            }
            System.out.println(file.getName() + " " + os + "  " + outputListMap.get(os));
        }
        return new CompilationInfo(configMap, commandMap, outputListMap);
    }

    public  Map<String, Boolean> analyzeCompilationInfo(List<CompilationInfo> compilationInfoList) {
        boolean hasTimeout = false;
        boolean allTimeout = true;
        boolean hasError = false;
        boolean allError = true;

        boolean isInconsistent = false;
        boolean isConfused = false;

        if (initialInfo.getOutputListMap().values().stream().distinct().count() > 1) {
            isInconsistent = true;
            inconInfoString.add("initial line inconsistent");
            inconInfoString.add("command: " + initialInfo.getFullCommand().get("-O0"));
            inconInfoString.add("");
        }

        for (CompilationInfo info : compilationInfoList) {
            if(info == null) continue;
            if(info.getSimpliedFilename().equals("initial")) continue;

            Map<String, List<String>> omap = oList.stream().collect(Collectors.toMap(opt -> opt, opt -> new ArrayList<>()));
            Map<String, List<String>> outputMap = info.getOutputListMap();
            Map<String, List<String>> initialOutputMap = initialInfo.getOutputListMap();

            outputMap.forEach((key, value) -> {
                if (omap.containsKey(key)) {
                    omap.get(key).addAll(value);
                }
            });

            initialOutputMap.forEach((key, value) -> {
                if (omap.containsKey(key)) {
                    omap.get(key).addAll(value);
                }
            });

            boolean isSame = false;
            boolean isNotSame = false;

            for(String s: omap.keySet()){
                System.out.println(omap.get(s));
                if(omap.get(s).stream().distinct().count() > 1) {
                    isNotSame = true;
                    inconInfoString.add(info.getSimpliedFilename() + " mutate column inconsistent");
                    inconInfoString.add("config: " + omap.get(s) + " " + info.getSpecificConfig().get(omap.get(s)));
                    inconInfoString.add("command: " + info.getFullCommand().get(omap.get(s)));
                    inconInfoString.add("");
                } else {
                    isSame = true;
                }
            }

            if(isNotSame && isSame) isInconsistent = true;
            else if(isNotSame && !isSame) isConfused = true;

            if (info.getOutputListMap().values().stream().distinct().count() > 1) {
                isInconsistent = true;
                for(String os: info.getSpecificConfig().keySet()) {
                    inconInfoString.add(info.getSimpliedFilename() + " mutate line inconsistent");
                    inconInfoString.add("config: " + os + " " + info.getSpecificConfig().get(os));
                    inconInfoString.add("command: " + info.getFullCommand().get(os));
                    inconInfoString.add("");
                }
            }

            for (List<String> outputs : info.getOutputListMap().values()) {
                //check timeout and error
                boolean currentHasTimeout = outputs.contains("timeout");
                boolean currentHasError = outputs.contains("error");

                hasTimeout = hasTimeout || currentHasTimeout;
                allTimeout = allTimeout && currentHasTimeout;

                hasError = hasError || currentHasError;
                allError = allError && currentHasError;
            }
        }

        Map<String, Boolean> resMap = new HashMap<>();
        resMap.put("output_timeout", hasTimeout && !allTimeout);
        resMap.put("output_error", hasError && !allError);
        resMap.put("output_inconsistent", isInconsistent);
        resMap.put("output_confused", isConfused);

        return resMap;
    }

//    public  Map<String, Boolean> analyzeCompilationInfo_Backups(List<CompilationInfo> compilationInfoList) {
//        boolean hasTimeout = false;
//        boolean allTimeout = true;
//        boolean hasError = false;
//        boolean allError = true;
//
//        boolean isInconsistent = false;
//        boolean isConfused = false;
//
//        for (CompilationInfo info : compilationInfoList) {
//            if(info == null) continue;
//            if(info.getSimpliedFilename().equals("initial")) continue;
//
//            Map<String, List<String>> omap = oList.stream().collect(Collectors.toMap(opt -> opt, opt -> new ArrayList<>()));
//            Map<String, List<String>> outputMap = info.getOutputListMap();
//            Map<String, List<String>> initialOutputMap = initialInfo.getOutputListMap();
//
//            outputMap.forEach((key, value) -> {
//                if (omap.containsKey(key)) {
//                    omap.get(key).addAll(value);
//                }
//            });
//
//            initialOutputMap.forEach((key, value) -> {
//                if (omap.containsKey(key)) {
//                    omap.get(key).addAll(value);
//                }
//            });
//
//            boolean isNotSame = false;
//            boolean isSame = false;
//            for(String s: omap.keySet()){
//                System.out.println(omap.get(s));
//                long count = omap.get(s).stream().distinct().count();
//                if(count == 1) isSame = true;
//                else isNotSame = true;
//            }
//            if(isSame && isNotSame) {
//                isInconsistent = true;
//                inconMuName.add(info.getSimpliedFilename());
//            }
//            if(isNotSame && !isSame) isConfused = true;
//
//            for (List<String> outputs : info.getOutputListMap().values()) {
//                //check timeout and error
//                boolean currentHasTimeout = outputs.contains("timeout");
//                boolean currentHasError = outputs.contains("error");
//
//                hasTimeout = hasTimeout || currentHasTimeout;
//                allTimeout = allTimeout && currentHasTimeout;
//
//                hasError = hasError || currentHasError;
//                allError = allError && currentHasError;
//
//                //check incon in the same line(same file)
//                if(outputs.stream().distinct().count() != 1) isInconsistent = true;
//
//            }
//        }
//
//        Map<String, Boolean> resMap = new HashMap<>();
//        resMap.put("output_timeout", hasTimeout && !allTimeout);
//        resMap.put("output_error", hasError && !allError);
//        resMap.put("output_inconsistent", isInconsistent);
//        resMap.put("output_confused", isConfused);
//
//        return resMap;
//    }

    public String simplifyFilename(File file){ // 0_0
        return file.getName().replaceAll("(random)|_|(\\.c)", "")
                .replace("statute", "s")
                .replace("minmax", "m")
                .replace("pt", "p");
    }

    public static String getHashedString(List<String> list){
        int hashCode = list.hashCode();
        // 将 hashCode 转换为十六进制字符串
        String hex = Integer.toHexString(hashCode);
        // 确保长度为 8 位，如果不足则左填充'0'
        while (hex.length() < 8) {
            hex = "0" + hex;
        }
        // 如果超出 8 位，只取最后 8 位
        if (hex.length() > 8) {
            hex = hex.substring(hex.length() - 8);
        }
        return hex;
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
//            System.out.println(aoutFilename + " has been deleted...");
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
            return new ArrayList<>(List.of("empty"));
        }
        for(String s: execLines){
            resultList.add(s.trim());
        }
        return resultList;
    }

}
