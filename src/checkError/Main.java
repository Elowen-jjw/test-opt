package checkError;

import java.io.File;
import java.util.*;

import ObjectOperation.file.getAllFileList;
import ObjectOperation.structure.StructureTransform;

public class Main {
    public static void main(String args[]){
        CheckTerminal ct = new CheckTerminal();
        ct.checkErrorFile("/home/elowen/桌面/CompilerMutation/Unswitching/TwoVar", "gcc");
        System.out.println("end");
    	 File sourceDir = new File("/home/elowen/extensionA/CompilerMutation/Unswitching");
    	 List<File> allFileList = new ArrayList<File>();

         getAllFileList getFileList = new getAllFileList(sourceDir);
         getFileList.getAllFile(sourceDir, allFileList);
         getFileList.compareFileList(allFileList);
         for(File file: allFileList) {
        	 if(!file.getName().endsWith(".c")) {
        		 continue;
        	 }
        	 StructureTransform.formatFile(file);
         }
    }
}
