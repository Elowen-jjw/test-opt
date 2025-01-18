package overall;

import objectoperation.file.FileModify;
import csmithgen.SwarmGen;
import mutationgen.*;
import testcompareresults.RandomIterator;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OverallProcess {
    String swarmDir;
    String muIndexPath;
    String muType;
    String outermostPath;
    String specificSwarmPath;
    public static String commandType = "clang";

    public OverallProcess(String swarmDir, String muIndexPath, String muType){
        this.swarmDir = swarmDir;
        this.muIndexPath = muIndexPath;
        this.muType = muType;
    }

    public void process(){
        getSpecificInfo();
        checkDir();
        int count = 0;
        do{
            int randomIndex = getTotalRandomFileCount();
            if(randomIndex % 2 == 0) {
                run("gcc", randomIndex);
            }else {
                run("clang", randomIndex);
            }
        }while(count++ < 9999);
    }

    public void run(String commandType, int randomIndex){
        OverallProcess.commandType = commandType;
        SwarmGen sg = new SwarmGen(specificSwarmPath);
        File randomFile = sg.genRandomTestcase(randomIndex);
        File randomDir = genMutations(randomFile);
        if(randomDir.exists()) {
            genTestResult(randomDir);
        }else{
            randomFile.delete();
            run(commandType, randomIndex);
        }
    }

    public void checkDir(){
        int maxIndex = -1;
        File specificSwarmDir = new File(specificSwarmPath);
        if(!specificSwarmDir.exists()){
            specificSwarmDir.mkdirs();
        }else{
            maxIndex = findLatestNum(specificSwarmDir);
            for (File file : Objects.requireNonNull(specificSwarmDir.listFiles())) {
                if (file.getName().endsWith(".c") && file.getName().contains("random" + maxIndex)) {
                    file.delete();
                }else if(!file.getName().endsWith(".c")){
                    file.delete();
                }
            }
        }

        File outermostDir = new File(outermostPath);
        if(!outermostDir.exists()){
            outermostDir.mkdirs();
        }else{
            for(File dir: Objects.requireNonNull(outermostDir.listFiles())){
                if(dir.isDirectory() && dir.getName().equals("random"+maxIndex)){
                    FileModify fm = new FileModify();
                    fm.deleteFolder(dir);
                }
            }
        }
    }

    public int findLatestNum(File swarmFolder) {
        int num = 0;
        String regex = "random([0-9]+)\\.c";
        Pattern p = Pattern.compile(regex);
        Matcher m;
        for(File file: Objects.requireNonNull(swarmFolder.listFiles())) {
            if(!file.getName().matches("random[0-9]+\\.c")) continue;

            m = p.matcher(file.getName());
            if(m.find()) {
                num = Math.max(num, Integer.parseInt(m.group(1)));
            }
        }
        return num;
    }


    public void getSpecificInfo(){
        String addPath = "";
        switch (muType) {
            case "fusion_sameheader" -> {
                addPath = "/Fusion/SameHeader";
            }
            case "fusion_add" -> {
                addPath = "/Fusion/Add";
            }
            case "fusion_max" -> {
                addPath = "/Fusion/Max";
            }
            case "sr" -> {
                addPath = "/StrengthReduction";
            }
            case "invariant" -> {
                addPath = "/Invariant";
            }
            case "unswitching_compound" -> {
                addPath =  "/Unswitching/Compound";
            }
            case "inversion" -> {
                addPath = "/Inversion";
            }
            case "unrolling" -> {
                addPath = "/Unrolling";
            }
        }
        outermostPath = muIndexPath + addPath;
        specificSwarmPath = swarmDir + addPath;
    }

    public int getTotalRandomFileCount(){
        File outestDir = new File(specificSwarmPath);
        int count = 0;
        for(File file: Objects.requireNonNull(outestDir.listFiles())){
            if(file.getName().matches("random[0-9]+\\.c")){
                count++;
            }
        }
        return count;
    }

    public File genMutations(File randomFile){
        switch (muType) {
            case "fusion_sameheader" -> {
                GenFusion gf = new GenFusion(muIndexPath, "SameHeader");
                gf.SingleFileMutation(randomFile);
            }
            case "fusion_add" -> {
                GenFusion gf = new GenFusion(muIndexPath, "Add");
                gf.SingleFileMutation(randomFile);
            }
            case "fusion_max" -> {
                GenFusion gf = new GenFusion(muIndexPath, "Max");
                gf.SingleFileMutation(randomFile);
            }
            case "sr" -> {
                GenStrengthReduction gsr = new GenStrengthReduction(muIndexPath);
                gsr.SingleFileMutation(randomFile);
            }
            case "invariant" -> {
                GenInvariant giv = new GenInvariant(muIndexPath);
                giv.SingleFileMutation(randomFile);
            }
            case "unswitching_compound" -> {
                GenUnswitching gus = new GenUnswitching(muIndexPath, "Compound");
                gus.SingleFileMutation(randomFile);
            }
            case "inversion" -> {
                GenInversion giv = new GenInversion(muIndexPath);
                giv.SingleFileMutation(randomFile);
            }
            case "unrolling" -> {
                GenUnrolling gur = new GenUnrolling(muIndexPath);
                gur.SingleFileMutation(randomFile);
            }
        }
        return new File(outermostPath + "/" + randomFile.getName().substring(0, randomFile.getName().lastIndexOf(".c")));
    }

    public void genTestResult(File randomDir){
        File outermostDir = new File(outermostPath);
        //gcc
        if(commandType.equals("gcc")) {
            RandomIterator itGcc = new RandomIterator(outermostDir, "gcc");
            itGcc.runSingleRandom(randomDir);
        }

        //llvm
        if(commandType.equals("clang")) {
            RandomIterator itLlvm = new RandomIterator(outermostDir, "llvm");
            itLlvm.runSingleRandom(randomDir);
        }
    }
}
