package CsmithGen;


import ObjectOperation.file.FileInfo;
import ObjectOperation.file.FileSort;
import ObjectOperation.list.CommonOperation;
import common.CommonInfoFromFile;
import common.PropertiesInfo;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class Main {
    public static void main(String args[]){
//        deleteSmall();

        int count = 0;
        do{
            int randomIndex = getTotalRandomFileCount();
            SwarmGen sg = new SwarmGen(PropertiesInfo.testSuiteInitialDir);
            File randomFile = sg.genRandomTestcase(randomIndex);

            DCE dce = new DCE(randomFile, "gcc");
            if(!dce.run()){
                randomFile.delete();
                System.out.println("after dce has sanitizer error....--------------------------------" + randomFile.getName());
            }

            AddAssertion aa = new AddAssertion(randomFile);
            aa.run();

        }while(count++ < 10000);

//            CsmithFlagDeletion cd = new CsmithFlagDeletion();
//            if(!cd.run(randomFile)){
//                randomFile.delete();
//                System.out.println("after csmithflagdeletion has compiler error--------------------------------" + randomFile.getName());
//                continue;
//            }
//        File randomFile = new File("/home/sdu/Desktop/RandomSuite_Struct/random7.c");
//        DCE dce = new DCE(randomFile, "gcc");
//        if(!dce.run()){
//            randomFile.delete();
//            System.out.println("after dce has sanitizer error....--------------------------------" + randomFile.getName());
//        }
//            if(randomFile.length() / 1024 < 3 || isHaveSmallCore(randomFile)){
//            	System.out.println(randomFile.lastModified()/1024 + "  file is too small......");
//                randomFile.delete();
//            }

//    	String command = "whereis sde";
//    	CommonOperation.printList(ProcessTerminal.listMemCheck(command, 10, "sh", false, false,""));
    }

    public static void deleteSmall(){
        FileInfo fi = new FileInfo();
        for(File file: fi.getSortedOverallFiles(new File(PropertiesInfo.testSuiteInitialDir))){
            if(file.length() / 1024 < 3 || isHaveSmallCore(file))
                file.delete();
        }
        FileSort.renameFilesBySize(PropertiesInfo.testSuiteInitialDir);
    }

    public static boolean isHaveSmallCore(File file){
        List<String> initialFileList = CommonOperation.genInitialList(file);
        int startLine = CommonInfoFromFile.getFuncStartLine(initialFileList);
        int count = 0;
        for(String s: initialFileList){
            count++;
            if(s.trim().equals("int main(void) {")) break;
        }
        return (count - 1 - startLine) < 18;
    }

    public static int getTotalRandomFileCount(){
        File outestDir = new File(PropertiesInfo.testSuiteInitialDir);
        int count = 0;
        for(File file: Objects.requireNonNull(outestDir.listFiles())){
            if(file.getName().matches("random[0-9]+\\.c")){
                count++;
            }
        }
        return count;
    }

}
