package common;

import objectoperation.file.getAllFileList;
import processmemory.ProcessCompiler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FirstFileFormat {
    public static void main(String args[]){
        deleteComment();
        deleteSpecial();
    }
    public static void deleteSpecial() {
        File dir = new File("/home/elowen/桌面/random-testsuite");
        List<File> allFileList = new ArrayList<File>();

        getAllFileList getFileList = new getAllFileList(dir);
        getFileList.getAllFile(dir, allFileList);
        getFileList.compareFileList(allFileList);
        for(File file: allFileList) {
            if(!file.getName().endsWith(".c")) {
                continue;
            }
            rw(file);
        }
        System.out.println("end!");
    }

    public static void rw(File file) {
        try {
            File tempFile = new File(file.getParent() + "/temp.c");
            if(tempFile.exists()) {
                tempFile.delete();
            }
            tempFile.createNewFile();
            FileInputStream fis = new FileInputStream(file);
            BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
            FileOutputStream fos = new FileOutputStream(tempFile);
            PrintWriter pw = new PrintWriter(fos);
            //tempFile.deleteOnExit();

            String line = "";
            while((line = bf.readLine()) != null) {
                if(line.trim().matches("#.*\"" + file.getName() + "\"")) {
                    System.out.println(line);
                    continue;
                }
                pw.println(line);
            }

            pw.flush();
            pw.close();
            bf.close();

            file.delete();

            tempFile.renameTo(file);
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public static void deleteComment() {
        File dir = new File("/home/elowen/桌面/random-testsuite");
        List<File> allFileList = new ArrayList<File>();

        getAllFileList getFileList = new getAllFileList(dir);
        getFileList.getAllFile(dir, allFileList);
        getFileList.compareFileList(allFileList);

        int count = 0;
        for(File file: allFileList) {
            if(!file.getName().endsWith(".c")) {
                continue;
            }
            System.out.println(file.getName());
            System.out.println(++count);
            String command = "cd " + file.getParent() + " && " + "gcc -fpreprocessed -dD -E " + file.getName() + " > "
                    + "/home/elowen/桌面/random-testsuite/" + file.getName();
            ProcessCompiler.processNotKillCompiler(command, 10, "sh", "");
        }
    }
}
