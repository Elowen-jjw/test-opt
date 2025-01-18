package mutate.cse;

import common.PropertiesInfo;

import java.util.*;

public class GenComplexExpr {

    // Random object for generating random values
    public final Random random = new Random();
    public final List<String> variables = new ArrayList<>();
    public static final List<String> arithmeticOperators = Arrays.asList("+", "-", "&", "|", "^", "%", "/");
    public static final List<String> relationOperators = Arrays.asList(">", ">=", "!=", "<", "<=", "==");

    public GenComplexExpr(List<String> variableList){
        variables.addAll(variableList);
    }

    public static void main(String args[]){
        List<String> newExp = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        GenComplexExpr gce = new GenComplexExpr(newExp);
        List<String> list = gce.genExpression(5);
        for(String s: list){
            System.out.println(s);
        }
    }

    public List<String> genExpression(int size){
        List<String> finalExpressionList = new ArrayList<>();
        List<List<String>> groupList = genParts();
        List<String> expression1 = groupList.get(0);
        List<String> expression2 = groupList.get(1);
        List<String> expression3 = groupList.get(2);

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

    public List<List<String>> genParts() {
        List<String> leftVars = getRandomSelectedVars(variables);
        List<String> rightVars = getRandomSelectedVars(variables);
        // Ensure that both lists combined cover all variables
        while (!coversAllVariables(leftVars, rightVars, variables)) {
            leftVars = getRandomSelectedVars(variables);
            rightVars = getRandomSelectedVars(variables);
        }

        // Generate the arithmetic expression
        String leftExpr = generateArithmeticExpression(leftVars);
        // Randomly choose a relation operator
        String relationOperator = relationOperators.get(random.nextInt(relationOperators.size()));
        // Generate the second part of the expression
        String rightExpr = generateArithmeticExpression(rightVars);

        System.out.println(leftExpr);
        System.out.println(rightExpr);

        List<List<String>> parts = new ArrayList<>();
        ExpressionSwapper es = new ExpressionSwapper();
        parts.add(genEquivalentExpression("(" + leftExpr + ")", "(" + rightExpr + ")", relationOperator));
        parts.add(new ArrayList<>(es.transformExpression(leftExpr)));
        parts.add(new ArrayList<>(es.transformExpression(rightExpr)));
        return parts;
    }
    
    public List<String> getRandomSelectedVars(List<String> variables){
        Collections.shuffle(variables);
        int numVariables = random.nextInt(3) + 2; //2-4
        return new ArrayList<>(variables.subList(0, numVariables));
    }

    private boolean coversAllVariables(List<String> leftVars, List<String> rightVars, List<String> allVars) {
        Set<String> combined = new HashSet<>(leftVars);
        combined.addAll(rightVars);
        return combined.containsAll(allVars);
    }

    public String generateArithmeticExpression(List<String> vars) {
        int numVars = vars.size();
        StringBuilder expression = new StringBuilder();

        for (int i = 0; i < numVars; i++) {
            if (i > 0) {
                String operator = arithmeticOperators.get(random.nextInt(arithmeticOperators.size()));
                if (operator.equals("%") || operator.equals("/")) {
                    vars.set(i, "(" + vars.get(i) + " ? " + vars.get(i) + " : " + vars.get(i) + " + 1)"); // Avoid division by zero
                }
                expression.append(" ").append(operator).append(" ");
            }
            if (random.nextDouble() < 0.3) {
                expression.append("~");
            }
            expression.append(vars.get(i));
        }
        return expression.toString();
    }

    public List<String> genEquivalentExpression(String exp1, String exp2, String op){
        List<String> availableReplacements = PropertiesInfo.operatorReplacements.get(op);
        List<String> chosenReplacements = new ArrayList<>();
        chosenReplacements.add(exp1 + " " + op + " " + exp2);
        if (availableReplacements != null) {
            for(String s: availableReplacements){
                chosenReplacements.add(s.replace("#a#", exp1).replace("#b#", exp2));
            }
        }
        return chosenReplacements;
    }

    public final Map<String, Integer> operatorPrecedence = new HashMap<>();
    {
        operatorPrecedence.put("+", 1);
        operatorPrecedence.put("-", 1);
        operatorPrecedence.put("%", 2);
        operatorPrecedence.put("/", 2);
        operatorPrecedence.put("&", 3);
        operatorPrecedence.put("|", 4);
        operatorPrecedence.put("^", 5);
    }

    // 定义交换律适用的运算符
    public final Set<String> commutativeOperators = new HashSet<>(Arrays.asList("+", "*", "&", "|", "^"));

    public String swapCommutative(String expression) {
        List<String> tokens = tokenize(expression);
        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();

        for (String token : tokens) {
            if (isOperator(token)) {
                while (!operators.isEmpty() && operatorPrecedence.get(operators.peek()) >= operatorPrecedence.get(token)) {
                    output.add(operators.pop());
                }
                operators.push(token);
            } else {
                output.add(token);
            }
        }

        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }

        List<String> swapped = new ArrayList<>();
        for (String token : output) {
            if (isCommutativeOperator(token) && swapped.size() >= 2) {
                Collections.swap(swapped, swapped.size() - 1, swapped.size() - 2);
            }
            swapped.add(token);
        }

        return assemble(swapped);
    }

    public List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        for (char c : expression.toCharArray()) {
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (isOperator(String.valueOf(c)) || c == '(' || c == ')') {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                if (c == '~') {
                    token.append(c);
                } else {
                    token.append(c);
                }
            }
        }
        if (token.length() > 0) {
            tokens.add(token.toString());
        }
        return tokens;
    }

    public boolean isOperator(String token) {
        return operatorPrecedence.containsKey(token);
    }

    public boolean isCommutativeOperator(String token) {
        return commutativeOperators.contains(token);
    }

    public String assemble(List<String> tokens) {
        StringBuilder expression = new StringBuilder();
        for (String token : tokens) {
            expression.append(token).append(" ");
        }
        return expression.toString().trim();
    }
}

