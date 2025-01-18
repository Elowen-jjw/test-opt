import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.model.LoopStatement;
import ObjectOperation.file.FileInfo;
import ObjectOperation.list.CommonOperation;
import common.PropertiesInfo;
import processtimer.ProcessTerminal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TestCode {
    public static void main(String args[]){

        File file1 = new File("/home/sdu/Desktop/incon_collection/sdu/allconfig/random3079_inline/random3079.c");
        File file2 = new File("/home/sdu/Desktop/incon_collection/sdu/allconfig/random3079_inline/random3079_0.c");
        List<String> execLines1 = ProcessTerminal.listMemCheck("cd " + file1.getParent() + " && clang " + file1.getName() + " -O1 -lm -w && ./a.out",
                10, "sh", true, true, new ArrayList<>(Arrays.asList("a.out")));
        List<String> execLines2 = ProcessTerminal.listMemCheck("cd " + file2.getParent() + " && clang " + file2.getName() + " -O0 -lm -w && ./a.out",
                10, "sh", true, true, new ArrayList<>(Arrays.asList("a.out")));
        for(int i = 0; i < execLines1.size(); i++){
            if(!execLines1.get(i).equals(execLines2.get(i))){
                System.out.println(execLines1.get(i));
                System.out.println(execLines2.get(i));
            }
        }

//        File file = new File("/home/sdu/Desktop/TestSuite/random" + 4 + ".c");
//        CSE cse = new CSE(file);
//        cse.run();
//
//        File file = new File("/home/sdu/Desktop/CsmithRandom/random" + 50 + ".c");
//        Inline inline = new Inline(file);
//        inline.run();

//        for(int i = 0; i < 100; i++) {
//            File file = new File("/home/sdu/Desktop/checking/random" + i + ".c");
//            CSE cse = new CSE(file);
//            cse.run();
//        }

        //SideEffort
//        File file = new File("/home/sdu/Desktop/TestSuite/random" + 11 + ".c");
//        List<String> initiaList = CommonOperation.genInitialList(file);
//        SideEffort se = new SideEffort(file);
//        se.getSideEffortVar("stl");
//        for(Integer lineNumber: se.sevMap.keySet()){
//            for(SideEffortInfo sev: se.sevMap.get(lineNumber)){
//                System.out.println(sev.getStatement() + "   " + sev.isOutermost());
//                System.out.println("lineNumber: " + sev.getLineNumber() + "  leftValue: " + sev.getLeftValue() + "  leftType: " + sev.getLeftType()
//                        + "  rightValue: " + sev.getRightValue() + "  rightType:" + sev.getRightType() + "  operator: " + sev.getOperator()
//                        + "  start: " + initiaList.get(sev.getLineNumber() - 1).trim().charAt(sev.getStartColumn()) + "  end: " + initiaList.get(sev.getLineNumber() - 1).trim().charAt(sev.getEndColumn() - 1)
//                        + "  start: " + sev.getStartColumn() + "  end: " + sev.getEndColumn()
//                        + "  start2: " + initiaList.get(sev.getLineNumber() - 1).trim().charAt(sev.getRightStartColumn()) + "  end2: " + initiaList.get(sev.getLineNumber() - 1).trim().charAt(sev.getRightEndColumn() - 1));
//
//            }
//        }

//        FileInfo fi = new FileInfo();
//        List<File> allFileList = fi.getSortedOverallFiles(new File("/home/sdu/Desktop/RandomSuite"));
//        for(File file: allFileList){
//            System.out.println(file.getName());
//            getLoopInfo(file, new File("/home/sdu/Desktop/revision.txt"));
//        }

    }

    public static String replaceRegex(String initial){
        return initial.replaceAll("\\[", "\\\\[")
                .replaceAll("\\(", "\\\\(")
                .replaceAll("\\)", "\\\\)")
                .replaceAll("\\*", "\\\\*")
                .replaceAll("\\.", "\\\\.");
    }

    public static void getLoopInfo(File inputFile, File outputFile) {
        List<LoopStatement> loopList = new ArrayList<>();
        Map<Integer, Integer> loopMap = new HashMap<>();
        AstInform_Gen astgen = new AstInform_Gen(inputFile);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<String> initialFileList = CommonOperation.genInitialList(inputFile);
        loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);

        for (LoopStatement loop : loopList) {
            loopMap.put(loop.getStartLine(), loop.getEndLine());
        }

        try (FileWriter fw = new FileWriter(outputFile, true); // 设置为追加模式
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("---------------------------" + inputFile.getName() + "---------------------\n");
            for (int i = 0; i < initialFileList.size(); i++) {
                if (loopMap.containsKey(i + 1)) {
                    List<String> loopLines = CommonOperation.getListPart(initialFileList, i + 1, loopMap.get(i + 1));
                    int count = i + 1;
                    for (String line : loopLines) {
                        if ((line.contains("+=") || line.contains("-=") ||
                                line.contains("^=") || line.contains("&=") ||
                                line.contains("*=") || line.contains(">>=") ||
                                line.contains("<<=") || line.contains("/=") ||
                                line.contains("|=") || line.contains("++") ||
                                line.contains("--")) &&
                                !line.trim().matches("for\\s*\\([ijklmno]\\s*=\\s*\\d+;\\s*[ijklmno]\\s*[><]=?\\s*\\d+;\\s*[ijklmno][+-]+\\)\\s*\\{")) {
                            bw.write(count + ": " + line.trim()); // 写入文件
                            bw.newLine(); // 添加换行
                        }
                        count++;
                    }
                    i = loopMap.get(i + 1);
                }
            }
            bw.write("----------------------------------------------------------------------------\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*

    #!/bin/bash

    gcc random6988_0.c -O0 -w -lm -o ./gcc-698800 && ./gcc-698800 &> output_gcc0.txt
    gcc random6988_0.c -O1 -w -lm -o ./gcc-698801 && ./gcc-698801 &> output_gcc1.txt
    gcc random6988_0.c -O2 -w -lm -o ./gcc-698802 && ./gcc-698802 &> output_gcc2.txt
    gcc random6988_0.c -O3 -w -lm -o ./gcc-698803 && ./gcc-698803 &> output_gcc3.txt
    gcc random6988_0.c -Os -w -lm -o ./gcc-698804 && ./gcc-698804 &> output_gcc4.txt

    clang random6988_0.c -O0 -w -lm -o ./clang-698800 && ./clang-698800 &> output_clang0.txt
    clang random6988_0.c -O1 -w -lm -o ./clang-698801 && ./clang-698801 &> output_clang1.txt
    clang random6988_0.c -O2 -w -lm -o ./clang-698802 && ./clang-698802 &> output_clang2.txt
    clang random6988_0.c -O3 -w -lm -o ./clang-698803 && ./clang-698803 &> output_clang3.txt
    clang random6988_0.c -Os -w -lm -o ./clang-698804 && ./clang-698804 &> output_clang4.txt

    grep -qi "error" output_gcc0.txt && exit 1
    grep -qi "error" output_gcc1.txt && exit 1
    grep -qi "error" output_gcc2.txt && exit 1
    grep -qi "error" output_gcc3.txt && exit 1
    grep -qi "error" output_gcc4.txt && exit 1

    grep -qi "error" output_clang0.txt && exit 1
    grep -qi "error" output_clang1.txt && exit 1
    grep -qi "error" output_clang2.txt && exit 1
    grep -qi "error" output_clang3.txt && exit 1
    grep -qi "error" output_clang4.txt && exit 1

    grep -q '^checksum = 6B5DAB1$' output_gcc0.txt || exit 1
    grep -q '^checksum = 6B5DAB1$' output_gcc1.txt || exit 1
    grep -q '^checksum = 6B5DAB1$' output_gcc2.txt || exit 1
    grep -q '^checksum = 6B5DAB1$' output_gcc3.txt || exit 1
    grep -q '^checksum = 6B5DAB1$' output_gcc4.txt || exit 1

    grep -q '^checksum = 9CBA296B$' output_clang0.txt || exit 1
    grep -q '^checksum = 9CBA296B$' output_clang1.txt || exit 1
    grep -q '^checksum = 9CBA296B$' output_clang2.txt || exit 1
    grep -q '^checksum = 9CBA296B$' output_clang3.txt || exit 1
    grep -q '^checksum = 9CBA296B$' output_clang4.txt || exit 1

    exit 0

     */

}
