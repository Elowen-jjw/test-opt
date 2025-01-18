package InconResultAnalysis;

import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import common.PropertiesInfo;
import processtimer.ProcessTerminal;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static InconResultAnalysis.CreduceProcessor.*;

public class InconReduced {
    File FILE1;
    File FILE2;
    File TEMP_FILE1;
    File TEMP_FILE2;

    List<String> initialFileList1;
    List<String> initialFileList2;
    Map<Integer, String> transList1 = new LinkedHashMap<>();
    Map<Integer, String> transList2 = new LinkedHashMap<>();
    Set<Integer> deletedSet1 = new HashSet<>();
    Set<Integer> deletedSet2 = new HashSet<>();

    String testCompiler = "";

    public InconReduced(String testCompiler) {
        this.testCompiler = testCompiler;
    }

    public InconReduced() {
    }

    public static void main(String[] args) {
        // 创建一个固定大小的线程池，一次运行4个线程
        ExecutorService executor = Executors.newFixedThreadPool(4);

        // 启动 creduceProcess 处理 gcc-incon 和 clang-incon 文件夹
        InconReduced inconReducedGcc = new InconReduced();
        InconReduced inconReducedClang = new InconReduced();

//        inconReducedGcc.creduceProcess(PropertiesInfo.indexDir + "/Confirming/gcc-incon", executor);
        inconReducedClang.creduceProcess(PropertiesInfo.indexDir + "/Confirming/clang-incon", executor);

        // 等待所有任务完成
        executor.shutdown();
    }

    public void creduceProcess(String inconPath, ExecutorService executor) {
        File inconFolder = new File(inconPath);
        for (File randomFolder : inconFolder.listFiles()) {
            if (randomFolder.isDirectory() && randomFolder.getName().matches("random\\d+")) {
                executor.submit(() -> processFolder(randomFolder));
            }
        }
    }

    // 处理每个 random 文件夹的逻辑
    private void processFolder(File randomFolder) {
        File creduceFile = new File(randomFolder, "creduce_0.sh");
        boolean isFinished = false;
        // 遍历每个文件夹中的文件
        for (File file : randomFolder.listFiles()) {
            if (file.getName().endsWith("_0.c")) {
                if (file.length() / 1024 < 5 && creduceFile.exists()) {
                    isFinished = true;
                }
            }
        }

        // 如果已经完成，则跳过
        if (isFinished) return;

        System.out.println(randomFolder.getAbsolutePath());

        // 选择 compilerType
        String compilerType = "";
        if (randomFolder.getAbsolutePath().contains("gcc")) {
            compilerType = "gcc";
        } else if (randomFolder.getAbsolutePath().contains("clang")) {
            compilerType = "clang";
        }

        // 处理 randomFolder
        InconReduced ir = new InconReduced(compilerType);
        ir.run(randomFolder);
    }

    public void multiRun(File inconFolder){

    }

