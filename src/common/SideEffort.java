package common;

import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.VarInform;
import AST_Information.model.AstVariable;
import ObjectOperation.list.CommonOperation;
import utity.AvailableVariable;
import utity.SideEffortInfo;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SideEffort {
    File file;
    List<String> initialFileList;
    public Map<Integer, List<SideEffortInfo>> sevMap = new HashMap<>();
    List<AvailableVariable> avarList = new ArrayList<>();

    public SideEffort(File file){
        this.file = file;
        this.initialFileList = CommonOperation.genInitialList(file);
    }

    public static boolean isDeclareLine(String line){
        if(line.length() == 0) return false;
        String[] words = line.split("\\s+");
        for(String word: words){
            if(PropertiesInfo.typeList.contains(word))
                return true;
        }
        return false;
    }

    public void getSideEffortVar(String compareSequence){ //得到整个文件中所有有副作用的变量信息
        AstInform_Gen astgen = new AstInform_Gen(file);
        Map<String, AstVariable> varMap = astgen.allVarsMap;
        List<AstVariable> astVarList = new ArrayList<>();
        for(String s: varMap.keySet()){
            astVarList.add(varMap.get(s));
//            System.out.println(varMap.get(s).getName() + ": " + varMap.get(s).getType());
        }
        //修改了varInform，可以得到关于union和结构体本身的变量，而不仅仅是成员变量
        avarList = VarInform.getInitialAvailableVarList(astVarList, astgen);
//        for(AvailableVariable avar: avarList) {
//            System.out.println(avar.getType() + " " + avar.getValue() + " " + avar.getIsConst() );
//        }

        int count = 0;

        for(String s: initialFileList){
            count++;
            if(s.trim().equals("int main(void) {")) break;
            if(SideEffort.isDeclareLine(s.trim())) continue;
            matchSideEffort(s, count, 0);
        }

        sevMap.forEach((key, value) -> {
            for(SideEffortInfo sei: value){
                boolean isNested = false;
                for(SideEffortInfo findSei: value){
                    if(sei == findSei) continue;
                    if(sei.getStartColumn() > findSei.getStartColumn() && sei.getEndColumn() < findSei.getEndColumn()) {
                        isNested = true;
                        sei.setOutermost(false);
                        break;
                    }
                }
                if(!isNested){
                    sei.setOutermost(true);
                }
            }
        });

        //cse
        if(compareSequence.equals("lts")) {
            for (Map.Entry<Integer, List<SideEffortInfo>> entry : sevMap.entrySet()) {
                entry.getValue().sort(new SideEffortInfoComparatorLTS());
            }
        }

        //inline
        if(compareSequence.equals("stl")) {
            for (Map.Entry<Integer, List<SideEffortInfo>> entry : sevMap.entrySet()) {
                entry.getValue().sort(new SideEffortInfoComparatorSTL());
            }
        }
    }

    public void matchSideEffort(String s, int count, int baseColumnNum){
//        String notHaveBraceRegex1 = PropertiesInfo.notHaveBraceVarRegex + "\\s+([\\*\\+\\-\\%\\/~\\&\\|\\^]?=|[<>]{2}=)\\s+" + PropertiesInfo.rightRegex;
//        String haveBraceRegex1 = PropertiesInfo.haveBraceVarRegex + "\\s+([\\*\\+\\-\\%\\/~\\&\\|\\^]?=|[<>]{2}=)\\s+" + PropertiesInfo.rightRegex;
        String generalRegex = "(" + PropertiesInfo.notHaveBraceVarRegex + "|" +PropertiesInfo.haveBraceVarRegex + ")" + "\\s+([\\*\\+\\-\\%\\/~\\&\\|\\^]?=|[<>]{2}=)\\s+" + PropertiesInfo.rightRegex;
        String part = "([+]{2}|[-]{2})";
        String increment = "((" + part + PropertiesInfo.notHaveBraceVarRegex + ")|(" + part + PropertiesInfo.haveBraceVarRegex + ")|("
                + PropertiesInfo.notHaveBraceVarRegex + part + ")|(" + PropertiesInfo.haveBraceVarRegex + part + "))";
        Pattern p1 = Pattern.compile(generalRegex);
        Pattern p2 = Pattern.compile(increment);
        Matcher m1;
        Matcher m2;
        m1 = p1.matcher(s.trim());
        m2 = p2.matcher(s.trim());
        addSideEffortVar(count, m1, baseColumnNum);
        while(m2.find()){
            String type = getIncrementVarType(m2.group().replaceAll("[\\+]{2}|[-]{2}", ""));
            SideEffortInfo sev = new SideEffortInfo(count, "", type, m2.group(), type, "", m2.group(),
                    baseColumnNum + m2.start(), baseColumnNum + m2.end(), baseColumnNum + m2.start(), baseColumnNum + m2.end());
            addSevToMap(sev);
        }
    }

    private void addSideEffortVar(int count, Matcher m, int baseColumnNum) {
        while(m.find()){
            SideEffortInfo sev = analysisAssignment(m, count,baseColumnNum);
            addSevToMap(sev);
        }
    }

    private void addSevToMap(SideEffortInfo sev) {
        if(sevMap.containsKey(sev.getLineNumber()) && !isHaveInList(sevMap.get(sev.getLineNumber()), sev))
            sevMap.get(sev.getLineNumber()).add(sev);
        else if(!sevMap.containsKey(sev.getLineNumber())){
            List<SideEffortInfo> sevList = new ArrayList<>();
            sevList.add(sev);
            sevMap.put(sev.getLineNumber(), sevList);
        }
    }

    public boolean isHaveInList(List<SideEffortInfo> sevList, SideEffortInfo sev){
        for(SideEffortInfo singleSev: sevList){
            if(singleSev.getStartColumn() == sev.getStartColumn())
                return true;
        }
        return false;
    }

    public SideEffortInfo analysisAssignment(Matcher m, int lineNumebr, int baseColumnNum){
        String varName = m.group(1);
        String operator = m.group(2).trim();
//        System.out.println("before:----------" + m.group(3));
        String rightValue = m.group(3).matches("(\\(void\\s*\\*+\\)[^;\\{\\}\\)]*)")? m.group(3): balanceBrackets(m.group(3));
//        System.out.println("After----------" + rightValue);
        matchSideEffort(m.group(3), lineNumebr, baseColumnNum + m.start(3));
        String statement = varName + " " + operator + " " + rightValue;
        for (AvailableVariable avar : avarList) {
            if (varName.replaceAll("[\\(\\)\\*\\&]", "").replaceAll(PropertiesInfo.indexPattern, "")
                    .equals(avar.getValue().replaceAll("[\\(\\)\\*]", "").replaceAll("\\[\\d*\\]", ""))) {
                int diff = matchVar(varName, avar);
                if(diff > 0){//type is pointer
                    String leftType = avar.getType() + " " + String.join("", Collections.nCopies(diff, "*"));
                    String rightType = avar.getType() + " " + String.join("", Collections.nCopies(diff - 1, "*"));
                    //start end based on trim
                    return new SideEffortInfo(lineNumebr, varName, leftType, rightValue, (leftType + " / " + "&" + rightType.trim()), operator, statement,
                            baseColumnNum + m.start(), baseColumnNum + m.start() + statement.length(), baseColumnNum + m.start(3), baseColumnNum + m.start(3) + rightValue.length()); // & count(*)-1
                }else{
                    return new SideEffortInfo(lineNumebr, varName, avar.getType(), rightValue, avar.getType(), operator, statement,
                            baseColumnNum + m.start(), baseColumnNum + m.start() + statement.length(), baseColumnNum + m.start(3), baseColumnNum + m.start(3) + rightValue.length());//line.trim().charAt
                }
            }
        }
        return null;
    }

    public String getIncrementVarType(String varName){
        for (AvailableVariable avar : avarList) {
            if (varName.replaceAll("[\\(\\)\\*]", "").replaceAll("\\[\\w+?\\]", "")
                    .equals(avar.getValue().replaceAll("[\\(\\)\\*]", "").replaceAll("\\[\\w+?\\]", ""))) {
                int diff = matchVar(varName, avar);
                if(diff > 0){
                    return avar.getType() + " " + String.join("", Collections.nCopies(diff, "*"));
                }else{
                    return avar.getType();
                }
            }
        }
        return null;
    }

    private int matchVar(String varName, AvailableVariable avar) {
        Pattern p_star = Pattern.compile("(\\*+)");
        Matcher m_star1 = p_star.matcher(varName);
        Matcher m_star2 = p_star.matcher(avar.getValue());
        return (m_star2.find() ? m_star2.group().length() : 0) - (m_star1.find() ? m_star1.group().length() : 0) ;
    }

    public String removeUnmatchedParentheses(String input) {
        StringBuilder sb = new StringBuilder(input);
        Stack<Integer> stack = new Stack<>();

        // 第一遍扫描：处理左括号和右括号
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '(') {
                stack.push(i);  // 把左括号的位置入栈
            } else if (c == ')') {
                if (stack.isEmpty()) {
                    sb.setCharAt(i, '"');  // 标记多余的右括号
                } else {
                    stack.pop();  // 匹配到左括号，出栈
                }
            }
        }

        // 清除所有未匹配的左括号
        while (!stack.isEmpty()) {
            sb.setCharAt(stack.pop(), '"');  // 标记多余的左括号
        }

        // 删除所有标记的字符
        return sb.toString().replaceAll("\"", "");
    }

    public String balanceBrackets(String input) {
        Stack<Character> stack = new Stack<>();
        int lastCloseIndex = -1;

        // Iterate over string characters
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '(' || c == '[') {
                stack.push(c);
            }
            // Check for closing brackets
            else if (c == ')' || c == ']') {
                if (!stack.isEmpty() && isMatching(stack.peek(), c)) {
                    // Pop the stack since we found a matching closing bracket
                    stack.pop();

                    // Update lastCloseIndex when all brackets are matched
                    if (stack.isEmpty()) {
                        lastCloseIndex = i + 1;
                    }
                } else {
                    lastCloseIndex = i;
                    break;
                }
            }
        }

        // If there are no unmatched opening brackets
        if (stack.isEmpty() && lastCloseIndex != -1) {
            int start = lastCloseIndex + 2;
            if(start < input.length() && input.substring(lastCloseIndex, lastCloseIndex + 2).equals(".f")){
                int end = start;
                while (end < input.length() && Character.isDigit(input.charAt(end))) {
                    end++;
                }
                lastCloseIndex = end;
            }
            return input.substring(0, lastCloseIndex);
        } else {
            // Return original string or an error message
            return input;  // or you could throw an exception or error message depending on your use case
        }
    }

    private boolean isMatching(char open, char close) {
        return (open == '(' && close == ')') || (open == '[' && close == ']');
    }

    public class SideEffortInfoComparatorLTS implements Comparator<SideEffortInfo> { //先大范围再小范围
        @Override
        public int compare(SideEffortInfo o1, SideEffortInfo o2) {
            // 判断包含关系
            if (o1.getStartColumn() <= o2.getStartColumn() && o1.getEndColumn() >= o2.getEndColumn()) {
                return -1; // o1 包含 o2
            } else if (o2.getStartColumn() <= o1.getStartColumn() && o2.getEndColumn() >= o1.getEndColumn()) {
                return 1;  // o2 包含 o1
            } else {
                // 按 start 排序
                return Integer.compare(o1.getStartColumn(), o2.getStartColumn());
            }
        }
    }

    public class SideEffortInfoComparatorSTL implements Comparator<SideEffortInfo> {//先小范围再大范围
        @Override
        public int compare(SideEffortInfo o1, SideEffortInfo o2) {
            // 判断包含关系
            if (o1.getStartColumn() <= o2.getStartColumn() && o1.getEndColumn() >= o2.getEndColumn()) {
                return 1; // o1 包含 o2
            } else if (o2.getStartColumn() <= o1.getStartColumn() && o2.getEndColumn() >= o1.getEndColumn()) {
                return -1;  // o2 包含 o1
            } else {
                // 按 start 排序
                return Integer.compare(o1.getStartColumn(), o2.getStartColumn());
            }
        }
    }

}


