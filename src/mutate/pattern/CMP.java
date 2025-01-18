package mutate.pattern;

import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.model.AstVariable;
import AST_Information.model.LoopStatement;
import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import common.CommonInfoFromFile;
import common.PropertiesInfo;
import mutate.minmax.SafeMathMacrosAnalysis;
import utity.AvailableVariable;
import utity.SafeMathMacros;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CMP {
    AstInform_Gen astgen;
    Map<Integer, Integer> loopMap = new HashMap<>();
    Map<String, AstVariable> varMap;
    File file;
    public List<AvailableVariable> avarList = new ArrayList<>();

    List<String> initialFileList;
    String loopIndex = "";
    String loopIndexType = "";
    int currentLineNumber = 0;
    int startLine = 0;
    int endLine = 0;
    Set<Integer> checkedLines = new HashSet<>();

    int mutCount = 0;

    public CMP(File file){
        this.file = file;
        initialFileList = CommonOperation.genInitialList(file);
        astgen = new AstInform_Gen(file);
        varMap = astgen.allVarsMap;
        getLoopMap();
    }

    public CMP(File file, List<String> initialFileList, AstInform_Gen astgen,
               Map<String, AstVariable> varMap, Map<Integer, Integer> loopMap, List<AvailableVariable> avarList){
        this.file = file;
        this.initialFileList = initialFileList;
        this.astgen = astgen;
        this.varMap = varMap;
        this.loopMap = loopMap;
        this.avarList = avarList;
    }

    public void getLoopMap(){
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);

        for (LoopStatement loop : loopList) {
            loopMap.put(loop.getStartLine(), loop.getEndLine());
        }
    }

    public static void main(String[] args) {
        File file = new File("/home/sdu/Desktop/random10054" + ".c");
        File genMuDir = new File(PropertiesInfo.mutateLoopIndexDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        if(genMuDir.exists()){
            FileModify fm = new FileModify();
            fm.deleteFolder(genMuDir);
        }
        CMP cmp = new CMP(file);
        cmp.run();
    }


    public void run(){
        int count = 0;
        while(count++ < 1){
            if(runCMP()) break;
        }
    }

    public boolean runCMP(){
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
//                        System.out.println("yes for: " + line);
                        loopIndex = m.group(1);
                        if(!loopIndex.matches("[ijklmn]")) {
                            loopIndexType = getAvarType(loopIndex);
                            String replacedSafeMath = narrowInvariantType(loopIndex, m.group(2).trim(), loopIndexType);
                            if(!replacedSafeMath.equals("Not match")) {
                                initialFileList.set(currentLineNumber - 1,
                                        initialFileList.get(currentLineNumber - 1).substring(0, m.start(2))
                                                + loopIndex + " = safe_" + replacedSafeMath
                                                + initialFileList.get(currentLineNumber - 1).substring(m.end(2)));
//                                System.out.println(initialFileList.get(currentLineNumber - 1));
                            } else {
//                                System.out.println("cmp: " + line);
//                                System.out.println("cmp line:  ---" + initialFileList.get(currentLineNumber - 1));
//                                System.out.println("cmp startLine: " + startLine + " endline: " + endLine + " loop index: " + loopIndex + " type: " + loopIndexType);
                            }
                        }
                    } else {
//                        System.out.println("no for: " + line);
                    }
                }
            }

        }

//        boolean isSuccess = genMutate();
//        System.out.println(file.getName() + " " + isSuccess);
//        return isSuccess;
        return true;
    }

    //将expression变成safe_math的形式
    public String narrowInvariantType(String loopIndex, String expression, String variableType) {
//        System.out.println("third expression: " + expression);
        if(expression.contains("safe_")) return "Not match";
        // 匹配操作符和操作数
        Pattern pattern = Pattern.compile("(\\+\\+|--)?(" + CommonInfoFromFile.replaceRegex(loopIndex) + ")\\s*([+-/*]=|\\+\\+|--|=)?\\s*(\\d*)");
        Matcher matcher = pattern.matcher(expression);
        if (matcher.find()) {
            String variableName = loopIndex;  // 变量名
            String operator = matcher.group(1) == null || matcher.group(1).isEmpty() ? matcher.group(3) : matcher.group(1);      // 操作符
            String operand =  matcher.group(4) == null || matcher.group(4).isEmpty() ? "1" : matcher.group(4);  // 操作数（默认是 1）

            // 分析变量的类型，确定是否无符号以及其位宽
            Matcher typeMatcher = Pattern.compile("(u?)int(\\d+)_t").matcher(variableType);
            if (!typeMatcher.find()) {
                return "Not match";
            }
            String signPrefix = typeMatcher.group(1) == null || typeMatcher.group(1).isEmpty() ? "s" : typeMatcher.group(1);  // 无符号（"u"）或有符号（""）
            int bitWidth = Integer.parseInt(typeMatcher.group(2));  // 变量的位宽（8、16、32、64）
//            System.out.println(variableName + " " + operator + " " + operand + " " + signPrefix + " " + bitWidth + " ");

            // 确定操作类型
            String operationType;
            switch (operator.trim()) {
                case "+=":
                case "++":
                    operationType = "add";
                    break;
                case "-=":
                case "--":
                    operationType = "sub";
                    break;
                case "*=":
                    operationType = "mul";
                    break;
                case "/=":
                    operationType = "div";
                    break;
                default:
                    return "Not match";
            }

            // 根据操作符查找比当前类型更小的 SafeMath 函数
            List<SafeMathMacros> compatibleFunctions = new ArrayList<>();
            Map<String, SafeMathMacros> allSafeMathMap = SafeMathMacrosAnalysis.getSafeMathMacrosMap();
            for (String functionName : allSafeMathMap.keySet()) {
                SafeMathMacros function = allSafeMathMap.get(functionName);
                if (function.getOpType().equals(operationType) && function.getSuTrans().equals(signPrefix + "_" + signPrefix)) {
                    int functionBitWidth = Integer.parseInt(function.getReturnType().replaceAll("u?int", "").replace("_t", ""));
                    if (functionBitWidth < bitWidth || (bitWidth == 8 && functionBitWidth == 8)) {
                        compatibleFunctions.add(function);
                    }
                }
            }

            // 随机选取一个兼容的 SafeMath 函数
            if (!compatibleFunctions.isEmpty()) {
                SafeMathMacros selectedFunction = compatibleFunctions.get(new Random().nextInt(compatibleFunctions.size()));
                return selectedFunction.getFunctionName() + "(" + variableName + ", " + operand + ")";
            } else {
                return "Not match";
            }
        }
        return "Not match";
    }


    public String getAvarType(String avarName){
        for(AvailableVariable av: avarList){
            if(av.getValue().equals(avarName))
                return av.getType();
        }
        return "int32_t";
    }

    public boolean genMutate(){
        boolean isCorrect = false;
        File muDir = new File(PropertiesInfo.mutateLoopIndexDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        CommonInfoFromFile.cpOriginalFile(muDir, file);

        File muFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_loopIndex_" + (mutCount++) + ".c");
        writeMuFile(muFile);

        isCorrect = CommonInfoFromFile.checkCorrection(muFile);
        if(!isCorrect) {
//            mutCount--;
//            muFile.delete();
            System.out.println(file.getName() + " " + isCorrect);
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

    public int getGlobalDeclareLine(){
        int count = 0;
        for(String s: initialFileList){
            count++;
            if(s.trim().matches("(\\s*\\w+\\s+)+func_\\d+\\s*\\(\\s*(?:\\w+\\s*(?:\\*+\\s*)?)*\\);"))
                return count - 2;
        }
        return 0;
    }

}
