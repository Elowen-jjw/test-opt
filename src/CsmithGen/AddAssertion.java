package CsmithGen;

import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.VarInform;
import AST_Information.model.AstVariable;
import AST_Information.model.LoopStatement;
import ObjectOperation.datatype.Data;
import ObjectOperation.datatype.StringOperation;
import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import ObjectOperation.structure.FindIfInfo;
import common.ExceptionCheck;
import common.PropertiesInfo;
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

import static common.before.LoopExecValues.deleteAoutFile;

public class AddAssertion {
    File file;
    File printfFile;
    File assertFile;
    List<String> initialFileList;
    AstInform_Gen astgen;
    Map<String, AstVariable> varMap;
    Map<Integer, Integer> loopMap = new HashMap<>();
    List<IfInfo> ifList = new ArrayList<>();
    List<AvailableVariable> avarList = new ArrayList<>();
    List<AstVariable> astVarList = new ArrayList<>();
    List<AddInfo> addInfoList = new ArrayList<>();
    Set<Integer> declareLines = new HashSet<>();
    Map<Integer, Set<String>> assertionMap = new HashMap<>();

    List<Interval> blockIntevals = new ArrayList<>();

    public AddAssertion(File file){
        this.file = file;
        this.printfFile = new File(file.getParent() +
                "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_printf_i.c");
        this.assertFile = new File(PropertiesInfo.testSuiteAssertDir +
                "/" + file.getName());

        this.initialFileList = CommonOperation.genInitialList(file);
        this.astgen = new AstInform_Gen(file);
        this.varMap = astgen.allVarsMap;
        ifList = new FindIfInfo().findAllIfInfo(initialFileList);
        getLoopMap();
        getAvarList();
        getBlockIntevals();
    }

