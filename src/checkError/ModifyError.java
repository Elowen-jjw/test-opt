package checkError;

import ObjectOperation.file.FileModify;
import ObjectOperation.file.getAllFileList;
import ObjectOperation.list.CommonOperation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyError {
    public static void main(String args[]){
        File sourceDir = new File("/home/elowen/桌面/CompilerMutation/Fusion");

        List<File> allFileList = new ArrayList<File>();
        getAllFileList getFileList = new getAllFileList(sourceDir);
        getFileList.getAllFile(sourceDir, allFileList);
        getFileList.compareFileList(allFileList);

        int fineCnt = 0;
        for(File file: allFileList) {
            if(!file.getName().endsWith(".c")) {
                continue;
            }
            System.out.println(fineCnt ++ + ": " + file.getName());
            update(file);
        }
        System.out.println("end!");
    }

    public static void update(File file){
        List<String> initialFileList = CommonOperation.genInitialList(file);
        Map<Integer, List<String>> addLines = new HashMap<Integer, List<String>>();

        for(int i=0; i<initialFileList.size(); i++){
            String line = initialFileList.get(i);
            if(line.trim().equals("int main(void) {")){
                if(!initialFileList.get(i + 1).contains("int i")){
                    List<String> lines = new ArrayList<>();
                    lines.add("int i;");
                    addLines.put(i+1, lines);
                }
                break;
            }
        }

        if(!addLines.isEmpty()){
            FileModify fm = new FileModify();
            fm.addLinesToFile(file, addLines, true);
            System.out.println(file.getAbsolutePath());
        }
    }
}
