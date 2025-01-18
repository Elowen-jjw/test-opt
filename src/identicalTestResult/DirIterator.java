package identicalTestResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

import ObjectOperation.file.FileModify;
import ObjectOperation.file.getAllFileList;

public class DirIterator {
    public File outermostDir;
    File llvmConFile;
    File llvmInConFile;
    File gccConFile;
    File gccInconFile;

    Set<String> checkedLlvmMuList = new TreeSet<>();
    Set<String> checkedGccMuList = new TreeSet<>();

    static int count = 0;

    public List<String> inconsistentLlvmList = new ArrayList<>();
    public List<String> consistentLlvmList = new ArrayList<>();
    public List<String> inconsistentGccList = new ArrayList<>();
    public List<String> consistentGccList = new ArrayList<>();

    public DirIterator(File outermostDir){
        this.outermostDir = outermostDir;

        llvmConFile = new File(outermostDir.getAbsolutePath() + "/llvm-consistent.txt");
        llvmInConFile = new File(outermostDir.getAbsolutePath() + "/llvm-inconsistent.txt");
        gccConFile = new File(outermostDir.getAbsolutePath() + "/gcc-consistent.txt");
        gccInconFile = new File(outermostDir.getAbsolutePath() + "/gcc-inconsistent.txt");

        createResultTxt(llvmConFile);
        createResultTxt(llvmInConFile);
        createResultTxt(gccConFile);
        createResultTxt(gccInconFile);

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

    public void checkLatestRandomEnd(){
        if(checkOutestDirCount() == 0){
            return;
        }
        String filePath = getLatestRandomDir();
        String randomName = subMuFilePath(filePath);// /random0

        checkExistedRandom(randomName);

        runSingleRandom(new File(filePath));
    }

    public void checkExistedRandom(String randomName){// /random0
        readResultFileToSet(randomName, llvmConFile, checkedLlvmMuList);
        readResultFileToSet(randomName, llvmInConFile, checkedLlvmMuList);
        readResultFileToSet(randomName, gccConFile, checkedGccMuList);
        readResultFileToSet(randomName, gccInconFile, checkedGccMuList);
    }

    public int checkOutestDirCount(){
        int count = 0;
        for(File file: outermostDir.listFiles()){
            if(file.isDirectory() && file.getName().matches("random[0-9]+(_while)?")){
                count++;
            }
        }
        return count;
    }

    public void readResultFileToSet(String randomName, File resultFile, Set<String> checkMuList){
        FileModify fm = new FileModify();
        List<String> initialFileList = fm.readFile(resultFile);
        for(String s: initialFileList){
            if(s.contains(randomName)) {
                checkMuList.add(s.trim());
            }
        }
    }

    public String getLatestRandomDir(){
        List<File> randomDirs = new ArrayList<>();
        for(File subFile: outermostDir.listFiles()){
            randomDirs.add(subFile);
        }
        getAllFileList fo = new getAllFileList();
        fo.compareFileList(randomDirs);

        String latestFilePath = "";
        long latestTime = 0;
        for(File singleRandom: randomDirs){
            if(singleRandom.isDirectory()) {
                long thisTime = getFileLatestModifyTime(singleRandom.getAbsolutePath());
                if(thisTime > latestTime){
                    latestTime = thisTime;
                    latestFilePath = singleRandom.getAbsolutePath();
                }
            }
        }
        System.out.println("latest random:  " + latestFilePath);
        return latestFilePath;
    }

    public long getFileLatestModifyTime(String filePath) {
        FileTime t = null;
        try {
            t = Files.readAttributes(Paths.get(filePath), BasicFileAttributes.class).lastModifiedTime();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t.toMillis();
    }


    public void runSingleRandom(File singleRandom){
        if(singleRandom.isDirectory()){
            iteratorRandomDir(singleRandom);
            if(!consistentLlvmList.isEmpty() || !inconsistentLlvmList.isEmpty()
                    || !consistentGccList.isEmpty() || !inconsistentGccList.isEmpty()){
                writeSingleBlock();
            }
        }
    }

    public void iteratorRandomDir(File singleRandom){
        List<File> blockDirs = new ArrayList<>();
        for(File subFile: singleRandom.listFiles()){
            blockDirs.add(subFile);
        }
        getAllFileList fo = new getAllFileList();
        fo.compareFileList(blockDirs);

        for(File singleBlock: blockDirs){
            if(singleBlock.isDirectory()) {
                iteratorBlockDir(singleBlock);
            }
        }
    }

    public void iteratorBlockDir(File singleBlock){
        List<File> muDirs = new ArrayList<>();
        for(File subFile: singleBlock.listFiles()){
            muDirs.add(subFile);
        }
        getAllFileList fo = new getAllFileList();
        fo.compareFileList(muDirs);

        for(File singleMu: muDirs){
        	if(singleMu.getName().equals("gcc.txt") || singleMu.getName().equals("llvm.txt")) {
        		singleMu.delete();
        		continue;
        	}
            if(singleMu.isDirectory()) {
                String simpleMuPath = subMuFilePath(singleMu.getAbsolutePath()).trim();

                if(checkedGccMuList.contains(simpleMuPath) && checkedLlvmMuList.contains(simpleMuPath)){
                    continue;
                }

                CompilerProcess cp = new CompilerProcess();
                Map<String, Boolean> resultMap = cp.genResult(singleMu);

                if(!checkedLlvmMuList.contains(simpleMuPath)) {
                    if (resultMap.get("llvm")) {
                        consistentLlvmList.add(simpleMuPath);
                    } else {
                        inconsistentLlvmList.add(simpleMuPath);
                    }
                }

                if(!checkedGccMuList.contains(simpleMuPath)) {
                    if (resultMap.get("gcc")) {
                        consistentGccList.add(simpleMuPath);
                    } else {
                        inconsistentGccList.add(simpleMuPath);
                    }
                }

                count++;
            }
            if(count >= 10){
                writeSingleBlock();
                count = 0;
            }
        }
    }

    public String subMuFilePath(String filename){
        return filename.replace(outermostDir.getAbsolutePath(), "");
    }

    public void writeSingleBlock(){
    	System.out.println("..................................");
        System.out.println(consistentLlvmList.size());
        System.out.println(inconsistentLlvmList.size());
    	System.out.println(consistentGccList.size());
        System.out.println(inconsistentGccList.size());
    	System.out.println("..................................");
        writePart(llvmConFile, consistentLlvmList);
        writePart(llvmInConFile, inconsistentLlvmList);
        writePart(gccConFile, consistentGccList);
        writePart(gccInconFile, inconsistentGccList);
    }

    public void writePart(File resultFile, List<String> writeList){
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
