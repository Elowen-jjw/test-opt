package performance;

import ObjectOperation.file.getAllFileList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DirIterator {
    public void iteratorMutations(File outestDir){
        List<File> allFileList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(outestDir.listFiles())));
        getAllFileList getFileList = new getAllFileList();
        getFileList.compareFileList(allFileList);

        for(File subDir: allFileList){
            if(subDir.isDirectory()) {
            	if(subDir.getName().contains("Fusion")){
            		continue;
            	}
                if (subDir.getName().contains("mutation_")) {
                	System.out.println(subDir.getAbsolutePath());
                    CompilerProcess cp = new CompilerProcess();
                    cp.genAllResult(subDir);
                }
                else{
                    iteratorMutations(subDir);
                }
            }
        }
    }
}
