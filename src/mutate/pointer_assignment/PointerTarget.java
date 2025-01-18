package mutate.pointer_assignment;

import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.VarInform;
import AST_Information.model.AstVariable;
import AST_Information.model.FunctionBlock;
import AST_Information.model.LoopStatement;
import ObjectOperation.datatype.Data;
import ObjectOperation.datatype.StringOperation;
import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import ObjectOperation.structure.FindIfInfo;
import common.CommonInfoFromFile;
import common.PropertiesInfo;
import mutate.minmax.WriteMutate;
import mutate.pattern.CMPVariable;
import processtimer.ProcessTerminal;
import utity.AvailableVariable;
import utity.IfInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ObjectOperation.datatype.StringOperation.*;
import static common.before.LoopExecValues.deleteAoutFile;

public class PointerTarget {
    AstInform_Gen astgen;
    List<AvailableVariable> avarList = new ArrayList<>();
    Map<Integer, Integer> loopMap = new HashMap<>();
    List<IfInfo> ifList = new ArrayList<>();
    Map<String, AstVariable> varMap;
    List<FunctionBlock> FuncfunctionBlockList;
    Set<AstVariable> cmpVarSet = new HashSet<>();
    List<AvailableVariable> cmpAvarList = new ArrayList<>();
    File file;
    File muDir;
    String sanitizerCompiler;

    List<String> initialFileList;
    List<String> liftedVarToGobal = new ArrayList<>();

    Map<String, String> createdGpVar = new HashMap<>();
    Map<Integer, Map<Boolean, List<String>>> ifStmtData = new HashMap<>();
    Set<Integer> checkedLineNumber = new HashSet<>();
    Map<Integer, String> newLineMap = new HashMap<>();

    int gpCnt = 0;

    int mutCount = 0;
    int correntCount = 0;
    int currentLineNumber = 0;
    FunctionBlock currentBlock;

    Set<Integer> declareLineSet = new HashSet<>();
    Set<Integer> deleteDeclaration = new HashSet<>();

    Map<Integer, List<String>> addedRightVarPrintfMap = new HashMap<>();
    Map<Integer, Map<Boolean, Set<String>>> rightValuesMap = new HashMap<>();

    Map<Integer, Set<String>> conditionMap = new HashMap<>(); //每一行可以添加的condition
    List<Map<Integer, String>> allIfStmtCom = new ArrayList<>();

    public PointerTarget(File file, String sanitizerCompiler){
        definedformat(file);
        this.file = file;
        this.sanitizerCompiler = sanitizerCompiler;
        initialFileList = CommonOperation.genInitialList(file);
        ifList = new FindIfInfo().findAllIfInfo(initialFileList);
        astgen = new AstInform_Gen(file);
        varMap = astgen.allVarsMap;
        this.FuncfunctionBlockList = astgen.getAllFunctionBlocks();
        getLoopMap();
        getAvarList();
        getAllCMPVar();
        List<AvailableVariable> overallCmpAvar = VarInform.getInitialAvailableVarList(new ArrayList<>(cmpVarSet), astgen);
        cmpAvarList = VarInform.removeStructAndUnionOverall(overallCmpAvar);
        createMuDir();
    }

