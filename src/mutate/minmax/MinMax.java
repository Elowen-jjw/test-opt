package mutate.minmax;

import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.VarInform;
import AST_Information.model.AstVariable;
import AST_Information.model.FunctionBlock;
import AST_Information.model.LoopStatement;
import ObjectOperation.datatype.Data;
import ObjectOperation.datatype.IntegerOperation;
import ObjectOperation.datatype.StringOperation;
import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import common.CommonInfoFromFile;
import common.PropertiesInfo;
import mutate.pattern.CMP;
import processtimer.ProcessTerminal;
import utity.AvailableVariable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ObjectOperation.datatype.StringOperation.truncateFromLeft;
import static ObjectOperation.datatype.StringOperation.truncateFromRight;
import static common.before.LoopExecValues.deleteAoutFile;

public class MinMax {
    public File file;
    String sanitizerCompiler;
    public File muDir;
    public List<String> initialFileList;
    public AstInform_Gen astgen;
    public List<FunctionBlock> FuncfunctionBlockList;
    public Map<String, AstVariable> varMap;

    public Map<Integer, Integer> loopMap = new HashMap<>();
    public List<AvailableVariable> avarList = new ArrayList<>();

    public Map<Integer, List<SafeMathInfo>> safeMathMap = new HashMap<>();

    public Map<Integer, List<String>> mutateMap = new HashMap<>();

    public List<String> replacedMinMaxList = new ArrayList<>();

    public Map<String, Set<BigInteger>> funcReturnValueMap = new HashMap<>();

    public int prinfCnt = 0;


    public MinMax(){}

