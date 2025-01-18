package TestResult;

import common.PropertiesInfo;
import processtimer.ProcessTerminal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class RandomIterator {
    public File outermostDir;

    String compilerType = "";
    String mutateType = "";

    public List<String> outputInconList = new ArrayList<>();
    public List<String> outputConfusedList = new ArrayList<>();
    public List<String> outputTimeoutList = new ArrayList<>();
    public List<String> outputErrorList = new ArrayList<>();


    public RandomIterator(File outermostDir, String compilerType, String mutateType){
        this.outermostDir = outermostDir;
        this.compilerType = compilerType;
        this.mutateType = mutateType;
        File compilerDir = new File(outermostDir.getAbsolutePath() + "/" + compilerType);
        if(!compilerDir.exists()){
            compilerDir.mkdirs();
        }
    }

    public void runSingleRandom(File singleRandom){
        if(singleRandom.isDirectory() && singleRandom.getName().matches("random\\d+") && singleRandom.listFiles().length > 1){
            String simpleMuPath = subMuFilePath(singleRandom.getAbsolutePath()).trim();

            TestCompiler testCompiler = new TestCompiler(singleRandom, compilerType);
            Map<String, Boolean> parseResult = testCompiler.run();
            String record = simpleMuPath + " " + compilerType;
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
                writeSingleBlock(singleRandom);
            } else {
                System.out.println(singleRandom.getAbsolutePath() + "  " + compilerType + " -------------------true");
            }
        }
    }

    public void dealRandomDir(File muDir, String resultType){
        File destFolder = new File(PropertiesInfo.inconResultIndexDir + "/" + mutateType + "/" + compilerType + "_configs/" + resultType);
        if(!destFolder.exists()){
            destFolder.mkdirs();
        }
        String command = "cp -r " + muDir.getAbsolutePath() + " " + destFolder.getAbsolutePath();
        ProcessTerminal.voidNotMemCheck(command, "sh");
    }

    public boolean isHave(Map<String, Boolean> resultMap, String key){
        if(resultMap == null) return false;
        return resultMap.containsKey(key) && resultMap.get(key);
    }

    public String subMuFilePath(String filename){
        return filename.replace(outermostDir.getAbsolutePath(), "");
    }

    public void writeSingleBlock(File muDir){
        System.out.println("..................................");
        System.out.println(muDir.getName());
        System.out.println(outputInconList.size());
        System.out.println(outputConfusedList.size());
        System.out.println(outputTimeoutList.size());
        System.out.println(outputErrorList.size());
        System.out.println("..................................");
    }
}
