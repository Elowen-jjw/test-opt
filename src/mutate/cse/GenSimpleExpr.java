package mutate.cse;

import common.PropertiesInfo;

import java.util.*;

public class GenSimpleExpr {
    public  final Random random = new Random();
    public  final List<String> variables = new ArrayList<>();
    public  final String[] logicalOps = {">", ">=", "!=", "<", "<=", "=="};
    public  final String[] arithmeticSuffixOps = {"+", "-", "%", "&", "|", "^"};//"*", "/",  "<<", ">>"
    public  final String[] arithmeticPrefixOps = {"~"}; //++ --

    public GenSimpleExpr(List<String> variableList){
        variables.addAll(variableList);
    }

    public List<String> genExpression(int size){
        List<String> finalExpressionList = new ArrayList<>();
        List<List<String>> groupList = randomAssign();
        List<String> expression1 = generateLogicalExpression(groupList.get(0));
        List<String> expression2 = generateArithmeticExpression(groupList.get(1));
        List<String> expression3 = generateArithmeticExpression(groupList.get(2));

        int count = 0;
        while(count++ < size){
            Collections.shuffle(expression1);
            Collections.shuffle(expression2);
            Collections.shuffle(expression3);
            Random random = new Random();
            String part1 = expression1.get(random.nextInt(expression1.size()));
            String part2 = expression2.get(random.nextInt(expression2.size()));
            String part3 = expression3.get(random.nextInt(expression3.size()));

            finalExpressionList.add( "(" + part1 + ") ? (" + part2 + ") : (" + part3 + ")");
        }
        return finalExpressionList;
    }


    public  List<List<String>> randomAssign() {
        List<List<String>> groupList = new ArrayList<>();
        List<String> variableList = new ArrayList<>();
        variableList.addAll(variables);

        Collections.shuffle(variableList);
        List<String> group1 = new ArrayList<>();
        List<String> group2 = new ArrayList<>();
        List<String> group3 = new ArrayList<>();

        group1.add(variableList.get(0));
        group1.add(variableList.get(1));

        group2.add(variableList.get(0));
        group3.add(variableList.get(1));

        group2.add(variableList.get(2));
        group3.add(variableList.get(3));

        groupList.add(group1);
        groupList.add(group2);
        groupList.add(group3);

        return groupList;
    }

    public List<String> generateLogicalExpression(List<String> group) {
        return genEquivalentExpression(group.get(0), group.get(1), logicalOps[random.nextInt(logicalOps.length)]);
    }

    public List<String> genEquivalentExpression(String var1, String var2, String op){
        List<String> availableReplacements = PropertiesInfo.operatorReplacements.get(op);
        List<String> chosenReplacements = new ArrayList<>();
        chosenReplacements.add(var1 + " " + op + " " + var2);
        if (availableReplacements != null) {
            for(String s: availableReplacements){
                chosenReplacements.add(s.replace("#a#", var1).replace("#b#", var2));
            }
        }
        return chosenReplacements;
    }

    public List<String> generateArithmeticExpression(List<String> group) {
        List<String> composedExpression = new ArrayList<>();
        String var1 = random.nextBoolean()? group.get(0) : arithmeticPrefixOps[random.nextInt(arithmeticPrefixOps.length)] + group.get(0);
        String var2 = random.nextBoolean()? group.get(1) : arithmeticPrefixOps[random.nextInt(arithmeticPrefixOps.length)] + group.get(1);

        if(group.size() > 1) {
            boolean isHaveTernary = random.nextBoolean();
            if (isHaveTernary) {
                List<String> part = genEquivalentExpression(var1, var2, logicalOps[random.nextInt(logicalOps.length)]);
                for(String s: part){
                    composedExpression.add("(" + s + ") ? " + group.get(0) + " : " + group.get(1));
                }
            } else {
                String op = arithmeticSuffixOps[random.nextInt(arithmeticSuffixOps.length)];
                composedExpression.add(var1 + " " + op + " " + var2);
            }
        }

        return composedExpression;
    }
}
