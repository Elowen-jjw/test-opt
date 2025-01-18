package common;

import objectoperation.file.getAllFileList;
import overall.OverallProcess;
import sanitizer.SanitizerCheck;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//sanitizer check: true->have error
public class EquivalenceCheck {
    public boolean muDirNotPassed(File muDir){//true -> this muDir didn't pass the check
        List<File> allFileList = new ArrayList<File>();
        getAllFileList getFileList = new getAllFileList(muDir);
        getFileList.getAllFile(muDir, allFileList);
        getFileList.compareFileList(allFileList);
        boolean result = oneSanitizerErrorCheck(allFileList, "undefined,address");
//                || oneSanitizerErrorCheck(allFileList, "memory");
        System.out.println("muDirNotPassed in undefined and address: " + result);
        return result;
    }

    public boolean memoryNotPassed(File muDir){//true -> this muDir didn't pass the check
        List<File> allFileList = new ArrayList<File>();
        getAllFileList getFileList = new getAllFileList(muDir);
        getFileList.getAllFile(muDir, allFileList);
        getFileList.compareFileList(allFileList);
        boolean result = oneSanitizerErrorCheck(allFileList, "memory");
        System.out.println("muDirNotPassed in memory: " + result);
        return result;
    }

    public boolean oneSanitizerErrorCheck(List<File> muFileList, String sanitizerType){//true -> have sanitizer error or checksum inconsistent in -O0
        Set<String> checksumSet = new HashSet<>();
        for(File file: muFileList) {
            if (!file.getName().endsWith(".c")) {
                continue;
            }

            SanitizerCheck sc = new SanitizerCheck(OverallProcess.commandType);
            String result = "";
            if(sanitizerType.equals("undefined,address")) {
                result = sc.filterUBAndAddress(file);
            }
            else if(sanitizerType.equals("memory")) {
                result = sc.filterMemory(file);
            }
            System.out.println(sanitizerType + ":" + result);
            if(result.equals("timeout") || result.equals("error")) {
                return true;
            }
            else if(result.contains("checksum")){
                checksumSet.add(result.replace("checksum", "").replace("=", "").trim());
            }
        }
        return checksumSet.size() != 1;
    }
}