    public static void main(String[] args) {
//        for(File file: new File(PropertiesInfo.testSuiteStructDir).listFiles()) {
            File file = new File(PropertiesInfo.testSuiteStructDir + "/random16265.c");
            System.out.println(file.getName());
            File genMuDir = new File(PropertiesInfo.mutatePointerTarget + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
            if (genMuDir.exists()) {
                FileModify fm = new FileModify();
                fm.deleteFolder(genMuDir);
//                continue;
            }
            PointerTarget pt = new PointerTarget(file, "clang");
            pt.run();
//        }
    }

    public void run(){
//        int count = 0;
//        while(count++ < 5){
//            if(runPointerTarget()) break;
//        }
        runPointerTarget();
    }

    public void runPointerTarget(){
        int count = 0;
        for(String s: initialFileList){
            count++;
            currentLineNumber = count;
            getCurrentFB();
            if(s.trim().equals("int main(void) {")) break;
            if(declareLineSet.contains(count)) continue;
            if(loopMap.containsKey(count)) continue;
            Map<Integer, String> interInfo = new TreeMap<>();
            findPattern(s, interInfo, 0);
            if(!interInfo.isEmpty()) {
                String newLine = "";
                for (int i = 0; i < s.length(); i++) {
                    if (interInfo.containsKey(i)) {
                        newLine += interInfo.get(i);
                    }
                    newLine += s.charAt(i);
                }
                newLineMap.put(count, newLine);
//                initialFileList.set(count - 1, newLine);
                checkedLineNumber.add(count);
            }
        }

        getRightVarValuesMap();
        getAddedConditions();

        for(Map<Integer, String> ifStmtMap: allIfStmtCom){
            if(correntCount > 499) break;
            if(mutCount > 200 && correntCount < 2) break;
            boolean isSuccess = genMutate(ifStmtMap);
            System.out.println(file.getName() + " " + isSuccess);
        }
    }

    public void getRightVarValuesMap(){
        File printfRightVarFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_printf_RVar.c");
        writeComparablePrintfFile(printfRightVarFile, addedRightVarPrintfMap);
        List<String> rightVarExecLines = runPrintFile(printfRightVarFile);
        if(rightVarExecLines == null) System.out.println("cmp printf file generation has errors.....");
        else {
            rightValuesMap = analysisComparableExecLines(rightVarExecLines);
            if (rightValuesMap == null) System.out.println("cmp printf file generation core dumped.....");
            else if (rightValuesMap.isEmpty())
                System.out.println(file.getName() + "cmp printf file generation does not have print!!!!");
        }
    }

    public void getAddedConditions(){
        if(rightValuesMap == null || rightValuesMap.isEmpty()) return;
        for (Integer i : rightValuesMap.keySet()) {
            // 检查当前key对应的值是否为null或为空，跳过空的值
            if (rightValuesMap.get(i) == null || rightValuesMap.get(i).isEmpty()) continue;

            Set<String> trueList = rightValuesMap.get(i).get(true);
            Set<String> falseList = rightValuesMap.get(i).get(false);

            if(trueList != null && !trueList.isEmpty()){
                for(String s: trueList){
                    if (conditionMap.containsKey(i)) {
                        conditionMap.get(i).add("if (" + s + ") {");
                    } else {
                        conditionMap.put(i, new HashSet<>(Arrays.asList("if (" + s + ") {")));
                    }
                }
            }

            if(falseList != null && !falseList.isEmpty()){
                for(String s: falseList){
                    if (conditionMap.containsKey(i)) {
                        conditionMap.get(i).add("if (!" + s + ") {");
                    } else {
                        conditionMap.put(i, new HashSet<>(Arrays.asList("if (!" + s + ") {")));
                    }
                }
            }
            // 打印调试信息
//            System.out.println(i + " " + "true" + " " + (trueList != null ? trueList : "null"));
//            System.out.println(i + " " + "false" + " " + (falseList != null ? falseList : "null"));
        }
        allIfStmtCom = GenComposition.getCombinations(conditionMap, 600);
//        for(Map<Integer, String> map: allIfStmtCom) {
//            System.out.println(map);
//        }
    }

    public void findPattern(String input, Map<Integer, String> insertInfo, int baseIndex){
        Pattern opPattern = Pattern.compile("([\\w\\*\\(\\)\\[\\]\\.]+)(\\s+=\\s+)(.*)");
        Matcher matcher = opPattern.matcher(input);

        if (matcher.find()) {
            String leftValue = matcher.group(1);
            String rightValue = matcher.group(3);

            String truncatedLeft = truncateFromRight(leftValue);
            String truncatedRight = truncateFromLeft(rightValue);

            int leftEndColumn = baseIndex + matcher.end(1);
            int leftStartColumn = leftEndColumn - truncatedLeft.length();
            int rightStartColumn = leftEndColumn + matcher.group(2).length();
            int rightEndColumn = rightStartColumn + truncatedRight.length();

            findPattern(rightValue, insertInfo, rightStartColumn);

            AstVariable astVar = getLeftPartVar(truncatedLeft);
            if(astVar != null){
//                System.out.printf("左值:%s 右值:%s%n", truncatedLeft, truncatedRight);
//                System.out.println("input: " + input);
//                    System.out.printf("左值:%s 右值:%s%n",
//                            initialFileList.get(currentLineNumber - 1).substring(leftStartColumn, leftEndColumn),
//                            initialFileList.get(currentLineNumber - 1).substring(rightStartColumn, rightEndColumn));
//                System.out.println("left: " + astVar.getName() + " " + astVar.getType());

                //add cmp printf
                addCMPPrintfToMap(truncatedRight);

                String pointerType = astVar.getType().replaceAll("(\\*)|(\\[(.*?)\\])", "")
                        + "*".repeat(StringOperation.countStars(astVar.getType()) - StringOperation.countStars(truncatedLeft));
                String intermediateVar = "";

                //在所有未使用的变量中，先考虑全局变量再考虑局部变量，都没有再创建全局变量
                Map<Integer, List<AstVariable>> notUsedAstVarMap = getAstVarListFromType(pointerType);
                AstVariable randomAstVar = null;
                boolean isHaveVar = true;
//                if(!notUsedAstVarMap.get(0).isEmpty()) randomAstVar = notUsedAstVarMap.get(0).get(new Random().nextInt(notUsedAstVarMap.get(0).size()));
                if(!notUsedAstVarMap.get(1).isEmpty()) randomAstVar = notUsedAstVarMap.get(1).get(new Random().nextInt(notUsedAstVarMap.get(1).size()));
                else if(!notUsedAstVarMap.get(2).isEmpty()) randomAstVar = notUsedAstVarMap.get(2).get(new Random().nextInt(notUsedAstVarMap.get(2).size()));
                else isHaveVar = false;

                if(isHaveVar) {
                    intermediateVar = randomAstVar.getName();
                    String line = initialFileList.get(randomAstVar.getDeclareLine() - 1);
                    if (randomAstVar.getName().contains("l_")) {
                        if(line.contains("=")){
                            liftedVarToGobal.add(line.substring(0, line.indexOf("=")).replaceAll("(const)|(volatile)", "") + ";");
                        } else {
                            liftedVarToGobal.add(line);
                        }
                        deleteDeclaration.add(randomAstVar.getDeclareLine());
                    } else if(randomAstVar.getName().contains("p_")){
                        System.out.println("intermediate var is function param...");
                    } else {
                        System.out.println("intermediate var is global...");
                        if(line.contains("=")) {
                            newLineMap.put(randomAstVar.getDeclareLine(), line.substring(0, line.indexOf("=")).replaceAll("(const)|(volatile)", "") + ";");
//                            initialFileList.set(randomAstVar.getDeclareLine() - 1, line.substring(0, line.indexOf("=")).replaceAll("(const)|(volatile)", "") + ";");
                        }
                    }
                } else {
                    if(!createdGpVar.containsKey(pointerType)){
                        createdGpVar.put(pointerType, "gp_" + gpCnt++);
                    }
                    intermediateVar = createdGpVar.get(pointerType);
                    liftedVarToGobal.add(pointerType + " " + intermediateVar + ";");
                }
                System.out.println(intermediateVar + " = " + truncatedLeft + " = " + truncatedRight);
                insertInfo.put(leftStartColumn, intermediateVar + " = ");
            }
        }
    }

    public AstVariable getLeftPartVar(String leftValue){
        for(String s: varMap.keySet()){
            AstVariable astVar = varMap.get(s);
            if(!astVar.getType().contains("*")) continue;
            if(containsVarName(leftValue, astVar.getName())){
                if(StringOperation.countStars(astVar.getType()) - StringOperation.countStars(leftValue) != 0)
                    return astVar;
            }
        }
        return null;
    }

    public void addCMPPrintfToMap(String input){
        Collections.shuffle(avarList);
        for(AvailableVariable av: avarList){
            if(av.getValue().trim().matches("[ijklmn]")) continue;
            if(StringOperation.containsVarName(input, av.getValue().replaceAll("(\\*)|(\\[(.*?)\\])|(\\.f\\d+)|\\(|\\)", ""))){
//                System.out.println("yes, contains var:" + av.getValue());
                if(addedRightVarPrintfMap.containsKey(currentLineNumber)){
                    addedRightVarPrintfMap.get(currentLineNumber).add(Data.addPrintf(currentLineNumber, av.getType(), av.getValue(), av.getValue()));
                } else {
                    addedRightVarPrintfMap.put(currentLineNumber, new ArrayList<>(
                            Arrays.asList(Data.addPrintf(currentLineNumber, av.getType(), av.getValue(), av.getValue()))));
                }
            }
        }
    }

    //check whether inputs contains function param
    public boolean isContainsParam(String input){
        for(String s: varMap.keySet()){
            AstVariable astVar = varMap.get(s);
            if(!astVar.getName().contains("p_")) continue;
            if(containsVarName(input, astVar.getName())){
                return true;
            }
        }
        return false;
    }

    public Map<Integer, List<AstVariable>> getAstVarListFromType(String varType) {
        Map<Integer, List<AstVariable>> astVarMap = new HashMap<>();
        astVarMap.put(0, new ArrayList<>()); //p_
        astVarMap.put(1, new ArrayList<>()); //g_
        astVarMap.put(2, new ArrayList<>()); //l_

        for (String s : varMap.keySet()) {
            AstVariable astVar = varMap.get(s);
            if (astVar.getType().equals(varType) && !astVar.getIsUsed()) {
                if (astVar.getName().startsWith("p_")) {
                    if(astVar.getDeclareLine() == currentBlock.startline) {
                        astVarMap.get(0).add(astVar);
                    }
                } else {
                    if(astVar.getName().contains("g_")) {
                        astVarMap.get(1).add(astVar);
                    } else if(astVar.getName().contains("g_")) {
                        astVarMap.get(2).add(astVar);
                    }
                }
            }
        }
        return astVarMap;
    }

//    public void addIfStmt(Set<Integer> runNumberSet){
//        Map<Integer, List<String>> addedPVarPrintfMap = new HashMap<>();//记录在initial file第几行添加的p_var printf语句
//        for(Integer i: runNumberSet){
//            addedPVarPrintfMap.put(i, getPVarPrintfList(i)); //如果被执行了在这一行添加该func的printf参数
//        }
//
//        //生成输出文件并且运行得到execlines
//        File printfPVarFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_printf_pVar.c");
//        writeComparablePrintfFile(printfPVarFile, addedPVarPrintfMap);
//        List<String> pVarExecLines = new MinMax().runPrintFile(printfPVarFile);
//        if(pVarExecLines == null) System.out.println("pVar printf file generation has errors.....");
//
//        //分析输出文件得到在每一个safe_math调用位置所有func参数的值
//        Map<Integer, Map<Boolean, List<String>>> pVarValueMap = analysisComparableExecLines(pVarExecLines);
//        if(pVarValueMap == null) System.out.println("pVar printf file generation core dumped.....");
//        else if(pVarValueMap.isEmpty()) System.out.println(file.getName() + "pVar printf file generation does not have print!!!!");
//        else {
//            mergeMaps(ifStmtData, pVarValueMap);
//        }
//    }

//    public void setSafeMathComparableLoopIndex(Set<Integer> runNumberSet){
//        Map<Integer, List<String>> addedLoopIndexPrintfMap = new HashMap<>();//记录在initial file第几行添加的p_var printf语句
//        for (Integer i : runNumberSet) {
//            for(Integer startLine: loopMap.keySet()){
//                int endLine = loopMap.get(startLine);
//                if(i > startLine && i < endLine){
//                    String line = initialFileList.get(startLine - 1);
//                    Matcher m = Pattern.compile(PropertiesInfo.forStmtPattern).matcher(line);
//                    if (m.find()) {
//                        String loopIndex = m.group(1);
//                        if(!loopIndex.matches("[ijklmn]")) {
//                            String loopIndexType = getAvarType(loopIndex);
//                            addedLoopIndexPrintfMap.put(i, new ArrayList<>(Arrays.asList(Data.addPrintf(i, loopIndexType, loopIndex, loopIndex))));
//                        }
//                    }
//                }
//            }
//        }
//
//        //生成输出文件并且运行得到execlines
//        File printfLoopIndexFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_printf_loopIndex.c");
//        writeComparablePrintfFile(printfLoopIndexFile, addedLoopIndexPrintfMap);
//        List<String> loopIndexExecLines = runPrintFile(printfLoopIndexFile);
//        if(loopIndexExecLines == null) System.out.println("loop index printf file generation has errors.....");
//
//        //分析输出文件得到在每一个safe_math调用位置所有func参数的值
//        Map<Integer, Map<Boolean, List<String>>> loopIndexValueMap = analysisComparableExecLines(loopIndexExecLines);
//        if(loopIndexValueMap == null) System.out.println("loop index printf file generation core dumped.....");
//        else if(loopIndexValueMap.isEmpty()) System.out.println(file.getName() + "loop index printf file generation does not have print!!!!");
//        else {
//            mergeMaps(ifStmtData, loopIndexValueMap);
//        }
//    }

    public Map<Integer, Map<Boolean, Set<String>>> analysisComparableExecLines(List<String> execLines){
        Map<Integer, Map<Boolean, Set<String>>> pVarValueMap = new HashMap<>();
        Map<String, Integer> removeNullPointer = new HashMap<>();
        for (String s : execLines) {
            if(s.contains("core dumped") || s.contains("error: ")) return null;
            Matcher m = Pattern.compile("(\\d+)\\s*:\\s*(.+)\\s*@\\s*(.*)").matcher(s);
            if(m.find()){
                int lineNumber = Integer.parseInt(m.group(1).trim());
                String varName = m.group(2).trim();
                String values = m.group(3).trim();
                if(values.equals("NULL")){
                    removeNullPointer.put(varName, lineNumber);
                    System.out.println(lineNumber + " " + varName);
                    continue;
                }
                System.out.println("not " + lineNumber + " " + varName + " " + values);
//                System.out.println("ssssssssssssssss" + varName + " " + getAvarType(varName) + " " + m.group(3).trim());
                BigInteger decimalValue = StringOperation.convertToDecimal(getAvarType(varName), m.group(3).trim());
                if (decimalValue != null) {
                    Boolean key = decimalValue.compareTo(BigInteger.ZERO) != 0;
                    if (pVarValueMap.containsKey(lineNumber)) {
                        if (pVarValueMap.get(lineNumber).containsKey(key)) {
                            pVarValueMap.get(lineNumber).get(key).add(varName);
                        } else {
                            pVarValueMap.get(lineNumber).put(key, new HashSet<>(Arrays.asList(varName)));
                        }
                    } else {
                        pVarValueMap.put(lineNumber, new HashMap<>() {{
                            put(key, new HashSet<>(Arrays.asList(varName)));
                        }});
                    }
                }
            }
        }

        //只要有一次输出为null就不要
        for (Map.Entry<String, Integer> entry : removeNullPointer.entrySet()) {
            String stringToRemove = entry.getKey();  // 要删除的字符串
            Integer keyInPVarValueMap = entry.getValue();  // 对应 pVarValueMap 的键
            System.out.println("has " + stringToRemove + " " + keyInPVarValueMap);

            // 获取 pVarValueMap 中对应的 Map<Boolean, Set<String>>
            Map<Boolean, Set<String>> boolMap = pVarValueMap.get(keyInPVarValueMap);
            if (boolMap != null) {
                // 遍历这个 Map 中所有的 Set<String>，删除匹配的元素
                for (Map.Entry<Boolean, Set<String>> boolEntry : boolMap.entrySet()) {
                    Set<String> stringSet = boolEntry.getValue();
                    System.out.println("all string in line is " + stringSet);
                    if (stringSet.remove(stringToRemove)) {
                        System.out.println("Removed " + stringToRemove + " from Set under key " + keyInPVarValueMap);
                    }
                }
            }
        }

        return pVarValueMap;
    }

    public void mergeMaps(Map<Integer, Map<Boolean, List<String>>> existingMap,
                          Map<Integer, Map<Boolean, List<String>>> smallMap) {

        // 遍历loopIndexValueMap的所有键
        for (Map.Entry<Integer, Map<Boolean, List<String>>> outerEntry : smallMap.entrySet()) {
            Integer outerKey = outerEntry.getKey();
            Map<Boolean, List<String>> innerMap = outerEntry.getValue();

            // 如果existingMap中没有该outerKey，则创建一个新的innerMap并放入existingMap
            if (!existingMap.containsKey(outerKey)) {
                existingMap.put(outerKey, new HashMap<>());
            }

            // 获取existingMap中的innerMap
            Map<Boolean, List<String>> existingInnerMap = existingMap.get(outerKey);

            // 遍历当前outerKey下的所有innerMap
            for (Map.Entry<Boolean, List<String>> innerEntry : innerMap.entrySet()) {
                Boolean innerKey = innerEntry.getKey();
                List<String> newList = innerEntry.getValue();

                // 如果existingInnerMap中没有该innerKey，则创建一个新的set并放入existingInnerMap
                if (!existingInnerMap.containsKey(innerKey)) {
                    existingInnerMap.put(innerKey, new ArrayList<>());
                }

                // 获取existingInnerMap中的set
                List<String> existingSet = existingInnerMap.get(innerKey);

                // 将新的set中的元素合并到existingSet中
                if (newList != null) {
                    existingSet.addAll(newList);
                }
            }
        }
    }

    public List<String> getPVarPrintfList(int lineNumber){
        List<String> pVarPrintfList = new ArrayList<>();
        for(FunctionBlock fb: FuncfunctionBlockList){
            if(lineNumber > fb.startline && lineNumber < fb.endline){
                List<AvailableVariable> pVarAvList = getPVarAvList(fb.startline);
                for(AvailableVariable param: pVarAvList){
                    pVarPrintfList.add(Data.addPrintf(lineNumber, param.getType(), param.getValue(), param.getValue()));
                }
                break;
            }
        }
        return pVarPrintfList;
    }

    public List<AvailableVariable> getPVarAvList(int declareLine){
        List<AstVariable> pAstList = new ArrayList<>();
        for(String s: varMap.keySet()){
            if(varMap.get(s).getName().startsWith("p_") && varMap.get(s).getDeclareLine() == declareLine){
                pAstList.add(varMap.get(s));
            }
        }
        return VarInform.removeStructAndUnionOverall(VarInform.getInitialAvailableVarList(pAstList, astgen));
    }


    public void writeComparablePrintfFile(File printfFile, Map<Integer, List<String>> printfMap) {
        try {
            FileWriter fw = new FileWriter(printfFile);
            PrintWriter pw = new PrintWriter(fw);

            int count = 0;
            for(String line: initialFileList){
                count++;
                if(printfMap.containsKey(count) && !printfMap.get(count).isEmpty()){
                    printfMap.get(count).forEach(pw::println);
                    pw.println(line);
                    continue;
                }
                pw.println(line);
            }

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

            FileModify.formatFile(printfFile);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> runPrintFile(File printfFile){
        String aoutFilename = printfFile.getName().substring(0, printfFile.getName().lastIndexOf(".c"))
                .replace("random", "").replace("printf", "pf");

        String command = "export LANGUAGE=en && export LANG=en_US.UTF-8 && "
                + "cd " + printfFile.getParent() + " && " + sanitizerCompiler + " " + printfFile.getName()
                + " -lm -I $CSMITH_HOME/include -o " + aoutFilename;
        ProcessTerminal.voidNotMemCheck(command, "sh");
        File aoutFile = new File(printfFile.getParent() + "/" + aoutFilename);
        if(!aoutFile.exists()){
            return null;
        }

        command = "cd " + printfFile.getParent() + " && " + "./" + aoutFilename;
        List<String> execLines = ProcessTerminal.listMemCheck(command, 10, "sh", false, true, new ArrayList<>(Arrays.asList(aoutFilename)));
        deleteAoutFile(printfFile, aoutFilename);
        return execLines;
    }

    public String getAvarType(String avarName){
        for(AvailableVariable av: avarList){
            if(av.getValue().equals(avarName))
                return av.getType();
        }
        return "int32_t";
    }

    public boolean isCMPVar(String varName){
        for(AstVariable cmpVar: cmpVarSet){
            if(cmpVar.getName().equals(varName))
                return true;
        }
        return false;
    }

    public boolean genMutate(Map<Integer, String> ifStmtMap){
        boolean isCorrect = false;
        File muDir = new File(PropertiesInfo.mutatePointerTarget + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        File muFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_pt_" + (mutCount++) + ".c");
        writeMuFile(muFile, ifStmtMap);

        File initFile = new File(muDir.getAbsolutePath() + "/" + file.getName());
        String initialResult = new WriteMutate(sanitizerCompiler).getFileExecResult(initFile);
        if(!initialResult.contains("checksum")) isCorrect = false;
        else {
            String result = new WriteMutate(sanitizerCompiler).getFileExecResult(muFile);
            if (result.contains("error") || result.equals("timeout")) isCorrect = false;
            else isCorrect = result.equals(initialResult);
        }

        if(!isCorrect) {
            File errorsDir = new File(PropertiesInfo.indexDir + "/pointer-target-errors/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
            if(!errorsDir.exists()){
                errorsDir.mkdirs();
            }
            ProcessTerminal.voidNotMemCheck("cp " + muFile.getAbsolutePath() +  " " + errorsDir.getAbsolutePath(), "sh");
            muFile.delete();
        } else {
            correntCount++;
        }

        return isCorrect;
    }

    public void writeMuFile(File muFile, Map<Integer, String> ifStmtMap){
        checkFileExists(muFile);
        try {
            FileWriter fw = new FileWriter(muFile, true);
            PrintWriter pw = new PrintWriter(fw);
            int count = 0;
            Set<Integer> addedIfBraceSet = new HashSet<>();
            for(String line: initialFileList){
                count++;
                if(addedIfBraceSet.contains(count)){
                    pw.println("}");
                    addedIfBraceSet.remove(count);
                }
                if(deleteDeclaration.contains(count)) continue;
                if(count == getGlobalDeclareLine()){
                    CommonOperation.removeDuplicates(liftedVarToGobal).forEach(pw::println);
                }
                if(newLineMap.containsKey(count)){
                    if(ifStmtMap.containsKey(count)){
                        boolean isIf = false;
                        if(newLineMap.get(count).trim().startsWith("if")){
                            IfInfo fii = getSpecificIfInfoFromStart(count);
                            if(fii != null){
                                isIf = true;
                                System.out.println("add brace: " + fii.getStartLine() + " " + fii.getEndLine());
                                addedIfBraceSet.add(fii.getEndLine() + 1); //在这一行要添加if的结束大括号
                            }
                        }
                        pw.println("//adding mutate part");
                        pw.println(ifStmtMap.get(count));
                        pw.println(newLineMap.get(count));
                        if(!isIf){
                            pw.println("}");
                        }
                    } else {
                        pw.println(newLineMap.get(count));
                    }
                    continue;
                }
                pw.println(line.replaceAll("(const)|(volatile)", ""));
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

    public IfInfo getSpecificIfInfoFromStart(int startLine){
        for(IfInfo fii: ifList){
            if(fii.getStartLine() == startLine){
                return fii;
            }
        }
        return null;
    }

    public void getCurrentFB(){
        for(FunctionBlock fb: FuncfunctionBlockList){
            if(currentLineNumber > fb.startline && currentLineNumber < fb.endline){
                this.currentBlock = fb;
            }
        }
    }

    public void getLoopMap(){
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);
        for (LoopStatement loop : loopList) {
            loopMap.put(loop.getStartLine(), loop.getEndLine());
        }
    }

    public void getAvarList(){
        List<AstVariable> astVarList = new ArrayList<>();
        for(String s: varMap.keySet()){
            astVarList.add(varMap.get(s));
            declareLineSet.add(varMap.get(s).getDeclareLine());
        }
        //修改了varInform，可以得到关于union和结构体本身的变量，而不仅仅是成员变量
//        List<AvailableVariable> overallavarList = VarInform.getInitialAvailableVarList(astVarList, astgen);
//        avarList = VarInform.removeStructAndUnionOverall(overallavarList);
        avarList = VarInform.getInitialAvailableVarList(astVarList, astgen);
    }

    public int getGlobalDeclareLine(){
        int count = 0;
        for(String s: initialFileList){
            count++;
            if(s.trim().matches("(\\s*\\w+\\s+)+func_\\d+\\s*\\(\\s*(?:\\w+\\s*(?:\\*+\\s*)?)*\\);"))
                return count - 2;
        }
        return 0;
    }

    public void getAllCMPVar(){
        int funcStartLine = CommonInfoFromFile.getFuncStartLine(initialFileList);
        for(String line: initialFileList) {
            CMPVariable cmp = new CMPVariable(varMap, cmpVarSet, funcStartLine);
            cmp.run(line);
        }
    }

    public void createMuDir(){
        muDir = new File(PropertiesInfo.mutatePointerTarget + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        CommonInfoFromFile.cpOriginalFile(muDir, file);
    }

    public void definedformat(File file){
        List<String> newFileList = new ArrayList<>();
        System.out.println(file.getAbsoluteFile());
        List<String> oldFileList = CommonOperation.genInitialList(file);
        System.out.println(oldFileList.size());
        for(int i = 0; i < oldFileList.size(); i++){
            String currentLine = oldFileList.get(i);
            if(currentLine.trim().startsWith("if") && !currentLine.trim().endsWith("}") && oldFileList.get(i + 1).trim().startsWith("{")){
                newFileList.add(currentLine + oldFileList.get(i + 1).trim());
                i++;
            } else {
                newFileList.add(currentLine);
            }
        }
        System.out.println(newFileList.size());
        new FileModify().writeFile(file, newFileList);
    }

}

