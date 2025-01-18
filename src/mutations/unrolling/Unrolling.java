package mutations.unrolling;

import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.model.AstVariable;
import astinfo.model.LoopStatement;
import objectoperation.datatype.IntegerOperation;
import objectoperation.list.CommonOperation;
import objectoperation.structure.FindInfoInLoop;
import utity.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Unrolling {
    AstInform_Gen astgen;
    File initialFile;
    List<String> initialFileList;
    int startLine;
    int endLine;
    LoopStatement currentLoop;

    Set<Integer> tempSet = new TreeSet<>();

    List<String> initialBlockList;
    InitialAndTransBlock newIta;
    List<InitialAndTransBlock> newItaList = new ArrayList<>();

    public boolean isTrans;

    String regexFor = "\\bfor\\s*\\((.*);(.*);(.*?)\\)\\s*\\{";
    String regex_symbol = "[<>!]=?";
    Pattern pFor = Pattern.compile(regexFor);
    Pattern p_symbol = Pattern.compile(regex_symbol);

    int loopExecTimes;
    String firstFactor;
    String secondFactor;
    String thirdFactor;

    public Unrolling(LoopStatement loop, int loopExecTimes){
        this.currentLoop = loop;
        this.loopExecTimes = loopExecTimes;
    }

    public void unrolling(FixedStuff fs){
        astgen = fs.getAst();
        initialFile = fs.getInitialFile();
        initialFileList = CommonOperation.genInitialList(initialFile);
        startLine = fs.getStartLine();
        endLine = fs.getEndLine();
        initialBlockList = fs.getIatList().get(0).getInitialBlock().getBlockList();
        getInsideMap();
        isTrans = true;

        for(InitialAndTransBlock ita: fs.getIatList()){
            newIta = new InitialAndTransBlock(ita);//backups
            initialBlockList = newIta.getInitialBlock().getBlockList();
            Trans();
            newItaList.add(newIta);
        }
        fs.setIatList(newItaList);
    }

    public void getInsideMap(){
        FindInfoInLoop fil = new FindInfoInLoop();
        List<IfInLoop> ifList = fil.findInfoInLoop(initialBlockList);
        for(IfInLoop singleIf: ifList){
            int start = singleIf.getStartLine();
            int end = singleIf.getEndLine();
            for(int i=start; i<=end; i++){
                tempSet.add(i);
            }
        }
//        System.out.println(tempSet);
    }

    public void Trans(){
        if(!isAvailableTrans()){
            isTrans = false;
            return;
        }
        modifyTransformed();
        modifyInitial();
    }

    public boolean isAvailableTrans(){
        String header = initialBlockList.get(0);
        Matcher mFor = pFor.matcher(header);
        if (mFor.find()) {
            firstFactor = mFor.group(1).trim();
            secondFactor = mFor.group(2).trim();
            if(secondFactor.startsWith("(") && secondFactor.endsWith(")")){
                secondFactor = secondFactor.substring(1, secondFactor.length() - 1);
            }
            thirdFactor = mFor.group(3).trim();
        }
        else{
            return false;
        }

        Matcher matcher_symbol = p_symbol.matcher(secondFactor);
        if(!matcher_symbol.find()) {
            return false;
        }

        return true;
    }

    public void modifyInitial(){
        initialBlockList.add(0, "//mutations.unrolling initial: ");
    }

    public void modifyTransformed(){

        List<ProcessingBlock> transBlockList = new ArrayList<>();

        Map<String, Integer> strNum = new HashMap<>();
        List<Number> numList = new ArrayList<>();
        Map<String, String> numOp = new HashMap<>();
        List<String> thirdOther = new ArrayList<String>();
        Map<Integer, List<AstVariable>> tempVar = new HashMap<>();
        List<String> inner = new ArrayList<String>();

        String header = initialBlockList.get(0);

        String regex_fornum = "([a-zA-Z_][a-zA-Z0-9\\[\\]_]*\\.?[a-zA-Z0-9\\[\\]_]*)\\s*[!<>]=?";
        Pattern p_fornum = Pattern.compile(regex_fornum);
        Matcher matcher_fornum;
        Matcher matcher_symbol;

        matcher_symbol = p_symbol.matcher(secondFactor);
        //满足second str < num
        if(matcher_symbol.find()) {
            matcher_fornum = p_fornum.matcher(secondFactor);
            while(matcher_fornum.find()) {
                String str = matcher_fornum.group(1).trim();
                strNum.put(str, loopExecTimes);
            }

            Iterator<Map.Entry<String, Integer>> it1 = strNum.entrySet().iterator();
            while(it1.hasNext()) {
                Map.Entry<String, Integer> entry = it1.next();
                int value = entry.getValue();
                int remainder = 0;
                //执行次数是否是合数，是合数remainder = 0，是素数寻找<n且因数最多的number为value, remainder为差值
                if(!IntegerOperation.prime(value)) {
                    remainder = 0;
                }
                else {
                    if(value != 2 && value != 3){
                        int newValue = IntegerOperation.findComNum(value);
                        remainder = value - newValue;
                        value = newValue;
                    }
                }
                List<Integer> factor = new ArrayList<Integer>();
                if(value == 2) {
                    factor.add(2);
                    remainder = 0;
                }
                else if(value == 3) {
                    value = 2;
                    factor.add(2);
                    remainder = 1;
                }
                else {
                    for(int i = 2; i <= (value/2); i++) {
                        if(value % i == 0 ) {
                            factor.add(i);
                        }
                    }
                }
                numList.add(new Number(value, factor, remainder));
            }

            //deal with thirdFactor of for statement
            for(String s: thirdFactor.split(",")) {
                if(s.contains("++")) {
                    numOp.put(s.replace("++", "").trim(), "+=1");
                    thirdOther.add(s.replace("++", "").trim() + "+=1");
                    header = header.replace(s, s.replace("++", "").trim() + "+=1");
                }
                else if(s.contains("--")) {
                    numOp.put(s.replace("--", "").trim(), "-=1");
                    thirdOther.add(s.replace("--", "").trim() + "-=1");
                    header = header.replace(s, s.replace("--", "").trim() + "-=1");
                }
                else if(s.contains("+=")) {
                    int opIndex = s.indexOf("+=");
                    numOp.put(s.substring(0, opIndex).trim(), s.substring(opIndex).trim());
                    thirdOther.add(s.substring(0, opIndex).trim() + s.substring(opIndex).trim());
                    header = header.replace(s, s.substring(0, opIndex).trim() + s.substring(opIndex).trim());
                }
                else if(s.contains("-=")) {
                    int opIndex = s.indexOf("-=");
                    numOp.put(s.substring(0, opIndex).trim(), s.substring(opIndex).trim());
                    thirdOther.add(s.substring(0, opIndex).trim() + s.substring(opIndex).trim());
                    header = header.replace(s, s.substring(0, opIndex).trim() + s.substring(opIndex).trim());
                }
            }

            //deal with varList 在此循环体中的临时变量在第几行被定义 且如果存在嵌套的情况这个temp variable declare一定在循环体之前被定义
            List<AstVariable> tempInsideDeclareList = currentLoop.getInsideList();
            List<AstVariable> declareList = new ArrayList<>();
            for(AstVariable ti: tempInsideDeclareList) {
                if(!tempSet.contains(ti.getDeclareLine() - startLine + 1)) {
//                    System.out.println("tempVar: " + ti.getDeclareLine()+ ": " + ti.getName() + ti.getIsConst());

                    int declareLine = ti.getDeclareLine() - startLine;
                    declareList = tempVar.getOrDefault(declareLine, new ArrayList<>());
                    declareList.add(ti);
                    tempVar.put(declareLine, declareList);
                }
            }

            //inner
            if(startLine + 1 == endLine)
                inner.add(" ");
            else
                inner = CommonOperation.getListPart(initialBlockList, 2, initialBlockList.size()-1);
        }

        Iterator<Map.Entry<String, Integer>> it2 = strNum.entrySet().iterator();
        int it2Index = 0;
        while(it2.hasNext()) {
            Map.Entry<String, Integer> next = it2.next();
            String key = next.getKey();

            if(numOp.containsKey(key)) {
                List<String> replicaThirdOther = new ArrayList<String>();
                replicaThirdOther.addAll(thirdOther);
                String op1 = numOp.get(key);
//                replicaThirdOther.remove(key+op1);

                String op2 = "";
                String op4 = "";
                String initN = "1";
                if(op1.contains("+=")) {
                    op2 = "+";
                    op4 = "-";
                    initN = op1.replace("+=", "").trim();
                }
                else if(op1.contains("-=")) {
                    op2 = "-";
                    op4 = "+";
                    initN = op1.replace("-=", "").trim();
                }

                List<Integer> factor = numList.get(it2Index).getFactor();
                int remainder = numList.get(it2Index).getRemainder();

                String regexKey = replaceRegex(key);

                Pattern p_remainder = Pattern.compile(regexKey+"\\s*[<>!]=?");
                Matcher m_remainder = p_remainder.matcher(secondFactor);
                String valueSecondFactor = secondFactor;
                String remainderOp = "";
                boolean isHaveEqual = false;//记录有无=号

                if(remainder != 0 && m_remainder.find()) {
                    int end = m_remainder.end();
                    String group = m_remainder.group();
                    if(group.contains("=") && !group.contains("!=")) {
                        isHaveEqual = true;
                    }
                    remainderOp = "(" + secondFactor.substring(end).trim() + op4 + remainder + " * " + initN + ")";
                    valueSecondFactor = secondFactor.substring(0, end) + remainderOp;
                }

                for(int f: factor) {
//                    String newThird = key+" "+op2+"= ("+f + " * " + initN + ")";
//                    String newheader = header.replace(key+op1, newThird);
//                    newheader = newheader.replace(secondFactor, valueSecondFactor);
                    String newheader = header.replace(secondFactor, valueSecondFactor);
                    List<String> transBlock = new ArrayList<>();
                    transBlock.add("//mutations.unrolling trans:");
                    transBlock.add(newheader);

                    for(int i=0; i<f; i++) {
//                        String op3 = op2;
                        int index = i - 1;
                        List<String> newInner = new ArrayList<>();
                        CommonOperation.copyStringList(newInner, inner);
                        for(int j=0; j<inner.size();j++) {
                            String s = newInner.get(j);

                            if(i != 0) {
                                if(tempVar.containsKey(j+1)) {
                                    List<AstVariable> tempavList = tempVar.get(j + 1);
                                    for (AstVariable tempav : tempavList) {
                                        if (tempav.getIsConst()) {
                                            s = "";
                                        } else if (s.contains("static ")) {
                                            s = "";
                                        } else {
                                            String varName = tempav.getName();
                                            String newVarName = varName + "_" + index;
                                            //s = s.replace(varName, newVarName);
                                            for (int ii = j; ii < newInner.size(); ii++) {
                                                String newInnerLine = newInner.get(ii);

                                                String newExp = "";
                                                int index_start = 0;
                                                String regexKey1 = replaceRegex(varName);
                                                String regex = "(\\W|^)" + regexKey1 + "(\\W|$)";
                                                Pattern p = Pattern.compile(regex);
                                                Matcher m = p.matcher(newInnerLine);
                                                while (m.find()) {
                                                    int start = m.start();
                                                    if (start > 0 && newInnerLine.charAt(start) == '.')
                                                        continue;    //结构体Struct S A; A.a中的点；
                                                    else if (newInnerLine.substring(0, start + 1).matches(".*->\\s*"))
                                                        continue;    //结构体指针Sttuct S *A; A->a中的a
                                                    else
                                                        newExp += newInnerLine.substring(index_start, start) + m.group().replace(varName, newVarName);
                                                    index_start = m.end();
                                                }
                                                newExp += newInnerLine.substring(index_start, newInnerLine.length());

                                                newInner.set(ii, newExp);
                                            }

                                            s = newInner.get(j);

//                                        int firstIndex = getVarFirstIndex(tempav.getDeclareLine());
//                                        s = s.substring(firstIndex);
                                        }
                                    }
                                }
                            }
                            transBlock.add(s);
                        }
                        if(i != (f-1)) {
                            for(String s: replicaThirdOther) {
                                transBlock.add(s+";");
                            }
                        }
                    }
                    transBlock.add("   }\n");
                    if(remainder != 0) {
                        StringBuilder sb = new StringBuilder(header);
                        boolean isFirstBlock = sb.substring(sb.indexOf("(") + 1, sb.indexOf(";")).trim().equals("");
                        String addComma= isFirstBlock? "": ", ";
                        sb.insert(sb.indexOf(";"), addComma + key + " = " + remainderOp + (isHaveEqual? (op2 + "1*" + initN): ""));
                        transBlock.add(sb.toString());
                        transBlock.addAll(inner);
                        transBlock.add("   }\n");
                    }

                    ProcessingBlock pb = new ProcessingBlock(transBlock);
                    transBlockList.add(pb);

                }

            }
            it2Index++;
        }
        newIta.setTransformedBlockList(transBlockList);
    }

    public int getVarFirstIndex(int declareLine) {

        List<String> varid = astgen.lineDeclMap.get(declareLine);
        String line = initialFileList.get(declareLine-1);
        int indexMin = line.length();
        if(varid != null) {
            for(String id: varid) {
                String name = astgen.allVarsMap.get(id).getName();
                String regex = "(\\W|^)" + name + "(\\W|$)";
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(line);
                while(m.find()) {
                    int start = m.start();
                    if(start > 0 && line.charAt(start) == '.') continue ;	//结构体Struct S A; A.a中的点；
                    else if(line.substring(0,start+1).matches(".*->\\s*")) continue;	//结构体指针Sttuct S *A; A->a中的a
                    else {	//因为是声明行，所以满足条件时必定前面多截一个字符 start是name前一个字符的下标，start+1是name第一个字符的下标
                        indexMin = Math.min(indexMin, start+1);
                        break ;
                    }
                }
            }
        }

        return indexMin;
    }

    public String replaceRegex(String initial){
        return initial.replaceAll("\\[", "\\\\[")
                .replaceAll("\\(", "\\\\(")
                .replaceAll("\\)", "\\\\)")
                .replaceAll("\\*", "\\\\*")
                .replaceAll("\\.", "\\\\.");
    }

}
