package ObjectOperation.list;

import common.ExceptionCheck;
import ObjectOperation.file.FileModify;
import utity.AvailableVariable;
import utity.AvarExecTimes;

import java.io.File;
import java.util.*;

public class RandomAndCheck {
    public List<AvailableVariable> getAvailableVarList(File file, List<AvailableVariable> var_value_type, int lineNumber){//lineNumber locates the next line of header
        List<AvailableVariable> availableVar = new ArrayList<>();
        for(AvailableVariable av: var_value_type){
            String value = av.getValue();
            String type = av.getType();

            FileModify fm = new FileModify();
            File tempFile = new File(file.getParent() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_temp.c");
            fm.copyFile(tempFile, file);

            Map<Integer, List<String>> addLines = new HashMap<>();
            List<String> addList = new ArrayList<>();
            addList.add(type + " temp_100 = " + value + ";");
            addLines.put(lineNumber, addList);

            fm.addLinesToFile(tempFile, addLines, true);
            ExceptionCheck ec = new ExceptionCheck();

            if(ec.filterUB(tempFile)){
                availableVar.add(av);
            }
            tempFile.delete();
        }
        return availableVar;
    }


    public List<AvailableVariable> getRandomAvailableVarNotChange(List<AvailableVariable> varList, int number){
        List<AvailableVariable> newValueList = new ArrayList<>(varList);
        return getAvailableVariables(newValueList, number);
    }

    public List<AvailableVariable> getRandomAvailableVarChange(List<AvailableVariable> varList, int number){
        return getAvailableVariables(varList, number);
    }

    private List<AvailableVariable> getAvailableVariables(List<AvailableVariable> varList, int number) {
        List<AvailableVariable> chosenVarList = new ArrayList<>();
        Random random = new Random();
        for(int i=0; i<number; i++){
            int ranIndex = random.nextInt(varList.size());
            chosenVarList.add(varList.get(ranIndex));
            varList.remove(ranIndex);
        }
        return chosenVarList;
    }

    public List<AvarExecTimes> getAvarExec(List<AvarExecTimes> varList, int number) {
        List<AvarExecTimes> chosenVarList = new ArrayList<>();
        Random random = new Random();
        for(int i=0; i<number; i++){
            int ranIndex = random.nextInt(varList.size());
            chosenVarList.add(varList.get(ranIndex));
            varList.remove(ranIndex);
        }
        return chosenVarList;
    }

}
