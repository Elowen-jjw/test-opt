package InconResultAnalysis;

import common.PropertiesInfo;
import processtimer.ProcessTerminal;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreduceProcessor {

    public static void main(String[] args) {
        String sourceDir = "/home/sdu/Desktop/cvise-test/testsuite"; // 设置你的 sourcedir 路径
        File dir = new File(sourceDir);

        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Source directory does not exist or is not a directory.");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(PropertiesInfo.Reduced_ThreadCount);

        File[] files = dir.listFiles((d, name) -> name.endsWith(".c"));
        if (files != null) {
            for (File file : files) {
                executor.execute(() -> processFile(file));
            }
        }

        executor.shutdown();
    }

    public static void processFile(File cFile) {
        try {
            String fileName = cFile.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            File parentDir = cFile.getParentFile();
            File randomDir = new File(parentDir.getParentFile(), baseName);

            if (!randomDir.exists()) {
                randomDir.mkdir();
            }

            // 移动和复制文件
            File copyFile = new File(randomDir, baseName + "_copy.c");
            Files.copy(cFile.toPath(), copyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.move(cFile.toPath(), new File(randomDir, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 运行 .c 文件并获取 checksum_value
            String checksumValue = getChecksumValue(new File(randomDir, fileName), "clang");

            // 创建 creduce.sh
            createCreduceScript(randomDir, fileName, checksumValue, "clang");

            // 运行 creduce 剪枝操作
            runCreduce(randomDir, new File(randomDir, fileName));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getChecksumValue(File cFile, String compilerCommand){
        String aoutFilename = "c-" + cFile.getName().substring(0, cFile.getName().indexOf(".c"))
                .replaceAll("[a-zA-Z]", "");
        String command = (compilerCommand.equals("gcc") ?
                "export LANGUAGE=en && export LANG=en_US.UTF-8 && " : "")
                + "cd " + cFile.getParent() + " &&  " + compilerCommand + " " + cFile.getName()
                + " -w -lm -fsanitize=undefined,address -o " + aoutFilename + " && ./"  + aoutFilename;
        List<String> execLines = ProcessTerminal.listMemCheck(command, 5, "sh", true, true, new ArrayList<>(Arrays.asList(aoutFilename)));

        String checksumValue = null;
        for(String line: execLines){
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + line);
            if(line.contains("AddressSanitizer:DEADLYSIGNAL")
                    || line.contains("error") || line.contains("ERROR") || line.contains("(core dumped)")) break;
            if (line.startsWith("checksum = ")) {
                checksumValue = line.split(" = ")[1];
                System.out.println("------------------------" + checksumValue);
                break;
            }
        }
        System.out.println(cFile.getName() + "------------------------" + checksumValue + checksumValue == null);

        return checksumValue != null ? checksumValue : "";
    }

    public static void createCreduceScript(File dir, String fileName, String checksumValue, String compilerCommand) throws IOException {
        File script = new File(dir, "creduce.sh");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(script))) {
            writer.write("#!/bin/bash\n\n");
            writer.write((compilerCommand.equals("gcc") ?
                    "export LANGUAGE=en && export LANG=en_US.UTF-8 && " : "")
                    + compilerCommand + " " + fileName + " -w -lm -fsanitize=undefined,address && ./a.out &> output.txt\n");
            writer.write("grep -qi \"error\" output.txt && exit 1\n");
            writer.write("grep '^checksum = " + checksumValue + "$' output.txt\n");
        }
        script.setExecutable(true);
    }

    public static void runCreduce(File dir, File singleFile) {
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c",
                    "cd " + dir.getAbsolutePath() +
                            " && chmod +x creduce_0.sh" +
                            " && creduce --timing --timeout 20 --n 6 creduce_0.sh " + singleFile.getName());
            builder.redirectErrorStream(true);
            process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            process.waitFor();

            System.out.println("Finished creduce on " + dir.getName());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 重新设置中断状态
            System.out.println("creduce Process was interrupted, but continuing execution.");
            return; // 跳过当前操作，返回方法
        } finally {
            if (process != null) {
                process.destroy(); // 确保在结束时清理进程
            }
        }
    }
}
