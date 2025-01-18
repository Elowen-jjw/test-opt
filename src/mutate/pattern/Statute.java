package mutate.pattern;

import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.VarInform;
import AST_Information.model.AstVariable;
import AST_Information.model.LoopStatement;
import ObjectOperation.datatype.StringOperation;
import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import common.CommonInfoFromFile;
import common.PropertiesInfo;
import mutate.minmax.WriteMutate;
import processtimer.ProcessTerminal;
import utity.AvailableVariable;
import utity.PointerInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ObjectOperation.datatype.StringOperation.truncateFromLeft;
import static ObjectOperation.datatype.StringOperation.truncateFromRight;

public class Statute {
    AstInform_Gen astgen;
    Map<Integer, Integer> loopMap = new HashMap<>();
    List<AvailableVariable> avarList = new ArrayList<>();
    Map<String, AstVariable> varMap;
    Set<AstVariable> cmpVarSet = new HashSet<>();
    List<AvailableVariable> cmpAvarList = new ArrayList<>();
    File file;

    String sanitizerCompiler = "";

    List<String> liftedVarToGobal = new ArrayList<>();
    List<String> newDeclareBeforeLoop = new ArrayList<>(); //每个loop都要清空
    List<String> pointerAliasAfterLoop = new ArrayList<>();//每个loop都要清空
    Map<Integer, List<String>> addedBeforeLoopStart = new HashMap<>();
    Map<Integer, List<String>> addedAfterLoopEnd = new HashMap<>();
    Map<Integer, List<String>> addedBeforeLoopEnd = new HashMap<>();

    List<String> initialFileList;
    List<String> pointerAnalysisList = new ArrayList<>();
    List<PointerInfo> allPointerInfoList = new ArrayList<>();
    String leftType = "";
    String leftVarName = "";
    String loopIndex = "";
    String loopIndexType = "";
    int currentLineNumber = 0;
    int startLine = 0;
    int endLine = 0;
    Set<Integer> checkedLines = new HashSet<>();
    Set<Integer> deleteDeclaration = new HashSet<>();

    boolean isLinkedCorrect = true;

    int gaCnt = 0;
    int lbCnt = 0;
    int apCnt = 0;
    int mutCount = 0;

    Pattern opPattern = Pattern.compile("([\\w\\*\\(\\)\\[\\]\\.]+)\\s*([\\+\\*-/\\|\\^&]=|>>=|<<=)\\s*(.*)");

    public Statute(File file, String sanitizerCompiler){
        this.file = file;
        this.sanitizerCompiler = sanitizerCompiler;
        initialFileList = CommonOperation.genInitialList(file);
        getPointerAnalysis();
//        CommonOperation.printList(pointerAnalysisList);
        getAllPointers();
        astgen = new AstInform_Gen(file);
        varMap = astgen.allVarsMap;
        getLoopMap();
        getAvarList();
        getAllCMPVar();
        List<AvailableVariable> overallCmpAvar = VarInform.getInitialAvailableVarList(new ArrayList<>(cmpVarSet), astgen);
        cmpAvarList = VarInform.removeStructAndUnionOverall(overallCmpAvar);
    }

    public void getLoopMap(){
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);

        for (LoopStatement loop : loopList) {
            loopMap.put(loop.getStartLine(), loop.getEndLine());
//            System.out.println(loop.getStartLine() + "    " + loop.getEndLine());
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
//        avarList = VarInform.getInitialAvailableVarList(astVarList, astgen);
    }