    public void getBlockIntevals(){
        for(Map.Entry<Integer, Integer> entry: loopMap.entrySet()) {
            blockIntevals.add(new Interval(entry.getKey(), entry.getValue()));
        }
        for(IfInfo iif: ifList){
            if(iif.getElseLine() == -1)
                blockIntevals.add(new Interval(iif.getStartLine(), iif.getEndLine()));
            else {
                blockIntevals.add(new Interval(iif.getStartLine(), iif.getElseLine()));
                blockIntevals.add(new Interval(iif.getElseLine(), iif.getEndLine()));
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
        for(String s: varMap.keySet()){
            astVarList.add(varMap.get(s));
            declareLines.add(varMap.get(s).getDeclareLine());
//            System.out.println(varMap.get(s).getName() + ": " + varMap.get(s).getType());
            for(AvailableVariable avar: VarInform.getAvarFromAstVar(varMap.get(s), astgen)){
//                System.out.println("   " + avar.getValue() + ": " + avar.getType());
            }
        }
        //修改了varInform，可以得到关于union和结构体本身的变量，而不仅仅是成员变量
        List<AvailableVariable> overallavarList = VarInform.getInitialAvailableVarList(astVarList, astgen);
        //删除struct或者union本身变量
        avarList = VarInform.removeStructAndUnionOverall(overallavarList);
        //int to int32_t long to int64_t
        for(AvailableVariable av: avarList){
            if(PropertiesInfo.commonToStandardType.containsKey(av.getType())){
                av.setType(PropertiesInfo.commonToStandardType.get(av.getType()));
            }
        }
    }

    public static void main(String[] args) {

        File folder = new File("/home/sdu/Desktop/RandomSuite_Struct");
        for(File file: folder.listFiles()) {
            AddAssertion aa = new AddAssertion(file);
            aa.run();
        }
    }

    public void run(){
        initialSearchLines();
        getPrintfList();
        genPrintfFile();
        List<String> execLines = runPrintFile();
        extractPrintfValue(execLines);
        addAssertionToOriginal();
        printfFile.delete();

//        execLines.forEach(s -> System.out.println(s));
        //get min and max for every printed variable and present this scope using assertions
    }

    public void initialSearchLines(){
        //printf statements added in the front of the end of if-then-else blocks
        for(IfInfo iif: ifList){
            Set<Integer> searchLineNumber = new TreeSet<>();
            for(int i = iif.getStartLine(); i <= iif.getEndLine(); i++){
                searchLineNumber.add(i);
            }
            addInfoList.add(new AddInfo("if", searchLineNumber, iif.getStartLine(), iif.getEndLine(), iif.getEndLine() + 1));
        }
        //printf statements added in the front of the last line of inner statements
        for(Map.Entry<Integer, Integer> entry: loopMap.entrySet()){
            int startLine = entry.getKey();
            int endLine = entry.getValue();
            Set<Integer> searchLineNumber = new TreeSet<>();
            for(int i = startLine; i <= endLine; i++){
                searchLineNumber.add(i);
            }
            addInfoList.add(new AddInfo("loop", searchLineNumber, startLine, endLine, initialFileList.get(endLine - 2).contains("return") ? endLine - 1 : endLine));
        }
    }

    public void getPrintfList(){
        for(AddInfo singleInfo: addInfoList){
            List<String> searchLines = new ArrayList<>();
            for(int i: singleInfo.searchLineNumber){
                if(declareLines.contains(i)) continue;
                searchLines.add(initialFileList.get(i - 1));
            }

            Set<String> varNames = new HashSet<>();
            Set<String> varArrayElements = new HashSet<>();
            List<String> printfList = new ArrayList<>();
            for(String s: searchLines){
                if(!s.contains("g_") && !s.contains("l_") && !s.contains("p_")) continue;
                for(AstVariable astVar: astVarList){
                    String varName = astVar.getName();
                    //exclude the variables called i, j, k, l, m, n this common loop index
                    if(varName.trim().matches("[ijklmno]")) continue;

                    //exclude the situation that print a pointer before initial it, because it doesn't mean that it's a null pointer
                    if(astVar.getKind().equals("array") && StringOperation.containsVarName(s, varName + "[i]")) continue;
                    if(StringOperation.containsVarName(s, varName) && !varNames.contains(varName)){//exclude the situation that print a variable outside its reasonable value scope
                        //对于使用的变量如果定义在if或者loop内，这里现在是直接skip掉。
                        //处理方法：写出每一个变量声明所在行最小的对应block区间，对于使用到的变量，看插入所在行是否在这个最小区间内，如果是，则添加进printf中，反之，continue；
                        Interval interval = findMinInterval(blockIntevals, astVar.getDeclareLine());
                        if(interval != null && interval.end < singleInfo.printfLine) continue;

                        boolean isHaveSpecific = false;
                        //
                        List<AvailableVariable> transedAvarList = VarInform.getAvarFromAstVar(astVar, astgen);
                        if(transedAvarList.size() > 1){
                            for (AvailableVariable avar : transedAvarList) {
                                String valueToCheck = astVar.getKind().equals("array") ? avar.getValue().replaceAll("\\.f\\d+", "") : avar.getValue();
                                if (StringOperation.containsVarName(s, valueToCheck)) {
                                    isHaveSpecific = true;
                                    if(!varArrayElements.contains(avar.getValue())) {
                                        varArrayElements.add(avar.getValue());
                                        printfList.add(Data.addPrintf(singleInfo.printfLine, avar.getType(), avar.getValue(), avar.getValue()));
                                    }
                                }
                            }

                        }

                        if(!isHaveSpecific) {
                            varNames.add(varName);
                            for (AvailableVariable avar : transedAvarList)
                                printfList.add(Data.addPrintf(singleInfo.printfLine, avar.getType(), avar.getValue(), avar.getValue()));
                        }
                    }
                }
            }

            singleInfo.printfList = printfList;
//            printfList.forEach(s -> System.out.println(s));
        }
    }

    public void genPrintfFile(){
        try {
            FileWriter fw = new FileWriter(printfFile);
            PrintWriter pw = new PrintWriter(fw);

            int count = 0;
            for(String line: initialFileList){
                count++;
                List<String> addedList = isNeedAddPrintf(count);
                if(!addedList.isEmpty()){
                    pw.println("// add printf statements: ");
                    addedList.forEach(pw::println);
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

    public List<String> runPrintFile(){
        String aoutFilename = printfFile.getName().substring(0, printfFile.getName().lastIndexOf(".c"))
                .replace("random", "").replace("printf", "pf").replaceAll("_", "");

        String command = "export LANGUAGE=en && export LANG=en_US.UTF-8 && "
                + "cd " + printfFile.getParent() + " && " + "gcc" + " " + printfFile.getName()
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

    public void extractPrintfValue(List<String> execLines){
        Map<Integer, Map<String, Set<BigInteger>>> pVarValueMap = analysisComparableExecLines(execLines);
        if (pVarValueMap == null)
            System.out.println(printfFile.getName() + " pVar printf file generation core dumped.....");
        else if (pVarValueMap.isEmpty())
            System.out.println(printfFile.getName() + " pVar printf file generation does not have print!!!!");
        else {
            for(Integer i: pVarValueMap.keySet()){
                for(String s: pVarValueMap.get(i).keySet()){
                    String assertionStmt = "";
                    List<BigInteger> valuesList = new ArrayList<>(pVarValueMap.get(i).get(s));
                    if(pVarValueMap.get(i).get(s).size() == 1){
                        assertionStmt = s + " == " + valuesList.get(0);
                    } else {
                        assertionStmt = s + " >= " + valuesList.get(0)
                                + " && " + s + " <= " + valuesList.get(valuesList.size() - 1);
                    }

                    //add assertion statements to an overall map
                    if(assertionMap.containsKey(i)){
                        assertionMap.get(i).add("assert(" + assertionStmt + ");");
                    } else {
                        assertionMap.put(i, new TreeSet<>(Arrays.asList("assert(" + assertionStmt + ");")));
                    }
                }
            }
        }
        //print
//        for(int i: assertionMap.keySet()){
//            assertionMap.get(i).forEach(s -> System.out.println(i + ": " + s));
//        }
    }

    public Map<Integer, Map<String, Set<BigInteger>>> analysisComparableExecLines(List<String> execLines){
        Map<Integer, Map<String, Set<BigInteger>>> pVarValueMap = new TreeMap<>();
        for (String s : execLines) {
            if(s.contains("core dumped") || s.contains("error: ")) return null;
            Matcher m = Pattern.compile("(\\d+)\\s*:\\s*(.+)\\s*@\\s*(.*)").matcher(s);
            if(m.find()){
                int lineNumber = Integer.parseInt(m.group(1).trim());
                String varName = m.group(2).trim();
                if(m.group(3).trim().equals("NULL")) continue;
                BigInteger decimalValue = StringOperation.convertToDecimal(getAvarType(varName), m.group(3).trim());
                if(pVarValueMap.containsKey(lineNumber)){
                    if(pVarValueMap.get(lineNumber).containsKey(varName)){
                        pVarValueMap.get(lineNumber).get(varName).add(decimalValue);
                    } else {
                        pVarValueMap.get(lineNumber).put(varName, new TreeSet<>(Arrays.asList(decimalValue)));
                    }
                } else {
                    pVarValueMap.put(lineNumber, new TreeMap<>(){{
                        put(varName, new TreeSet<>(Arrays.asList(decimalValue)));
                    }});
                }
            }
        }
        return pVarValueMap;
    }

    public void addAssertionToOriginal(){
        try {
            FileWriter fw = new FileWriter(assertFile);
            PrintWriter pw = new PrintWriter(fw);

            int count = 0;
            for(String line: initialFileList){
                count++;
                if(assertionMap.containsKey(count)){
                    assertionMap.get(count).forEach(pw::println);
                }
                pw.println(line);
            }

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

            ExceptionCheck ec = new ExceptionCheck();
            boolean isCorrect = ec.filterUB(assertFile);
            if(!isCorrect) {
                System.out.println(file.getName() + ": " + isCorrect);
                assertFile.delete();
            } else {
                FileModify.formatFile(assertFile);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Interval findMinInterval(List<Interval> intervals, int integer) {
        intervals.sort(Comparator.comparingInt((Interval a) -> a.start).thenComparingInt(a -> a.end));

        Interval minInterval = null;

        for (Interval interval : intervals) {
            if (interval.start <= integer && interval.end >= integer) {
                if (minInterval == null || (interval.end - interval.start < minInterval.end - minInterval.start)) {
                    minInterval = interval;
                }
            }
        }

        if (minInterval != null) {
            return minInterval;
        }

        return null;
    }

    public String getAvarType(String avarName){
        for(AvailableVariable av: avarList){
            if(av.getValue().equals(avarName))
                return av.getType();
        }
        return "int32_t";
    }

    public List<String> isNeedAddPrintf(int count){
        for(AddInfo singleInfo: addInfoList){
            if(singleInfo.printfLine == count)
                return singleInfo.printfList;
        }
        return new ArrayList<>();
    }

    class AddInfo{
        String flag;
        Set<Integer> searchLineNumber;
        int startLine;
        int endLine;
        int printfLine;
        List<String> printfList;

        AddInfo(){}
        AddInfo(String flag, Set<Integer> searchLineNumber, int startLine, int endLine, int printfLine){
            this.flag = flag;
            this.searchLineNumber = searchLineNumber;
            this.startLine = startLine;
            this.endLine = endLine;
            this.printfLine = printfLine;
        }
    }

    class Interval {
        int start;
        int end;

        public Interval(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }


}
