package InconResultAnalysis.pattern1;

import InconResultAnalysis.CreduceProcessor;
import ObjectOperation.list.CommonOperation;
import processtimer.ProcessTerminal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Creduce {
    final static List<String> oList = new ArrayList<>(Arrays.asList("O0", "O1", "O2", "O3", "Os"));
    File outermostFolder; //gcc_line folder
    String testCompiler;
    String contrastCompiler;
    Map<String, Map<String, String>> checksumValueMap = new HashMap<>();

    public Creduce(File outermostFolder){
        this.outermostFolder = outermostFolder;
        if(outermostFolder.getName().contains("gcc")){
            this.testCompiler = "gcc";
            this .contrastCompiler = "clang";
        } else if(outermostFolder.getName().contains("clang")){
            this.testCompiler = "clang";
            this .contrastCompiler = "gcc";
        }
    }

    public void run(File randomFolder){
        File mutateFile = dealMutateInfo(randomFolder);
        if(mutateFile != null){
            generateCreduceScript(mutateFile.getName());
        }
        CreduceProcessor.runCreduce(randomFolder, mutateFile);
    }

    public File dealMutateInfo(File randomFolder){
        File mutateFile = null;
        for(File file: randomFolder.listFiles()){
            if(!file.getName().endsWith(".txt")) continue;
            if(file.getName().contains(testCompiler + "_output")){
                List<String> outputFileList = CommonOperation.genInitialList(file);
                String simplifiedFilename = "";
                for(int i = 0; i<outputFileList.size(); i++){
                    String s = outputFileList.get(i);
                    //get mutate incon checksum result
                    if(s.trim().startsWith("//mutate")){
                        simplifiedFilename = outputFileList.get(i + 1).trim().substring(2);
                        for(String line: outputFileList){
                            if(line.trim().startsWith("initial ")){
                                String[] parts = line.split(" ");
                                List<String> partList = Arrays.asList(parts);
                                Map<String, String> outputMap = new TreeMap<>();
                                for (int j = 0; j < oList.size(); j++) {
                                    outputMap.put(oList.get(j), partList.get(j + 1));
                                }
                                checksumValueMap.put(contrastCompiler, outputMap);
                            }
                            if(line.trim().startsWith(simplifiedFilename + " ")){
                                String[] parts = line.split(" ");
                                List<String> partList = Arrays.asList(parts);
                                Map<String, String> outputMap = new TreeMap<>();
                                for (int j = 0; j < oList.size(); j++) {
                                    outputMap.put(oList.get(j), partList.get(j + 1));
                                }
                                checksumValueMap.put(testCompiler, outputMap);
                            }
                        }
                        break;
                    }
                }
                //get mutate file
                if(!simplifiedFilename.isEmpty()){
                    for(File file2: randomFolder.listFiles()){
                        if(!file.getName().endsWith(".c")) continue;
                        if(simplifyFilename(file2).equals(simplifiedFilename))
                            mutateFile = file2;
                    }
                }
            }
        }
        if(mutateFile != null){
            File newFile = new File(mutateFile.getParent(), randomFolder.getName() + "_0.c");
            ProcessTerminal.voidNotMemCheck("cp " + mutateFile.getAbsolutePath()
                    +  " " + mutateFile.getAbsolutePath().substring(0, mutateFile.getAbsolutePath().lastIndexOf(".c"))
                    + "_copy.c", "sh");
            mutateFile.renameTo(newFile);
        }
        return mutateFile;
    }

    public void generateCreduceScript(String mutateFilename){
        // 定义脚本文件名
        String scriptFile = "creduce_2.sh";

        // 开始写脚本内容
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile))) {
            writer.write("#!/bin/bash\n");

            // 遍历每个编译器（gcc, clang）
            for (String compiler : checksumValueMap.keySet()) {
                Map<String, String> optimizationChecksumMap = checksumValueMap.get(compiler);

                // 遍历每个优化级别（O0 - Os）
                for (String optimizationLevel : optimizationChecksumMap.keySet()) {
                    String checksum = optimizationChecksumMap.get(optimizationLevel);
                    String outputFileName = String.format("./%s-%s", compiler,
                            mutateFilename.replaceAll("[a-zA-Z_]", "") + optimizationLevel.replaceAll("[a-zA-Z_]", "")); // 生成相应的输出文件名
                    String outputTxtFile = String.format("output_%s%s.txt", compiler, optimizationLevel);

                    // 写入编译和执行命令
                    writer.write(String.format("%s %s -%s -w -lm -o %s && %s &> %s\n",
                            compiler, mutateFilename, optimizationLevel, outputFileName, outputFileName, outputTxtFile));
                }
            }

            // 写入错误检查命令
            for (String compiler : checksumValueMap.keySet()) {
                for (String optimizationLevel : checksumValueMap.get(compiler).keySet()) {
                    String outputTxtFile = String.format("output_%s%s.txt", compiler, optimizationLevel);

                    // 检查是否有错误输出
                    writer.write(String.format("grep -qi \"error\" %s && exit 1\n", outputTxtFile));
                }
            }

            // 写入checksum检查命令
            for (String compiler : checksumValueMap.keySet()) {
                for (String optimizationLevel : checksumValueMap.get(compiler).keySet()) {
                    String checksum = checksumValueMap.get(compiler).get(optimizationLevel);
                    String outputTxtFile = String.format("output_%s%s.txt", compiler, optimizationLevel);

                    // 检查checksum值
                    writer.write(String.format("grep -q '^checksum = %s$' %s || exit 1\n", checksum, outputTxtFile));
                }
            }

            // 正常结束
            writer.write("exit 0\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("creduce.sh 脚本已生成！");
    }

    public String simplifyFilename(File file){ // 0_0
        return file.getName().replaceAll("(random)|_|(\\.c)", "")
                .replace("statute", "s")
                .replace("minmax", "m")
                .replace("pt", "p");
    }
}
