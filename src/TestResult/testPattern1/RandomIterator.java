package TestResult.testPattern1;

import TestResult.ConfigureOp;
import common.PropertiesInfo;
import processtimer.ProcessTerminal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class RandomIterator {
    public File outermostDir;

    File outputInconFile;
    File outputTimeoutFile;
    File outputErrorFile;
    File outputConfusedFile;

    String compilerType = "";
    String mutateType = "";

    public List<String> outputInconList = new ArrayList<>();
    public List<String> outputConfusedList = new ArrayList<>();
    public List<String> outputTimeoutList = new ArrayList<>();
    public List<String> outputErrorList = new ArrayList<>();

    public int testCnt = 0;

    List<String> gccConfigs = new ArrayList<>();
    List<String> clangConfigs = new ArrayList<>();

    public RandomIterator(File outermostDir, String compilerType, String mutateType){
        this.outermostDir = outermostDir;
        this.compilerType = compilerType;
        this.mutateType = mutateType;
        testCnt = 0;
        File compilerDir = new File(outermostDir.getAbsolutePath() + "/" + compilerType);
        if(!compilerDir.exists()){
            compilerDir.mkdirs();
        }

        outputInconFile = new File(compilerDir.getAbsolutePath() + "/" + compilerType + "_output_incon.txt");
        outputConfusedFile = new File(compilerDir.getAbsolutePath() + "/" + compilerType + "_output_confused.txt");
        outputTimeoutFile = new File(compilerDir.getAbsolutePath() + "/" + compilerType + "_output_timeout.txt");
        outputErrorFile = new File(compilerDir.getAbsolutePath() + "/" + compilerType + "_output_error.txt");

        createResultTxt(outputInconFile);
        createResultTxt(outputConfusedFile);
        createResultTxt(outputTimeoutFile);
        createResultTxt(outputErrorFile);

        gccConfigs = ConfigureOp.getGccConfigs();
        clangConfigs = ConfigureOp.getClangConfigs();
    }

    public void createResultTxt(File resultFile){
        if(!resultFile.exists()){
            try {
                resultFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void runSingleRandom(File singleRandom){
        if(singleRandom.isDirectory() && singleRandom.getName().matches("random\\d+") && singleRandom.listFiles().length > 1){
            String simpleMuPath = subMuFilePath(singleRandom.getAbsolutePath()).trim();
            List<String> configs = compilerType.equals("gcc") ? gccConfigs : clangConfigs;

            File initialOutputFile = new File(singleRandom.getAbsolutePath() + "/" + compilerType + "_initial.txt");
            if(initialOutputFile.exists()){
                initialOutputFile.delete();
            }
            TestCompiler tc = new TestCompiler(singleRandom, compilerType, "");
            tc.genInitialOutputFile(configs);

            for(String config: configs) {
                TestCompiler testCompiler = new TestCompiler(singleRandom, compilerType, config);
                Map<String, Boolean> parseResult = testCompiler.run();
                String record = simpleMuPath + " " + compilerType + " " + config;
                if (isHave(parseResult, "output_inconsistent")) {
                    outputInconList.add(record);
                    dealRandomDir(singleRandom, "output_inconsistent");
                }
                if (isHave(parseResult, "output_confused")) {
                    outputConfusedList.add(record);
                    dealRandomDir(singleRandom, "output_confused");
                }
                if (isHave(parseResult, "output_timeout")) {
                    outputTimeoutList.add(record);
                    dealRandomDir(singleRandom, "output_timeout");
                }
                if (isHave(parseResult, "output_error")) {
                    outputErrorList.add(record);
                    dealRandomDir(singleRandom, "output_error");
                }
                if (!outputInconList.isEmpty() || !outputTimeoutList.isEmpty()
                        || !outputErrorList.isEmpty() || !outputConfusedList.isEmpty()) {
                    writeSingleBlock(singleRandom, config);
                } else {
                    System.out.println(singleRandom.getAbsolutePath() + "  " + compilerType + " " +  config + " -------------------true");
                }
                testCnt++;
            }
        }
    }

    public void dealRandomDir(File muDir, String resultType){
        File destFolder = new File(PropertiesInfo.inconResultIndexDir + "/" + mutateType + "/" + compilerType + "_configs/" + resultType);
        if(!destFolder.exists()){
            destFolder.mkdirs();
        }
        String command = "cp -r " + muDir.getAbsolutePath() + " " + destFolder.getAbsolutePath() + "/" + muDir.getName() + "_" + testCnt;
        ProcessTerminal.voidNotMemCheck(command, "sh");
    }

    public boolean isHave(Map<String, Boolean> resultMap, String key){
        if(resultMap == null) return false;
        return resultMap.containsKey(key) && resultMap.get(key);
    }

    public String subMuFilePath(String filename){
        return filename.replace(outermostDir.getAbsolutePath(), "");
    }

    public void writeSingleBlock(File muDir, String config){
        System.out.println("..................................");
        System.out.println(muDir.getName() + " " + config);
        System.out.println(outputInconList.size());
        System.out.println(outputConfusedList.size());
        System.out.println(outputTimeoutList.size());
        System.out.println(outputErrorList.size());
        System.out.println("..................................");

        writePart(outputInconFile, outputInconList);
        writePart(outputConfusedFile, outputConfusedList);
        writePart(outputTimeoutFile, outputTimeoutList);
        writePart(outputErrorFile, outputErrorList);
    }

    public void writePart(File resultFile, List<String> writeList) {
        synchronized (resultFile) {
            try {
                FileWriter fw = new FileWriter(resultFile, true);
                PrintWriter pw = new PrintWriter(fw);

                writeList.forEach(pw::println);

                pw.flush();
                fw.flush();
                pw.close();
                fw.close();

                writeList.clear();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
