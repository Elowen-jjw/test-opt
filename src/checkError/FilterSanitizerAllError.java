package checkError;

import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilterSanitizerAllError {
    public static void main(String args[]){
        deleteShift();
        System.out.println("end");
    }

    public static void deleteShift(){
        File file = new File("/home/elowen/桌面/sanitizer-error.txt");
        FileModify fm = new FileModify();
        List<String> initialList = fm.readFile(file);
        List<String> endList = new ArrayList<String>();
        List<String> blockList = new ArrayList<>();
        System.out.println(initialList.size());

        for(int i = 0; i < initialList.size(); i++){
            if(initialList.get(i).trim().isEmpty()
                    && initialList.get(i+1).trim().isEmpty()
                    && initialList.get(i+2).trim().isEmpty()
                    && initialList.get(i+3).trim().isEmpty()
            ){
                List<String> copiedBlockList = new ArrayList<>();
                CommonOperation.copyStringList(copiedBlockList, blockList);
                for(int j = 0; j < blockList.size(); j++){
                    String line = blockList.get(j);
                    if(line.contains("error:")){
                        if(line.contains("shift")
                                || line.contains("load of null pointer")
                                || line.contains("store to null pointer")
                                || line.contains("division")
                                || (line.contains("index") && line.contains("out of bounds"))
                        ){
                            copiedBlockList.remove(line);
                        }
                    }
                }
                if(copiedBlockList.size() > 2) {
                    endList.addAll(copiedBlockList);
                    endList.add("\n\n\n");
                }
                else{
                    CommonOperation.printList(blockList);
                }
                blockList.clear();
                i+=3;
                continue;
            }
            blockList.add(initialList.get(i));
        }
        System.out.println(endList.size());
        fm.writeFile(file, endList);
    }
}
