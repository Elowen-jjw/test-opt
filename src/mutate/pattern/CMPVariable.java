package mutate.pattern;

import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.model.AstVariable;
import ObjectOperation.datatype.StringOperation;
import ObjectOperation.list.CommonOperation;
import utity.AvailableVariable;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CMPVariable {

    public Set<AstVariable> cmpVarList = new HashSet<>();
    public List<AvailableVariable> avarList = new ArrayList<>();
    
    Map<String, AstVariable> varMap;
    int funcStartLine = 0;

    public CMPVariable(){}


    public CMPVariable(Map<String, AstVariable> varMap, Set<AstVariable> cmpVarList, int funcStartLine) {
    	this.varMap = varMap;
        this.funcStartLine = funcStartLine;
        this.cmpVarList = cmpVarList;
    }

    public void analyzeComparisonOp(String s){
        String comparisonOperatorsPattern = "([^;]*)(<=|<|>=|>|!=|==)([^;]*)";
        Pattern comparisonPattern = Pattern.compile(comparisonOperatorsPattern);

        Matcher comparisonMatcher = comparisonPattern.matcher(s.trim());
        while (comparisonMatcher.find()) {
            String leftPart = comparisonMatcher.group(1).trim();
            String rightPart = comparisonMatcher.group(3).trim();

            String truncatedLeft = StringOperation.truncateFromRight(leftPart).trim();
            String truncatedRight = StringOperation.truncateFromLeft(rightPart).trim();

//            if(!truncatedLeft.matches("[ijklmn]|\\d+")) {
//                System.out.println("leftValue: " + truncatedLeft);
//            }
//            if(!truncatedRight.matches("[ijklmn]|\\d+")) {
//                System.out.println("rightValue: " + truncatedRight);
//            }

//            System.out.printf("左值:%s 运算符:%s 右值:%s%n", (truncatedLeft), comparisonMatcher.group(2), truncatedRight);

            if(!truncatedLeft.matches("((0x)?[0-9a-fA-F]+(U?L{0,2})?)|[ijklmn]")) {
                checkCmpVar(truncatedLeft);
                analyzeComparisonOp(leftPart);
            }
            if(!truncatedRight.matches("((0x)?[0-9a-fA-F]+(U?L{0,2})?)|[ijklmn]")){
                checkCmpVar(truncatedRight);
                analyzeComparisonOp(rightPart);
            }
        }
    }

    public void analyzeSafeMath(String s){
        String safeMathPattern = "(safe_\\w+)(\\([^;]+)";
        Pattern safeMathPatternCompiled = Pattern.compile(safeMathPattern);

        Matcher safeMathMatcher = safeMathPatternCompiled.matcher(s.trim());
        while (safeMathMatcher.find()) {
            String safemathName = safeMathMatcher.group(1).trim();
            String safeMathPart = safeMathMatcher.group(2).trim();
            String truncatedPart = StringOperation.truncateFromLeft(safeMathPart);
//            System.out.println("SAFE_MATH: " + safemathName + " -> " + truncatedPart);
            checkCmpVar(truncatedPart);
            analyzeSafeMath(safeMathPart);
        }
    }

    public void checkCmpVar(String s){
        for(String id: varMap.keySet()){
            if(!varMap.get(id).getName().matches("[glp]_\\d+")) continue;
            if(varMap.get(id).getName().contains("p_") && varMap.get(id).getDeclareLine() < funcStartLine) continue;
        	if(StringOperation.containsVarName(s, varMap.get(id).getName())) {
        		cmpVarList.add(varMap.get(id));
        	}
        }
    }
    
    public void run(String line) {
    	analyzeComparisonOp(line);
    	analyzeSafeMath(line);
    }

    public static void main(String[] args) {

    }
}
