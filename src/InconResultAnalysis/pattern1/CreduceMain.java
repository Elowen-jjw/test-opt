package InconResultAnalysis.pattern1;

import TestResult.Test;
import common.PropertiesInfo;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreduceMain {
    public static void main(String args[]){
        File inconFolder = new File(PropertiesInfo.inconResultIndexDir);
        if (inconFolder.isDirectory()) {
            searchFoldersRecursively(inconFolder);
        }
    }

    public static void searchFoldersRecursively(File folder) {
        File[] filesAndFolders = folder.listFiles();

        if (filesAndFolders != null) {
            for (File file : filesAndFolders) {
                if (file.isDirectory()) {
                    String folderName = file.getName();
                    if (folderName.contains("gcc_line") || folderName.contains("clang_line")) {
                        execCreduce(new File(folderName));
                    }
                    searchFoldersRecursively(file);
                }
            }
        }
    }

    public static void execCreduce(File folder){
        ExecutorService executor = Executors.newFixedThreadPool(PropertiesInfo.Reduced_ThreadCount);
        for (File file : folder.listFiles()) {
            if (!file.getName().endsWith(".c")) continue;
            executor.submit(() -> {
                Creduce creduce = new Creduce(folder);
                creduce.run(file);
                System.out.println("Processed file: " + file.getName() + " by thread: " + Thread.currentThread().getName());
            });
        }
        executor.shutdown(); // 关闭线程池
    }
}
