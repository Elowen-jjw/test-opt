package testcompareresults;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import objectoperation.file.getAllFileList;

public class RandomIterator {
    public File outermostDir;

    File sanitizerInconFile;
    File sanitizerErrorFile;
    File sanitizerTimeoutFile;

    File outputInconFile;
    File outputTimeoutFile;
    File outputErrorFile;

    String compilerType = "";

    static int count = 0;

    public List<String> sanitizerInconList = new ArrayList<>();
    public List<String> sanitizerErrorList = new ArrayList<>();
    public List<String> sanitizerTimeoutList = new ArrayList<>();

    public List<String> outputInconList = new ArrayList<>();
    public List<String> outputTimeoutList = new ArrayList<>();
    public List<String> outputErrorList = new ArrayList<>();

    public RandomIterator(File outermostDir, String compilerType){
        this.outermostDir = outermostDir;
        this.compilerType = compilerType;
        File compilerDir = new File(outermostDir.getAbsolutePath() + "/" + compilerType);
        if(!compilerDir.exists()){
            compilerDir.mkdirs();
        }

        sanitizerInconFile = new File(compilerDir.getAbsolutePath() + "/" + compilerType + "_sanitizer_incon.txt");
        sanitizerErrorFile = new File(compilerDir.getAbsolutePath() + "/" + compilerType + "_sanitizer_error.txt");
        sanitizerTimeoutFile = new File(compilerDir.getAbsolutePath() + "/" + compilerType + "_sanitizer_timeout.txt");

        outputInconFile = new File(compilerDir.getAbsolutePath() + "/" + compilerType + "_output_incon.txt");
        outputTimeoutFile = new File(compilerDir.getAbsolutePath() + "/" + compilerType + "_output_timeout.txt");
        outputErrorFile = new File(compilerDir.getAbsolutePath() + "/" + compilerType + "_output_error.txt");

        createResultTxt(sanitizerInconFile);
        createResultTxt(sanitizerErrorFile);
        createResultTxt(sanitizerTimeoutFile);

        createResultTxt(outputInconFile);
        createResultTxt(outputTimeoutFile);
        createResultTxt(outputErrorFile);
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
        if(singleRandom.isDirectory()){
            iteratorRandomDir(singleRandom);
            if(!sanitizerInconList.isEmpty() || !sanitizerErrorList.isEmpty()
                    || !sanitizerTimeoutList.isEmpty()
                    || !outputInconList.isEmpty() || !outputTimeoutList.isEmpty()
                    || !outputErrorList.isEmpty()){
                writeSingleBlock();
            }
        }
    }

    public void iteratorRandomDir(File singleRandom){
        List<File> blockDirs = new ArrayList<>(Arrays.asList(Objects.requireNonNull(singleRandom.listFiles())));
        getAllFileList fo = new getAllFileList();
        fo.compareFileList(blockDirs);

        for(File singleBlock: blockDirs){
            if(singleBlock.isDirectory()) {
                iteratorBlockDir(singleBlock);
            }
        }
    }

    public void iteratorBlockDir(File singleBlock){
        List<File> muDirs = new ArrayList<>(Arrays.asList(Objects.requireNonNull(singleBlock.listFiles())));
        getAllFileList fo = new getAllFileList();
        fo.compareFileList(muDirs);

        for(File singleMu: muDirs){
            if(singleMu.isDirectory()) {
                String simpleMuPath = subMuFilePath(singleMu.getAbsolutePath()).trim();

                TestCompiler testCompiler = new TestCompiler(singleMu, compilerType);
                Map<String, Boolean> parseResult = testCompiler.run();

                if(isHave(parseResult, "sanitizer_incon")){
                    sanitizerInconList.add(simpleMuPath);
                }
                if(isHave(parseResult, "sanitizer_error")){
                    sanitizerErrorList.add(simpleMuPath);
                }
                if(isHave(parseResult, "sanitizer_timeout")){
                    sanitizerTimeoutList.add(simpleMuPath);
                }
                if(isHave(parseResult, "output_inconsistent")){
                    outputInconList.add(simpleMuPath);
                }
                if(isHave(parseResult, "output_timeout")){
                    outputTimeoutList.add(simpleMuPath);
                }
                if(isHave(parseResult, "output_error")){
                    outputErrorList.add(simpleMuPath);
                }

                count++;
            }
            if(count >= 10){
                writeSingleBlock();
                count = 0;
            }
        }
    }

    public boolean isHave(Map<String, Boolean> resultMap, String key){
        return resultMap.containsKey(key) && resultMap.get(key);
    }

    public String subMuFilePath(String filename){
        return filename.replace(outermostDir.getAbsolutePath(), "");
    }

    public void writeSingleBlock(){
        System.out.println("..................................");
        System.out.println(sanitizerInconList.size());
        System.out.println(sanitizerErrorList.size());
        System.out.println(sanitizerTimeoutList.size());

        System.out.println(outputInconList.size());
        System.out.println(outputTimeoutList.size());
        System.out.println(outputErrorList.size());
        System.out.println("..................................");
        writePart(sanitizerInconFile, sanitizerInconList);
        writePart(sanitizerErrorFile, sanitizerErrorList);
        writePart(sanitizerTimeoutFile, sanitizerTimeoutList);

        writePart(outputInconFile, outputInconList);
        writePart(outputTimeoutFile, outputTimeoutList);
        writePart(outputErrorFile, outputErrorList);
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
