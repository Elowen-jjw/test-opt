package mutate.inline;

import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.VarInform;
import AST_Information.model.AstVariable;
import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import ObjectOperation.structure.FindIfInfo;
import common.before.CommonMutate;
import common.CommonInfoFromFile;
import common.PropertiesInfo;
import common.SideEffort;
import utity.AvailableVariable;
import utity.IfInfo;
import utity.SideEffortInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Inline {
    AstInform_Gen astgen;
    File file;
    List<String> initialFileList;
    List<AvailableVariable> avarList = new ArrayList<>();
    Map<Integer, List<SideEffortInfo>> sevMap;
    int addFunctionLineNumber = 0;

    int mutCount = 0;
    int callCount = 0;

    int pNum = 0;
    //fresh every side effort
    SideEffortInfo currentSev;
    SideEffortInfo lastSev;
    String returnType = "";
    StringBuilder call = new StringBuilder();
    StringBuilder overAllFunctions = new StringBuilder();
    List<String> seCurrentLine = new ArrayList<>();
    List<String> seCallPartList = new ArrayList<>();
    List<String> seDeclarationPartList = new ArrayList<>();
    List<String> lastParamList = new ArrayList<>();
    List<String> seReplacedReturn = new ArrayList<>();
    StringBuilder lastCall = new StringBuilder();

    //if
    String ifCurrentCondition = "";
    List<String> ifReplacedReturn = new ArrayList<>();
    List<String> ifCallPartList = new ArrayList<>();
    List<String> ifDeclarationPartList = new ArrayList<>();

    List<AvailableVariable> structUnionAvarList = new ArrayList<>();
    Pattern pa_var = Pattern.compile("\\&?(" + PropertiesInfo.notHaveBraceVarRegex + "|" + PropertiesInfo.haveBraceVarRegex + ")");

    Map<Integer, IfInfo> ifMap = new HashMap<>();
    Set<Integer> specialLineNumber = new HashSet<>();

    public Inline(File file){
//        dealStack(file);
        CommonMutate cm = new CommonMutate();
        cm.deleteVolatile(file);
        this.file = file;
        this.initialFileList = CommonOperation.genInitialList(file);
        SideEffort se = new SideEffort(file);
        se.getSideEffortVar("stl");
        sevMap = se.sevMap;
        addFunctionLineNumber = CommonInfoFromFile.getFuncStartLine(this.initialFileList);

        //getIf
        FindIfInfo fii = new FindIfInfo();
        List<IfInfo> ifList = fii.findAllIfInfo(initialFileList);
        for(IfInfo ii: ifList){
            ifMap.put(ii.getStartLine(), ii);
        }

        specialLineNumber.addAll(sevMap.keySet());
        specialLineNumber.addAll(ifMap.keySet());

        //get available variables in overall file
        astgen = new AstInform_Gen(file);
        Map<String, AstVariable> varMap = astgen.allVarsMap;
        List<AstVariable> astVarList = new ArrayList<>();
        for(String s: varMap.keySet()){
            astVarList.add(varMap.get(s));
        }

        List<AvailableVariable> allAvarList = VarInform.getInitialAvailableVarList(astVarList, astgen);
        for(AvailableVariable avar: allAvarList) {
            if(avar.getType().contains("union") || avar.getType().contains("struct")){
                structUnionAvarList.add(avar);
            }
            avarList.add(avar);
        }
    }

    public void dealStack(File file){
        FileModify fm = new FileModify();
        List<String> fileList = CommonOperation.genInitialList(file);
        Set<Integer> deleteLines = new HashSet<>();
        for(int i = 0; i < fileList.size(); i++){
            if(fileList.get(i).trim().matches("#pragma pack\\(.*\\)"))
                deleteLines.add(i + 1);
        }
        fm.deleteLinesToFile(file, deleteLines);
    }

    public void run(){
        if(sevMap.size() + ifMap.size() <= 2) return;
        processSideEffort();
        genInlineMutate();
    }

    public void processSideEffort() {
        for(Integer lineNumber: specialLineNumber){
            boolean isSkip = false;
            Map<Integer, Integer> indexMap = new HashMap<>();
            Map<Integer, String> seReplacements = new HashMap<>();
            Map<Integer, String> ifReplacements = new HashMap<>();
            if(sevMap.containsKey(lineNumber)) {
                int sevCount = 0;
                for (SideEffortInfo sev : sevMap.get(lineNumber)) {
                    sevCount++;
                    currentSev = sev;
                    returnType = sev.getLeftType();

                    if (seCurrentLine.isEmpty()) {
                        seCurrentLine.add(sev.getRightValue());
                        seCurrentLine.add("");
                        seCurrentLine.add("");
                        copyList(seReplacedReturn, seCurrentLine, -1);
                    }
                    if (lastSev != null) {
                        try {
                            seCurrentLine.set(0, sev.getRightValue().substring(0, lastSev.getRightStartColumn() - sev.getRightStartColumn()));
                            seCurrentLine.set(2, sev.getRightValue().substring(lastSev.getRightEndColumn() - sev.getRightStartColumn()));
                            seReplacedReturn.set(0, sev.getRightValue().substring(0, lastSev.getRightStartColumn() - sev.getRightStartColumn()));
                            seReplacedReturn.set(2, sev.getRightValue().substring(lastSev.getRightEndColumn() - sev.getRightStartColumn()));
                        } catch (Exception e){
                            isSkip = true;
                        }
                    }
                    if(isSkip){
                        refreshGlobalSeVar();
                        refreshGlobalIfVar();
                        pNum = 0;
                        break;
                    }

                    getAvListInSe();

                    if (!seCallPartList.isEmpty() && !seDeclarationPartList.isEmpty()) {
                        generateSeFunction();
                        seCurrentLine.set(1, call.toString());
                        seReplacedReturn.set(1, lastCall.toString());
                        if (sev.isOutermost()) {//最外层
                            ifCallPartList.addAll(seCallPartList);
                            ifDeclarationPartList.addAll(seDeclarationPartList);

                            indexMap.put(currentSev.getRightStartColumn(), currentSev.getRightEndColumn());
//                            System.out.println("before: -----" + initialFileList.get(lineNumber - 1).trim().substring(currentSev.getRightStartColumn(), currentSev.getRightEndColumn()));
//                            System.out.println("After: -----" + String.join("", seCurrentLine.get(1)));
                            seReplacements.put(currentSev.getRightStartColumn(), String.join("", seCurrentLine.get(1)));
                            ifReplacements.put(currentSev.getRightStartColumn(), String.join("", seReplacedReturn.get(1)));
                            if(sevCount == sevMap.get(lineNumber).size()){
                                if(!ifMap.containsKey(lineNumber)) {
                                    initialFileList.set(lineNumber - 1, replaceSubstrings(initialFileList.get(lineNumber - 1).trim(), indexMap, seReplacements));
                                }
                                else {
                                    //if
                                    seReplacements.replaceAll((key, value) -> "");
                                    String s_if = "\\bif\\s*\\((.*)\\)\\s*\\{";
                                    Pattern pa_if = Pattern.compile(s_if);
                                    Matcher m_if1 = pa_if.matcher(replaceSubstrings(initialFileList.get(lineNumber - 1).trim(), indexMap, ifReplacements));
                                    Matcher m_if2 = pa_if.matcher(replaceSubstrings(initialFileList.get(lineNumber - 1).trim(), indexMap, seReplacements));
                                    if(m_if1.find()){
                                        ifReplacedReturn.add(m_if1.group(1));
                                    }
                                    if(m_if2.find()){
                                        ifCurrentCondition = m_if2.group(1);
                                    }
                                    getAvListInIfCondition();
                                    generateIfFunction();
                                    initialFileList.set(lineNumber - 1, "if(" + call + ") {");
                                }
                                refreshGlobalIfVar();
                                pNum = 0;
                            }
                            refreshGlobalSeVar();
                        }
                    } else if (sev.isOutermost()) {
                        refreshGlobalSeVar();
                        pNum = 0;
                    }
                }
            } else if(specialLineNumber.contains(lineNumber)){//if中没有side effort
                ifCurrentCondition = ifMap.get(lineNumber).getCondition();
                ifReplacedReturn.add(ifMap.get(lineNumber).getCondition());
                getAvListInIfCondition();
                if (!ifCallPartList.isEmpty() && !ifDeclarationPartList.isEmpty()) {
                    generateIfFunction();
                    initialFileList.set(lineNumber - 1, "if(" + call + ") {");
                }
                refreshGlobalIfVar();
                pNum = 0;
            }
        }
    }

    public void refreshGlobalSeVar(){
        seCallPartList.clear();
        seDeclarationPartList.clear();
        lastSev = null;
        lastCall = new StringBuilder();
        lastParamList.clear();
        seCurrentLine.clear();
        call = new StringBuilder();
        seReplacedReturn.clear();
    }

    public void refreshGlobalIfVar(){
        ifCurrentCondition = "";
        ifReplacedReturn.clear();
        ifDeclarationPartList.clear();
        ifCallPartList.clear();
    }

    private String replaceSubstrings(String original, Map<Integer, Integer> indexMap, Map<Integer, String> replacements) {
        // 转换为StringBuilder方便修改
        StringBuilder builder = new StringBuilder(original);

        // 将索引按照从后向前的顺序排序
        indexMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByKey().reversed())
                .forEach(entry -> {
                    int start = entry.getKey();
                    int end = entry.getValue();
//                    System.out.println("origial: " + original.substring(start, end));
                    String replacement = replacements.get(start);
                    if (replacement != null) {
                        builder.replace(start, end, replacement);
                    }
                });

        return builder.toString();
    }

    public void copyList(List<String> newList, List<String> oldList, int exceptIndex){
        newList.clear();
        for(int i = 0; i < oldList.size(); i++){
            if(i == exceptIndex) continue;
            newList.add(new String(oldList.get(i)));
        }
    }

    public void getAvListInIfCondition(){
        generateInfo(ifCurrentCondition, ifReplacedReturn, ifCallPartList, ifDeclarationPartList);
    }

    public void getAvListInSe() {
        for(int i = 0; i < 3; i++) {
            if(i == 1) continue;
            generateInfo(seCurrentLine.get(i), seReplacedReturn, seCallPartList, seDeclarationPartList);
        }
    }

    private void generateInfo(String patternString, List<String> replacedReturn, List<String> callPartList, List<String> declarationPartList){
        Matcher m_var = pa_var.matcher(patternString);
        while (m_var.find()) {
            String varName = m_var.group();//varName[\\w+]
//            System.out.println(varName);
            for (AvailableVariable avar : avarList) {
                if (varName.replaceAll("[\\(\\)\\*\\&]", "").replaceAll(PropertiesInfo.indexPattern, "")
                        .equals(avar.getValue().replaceAll("[\\(\\)\\*]", "").replaceAll("\\[\\d*\\]", ""))) {
                    String varType = avar.getType();
                    Pattern p = Pattern.compile("^(.*)\\.f\\d+$");
                    Matcher m = p.matcher(avar.getValue());
                    String matchedVarName = "";
                    if(m.find()){
                        matchedVarName = m.group(1); //varName[\\d+]
                        varName = varName.substring(0, varName.lastIndexOf(".f"));
                        varType = CommonInfoFromFile.getComplexType(matchedVarName, structUnionAvarList);
                        if(!CommonInfoFromFile.isBit(varType, avar.getValue().substring(m.end(1)).trim().substring(1), astgen)){
                            varName = m_var.group();
                            varType = avar.getType();
                        }
                    }

                    Pattern pa_star = Pattern.compile("(\\*+)");
                    Matcher m_star1 = pa_star.matcher(varName);
                    Matcher m_star2 = pa_star.matcher(avar.getValue());
                    int pointerLevelUse = (m_star1.find() ? m_star1.group().length() : 0);
                    int pointerLevelDeclare = (m_star2.find() ? m_star2.group().length() : 0);
                    int diff = pointerLevelDeclare - pointerLevelUse;
                    pNum++;
                    if (varName.startsWith("&")) {
                        callPartList.add(varName);
                        declarationPartList.add(varType + " " + "*".repeat(diff + 1) + "pa_" + pNum);
                        lastParamList.add("pa_" + pNum);
//                        replacedPartList(replacedReturn, varName, (diff != 0 ? "(" : "") + "*".repeat(diff) + "pa_" + pNum + (diff != 0 ? ")" : ""));
                        replacedPartList(replacedReturn, varName, "pa_" + pNum);
                    } else if (!varName.replace("(", "").startsWith("*")) {
                        if (diff == 0) {
                            if(varName.matches(".*\\.f[0-9]+")){
                                callPartList.add(String.format("&%s", varName));
                            }else {
                                callPartList.add("&" + varName);
                            }
                            declarationPartList.add(varType + " " + "*".repeat(1) + "pa_" + pNum);
                            lastParamList.add("pa_" + pNum);
                            replacedPartList(replacedReturn, varName, "(" + "*".repeat(1) + "pa_" + pNum + ")");
                        } else {
                            callPartList.add(varName);
                            declarationPartList.add(varType + " " + "*".repeat(diff) + "pa_" + pNum);
                            lastParamList.add("pa_" + pNum);
                            replacedPartList(replacedReturn, varName, "pa_" + pNum);
                        }
                    } else {
                        if(varName.matches(".*\\.f[0-9]+")){
                            callPartList.add(String.format("&(%s)", varName.replaceFirst("\\*", "").replaceAll("\\.(?=f\\d+)", "->")));
                            declarationPartList.add(varType + " " + "*".repeat(1) + "pa_" + pNum);
                            lastParamList.add("pa_" + pNum);
                            replacedPartList(replacedReturn, varName, "(*".repeat(1) + "pa_" + pNum + ")");
                        }else {
                            callPartList.add(varName.replaceAll("\\*", ""));
                            declarationPartList.add(varType + " " + "*".repeat(pointerLevelDeclare) + "pa_" + pNum);
                            lastParamList.add("pa_" + pNum);
                            replacedPartList(replacedReturn, varName, "(" + "*".repeat(pointerLevelUse) + "pa_" + pNum + ")");
                        }
                    }
                    break;
                }
            }
        }
    }

    public void replacedPartList(List<String> lists, String oldString, String newString){
        for(int i = 0; i < lists.size(); i++){
            if(i == 1) continue;
            if(oldString.contains("(") || oldString.contains("*") || oldString.contains(")")
                    || oldString.contains("[") || oldString.contains("]") || oldString.contains("&")){
                lists.set(i, lists.get(i).replaceAll(CommonInfoFromFile.replaceRegex(oldString), newString));
            }else {
                lists.set(i, lists.get(i).replaceAll("\\b" + CommonInfoFromFile.replaceRegex(oldString) + "\\b", newString));
            }
        }
    }

    public void generateSeFunction() {
        callCount++;
        call = new StringBuilder("secall_" + callCount + "(");
        lastCall = new StringBuilder("secall_" + callCount + "(");
        StringBuilder function = new StringBuilder("inline __attribute__((always_inline)) " + returnType + " secall_" + callCount + "(");

        if(seCallPartList.size() != seDeclarationPartList.size()){
            System.out.println("!!!ERROR!!!!, the size of declaration is not the same as callPartList");
        }
        for(int i = 0; i < seDeclarationPartList.size(); i++){
            call.append(seCallPartList.get(i));
            lastCall.append(lastParamList.get(i));
            function.append(seDeclarationPartList.get(i));
            if(i != seDeclarationPartList.size() - 1){
                call.append(", ");
                lastCall.append(", ");
                function.append(", ");
            }else{
                call.append(")");
                lastCall.append(")");
                function.append(")");
            }
        }

        function.append(" {\n").append("return ").append(String.join("", seReplacedReturn)).append(";\n}\n");
        overAllFunctions.append("\n").append(function);
        lastSev = currentSev;
    }

    public void generateIfFunction() {
        callCount++;
        call = new StringBuilder("ifcall_" + callCount + "(");
        StringBuilder function = new StringBuilder("inline __attribute__((always_inline)) _Bool ifcall_" + callCount + "(");

        if(ifDeclarationPartList.size() != ifCallPartList.size()){
            System.out.println("!!!ERROR!!!!, the size of declaration is not the same as callPartList");
        }
        for(int i = 0; i < ifDeclarationPartList.size(); i++){
            call.append(ifCallPartList.get(i));
            function.append(ifDeclarationPartList.get(i));
            if(i != ifDeclarationPartList.size() - 1){
                call.append(", ");
                function.append(", ");
            }else{
                call.append(")");
                function.append(")");
            }
        }

        function.append(" {\n").append("return (").append(String.join("", ifReplacedReturn)).append(") ? 1 : 0;\n}\n");
        overAllFunctions.append("\n").append(function);
    }

    public boolean genInlineMutate(){
        boolean isCorrect = false;
        File muDir = new File(PropertiesInfo.mutateInlineDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        if(mutCount == 0) {
            CommonInfoFromFile.cpOriginalFile(muDir, file);
        }

        File muFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_" + (mutCount++) + ".c");

        writeMuMainFile(muFile);

        List<File> files = new ArrayList<>();
        files.add(muFile);

        isCorrect = CommonInfoFromFile.checkCorrection(muFile);
        if(!isCorrect) {
            mutCount--;
            muFile.delete();
            System.out.println(file.getName() + " " + isCorrect);
        }

        return isCorrect;
    }

    public void writeMuMainFile(File muFile){
        checkFileExists(muFile);
        try {
            FileWriter fw = new FileWriter(muFile, true);
            PrintWriter pw = new PrintWriter(fw);
            int count = 0;
            for(String line: initialFileList){
                count++;
                if(count == addFunctionLineNumber){
                    pw.println(overAllFunctions);
                }
                pw.println(line);
            }

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileModify.formatFile(muFile);
    }


    public void checkFileExists(File file){
        if(file.exists()){
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
