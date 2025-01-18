package mutate.minmax;

import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import common.CommonInfoFromFile;
import common.PropertiesInfo;
import processtimer.ProcessTerminal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;


public class WriteMutate {
    int mutCount = 0;
    int correctCount = 0;
    String sanitizerCompiler;

    public WriteMutate(String sanitizerCompiler){
        this.sanitizerCompiler = sanitizerCompiler;
    }
    public void writeMuFile(File file, Map<Integer, List<String>> mutateMap, List<String> initialFileList) {
        File muDir = new File(PropertiesInfo.mutateMinMaxDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        //没有可以添加min max，直接跳出
        if(mutateMap.isEmpty()){
            File noMutateFile = new File(muDir.getAbsolutePath() + "/noMutate.txt");
            if(noMutateFile.exists()){
                noMutateFile.delete();
            }
            try {
                noMutateFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        File initFile = new File(muDir.getAbsolutePath() + "/" + file.getName());

        String initialResult = getFileExecResult(initFile);
        if(!initialResult.contains("checksum")) return; //initial file has errors or does not available

        List<Map<Integer, String>> allCombinations = new ArrayList<>();
        List<Integer> keys = new ArrayList<>(mutateMap.keySet());
        generateCombinations(keys, 0, new HashMap<>(), allCombinations, mutateMap);

        // Write each combination to a file
        for (Map<Integer, String> mutate : allCombinations) {
            if(mutCount > 200 && correctCount < 2) break;
            if(correctCount > 499) break;
            writeToFile(muDir, file, mutate, initialFileList, initialResult);
        }
    }

    private void generateCombinations(List<Integer> keys, int index, Map<Integer, String> currentCombination,
                                      List<Map<Integer, String>> allCombinations,
                                      Map<Integer, List<String>> mutateMap) {
        // 使用 BigInteger 计算组合的总数
        BigInteger totalCombinations = BigInteger.ONE;
        for (Integer key : keys) {
            List<String> options = mutateMap.get(key);
            if (options != null) {
                totalCombinations = totalCombinations.multiply(BigInteger.valueOf(options.size()));
            }
        }

        // 判断组合数量是否超过2000
        if (totalCombinations.compareTo(BigInteger.valueOf(2000)) > 0) {
            Random random = new Random();
            // 随机选择组合，直到达到2000个
            while (allCombinations.size() < 2000) {
                currentCombination.clear(); // 清空当前组合
                for (Integer key : keys) {
                    List<String> options = mutateMap.get(key);
                    if (options != null && !options.isEmpty()) {
                        // 随机选择一个选项
                        String option = options.get(random.nextInt(options.size()));
                        currentCombination.put(key, option); // 添加到当前组合
                    }
                }
                allCombinations.add(new HashMap<>(currentCombination)); // 深拷贝当前组合
            }
        } else {
            // 如果没有超过2000，正常生成组合
            if (index >= keys.size()) {
                // 如果所有索引都处理完，存储当前组合
                allCombinations.add(new HashMap<>(currentCombination)); // 深拷贝当前组合
                return;
            }

            Integer currentKey = keys.get(index); // 获取当前键
            List<String> options = mutateMap.get(currentKey);

            if (options != null) {
                for (String option : options) {
                    currentCombination.put(currentKey, option); // 添加当前选项到组合
                    generateCombinations(keys, index + 1, currentCombination, allCombinations, mutateMap); // 递归处理下一个索引
                }
            } else {
                // 如果没有选项，移动到下一个索引
                generateCombinations(keys, index + 1, currentCombination, allCombinations, mutateMap);
            }
        }
    }

    private void writeToFile(File muDir, File file, Map<Integer, String> mutate, List<String> initialFileList, String initialFileResult) {
        int addedFuncLine = CommonInfoFromFile.getFuncStartLine(initialFileList);
        File muFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_minmax_" + (mutCount++) + ".c");
        try (FileWriter fw = new FileWriter(muFile);
             PrintWriter pw = new PrintWriter(fw)) {
            int count = 0;
            for (String line : initialFileList) {
                count++;
                if(count == addedFuncLine){
                    PropertiesInfo.minmaxFuncList.forEach(pw::println);
                }
                if(mutate.containsKey(count)) {
                    pw.println("//mutate");
                    pw.println(mutate.get(count));
                }
                else pw.println(line);
            }

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

            boolean isCorrect;
            String result = getFileExecResult(muFile);
            if(result.contains("error") || result.equals("timeout")) isCorrect = false;
            else isCorrect = result.equals(initialFileResult);

            System.out.println(initialFileResult);
            System.out.println(result);
            System.out.println(isCorrect);
            System.out.println();

            if(!isCorrect) {
                File errorsDir = new File(PropertiesInfo.indexDir + "/minmax-errors/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
                if(!errorsDir.exists()){
                    errorsDir.mkdirs();
                }
                ProcessTerminal.voidNotMemCheck("cp " + muFile.getAbsolutePath() +  " " + errorsDir.getAbsolutePath(), "sh");
                muFile.delete();
            } else {
                correctCount++;
                FileModify.formatFile(muFile);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFileExecResult(File checkFile){
        String aoutFilename = "ip" + checkFile.getName().substring(0, checkFile.getName().indexOf(".c"))
                .replaceAll("[a-zA-Z]", "");
        System.out.println("ip check::: " + aoutFilename);

        String command = (sanitizerCompiler.equals("gcc") ?
                "export LANGUAGE=en && export LANG=en_US.UTF-8 && " : "")
                + "cd " + checkFile.getParent() + " && " + sanitizerCompiler + " " + checkFile.getName()
                + " -fsanitize=undefined,address "
                + " -fno-omit-frame-pointer -g "
                + " -w -lm -o " + aoutFilename;
        List<String> compilationList = ProcessTerminal.listNotMemCheck(command, "sh");

        File aoutFile = new File(checkFile.getParent() + "/" + aoutFilename);
        if(!aoutFile.exists()){
            System.out.println(checkFile.getAbsolutePath() + " has compiler errors during checksum consistent check.......");
            CommonOperation.printList(compilationList);
            return "compiler_error";
        }

        command = (sanitizerCompiler.equals("gcc") ?
                "export LANGUAGE=en && export LANG=en_US.UTF-8 && " : "")
                + "cd " + checkFile.getParent() + " && " + "./" + aoutFilename;
//        System.out.println(command);

        List<String> execLines = ProcessTerminal.listMemCheck(command, 8, "sh", true,
                true, new ArrayList<>(Arrays.asList(aoutFilename)));
        deleteAoutFile(checkFile, aoutFilename);

        return analysisResult(execLines);
    }

    public String analysisResult(List<String> execLines){
        if(execLines.isEmpty()){
            return "empty";
        }
        else if(execLines.get(0).equals("timeout")){
            return "timeout";
        }
        else {
            for(String s: execLines){
                if(s.contains("error:") || s.contains("ERROR:") || s.contains("(core dumped)")){
                    System.out.println("Error: " + s);
                    return "runtime_error";
                }
            }
            for(String s: execLines){
                if(s.matches("checksum\\s*=\\s*[0-9A-Za-z]+")){
                    return s.trim();
                }
            }
        }
        return "";
    }

    public void deleteAoutFile(File file, String aoutFilename){
        File outFile = new File(file.getParent() + "/" + aoutFilename);
        if(outFile.exists()){
            outFile.delete();
        }
    }
}