    public void run(File randomFolder){
        FILE1 = new File(randomFolder.getAbsoluteFile() + "/" + randomFolder.getName() + ".c");
        FILE2 = new File(randomFolder.getAbsoluteFile() + "/" + randomFolder.getName() + "_0.c");
        TEMP_FILE1 = new File(randomFolder.getAbsoluteFile() + "/temp1.c");
        TEMP_FILE2 = new File(randomFolder.getAbsoluteFile() + "/temp2.c");
        initialFileList1 = CommonOperation.genInitialList(FILE1);
        initialFileList2 = CommonOperation.genInitialList(FILE2);
        getTransList(initialFileList1, transList1);
        getTransList(initialFileList2, transList2);
        cpFile(FILE1, FILE1.getParent() + "/" + FILE1.getName().substring(0, FILE1.getName().lastIndexOf(".c")) + "_initial.c");
        cpFile(FILE2, FILE2.getParent() + "/" + FILE2.getName().substring(0, FILE2.getName().lastIndexOf(".c")) + "_initial.c");

        System.out.println(randomFolder.getName() + " Start to checksum reducing.....");
        try {
            findDiffingLines();
        } catch (IOException e) {
            e.printStackTrace();
        }
        deletedSet1.clear();
        deletedSet2.clear();
        System.out.println(randomFolder.getName() + " End checksum reducing.....");

        cpFile(FILE1, FILE1.getParent() + "/" + FILE1.getName().substring(0, FILE1.getName().lastIndexOf(".c")) + "_checksum.c");
        cpFile(FILE2, FILE2.getParent() + "/" + FILE2.getName().substring(0, FILE2.getName().lastIndexOf(".c")) + "_checksum.c");

        System.out.println(randomFolder.getName() + " Start to creduce reducing.....");
//        runCReduced(randomFolder, FILE1);
        runCReduced(randomFolder, FILE2);
        System.out.println(randomFolder.getName() + " End creduce reducing.....");
    }
    public void runCReduced(File randomFolder, File file){
        try {
            String gccCheckSum = getChecksumValue(file, "gcc");
            String clangCheckSum = getChecksumValue(file, "clang");
            System.out.println("gcc checkValues: " + gccCheckSum);
            System.out.println("clang checkValues: " + clangCheckSum);
            if(gccCheckSum.equals(clangCheckSum)) return;
            if(gccCheckSum == null || clangCheckSum == null) return;

            // 创建 creduce.sh
            createCreduceScript(randomFolder, file.getName(), gccCheckSum, clangCheckSum, testCompiler);

            // 运行 creduce 剪枝操作
            runCreduce(randomFolder, file);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void cpFile(File file, String destDir){//同文件夹下copy
        String command = "cp " + file.getAbsolutePath() + " " + destDir;	//move gcc or llvm folder
        ProcessTerminal.voidNotMemCheck(command, "sh");
    }

    public void getTransList(List<String> initialFileList, Map<Integer, String> transList) {
        transList.clear();
        int count = 0;
        boolean isStart = false;
        for (String s : initialFileList) {
            count++;
            if (s.trim().equals("platform_main_begin();"))
                isStart = true;
            else if (s.contains("platform_main_end"))
                isStart = false;
            else if (isStart && s.trim().startsWith("transparent_crc")) {
                transList.put(count, initialFileList.get(count - 1));
            }
        }
    }

    public void findDiffingLines() throws IOException {
        // 获取两个 transList 的键
        List<Integer> keys1 = new ArrayList<>(transList1.keySet());
        List<Integer> keys2 = new ArrayList<>(transList2.keySet());
//        System.out.println(keys1);
//        System.out.println(keys2);

        // 确保两个列表的大小一致
        int size = Math.min(keys1.size(), keys2.size());

        // 按顺序逐行注释并检查
        for (int i = 0; i < size; i++) {
            int lineToComment1 = keys1.get(i);
            int lineToComment2 = keys2.get(i);

            List<Integer> newList1 = keys1.stream()
                    .filter(element -> element != lineToComment1)
                    .collect(Collectors.toList());
            List<Integer> newList2 = keys2.stream()
                    .filter(element -> element != lineToComment2)
                    .collect(Collectors.toList());

            // 注释 FILE1 的行
            commentLines(newList1, FILE1.getAbsolutePath(), TEMP_FILE1.getAbsolutePath());
            // 注释 FILE2 的行
            commentLines(newList2, FILE2.getAbsolutePath(), TEMP_FILE2.getAbsolutePath());

            // 获取 checksum
            String checksum1 = getChecksum(TEMP_FILE1);
            String checksum2 = getChecksum(TEMP_FILE2);

            // 检查 checksum 是否一致
            if (!checksum1.equals(checksum2)) {
                System.out.println("导致 checksum 不一致的行: " + transList1.get(lineToComment1) + " 和 " + transList2.get(lineToComment2));
            } else {
                deletedSet1.add(lineToComment1);
                deletedSet2.add(lineToComment2);
            }

            TEMP_FILE1.delete();
            TEMP_FILE2.delete();
        }

        FileModify fm = new FileModify();
        fm.deleteLinesToFile(FILE1, deletedSet1);
        fm.deleteLinesToFile(FILE2, deletedSet2);
    }

    private  void commentLines(List<Integer> linesToComment, String sourceFile, String tempFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
             PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (linesToComment.contains(lineNumber)) {
                    writer.println("// " + line); // 注释行
                } else {
                    writer.println(line);
                }
            }
        }
    }

    private  String getChecksum(File file) throws IOException {
        String aoutFilename = "c-" + file.getName().substring(0, file.getName().indexOf(".c"))
                .replaceAll("[a-zA-Z]", "");
        String compileCommand = "cd " + file.getParent() + " && "
                + testCompiler + " " + file.getName()
                + " -w -lm -I $CSMITH_HOME/include -o " + aoutFilename;
        ProcessTerminal.voidNotMemCheck(compileCommand, "sh");

        String execCommand = "cd " + file.getParent() + " && " + "./" + aoutFilename;
        File aoutFile = new File(file.getParent() + "/" + aoutFilename);
        if (!aoutFile.exists()) {
            return "comerror";
        } else {
            List<String> execLines = ProcessTerminal.listMemCheck(execCommand, 10, "sh", false,
                    true, new ArrayList<>(Arrays.asList(aoutFilename)));
//            System.out.println(execLines);
            if (execLines.get(execLines.size() - 1).trim().matches("checksum\\s*=\\s*[0-9A-Za-z]+")) {
                return execLines.get(execLines.size() - 1).trim().replaceAll("checksum\\s*=\\s*", "").trim();
            }
        }
        return "error";
    }

