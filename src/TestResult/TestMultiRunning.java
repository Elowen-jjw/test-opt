package TestResult;

import InconResultAnalysis.InconFileOperation;
import InconResultAnalysis.InconReduced;
import ObjectOperation.file.FileInfo;
import ObjectOperation.list.CommonOperation;
import common.PropertiesInfo;
import mutate.minmax.WriteMutate;
import processtimer.ProcessTerminal;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestMultiRunning {
    public static String mutateType = "pt";
    public static void main(String[] args) {
        //Gen mutate and gen result
        File suiteDir = new File(PropertiesInfo.testSuiteStructDir);
        FileInfo fi = new FileInfo();
        List<File> fileList = fi.getSortedOverallFiles(suiteDir);

        ExecutorService executor = Executors.newFixedThreadPool(PropertiesInfo.GenAndTest_ThreadCount);

        for (File file : fileList) {
            if (!file.getName().endsWith(".c")) continue;
            executor.submit(() -> {
                Test test = new Test(mutateType);
                if (mutateType.equals("statute")) {
                    test.runStatute(file);
                } else if (mutateType.equals("minmax")) {
                    test.runMinmax(file);
                } else if (mutateType.equals("pt")) {
                    test.runPT(file);
                }

                System.out.println("Processed file: " + file.getName() + " by thread: " + Thread.currentThread().getName());
            });
        }
        executor.shutdown(); // 关闭线程池

        System.out.println("gen and test mutate ends....");

        //filter incon
//        InconFileOperation ifo = new InconFileOperation();
//        ifo.filterSanitize();

        //reduce
//       File folder = new File(PropertiesInfo.inconResultIndexDir + "/" + mutateType);
//        InconReduced ir = new InconReduced();
//        ir.multiRun(folder);

        //delete all txt files
//        Test test = new Test(mutateType);
//        test.deleteTxt(new File(PropertiesInfo.mutateStatuteDir));

        //test version cd PropertiesInfo.testSuite && ./pointer_parser random11108.c
//        String command = "cd /home/sdu/Desktop && ./pointer_parser random11108.c";
//        CommonOperation.printList(ProcessTerminal.listNotMemCheck(command, "sh"));


//        ExecutorService executor = Executors.newFixedThreadPool(5);
//
//        for (int i = 0; i<10; i++) {
//            executor.submit(() -> {
//                String command = "cd /home/sdu/Desktop && clang loop.c -w -lm --target=riscv64-linux-gnu && qemu-riscv64 -L /usr/riscv64-linux-gnu/ ./a.out";
//                CommonOperation.printList(ProcessTerminal.listMemCheck(command, 10, "sh", false, true, new ArrayList<>(Arrays.asList("a.out"))));
//            });
//        }
//        executor.shutdown(); // 关闭线程池


    }
}