    public static void main(String[] args) {
//        List<String> lists = CommonOperation.genInitialList(new File("/home/sdu/Desktop/clang_output_error.txt"));
//        for(String s: lists) {
        File file = new File("/home/sdu/Desktop/random11108" + ".c");
        File genMuDir = new File(PropertiesInfo.mutateStatuteDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        if(genMuDir.exists()){
            FileModify fm = new FileModify();
            fm.deleteFolder(genMuDir);
        }
        Statute st = new Statute(file, "clang");
//        for(AstVariable astvar: st.cmpVarSet){
//            System.out.println(astvar.getName() + "  " + astvar.getType());
//        }
        st.run();
//        }
//
//        String pattern = "\\{(.*)\\}";
//        Pattern r = Pattern.compile(pattern);
//        Matcher m = r.matcher("{{{{{&l_682[2]}, {&l_680}}}, {{{&l_682[2]}, {&l_680}}}}}");
//        String loopIndex = "";
//        if (m.find()) {
//            loopIndex = m.group(1);
//        }
//        System.out.println(loopIndex);
    }

    public void run(){
        int count = 0;
        while(count++ < 5){
            if(runStatute()) break;
        }
    }

    public boolean runStatute(){
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(loopMap.entrySet());

        // 对List进行排序
        Collections.sort(entries, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                int start1 = o1.getKey();
                int end1 = o1.getValue();
                int start2 = o2.getKey();
                int end2 = o2.getValue();

                // 如果o1被o2包含，先输出o1
                if (start2 <= start1 && end1 <= end2) {
                    return -1; // o1 在 o2 之前
                }
                // 如果o2被o1包含，先输出o2
                else if (start1 <= start2 && end2 <= end1) {
                    return 1;  // o2 在 o1 之前
                }
                // 否则按 start 升序排列
                return Integer.compare(start1, start2);
            }
        });

        //循环内部
        for (Map.Entry<Integer, Integer> entry : entries) {
            startLine = entry.getKey();
            endLine = entry.getValue();
            loopIndex = "";
            loopIndexType = "";
            for(int i = startLine; i <= endLine; i++){
                if(checkedLines.contains(i)) continue;
                checkedLines.add(i);
                String line = initialFileList.get(i - 1);
                currentLineNumber = i;
                if(i == startLine){
                    String pattern = "for\\s*\\(\\s*([a-zA-Z0-9_\\[\\]\\.]*)\\s*=\\s*\\(?[^;]*;" +
                            "\\s*\\(?\\1\\s*[<>=!]+\\s*[^;]*;" +
                            "\\s*([\\+\\-]{0,2}\\1\\s*[\\+\\-]{0,2}=?\\s*[^;]*)\\)";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(line);
                    if (m.find()) {
                        loopIndex = m.group(1);
                        if(!loopIndex.matches("[ijklmn]")) loopIndexType = getVarType(loopIndex);
                    }
                } else {
                    if (isHaveOp(line)) {
                        findPatternInsideLoop(line);
                    }
                }
            }
//            System.out.println("startLine: " + startLine + " endline: " + endLine + " loop index: " + loopIndex + " type: " + loopIndexType);
            if(!newDeclareBeforeLoop.isEmpty())
                addedBeforeLoopStart.put((initialFileList.get(startLine - 2).trim().endsWith(":") ? startLine - 1 : startLine), CommonOperation.removeDuplicates(newDeclareBeforeLoop));
            newDeclareBeforeLoop.clear();
        }