    public MinMax(File file, String sanitizerCompiler){
        this.file = file;
        this.sanitizerCompiler = sanitizerCompiler;
        this.initialFileList = CommonOperation.genInitialList(file);
        this.astgen = new AstInform_Gen(file);
        this.varMap = astgen.allVarsMap;
        getLoopMap();
        getAvarList();
        this.FuncfunctionBlockList = astgen.getAllFunctionBlocks();
        createMuDir();
        getAllFuncReturnValue();
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
        }
        //修改了varInform，可以得到关于union和结构体本身的变量，而不仅仅是成员变量
        List<AvailableVariable> overallavarList = VarInform.getInitialAvailableVarList(astVarList, astgen);
        avarList = VarInform.removeStructAndUnionOverall(overallavarList);
        //int to int32_t long to int64_t
        for(AvailableVariable av: avarList){
            if(PropertiesInfo.commonToStandardType.containsKey(av.getType())){
                av.setType(PropertiesInfo.commonToStandardType.get(av.getType()));
            }
        }
    }

    public static void main(String[] args) {
//        for(File file: new File(PropertiesInfo.testSuiteStructDir).listFiles()) {
            File file = new File(PropertiesInfo.testSuiteStructDir + "/random10274" + ".c");
            System.out.println(file.getName());
            File genMuDir = new File(PropertiesInfo.mutateMinMaxDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
            if (genMuDir.exists()) {
                FileModify fm = new FileModify();
                fm.deleteFolder(genMuDir);
            }
        MinMax mm = new MinMax(file, "clang");
        mm.run();

//        }
    }

    public void run(){
        CMP cmp = new CMP(file, initialFileList, astgen, varMap, loopMap, avarList);
        cmp.runCMP();

        setActualValue(); //set param name type values comparableValues
        setMinMaxRelation(); //set replacedMinMaxList
//        printfInfo(); //printf related info

        ParamReplacedOp rro = new ParamReplacedOp();
        rro.generateReplacements(safeMathMap, initialFileList, mutateMap);

//        for(Integer i: mutateMap.keySet()){
//            for(String s: mutateMap.get(i)) {
//                System.out.println(i + ": " + s);
//            }
//        }

        WriteMutate wm = new WriteMutate(sanitizerCompiler);
        wm.writeMuFile(file, mutateMap, initialFileList);
    }

    public void createMuDir(){
        muDir = new File(PropertiesInfo.mutateMinMaxDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        CommonInfoFromFile.cpOriginalFile(muDir, file);
    }

    public void printfInfo(){
        for(Integer i: safeMathMap.keySet()){
            for(SafeMathInfo func: safeMathMap.get(i)){
                if(!func.getFirstReplacedMinMax().isEmpty()){//!isConst(func.getFirstParam()) && !func.getFirstValue().isEmpty()
                    System.out.println(i);
                    System.out.println("first param: " + func.getFirstParam());
                    System.out.println(initialFileList.get(i - 1).substring(func.getFirstStartColumn(), func.getFirstEndColumn()));
                    System.out.println("first value: " + func.getFirstValue());
                    System.out.println("comparable map: " + func.getComparableMap().toString());
                    CommonOperation.printList(func.getFirstReplacedMinMax());
                    System.out.println();
                }
                if (!func.getSecondReplacedMinMax().isEmpty()) {//!func.getSecondParam().isEmpty() && !isConst(func.getSecondParam()) && !func.getSecondValue().isEmpty()
                    System.out.println(i);
                    System.out.println("second param: " + func.getSecondParam());
                    System.out.println(initialFileList.get(i - 1).substring(func.getSecondStartColumn(), func.getSecondEndColumn()));
                    System.out.println("second value: " + func.getSecondValue());
                    System.out.println("comparable map: " + func.getComparableMap().toString());
                    CommonOperation.printList(func.getSecondReplacedMinMax());
                    System.out.println();
                }
            }
        }

    }


    //得到每一个param此时的值，在语句前输出一次，语句之后输出一次
    public void setActualValue(){
        SafeMathExtractor sme = new SafeMathExtractor();
        safeMathMap = sme.getAllSafeMathCall(initialFileList);

        Set<Integer> runLineNumberSet = new HashSet<>();

        // 记录每个键的List当前处理的索引
        Map<Integer, Integer> currentIndexMap = new HashMap<>();

        // 初始化 currentIndexMap，所有键对应的索引初始为0
        for (Integer i : safeMathMap.keySet()) {
            currentIndexMap.put(i, 0);
        }

        boolean allProcessed = false;

        while (!allProcessed) {
            allProcessed = true;  // 假设这轮所有元素都处理完
            Map<Integer, List<String>> addedSafeMathPrintfMap = new HashMap<>();//记录在initial file第几行添加的一系列printf语句

            for (Integer i : safeMathMap.keySet()) {
                List<SafeMathInfo> calls = safeMathMap.get(i);
                int currentIndex = currentIndexMap.get(i);  // 获取当前索引

                // 如果当前索引还没有超出 calls.size()
                if (currentIndex < calls.size()) {
                    allProcessed = false;  // 有元素没有处理完
                    SafeMathInfo func = calls.get(currentIndex);  // 获取当前元素

                    List<String> safeMathParamPrintfList = new ArrayList<>();

                    if (!isConst(func.getFirstParam())) {
                        dealAvailableParam(i, func.getFirstParam(), func.getFirstType(), safeMathParamPrintfList);
                    }
                    if (!func.getSecondParam().isEmpty() && !isConst(func.getSecondParam())) {
                        dealAvailableParam(i, func.getSecondParam(), func.getSecondType(), safeMathParamPrintfList);
                    }

                    addedSafeMathPrintfMap.put(i, safeMathParamPrintfList);
//                    CommonOperation.printList(safeMathParamPrintfList);
//                    System.out.println();

                    // 更新当前索引，准备下一次遍历
                    currentIndexMap.put(i, currentIndex + 1);
                }
            }

            // 每次大遍历结束，即每一行都输出了一个printf执行以下操作：
            File printfFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_printf_" + prinfCnt++ + ".c");
            writeSafeMathPrintfFileSimple(printfFile, addedSafeMathPrintfMap);
            List<String> execLines = runPrintFile(printfFile);
            if (execLines == null) System.out.println(printfFile.getName() + " Printf generation has errors!!!!");
            else if(execLines.isEmpty()) System.out.println(printfFile.getName() + " Printf generation does not have print!!!!");
            else {
                for (Integer i : safeMathMap.keySet()) {
                    for (SafeMathInfo func : safeMathMap.get(i)) {
                        Set<BigInteger> firstValues = new TreeSet<>(getSafeMathParamValues(execLines, func.getFirstParam(), func.getFirstType(), i, runLineNumberSet));
                        Set<BigInteger> secondValues = new TreeSet<>(getSafeMathParamValues(execLines, func.getSecondParam(), func.getSecondType(), i, runLineNumberSet));
                        if (func.getFirstValue().isEmpty()) {
                            func.setFirstValue(firstValues);
                        } else {
                            func.getFirstValue().addAll(firstValues);
                        }
                        if (!func.getSecondParam().isEmpty()) {
                            if (func.getSecondValue().isEmpty()) {
                                func.setSecondValue(secondValues);
                            } else {
                                func.getSecondValue().addAll(secondValues);
                            }
                        }
                    }
                }
            }

            if(runLineNumberSet.isEmpty()) return;
        }

//        System.out.println("runNumberSet: " + runLineNumberSet);

        setSafeMathComparablePVar(safeMathMap, runLineNumberSet);
        setSafeMathComparableLoopIndex(safeMathMap, runLineNumberSet);
    }

    public void dealAvailableParam(int lineNumber, String initialParam, String type, List<String> safeMathParamPrintfList){
//        System.out.println("initial: " + initialParam);
        String calculateParam = initialParam;
        Matcher m = Pattern.compile("func_\\d+").matcher(calculateParam);
        //find到最外层的func
        if (m.find() && funcReturnValueMap != null && funcReturnValueMap.containsKey(m.group())) {
            for (BigInteger bi : funcReturnValueMap.get(m.group())) {
                calculateParam = calculateParam.substring(0, m.start()) + bi
                        + calculateParam.substring(m.start() + truncateFromLeft(initialParam.substring(m.start())).length());
                safeMathParamPrintfList.add(Data.addPrintf(lineNumber, type, initialParam,
                        calculateParam.replaceAll("\\+\\+|--", "")));
                if(CommonInfoFromFile.isHaveOp(calculateParam)) {
                    dealStatute(lineNumber, type, initialParam, calculateParam, calculateParam, safeMathParamPrintfList, 0);
                }
            }
        } else {
            if (!CommonInfoFromFile.isHaveOp(calculateParam)) {
                safeMathParamPrintfList.add(Data.addPrintf(lineNumber, type, initialParam,
                        calculateParam.replaceAll("\\+\\+|--", "")));
            } else {
                dealStatute(lineNumber, type, initialParam, calculateParam, calculateParam, safeMathParamPrintfList, 0);
            }
        }

//        System.out.println("after: " + calculateParam);
    }

    public void dealStatute(int lineNumebr, String printfType, String initialParam,
                            String firstInput, String currentInput, List<String> safeMathParamPrintfList, int baseIndex){
        Pattern opPattern = Pattern.compile("([\\w\\*\\(\\)\\[\\]\\.]+)\\s+([\\+\\*-/\\|\\^&]=|>>=|<<=)\\s+(.*)");
        Matcher matcher = opPattern.matcher(currentInput);

        while (matcher.find()) {
            String leftValue = matcher.group(1);
            if(leftValue.matches(".*\\[.*[lgp]_.*\\].*")) continue;
            String operator = matcher.group(2);
            String rightValue = matcher.group(3);
            String truncatedLeft = truncateFromRight(leftValue);
            String truncatedRight = truncateFromLeft(rightValue);
            safeMathParamPrintfList.add(Data.addPrintf(lineNumebr, printfType, initialParam,
                    (firstInput.substring(0, baseIndex + matcher.end(1)) + firstInput.substring(baseIndex + matcher.start(3) + truncatedRight.length())).replaceAll("\\+\\+|--", "")));
//            System.out.println(firstInput);
//            System.out.println(safeMathParamPrintfList.get(safeMathParamPrintfList.size() - 1));
//            System.out.println();
            dealStatute(lineNumebr, printfType, initialParam, firstInput, rightValue, safeMathParamPrintfList, baseIndex + matcher.start(3));
//            System.out.printf("左值:%s运算符:%s右值:%s%n", firstInput.substring(baseIndex + matcher.end(1) - truncatedLeft.length(), baseIndex + matcher.end(1)),
//                    firstInput.substring(baseIndex + matcher.start(2), baseIndex + matcher.end(2)),
//                    firstInput.substring(baseIndex + matcher.start(3), baseIndex + matcher.start(3) + truncatedRight.length()));
//            System.out.printf("左值:%s运算符:%s右值:%s%n", truncateFromRight(leftValue), operator, truncateFromLeft(rightValue));
        }
    }

    public void setMinMaxRelation() {
        for(Integer i: safeMathMap.keySet()){
            for(SafeMathInfo func: safeMathMap.get(i)){
                processParam(func.getFirstParam(), func.getFirstValue(), func.getFirstType(), func.getComparableMap());
                if(!replacedMinMaxList.isEmpty()){
                    func.setFirstReplacedMinMax(new ArrayList<>(replacedMinMaxList));
                    replacedMinMaxList.clear();
                }
                if (!func.getSecondParam().isEmpty() && !isConst(func.getSecondParam()) && !func.getSecondValue().isEmpty()) {
                    processParam(func.getSecondParam(), func.getSecondValue(), func.getSecondType(), func.getComparableMap());
                    if(!replacedMinMaxList.isEmpty()){
                        func.setSecondReplacedMinMax(new ArrayList<>(replacedMinMaxList));
                        replacedMinMaxList.clear();
                    }
                }
            }
        }
    }

    private void processParam(String paramName, Set<BigInteger> values, String paramType, Map<String, Set<BigInteger>> comparableMap) {
        if (!isConst(paramName) && !values.isEmpty()) {
            for (String specific : comparableMap.keySet()) {
                Set<BigInteger> specificValues = comparableMap.get(specific);
                MinMaxResult specificResult = IntegerOperation.getMinMax(specificValues);

                // Check conditions for less than
                boolean allLessThanSpecific = checkAllLessThan(values, specificResult.min);//any of param values is less than any of comparable var values
                // Check conditions for greater than
                boolean allGreaterThanSpecific = checkAllGreaterThan(values, specificResult.max);//any of param values is greater than any of comparable var values

                if (allLessThanSpecific) {
                    getMinFunction(paramName, specific, paramType);
                }

                if (allGreaterThanSpecific) {
                    getMaxFunction(paramName, specific, paramType);
                }
            }
        }
    }

    private boolean checkAllLessThan(Set<BigInteger> values, BigInteger min) {
        for (BigInteger value : values) {
            if (value.compareTo(min) >= 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkAllGreaterThan(Set<BigInteger> values, BigInteger max) {
        for (BigInteger value : values) {
            if (value.compareTo(max) <= 0) {
                return false;
            }
        }
        return true;
    }

    private void getMinFunction(String paramName, String specific, String paramType) {
        replacedMinMaxList.add((paramType.startsWith("u") ? "umin" : "smin") + "("  + specific + ", " + paramName + ")");
//        System.out.println("All values of " + paramName + " are less than max of " + specific);
    }

    private void getMaxFunction(String paramName, String specific, String paramType) {
        replacedMinMaxList.add((paramType.startsWith("u") ? "umax" : "smax") + "("  + specific + ", " + paramName + ")");
//        System.out.println("All values of " + paramName + " are greater than min of " + specific);
    }

    public String getMinMaxFuncType(String type){
        if(type.startsWith("u")) return "uint64_t";
        else return "long";
    }

    public void setSafeMathComparablePVar(Map<Integer, List<SafeMathInfo>> safeMathMap, Set<Integer> runNumberSet){
        Map<Integer, List<String>> addedPVarPrintfMap = new HashMap<>();//记录在initial file第几行添加的p_var printf语句
        for (Integer i : safeMathMap.keySet()) {
            if(runNumberSet.contains(i)){
                addedPVarPrintfMap.put(i, getPVarPrintfList(i)); //如果被执行了在这一行添加该func的printf参数
            }
        }

        //生成输出文件并且运行得到execlines
        File printfPVarFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_printf_pVar.c");
        writeComparablePrintfFile(printfPVarFile, addedPVarPrintfMap);
        List<String> pVarExecLines = runPrintFile(printfPVarFile);
        if(pVarExecLines == null) System.out.println(printfPVarFile.getName() + " pVar printf file generation has errors.....");
        else if(pVarExecLines.isEmpty()) System.out.println(printfPVarFile.getName() + " pVar printf file generation does not have print.....");
        else {
            //分析输出文件得到在每一个safe_math调用位置所有func参数的值
            Map<Integer, Map<String, Set<BigInteger>>> pVarValueMap = analysisComparableExecLines(pVarExecLines);
            if (pVarValueMap == null)
                System.out.println(printfPVarFile.getName() + " pVar printf file generation core dumped.....");
            else if (pVarValueMap.isEmpty())
                System.out.println(printfPVarFile.getName() + " pVar printf file generation does not have print!!!!");
            else {
                addElementsToCompareMap(safeMathMap, pVarValueMap);
            }
        }
//        printfPVarFile.delete();
    }

    public void setSafeMathComparableLoopIndex(Map<Integer, List<SafeMathInfo>> safeMathMap, Set<Integer> runNumberSet){
        Map<Integer, List<String>> addedLoopIndexPrintfMap = new HashMap<>();//记录在initial file第几行添加的p_var printf语句
        for (Integer i : runNumberSet) {
            for(Integer startLine: loopMap.keySet()){
                int endLine = loopMap.get(startLine);
                if(i > startLine && i < endLine){
                    String line = initialFileList.get(startLine - 1);
                    Matcher m = Pattern.compile(PropertiesInfo.forStmtPattern).matcher(line);
                    if (m.find()) {
                        String loopIndex = m.group(1);
                        if(!loopIndex.matches("[ijklmn]")) {
                            String loopIndexType = getAvarType(loopIndex);
//                            System.out.println(line);
//                            System.out.println("loop index: " + loopIndex + " type: " + loopIndexType);
                            addedLoopIndexPrintfMap.put(i, new ArrayList<>(Arrays.asList(Data.addPrintf(i, loopIndexType, loopIndex, loopIndex))));
                        }
                    }
                }
            }
        }

        //生成输出文件并且运行得到execlines
        File printfLoopIndexFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_printf_loopIndex.c");
        writeComparablePrintfFile(printfLoopIndexFile, addedLoopIndexPrintfMap);
        List<String> loopIndexExecLines = runPrintFile(printfLoopIndexFile);
        if(loopIndexExecLines == null) System.out.println(printfLoopIndexFile.getName() + " loop index printf file generation has errors.....");
        else if(loopIndexExecLines.isEmpty()) System.out.println(printfLoopIndexFile.getName() + " loop index printf file generation does not have printf.....");
        else {
            //分析输出文件得到在每一个safe_math调用位置所有func参数的值
            Map<Integer, Map<String, Set<BigInteger>>> loopIndexValueMap = analysisComparableExecLines(loopIndexExecLines);
            if (loopIndexValueMap == null)
                System.out.println(printfLoopIndexFile.getName() + " loop index printf file generation core dumped.....");
            else if (loopIndexValueMap.isEmpty())
                System.out.println(printfLoopIndexFile.getName() + " loop index printf file generation does not have print!!!!");
            else {
                addElementsToCompareMap(safeMathMap, loopIndexValueMap);
            }
        }
//        printfLoopIndexFile.delete();
    }

    public List<String> getPVarPrintfList(int lineNumber){
        List<String> pVarPrintfList = new ArrayList<>();
        for(FunctionBlock fb: FuncfunctionBlockList){
            if(lineNumber > fb.startline && lineNumber < fb.endline){
                List<AvailableVariable> pVarAvList = getPVarAvList(fb.startline);
                for(AvailableVariable param: pVarAvList){
                    pVarPrintfList.add(Data.addPrintf(lineNumber, param.getType(), param.getValue(), param.getValue()));
                    //添加指针！=null的操作
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

    public Map<Integer, Map<String, Set<BigInteger>>> analysisComparableExecLines(List<String> execLines){
        Map<Integer, Map<String, Set<BigInteger>>> pVarValueMap = new HashMap<>();
        for (String s : execLines) {
            if(s.contains("core dumped") || s.contains("error: ")) return null;
            Matcher m = Pattern.compile("(\\d+)\\s*:\\s*(.+)\\s*@\\s*(.*)").matcher(s);
            if(m.find()){
                int lineNumber = Integer.parseInt(m.group(1).trim());
                String varName = m.group(2).trim();
//                System.out.println("ssssssssssssssss" + varName + " " + getAvarType(varName) + " " + m.group(3).trim());
                BigInteger decimalValue = StringOperation.convertToDecimal(getAvarType(varName), m.group(3).trim());
                if(pVarValueMap.containsKey(lineNumber)){
                    if(pVarValueMap.get(lineNumber).containsKey(varName)){
                        pVarValueMap.get(lineNumber).get(varName).add(decimalValue);
                    } else {
                        pVarValueMap.get(lineNumber).put(varName, new TreeSet<>(Arrays.asList(decimalValue)));
                    }
                } else {
                    pVarValueMap.put(lineNumber, new HashMap<>(){{
                        put(varName, new TreeSet<>(Arrays.asList(decimalValue)));
                    }});
                }
            }
        }
        return pVarValueMap;
    }

    public String getAvarType(String avarName){
        for(AvailableVariable av: avarList){
            if(av.getValue().equals(avarName))
                return av.getType();
        }
        return "int32_t";
    }

    private void addElementsToCompareMap(Map<Integer, List<SafeMathInfo>> safeMathMap, Map<Integer, Map<String, Set<BigInteger>>> loopIndexValueMap) {
        for (Integer i : safeMathMap.keySet()) {
            if (!loopIndexValueMap.containsKey(i)) continue;
            Map<String, Set<BigInteger>> comparableMap = deepCopyMap2(loopIndexValueMap.get(i));
            for (SafeMathInfo func : safeMathMap.get(i)) {
                if(func.getComparableMap().isEmpty()) {
                    func.setComparableMap(comparableMap);
                } else {
                    comparableMap.forEach((key, value) ->
                            func.getComparableMap().merge(key, value, (list1, list2) -> {
                                list1.addAll(list2);
                                return list1;
                            }));
                }
            }
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

    public List<BigInteger> getSafeMathParamValues(List<String> execLines, String paramExpression, String paramType, int lineNumber, Set<Integer> runLineNumberSet){
        if(isConst(paramExpression)) {
//            System.out.println("initial const: " + paramExpression);
            BigInteger decimalValue = StringOperation.convertToDecimal(paramType, paramExpression);
//            System.out.println("dealed const: " + decimalValue);
            return new ArrayList<>(Arrays.asList(decimalValue));
        }
        List<BigInteger> valueList = new ArrayList<>();
        for (String s : execLines) {
            Matcher m = Pattern.compile("(\\d+)\\s*:\\s*(.+)\\s*@\\s*(.*)").matcher(s);
            if(m.find()){
//                    System.out.println("1 " + m.group(1));
//                    System.out.println("2 " + m.group(2));
//                    System.out.println("3 " + m.group(3));
                if (lineNumber == Integer.parseInt(m.group(1).trim())
                        && paramExpression.trim().equals(m.group(2).trim())) {
//                    System.out.println("hhh initial const: " + m.group(3).trim());
                    BigInteger decimalValue = StringOperation.convertToDecimal(paramType, m.group(3).trim());
//                    System.out.println("hhh dealed const: " + decimalValue);
                    valueList.add(decimalValue);
                    runLineNumberSet.add(lineNumber);
                }
            }
        }
        return valueList;
    }

    public boolean isConst(String expr){
        return expr.matches("\\(?(0x)?\\-?[0-9a-fA-F]+(U?L{0,2})?\\)?");
    }

    public void writeFuncReturnPrintfFile(File printfFile, Map<Integer, String> printfMap) {
        try {
            FileWriter fw = new FileWriter(printfFile);
            PrintWriter pw = new PrintWriter(fw);

            int count = 0;
            for(String line: initialFileList){
                count++;
                if(printfMap.containsKey(count) && !printfMap.get(count).isEmpty()){
                    pw.println(printfMap.get(count));
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

    public void writeComparablePrintfFile(File printfFile, Map<Integer, List<String>> printfMap) {
        try {
            FileWriter fw = new FileWriter(printfFile);
            PrintWriter pw = new PrintWriter(fw);

            for(int count = 1; count <= initialFileList.size(); count++){
                String line = initialFileList.get(count - 1);
                if(printfMap.containsKey(count) && !printfMap.get(count).isEmpty()){
                    if(!initialFileList.get(count).replaceAll("/\\*.*\\*/", "").trim().equals("{")){
                        printfMap.get(count).forEach(pw::println);
                        pw.println(line);
                        printfMap.get(count).forEach(pw::println);
                    } else {
                        printfMap.get(count).forEach(pw::println);
                        pw.println(line);
                        pw.println(initialFileList.get(count));
                        printfMap.get(count).forEach(pw::println);
                        count++;
                    }
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

    public void writeSafeMathPrintfFileSimple(File printfFile, Map<Integer, List<String>> originalPrintfMap) {
        try {
            FileWriter fw = new FileWriter(printfFile);
            PrintWriter pw = new PrintWriter(fw);

            for(int count = 1; count <= initialFileList.size(); count++){
                String line = initialFileList.get(count - 1);
                if(originalPrintfMap.containsKey(count)){
                    if(line.trim().startsWith("for")){
                        originalPrintfMap.get(count).forEach(pw::println);
                        pw.println(line);
                    } else if(line.trim().startsWith("if")){
                        if(!initialFileList.get(count).replaceAll("/\\*.*\\*/", "").trim().equals("{")){
                            originalPrintfMap.get(count).forEach(pw::println);
                            pw.println(line);
                            originalPrintfMap.get(count).forEach(pw::println);
                        } else {
                            originalPrintfMap.get(count).forEach(pw::println);
                            pw.println(line);
                            pw.println(initialFileList.get(count));
                            originalPrintfMap.get(count).forEach(pw::println);
                            count++;
                        }
                    } else {
                        originalPrintfMap.get(count).forEach(pw::println);
                        pw.println(line);
                        originalPrintfMap.get(count).forEach(pw::println);
                    }
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

    public void getAllFuncReturnValue(){
        Map<Integer, String> addedFuncReturnMap = new HashMap<>();
        for(FunctionBlock fb: FuncfunctionBlockList){
//            System.out.println(fb.name + " " + initialFileList.get(fb.startline - 1));
            Matcher matcher = Pattern.compile("(\\w+\\s+\\**)\\w+\\s*\\(.*\\)").matcher(initialFileList.get(fb.startline - 1));
            String returnType = "";
            if(matcher.find()) {
                returnType = matcher.group(1);
//                System.out.println(fb.name + " " + returnType);
                fb.returnType = returnType.matches("u?int\\d+_t") ? returnType : "";
            }
            if(fb.returnType.trim().equals("")){
                continue;
            }
            for(int i: fb.returnLineSet){
                String param = initialFileList.get(i - 1).replace("return", "").replace(";", "").trim();
                addedFuncReturnMap.put(i, Data.addPrintf(Integer.parseInt(fb.name.replace("func_", "")), returnType, param, param));
            }
        }

        File printfFuncReturnFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_printf_funcReturn.c");
        writeFuncReturnPrintfFile(printfFuncReturnFile, addedFuncReturnMap);
        List<String> funcReturnExecLines = runPrintFile(printfFuncReturnFile);
        if(funcReturnExecLines == null) System.out.println(printfFuncReturnFile.getName() + " func return printf file generation has errors.....");

        funcReturnValueMap = analysisFuncReturnExecLines(funcReturnExecLines);
        for(String s: funcReturnValueMap.keySet()){
            System.out.println(s + " " + funcReturnValueMap.get(s));
        }
    }

    public Map<String, Set<BigInteger>> analysisFuncReturnExecLines(List<String> execLines){
        Map<String, Set<BigInteger>> returnValueMap = new HashMap<>();
        for (String s : execLines) {
            if(s.contains("core dumped") || s.contains("error: ")) return null;
            Matcher m = Pattern.compile("(\\d+)\\s*:\\s*(.+)\\s*@\\s*(.*)").matcher(s);
            if(m.find()){
                String funcName = "func_" + m.group(1).trim();
                FunctionBlock fb = getFuncInfo(funcName);
                String returnType = fb == null ? "" : fb.returnType;
                if(!returnType.isEmpty()) {
                    BigInteger decimalValue = StringOperation.convertToDecimal(returnType, m.group(3).trim());
                    if (returnValueMap.containsKey(funcName)) {
                        returnValueMap.get(funcName).add(decimalValue);
                    } else {
                        returnValueMap.put(funcName, new TreeSet<>(Arrays.asList(decimalValue)));
                    }
                }
            }
        }
        return returnValueMap;
    }

    public FunctionBlock getFuncInfo(String funcName){
        for(FunctionBlock fb: FuncfunctionBlockList){
            if(fb.name.equals(funcName))
                return fb;
        }
        return null;
    }


    public void writeSafeMathPrintfFile(File printfFile, Map<Integer, List<String>> cpPrintfMap) {
        try {
            FileWriter fw = new FileWriter(printfFile);
            PrintWriter pw = new PrintWriter(fw);

            int count = 0;
            for(String line: initialFileList){
                count++;
                if(cpPrintfMap.containsKey(count) && !cpPrintfMap.get(count).isEmpty()){
                    List<String> addedList = uniqueSideEffort(cpPrintfMap.get(count));
                    if(line.trim().startsWith("for")){
                        pw.println(line);
                        addedList.forEach(pw::println);
                    } else if(line.trim().startsWith("if")){
                        addedList.forEach(pw::println);
                        pw.println(line);
                    } else {
                        addedList.forEach(pw::println);
                    }
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

    public List<String> uniqueSideEffort(List<String> originalPrintfList) {
        List<String> addedPrinfList = new ArrayList<>();

        boolean writtenSpecialString = false; // 标志，检查是否已写入特定字符串

        for (String printfLine : new ArrayList<>(originalPrintfList)) { // 使用新的ArrayList来避免ConcurrentModificationException
            if (CommonInfoFromFile.isHaveOp(printfLine) || CommonInfoFromFile.containsAssignmentOperator(printfLine)) {
                if (!writtenSpecialString) {
                    addedPrinfList.add(printfLine); // 写入第一个包含特殊操作的字符串
                    writtenSpecialString = true; // 设置标志为true
                    originalPrintfList.remove(printfLine); // 从列表中移除已写入的字符串
                }
            } else {
                addedPrinfList.add(printfLine); // 直接写入
                originalPrintfList.remove(printfLine);
            }
        }
        return addedPrinfList;

    }


    public Map<Integer, List<String>> deepCopyMap1(Map<Integer, List<String>> originalMap) {
        return originalMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new ArrayList<>(entry.getValue())
                ));
    }

    public static Map<String, Set<BigInteger>> deepCopyMap2(Map<String, Set<BigInteger>> originalMap) {
        Map<String, Set<BigInteger>> copiedMap = new HashMap<>();

        for (Map.Entry<String, Set<BigInteger>> entry : originalMap.entrySet()) {
            String key = entry.getKey();
            Set<BigInteger> originalSet = entry.getValue();

            Set<BigInteger> copiedSet = new HashSet<>(originalSet);
            copiedMap.put(key, copiedSet);
        }

        return copiedMap;
    }

    public boolean checkForEmptyLists(Map<Integer, List<String>> map) {
        for (List<String> valueList : map.values()) {
            if (!valueList.isEmpty()) {
                return false;
            }
        }
        return true;
    }


}
