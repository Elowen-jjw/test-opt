package TestResult;


import ObjectOperation.file.FileInfo;
import ObjectOperation.file.FileModify;
import common.PropertiesInfo;
import mutate.cse.CSE;
import mutate.inline.Inline;
import mutate.minmax.MinMax;
import mutate.minmax.WriteMutate;
import mutate.pattern.Statute;
import mutate.pointer_assignment.PointerTarget;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {
    String mutateType = "";
    String sanitizerCompiler = "";
    String testCompiler = "";
    public Test(String mutateType){
        this.mutateType = mutateType;
    }

    public void runStatute(File file){
        File genMuDir = new File(PropertiesInfo.mutateStatuteDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        File endFile = new File(genMuDir.getAbsolutePath() + "/" + mutateType + "_end.txt");
        if(endFile.exists()) {
            return;
        }
        boolean isCanMutate = confirmCompiler(file);
        if(!isCanMutate) return;

        if((!genMuDir.exists()) || (genMuDir.exists() && !isHaveRunResult(genMuDir))) {
            Statute st = new Statute(file, sanitizerCompiler);
            st.run();
        }

        if(isHaveMutate(genMuDir)) {
            genTestResult(genMuDir);
        }

    }

    public void runMinmax(File file){
        File genMuDir = new File(PropertiesInfo.mutateMinMaxDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        File endFile = new File(genMuDir.getAbsolutePath() + "/" + mutateType + "_end.txt");
        File noMutate = new File(genMuDir.getAbsolutePath() + "/" + "noMutate.txt");

        if(endFile.exists() || noMutate.exists()) {
            return;
        }

        boolean isCanMutate = confirmCompiler(file);
        if(!isCanMutate) return;

        if((!genMuDir.exists()) || (genMuDir.exists() && !isHaveRunResult(genMuDir))) {
            deleteAllOtherFiles(genMuDir);
            MinMax minmax = new MinMax(file, sanitizerCompiler);
            minmax.run();
        }

        if(isHaveMutate(genMuDir)) {
            deleteAllOtherFiles(genMuDir);
            genTestResult(genMuDir);
        }
    }

    public void runPT(File file){
        File genMuDir = new File(PropertiesInfo.mutatePointerTarget + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        File endFile = new File(genMuDir.getAbsolutePath() + "/" + mutateType + "_end.txt");

        if(endFile.exists()) {
            return;
        }

        boolean isCanMutate = confirmCompiler(file);
        if(!isCanMutate) return;

        if((!genMuDir.exists()) || (genMuDir.exists() && !isHaveRunResult(genMuDir))) {
            deleteAllOtherFiles(genMuDir);
            PointerTarget pt = new PointerTarget(file, sanitizerCompiler);
            pt.run();
        }

        if(isHaveMutate(genMuDir)) {
            deleteAllOtherFiles(genMuDir);
            genTestResult(genMuDir);
        }
    }

    public boolean confirmCompiler(File initialFile) {
        String sanitizeCompiler = "";
        String testCompiler = "";
        String initialGccResult = new WriteMutate("gcc").getFileExecResult(initialFile);
        String initialClangResult = new WriteMutate("clang").getFileExecResult(initialFile);
        boolean gccCorrect = initialGccResult.contains("checksum");
        boolean clangCorrect = initialClangResult.contains("checksum");
        if (!gccCorrect && !clangCorrect) {
            return false;
        } else {
            if (!gccCorrect && clangCorrect) {
                sanitizeCompiler = "clang";
                testCompiler = "clang";
            } else if (gccCorrect && !clangCorrect) {
                sanitizeCompiler = "gcc";
                testCompiler = "gcc";
            } else {
                int randomNumber = getRandomNumber(initialFile);
                sanitizeCompiler = randomNumber % 2 == 0 ? "clang" : "gcc";
                testCompiler = randomNumber % 2 == 0 ? "clang" : "gcc";
            }
            this.sanitizerCompiler = sanitizeCompiler;
            this.testCompiler = testCompiler;
            System.out.println(initialFile.getName() + " " + sanitizeCompiler);
            return true;
        }
    }

    public boolean isHaveMutate(File muDir){
        for(File file: muDir.listFiles()){
            if (!file.getName().endsWith(".c")) continue;
            if(file.getName().contains(mutateType) ) {
                return true;
            }
        }
        return false;
    }

    public boolean isHaveRunResult(File muDir){
        for(File file: muDir.listFiles()){
            if (!file.getName().endsWith(".txt")) continue;
            if (file.getName().contains("gcc") || file.getName().contains("clang")){
                return true;
            }
        }
        return false;
    }

    public void deleteAllOtherFiles(File muDir){
        if(muDir.isDirectory()){
            for(File file: muDir.listFiles()){
                if(!file.getName().endsWith(".txt") && !file.getName().endsWith(".c")) {
                    System.out.println(file.getAbsoluteFile() + " deletion op is " + file.delete());
                }
            }
        }
    }

    public void genTestResult(File randomDir){
        if(randomDir.exists() && randomDir.isDirectory() && randomDir.getName().contains("random")) {
            File outermostDir = new File(PropertiesInfo.mutateOutermostDir + "/" + mutateType);

            if(testCompiler.equals("gcc")) {
                RandomIterator itGcc = new RandomIterator(outermostDir, "gcc", mutateType);
                itGcc.runSingleRandom(randomDir);
            }
            else if(testCompiler.equals("clang")) {
                RandomIterator itClang = new RandomIterator(outermostDir, "clang", mutateType);
                itClang.runSingleRandom(randomDir);
            }

            File endFile = new File(randomDir.getAbsolutePath() + "/" + mutateType + "_end.txt");
            try {
                if(!endFile.exists())
                    endFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void runCse(File file){
        File genMuDir = new File(PropertiesInfo.mutateCseDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        if(!genMuDir.exists()) {
            CSE cse = new CSE(file);
            cse.run();
        }
        genTestResult(genMuDir);
    }

    public void runInline(File file){
        File genMuDir = new File(PropertiesInfo.mutateInlineDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        if(!genMuDir.exists()) {
            Inline inline = new Inline(file);
            inline.run();
        }
        File endFile = new File(genMuDir.getAbsolutePath() + "/" + "end.txt");
        if(!endFile.exists()) {
            genTestResult(genMuDir);
        }
    }

//    deleteLatestMuDir(new File(PropertiesInfo.mutateCseDir));
//    deleteLatestMuDir(new File(PropertiesInfo.mutateInlineDir));
    public void deleteLatestMuDir(File muFolder){
        if(!muFolder.exists() || muFolder.listFiles().length == 0) return;
        int maxIndex = -1;
        Pattern p = Pattern.compile("random([0-9]+)");
        Matcher m;
        for(File file: Objects.requireNonNull(muFolder.listFiles())) {
            if(!file.isDirectory() || !file.getName().matches("random[0-9]+")) continue;
            m = p.matcher(file.getName());
            if(m.find()) {
                maxIndex = Math.max(maxIndex, Integer.parseInt(m.group(1)));
            }
        }
        if(maxIndex != -1) {
            FileModify fm = new FileModify();
            fm.deleteFolder(new File(muFolder.getAbsolutePath() + "/random" + maxIndex));
        }
    }

    public int getRandomNumber(File file){
        return Integer.parseInt(file.getName().replace(".c", "").replace("random", ""));
    }

    public void deleteTxt(File folder){
        FileInfo fi = new FileInfo();
        List<File> allFileList = fi.getSortedOverallFiles(folder);
        for(File file: allFileList){
            if(file.getName().endsWith(".txt")) file.delete();
        }
    }

}