        boolean isSuccess = genMutate();
        System.out.println(file.getName() + " " + isSuccess);
        return isSuccess;
    }


    public void findPatternInsideLoop(String input){
        Matcher matcher = opPattern.matcher(input);

        //找到所有规约，截取左值和右值
        while (matcher.find()) {
            String leftValue = matcher.group(1);
            if(leftValue.matches(".*\\[.*[lgp]_.*\\].*")) continue;
//          String operator = matcher.group(2);
            String rightValue = matcher.group(3);

//            System.out.println(truncateFromLeft(rightValue));

            dealLeftPart(truncateFromRight(leftValue));
            dealRightPart(truncateFromLeft(rightValue));
            addPointerAlias();

            if(!pointerAliasAfterLoop.isEmpty()) {
                int declareLine = getVarDeclareLine(leftVarName);
//                System.out.println("startLine: " + startLine + " endline: " + endLine + " declareLIne: " + declareLine + " leftvarName: " + leftVarName);
                if (declareLine >= startLine && declareLine <= endLine && !deleteDeclaration.contains(declareLine)) {
                    if(!addedBeforeLoopEnd.containsKey(endLine - 1))
                        addedBeforeLoopEnd.put(endLine - 1, new ArrayList<>(pointerAliasAfterLoop));
                    else{
                        addedBeforeLoopEnd.get(endLine - 1).addAll(new ArrayList<>(pointerAliasAfterLoop));
                    }
                }
                else {
                    if(!addedAfterLoopEnd.containsKey(endLine)) {
                        addedAfterLoopEnd.put(endLine, new ArrayList<>(pointerAliasAfterLoop));
                    } else {
                        addedAfterLoopEnd.get(endLine).addAll(new ArrayList<>(pointerAliasAfterLoop));
                    }
                }
            }
            pointerAliasAfterLoop.clear();

//            System.out.printf("左值:%s运算符:%s右值:%s%n", (leftValue), operator, (rightValue));
            findPatternInsideLoop(rightValue);
        }
    }

    //左值必须为指针形式且该指针为全局指针指向全局变量
    public void dealLeftPart(String leftValue){
        if(leftValue.contains("p_")) return;
        if(leftValue.contains("*")){ //not need to replace varName
            dealPointer(truncateFromRight(leftValue).trim());
        } else{
            if((leftValue.contains("l_") || leftValue.contains("g_")) && !leftValue.contains("p_")) {
                liftedLocalToGlobal(leftValue.trim(), true, false);
                initialFileList.set(currentLineNumber - 1, initialFileList.get(currentLineNumber - 1)
                        .replaceAll(CommonInfoFromFile.replaceRegex(leftValue) + "(?![0-9])", "*ga_" + (gaCnt - 1)));
            }
        }
    }

    public void dealRightPart(String rightValue){
        Collections.shuffle(allPointerInfoList);
        boolean isContainsPointer = false;
        for (PointerInfo pointerInfo : allPointerInfoList) {
            if(pointerInfo.getName().matches("p_\\d+")) continue;
            if (StringOperation.containsVarName(rightValue,pointerInfo.getName())) {
                if(!isCMPVar(pointerInfo.getName())) continue; //如果这个指针变量恰好在cmpVarSet中
//                System.out.println("---------------pointer yse: " + pointerInfo.getName());
                isContainsPointer = true;
                if (pointerInfo.isLocal()) { //not to replace right part
                    if (pointerInfo.getPointsLink().get(pointerInfo.getPointsLink().size() - 1)[0].contains("g_")) {
                        //右值包含某一最终指向全局变量的局部指针
                        return;
                    } else if (pointerInfo.getPointsLink().get(pointerInfo.getPointsLink().size() - 1)[0].contains("l_")) {
                        //右值包含某一最终指向全局变量的局部指针
                        dealPointerLinkLift(pointerInfo, false);
                        return;
                    }
                } else {
                    checkVarDeclareScale(pointerInfo.getPointsLink().get(0)[0]);
                    String points = pointerInfo.getPointsLink().get(0)[0];
                    newDeclareBeforeLoop.add(pointerInfo.getType() + " lb_" + lbCnt++ + " = " + (points.equals("null") ? "(void *)0" : "&"+points) + ";");
                    initialFileList.set(currentLineNumber - 1, initialFileList.get(currentLineNumber - 1)
                            .replaceAll(CommonInfoFromFile.replaceRegex(pointerInfo.getName()) + "(?![0-9])", "lb_" + (lbCnt - 1)));
                }
//                break;
            }
        }
        if(!isContainsPointer){//若没有指针变量
            boolean isConst = true;
            Collections.shuffle(cmpAvarList);
            for(AvailableVariable av: cmpAvarList){
                String varName = av.getValue();
                if(varName.contains("p_")) continue;
                String type = av.getType();
                if(StringOperation.containsVarName(rightValue, varName)){
                    isConst = false;
                    if(varName.contains("g_") || varName.contains("l_")){
//                        System.out.println("---------------varName yse: " + varName);
                        if(varName.contains("l_")){
                            int declareLine = getVarDeclareLine(varName);
//                            System.out.println(declareLine + " " + varName);
                            if(checkInitElement(initialFileList.get(declareLine - 1).substring(
                                    initialFileList.get(declareLine - 1).indexOf("=") == -1 ? 0 : initialFileList.get(declareLine - 1).indexOf("=")
                            ))) {
                                liftedVarToGobal.add(initialFileList.get(declareLine - 1).trim());
                                deleteDeclaration.add(declareLine);
                            } else {
                                continue;
                            }
                        }
                        checkVarDeclareScale(varName);
                        if(varName.contains(".f")){
                            newDeclareBeforeLoop.add(getVarType(varName.substring(0, varName.lastIndexOf(".f"))) + " *lb_" + lbCnt++ + " = &" + varName.substring(0, varName.lastIndexOf(".f")) + ";");
                            newDeclareBeforeLoop.add(type + " lb_" + lbCnt++ + " = "
                                    + "lb_" + (lbCnt - 2)
                                    + "->"
                                    + varName.substring(varName.lastIndexOf(".f") + 1) + ";");
                            initialFileList.set(currentLineNumber - 1, initialFileList.get(currentLineNumber - 1)
                                    .replaceAll(CommonInfoFromFile.replaceRegex(varName) + "(?![0-9])", "lb_" + (lbCnt - 1)));
                        } else {
                            newDeclareBeforeLoop.add(type + " *lb_" + lbCnt++ + " = &" + varName + ";");
                            initialFileList.set(currentLineNumber - 1, initialFileList.get(currentLineNumber - 1)
                                    .replaceAll(CommonInfoFromFile.replaceRegex(varName) + "(?![0-9])", "*lb_" + (lbCnt - 1)));
                        }
//                        break;
                    }
                }
            }
            if(isConst){ //只包含常数
                String varType = getDataType(rightValue);
                if(!varType.equals("No match")) {
                    liftedVarToGobal.add(varType + " ga_" + gaCnt++ + " = " + rightValue + ";");
                    newDeclareBeforeLoop.add(varType + " *lb_" + lbCnt++ + " = &ga_" + (gaCnt - 1) + ";");
                    initialFileList.set(currentLineNumber - 1, initialFileList.get(currentLineNumber - 1)
                            .replaceAll("(?![0-9])" + CommonInfoFromFile.replaceRegex(rightValue) + "(?![0-9])", "*lb_" + (lbCnt - 1)));
                }
            }
        }
    }

    public boolean isCMPVar(String varName){
        for(AstVariable cmpVar: cmpVarSet){
            if(cmpVar.getName().equals(varName))
                return true;
        }
        return false;
    }

    public boolean checkInitElement(String line){
        if(line.contains("p_")) return false;
        Pattern p = Pattern.compile("\\[(.*?)\\]");
        Matcher m = p.matcher(line);
        if(m.find()){
            if(m.group(1).contains("g_") || m.group(1).contains("l_")) {
                return false;
            }
        }
        p = Pattern.compile("\\{(.*)\\}");
        m = p.matcher(line);
        if(m.find()){
            if(line.contains("l_")){
                for(String s: varMap.keySet()){
                    if(varMap.get(s).getName().contains("g_") || varMap.get(s).getName().matches("[ijklmn]")) continue;
                    if(StringOperation.containsVarName(line, varMap.get(s).getName())){
                        liftedVarToGobal.add(initialFileList.get(varMap.get(s).getDeclareLine() - 1));
//                        printLastLine(liftedVarToGobal);
                        deleteDeclaration.add(varMap.get(s).getDeclareLine());
                    }
                }
            }
        }
        return true;
    }

    public void checkVarDeclareScale(String varName){
        int tempDeclareLine = getVarDeclareLine(varName);
        if(tempDeclareLine >= startLine && tempDeclareLine <= endLine){
            newDeclareBeforeLoop.add(initialFileList.get(tempDeclareLine - 1).trim());
            deleteDeclaration.add(tempDeclareLine);
        }
    }

    public int getVarDeclareLine(String varName){
        for(String s: varMap.keySet()){
            if(varMap.get(s).getName().equals(varName.replaceAll("(\\.f\\d+)|(\\[.*?\\])|(\\**)", "")))
                return varMap.get(s).getDeclareLine();
        }
        return 0;
    }

    public String getVarType(String varName){
        for(String s: varMap.keySet()){
            if(varMap.get(s).getName().equals(varName.replaceAll("(\\[.*?\\])|\\*", "")))
                return varMap.get(s).getType().replaceAll("\\[.*?\\]", "");
        }
        return "null";
    }

    public void addPointerAlias(){
        if(leftType.isEmpty() || leftVarName.isEmpty()) return;
        String apName = "a_" + apCnt;
        String bpName = "b_" + apCnt;
        pointerAliasAfterLoop.add(leftType + " *" + apName + " = &" + leftVarName + ";");
        pointerAliasAfterLoop.add(leftType + " *" + bpName + " = &" + leftVarName + ";");
        pointerAliasAfterLoop.add(loopIndex + " = (&" + apName + " == &" + bpName + ") ? (&" + apName + " == &" + bpName + ") : " + loopIndex + ";");
        apCnt++;
    }

    public void dealPointer(String varValue){
        for(PointerInfo pi: allPointerInfoList){
            if(pi.getName().equals(varValue.replaceAll("[\\(\\)\\*]", ""))){
                dealPointerLinkLift(pi, true);
                leftType = pi.getType();
                leftVarName = pi.getName();
                break;
            }
        }
    }

    public void dealPointerLinkLift(PointerInfo pi, boolean isNeedLiftSelf){
        List<String> pointToList = new ArrayList<>();//存放指向链中的变量名称 varMap中的varName
        pointToList.add(pi.getName());
        for(String singleLevel[]: pi.getPointsLink()){
            pointToList.add(singleLevel[0]);
        }
//        for(String ss: pointToList)
//            System.out.print(ss + " -> ");
//        System.out.println();
        isLinkedCorrect = true;
        for(int i = pointToList.size() - 1; i >= (isNeedLiftSelf ? 0 : 1); i--){
            String pointToVar = pointToList.get(i);
            if(pointToVar.contains("l_") && isLinkedCorrect) {
                liftedLocalToGlobal(pointToVar, false, i == 0);
            }
        }
    }

    public void liftedLocalToGlobal(String varName, boolean isCreateGlobalPointer, boolean isNeedAddStatic){
        for(String s: varMap.keySet()){
            if(varMap.get(s).getName().equals(varName.replaceAll("\\[.*?\\]", ""))) {
                if(varName.contains("l_") && checkInitElement(initialFileList.get(varMap.get(s).getDeclareLine() - 1)
                        .substring(initialFileList.get(varMap.get(s).getDeclareLine() - 1).indexOf("=") == -1 ? 0 : initialFileList.get(varMap.get(s).getDeclareLine() - 1).indexOf("=")
                        ))) {
                    liftedVarToGobal.add((isNeedAddStatic ? "static " : "") + initialFileList.get(varMap.get(s).getDeclareLine() - 1).trim());
                    deleteDeclaration.add(varMap.get(s).getDeclareLine());
                } else {
                    if(!isCreateGlobalPointer){
                        isLinkedCorrect = false;
                    }
                }
                if(isCreateGlobalPointer && checkInitElement(varName)){
                    liftedVarToGobal.add("static " + varMap.get(s).getType().replaceAll("\\[.*?\\]", "") + " *ga_" + gaCnt++ + " = &" + varName + ";");//一个全局指针指向全局变量
                    leftType = varMap.get(s).getType().replaceAll("\\[.*?\\]", "");
                    leftVarName = varName;
                }
                break;
            }
        }
    }

    public boolean isHaveOp(String line){
        return line.contains("+=") || line.contains("-=") ||
                line.contains("^=") || line.contains("&=") ||
                line.contains("*=") || line.contains(">>=") ||
                line.contains("<<=") || line.contains("/=") ||
                line.contains("%=") ||line.contains("|=");
    }


    public boolean genMutate(){
        boolean isCorrect = false;
        File muDir = new File(PropertiesInfo.mutateStatuteDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        CommonInfoFromFile.cpOriginalFile(muDir, file);

        File muFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_statute_" + (mutCount++) + ".c");
        writeMuFile(muFile);

        File initFile = new File(muDir.getAbsolutePath() + "/" + file.getName());
        String initialResult = new WriteMutate(sanitizerCompiler).getFileExecResult(initFile);
        if(!initialResult.contains("checksum")) isCorrect = false;

        String result = new WriteMutate(sanitizerCompiler).getFileExecResult(muFile);
        if(result.contains("error") || result.equals("timeout")) isCorrect = false;
        else isCorrect = result.equals(initialResult);

        if(!isCorrect) {
            File errorsDir = new File(PropertiesInfo.indexDir + "/statute-errors/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
            if(!errorsDir.exists()){
                errorsDir.mkdirs();
            }
            ProcessTerminal.voidNotMemCheck("cp " + muFile.getAbsolutePath() +  " " + errorsDir.getAbsolutePath(), "sh");
            muFile.delete();
        }

        return isCorrect;
    }

    public void writeMuFile(File muFile){
        checkFileExists(muFile);
        try {
            FileWriter fw = new FileWriter(muFile, true);
            PrintWriter pw = new PrintWriter(fw);
            int count = 0;
            for(String line: initialFileList){
                count++;
                if(deleteDeclaration.contains(count)) continue;
                if(count == getGlobalDeclareLine()){
                    CommonOperation.removeDuplicates(liftedVarToGobal).forEach(pw::println);
                    pw.println(line);
                }
                else if(addedBeforeLoopStart.containsKey(count)){
                    addedBeforeLoopStart.get(count).forEach(pw::println);
                    pw.println(line);
                }
                else if(addedBeforeLoopEnd.containsKey(count)){
                    pw.println(line);
                    CommonOperation.removeDuplicates(addedBeforeLoopEnd.get(count)).forEach(pw::println);
                }
                else if(addedAfterLoopEnd.containsKey(count)){
                    pw.println(line);
                    CommonOperation.removeDuplicates(addedAfterLoopEnd.get(count)).forEach(pw::println);
                } else {
                    pw.println(line);
                }
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

    public int getGlobalDeclareLine(){
        int count = 0;
        for(String s: initialFileList){
            count++;
            if(s.trim().matches("(\\s*\\w+\\s+)+func_\\d+\\s*\\(\\s*(?:\\w+\\s*(?:\\*+\\s*)?)*\\);"))
                return count - 2;
        }
        return 0;
    }

    public void getPointerAnalysis(){
        //export LD_LIBRARY_PATH=" + PropertiesInfo.LLVM_HOME + "/lib:$LD_LIBRARY_PATH &&
        String command = "export LD_LIBRARY_PATH=" + PropertiesInfo.pointerAnalysisExport + ":$LD_LIBRARY_PATH" + " && cd " + PropertiesInfo.indexDir + " && " +
                "./pointer_parser " + file.getAbsolutePath();
        pointerAnalysisList = ProcessTerminal.listNotMemCheck(command, "sh");
    }

    public void getAllCMPVar(){
        int funcStartLine = CommonInfoFromFile.getFuncStartLine(initialFileList);
        for(String line: initialFileList) {
            CMPVariable cmp = new CMPVariable(varMap, cmpVarSet, funcStartLine);
            cmp.run(line);
        }
    }

    public int countStars(String input) {
        int count = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '*') {
                count++;
            }
        }
        return count;
    }

    public void getAllPointers(){
        for(String s: pointerAnalysisList){
            Pattern p = Pattern.compile("Pointer:\\s*(.*)\\s*\\((.*)\\):.*");
            Matcher m = p.matcher(s);
            if(m.find()){
                String pointerName = m.group(1).trim();
                String pointerType = m.group(2).trim();
                List<String[]> pointerLink = new ArrayList<>();
                for(String singleLevel: s.substring(s.lastIndexOf(":") + 1).split("->")){
                    Pattern p_link = Pattern.compile("\\s*(.*)\\s*\\((.*)\\)");
                    Matcher m_link = p_link.matcher(singleLevel);
                    if(m_link.find())
                        pointerLink.add(new String[]{m_link.group(1).trim(), m_link.group(2).trim()});
                    else
                        pointerLink.add(new String[]{"null", "null"});
                }
                allPointerInfoList.add(new PointerInfo(pointerName, pointerType, pointerLink));
            }
        }
    }

    public void printLastLine(List<String> list){
        System.out.println(list.get(list.size() - 1));
    }

    public String getDataType(String input) {
        if(input.trim().startsWith("(") && input.trim().endsWith(")")){
            input = input.trim().substring(1, input.trim().length() - 1);
        }
        String regex = "(0x)?\\-?[0-9a-fA-F]+(U?L{0,2})?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            String suffix = matcher.group(2);
            if (suffix == null) {
                return "int";
            }
            switch (suffix) {
                case "UL":
                case "LU":
                    return "unsigned long";
                case "ULL":
                case "LLU":
                    return "unsigned long long";
                case "L":
                    return "long";
                case "LL":
                    return "long long";
                case "U":
                    return "unsigned int";
                default:
                    return "int";  // 如果没有后缀或不符合其他情况，返回 int
            }
        }
        return "No match";  // 如果输入不匹配
    }
}
