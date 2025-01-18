package objectoperation.list;

import objectoperation.file.FileModify;
import overall.OverallProcess;
import processmemory.ProcessCompiler;
import processmemory.ProcessTerminal;
import utity.AvailableVariable;
import utity.AvarExecTimes;

import java.io.File;
import java.util.*;

public class RandomAndCheck {

    //if it had segmentation fault, it would return false;
    public boolean checkSegmentation(File file){
        String aoutFilename = file.getName().substring(0, file.getName().lastIndexOf(".c"));

        String command = (OverallProcess.commandType.equals("gcc")? "export LANGUAGE=en && export LANG=en_US.UTF-8 && ": "")
                + "cd " + file.getParent() + " && " + OverallProcess.commandType + " " + file.getName()
                + " -lm -I $CSMITH_HOME/include -o " + aoutFilename;
        ProcessTerminal pt = new ProcessTerminal();
        pt.processThreadNotLimitJustExec(command, "sh");

        File aoutFile = new File(file.getParent() + "/" + aoutFilename);
        if(!aoutFile.exists()){
            return false;
        }

        command = "cd " + file.getParent() + " && " + "./" + aoutFilename;

        List<String> execLines = ProcessCompiler.processNotKillCompiler(command, 30, "sh", aoutFilename);

        deleteAoutFile(file, aoutFilename);

        for(String s: execLines) {
            if(s.contains("Segmentation fault (core dumped)")) {
                return false;
            }
        }
        return true;
    }

    public void deleteAoutFile(File file, String aoutFilename){
        File outFile = new File(file.getParent() + "/" + aoutFilename);
        if(outFile.exists()){
            outFile.delete();
        }
    }

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
            if(checkSegmentation(tempFile)){
                availableVar.add(av);
            }
            tempFile.delete();

        }
        return availableVar;
    }


    public List<AvailableVariable> getRandomAvailableVarNotChange(List<AvailableVariable> varList, int number){
        List<AvailableVariable> newValueList = new ArrayList<>();
        CommonOperation.copyAvaiableVarList(newValueList, varList);
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