    public static void createCreduceScript(File dir, String fileName,
                                           String gccChecksumValue, String clangChecksumValue,
                                           String compilerType) {
        File script = new File(dir, "creduce_0.sh");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(script))) {
            writer.write("#!/bin/bash\n\n");

            if ("gcc".equals(compilerType) || "clang".equals(compilerType)) {
                // Generate script for gcc
                writer.write("gcc " + fileName + " -O0 -lm -o gcc-" + fileName.replaceAll("[a-zA-Z_]", "")
                        + " &> output_gcc0.txt && ./gcc-" + fileName.replaceAll("[a-zA-Z_]", "")
                        + " &>> output_gcc0.txt\n");
                writer.write("clang " + fileName + " -fsanitize=undefined,address -lm -o clang-"
                        + fileName.replaceAll("[a-zA-Z_\\.]", "") + " &> output_clang0.txt && ./clang-"
                        + fileName.replaceAll("[a-zA-Z_\\.]", "") + " &>> output_clang0.txt\n");
                writer.write("clang " + fileName + " -fsanitize=memory -w -lm -o clang-"
                        + fileName.replaceAll("[a-zA-Z_\\.]", "") + "1 &> output_clang1.txt && ./clang-"
                        + fileName.replaceAll("[a-zA-Z_\\.]", "") + "1 &>> output_clang1.txt\n\n");

                writer.write("grep -qi \"error\" output_gcc0.txt && exit 1\n");
                writer.write("grep -qi \"error\" output_clang0.txt && exit 1\n");
                writer.write("grep -qi \"WARNING: MemorySanitizer: use-of-uninitialized-value\" output_clang1.txt && exit 1\n");
                writer.write("grep -qi \"\\[-Wdiscarded-qualifiers\\]\" output_gcc0.txt && exit 1\n");
                writer.write("grep -q \"\\[-Wincompatible-pointer-types\\]\" output_gcc0.txt && exit 1\n");
                writer.write("grep -qi \"warning: no semicolon at end of struct or union\" output_gcc0.txt && exit 1\n");
                writer.write("grep -qi \"\\[-Wdeprecated-non-prototype\\]\" output_clang0.txt && exit 1\n\n");

                writer.write("grep -q '^checksum = " + gccChecksumValue + "$' output_gcc0.txt || exit 1\n");
                writer.write("grep -q '^checksum = " + clangChecksumValue + "$' output_clang0.txt || exit 1\n\n");
            }

//            if ("clang".equals(compilerType)) {
//                // Generate script for clang
//                writer.write("gcc " + fileName + " -fsanitize=undefined,address -O0 -lm -o gcc-"
//                        + fileName.replaceAll("[a-zA-Z_\\.]", "") + " &> output_gcc0.txt && ./gcc-"
//                        + fileName.replaceAll("[a-zA-Z_\\.]", "") + " &>> output_gcc0.txt\n");
//                writer.write("clang " + fileName + " -fsanitize=memory -O0 -lm -o clang-"
//                        + fileName.replaceAll("[a-zA-Z_\\.]", "") + " &> output_clang0.txt && ./clang-"
//                        + fileName.replaceAll("[a-zA-Z_\\.]", "") + " &>> output_clang0.txt\n\n");
//
//                writer.write("grep -qi \"error\" output_gcc0.txt && exit 1\n");
//                writer.write("grep -qi \"error\" output_clang0.txt && exit 1\n");
//                writer.write("grep -qi \"WARNING: MemorySanitizer: use-of-uninitialized-value\" output_clang0.txt && exit 1\n");
//                writer.write("grep -qi \"\\[-Wdiscarded-qualifiers\\]\" output_gcc0.txt && exit 1\n");
//                writer.write("grep -q \"\\[-Wincompatible-pointer-types\\]\" output_gcc0.txt && exit 1\n");
//                writer.write("grep -qi \"warning: no semicolon at end of struct or union\" output_gcc0.txt && exit 1\n");
//                writer.write("grep -qi \"\\[-Wdeprecated-non-prototype\\]\" output_clang0.txt && exit 1\n\n");
//
//                writer.write("grep -q '^checksum = " + gccChecksumValue + "$' output_gcc0.txt || exit 1\n");
//                writer.write("grep -q '^checksum = " + clangChecksumValue + "$' output_clang0.txt || exit 1\n\n");
//
//            }

            writer.write("exit 0\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        script.setExecutable(true);
    }

}
