package mutate.cse;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ExpressionSwapper {

    public static void main(String[] args) {
        String expression = "~(*d) % (~(**a).f2 ? (**a).f2 + 1 : (**a).f2) & (**c) + (**a)";
        Set<String> transformedExpressions = new ExpressionSwapper().transformExpression(expression);

        for (String expr : transformedExpressions) {
            System.out.println("Transformed Expression: " + expr);
        }
    }

    public Set<String> transformExpression(String expression) {
        Set<String> results = new HashSet<>();
        Node root = parseExpression(expression);

        // Apply commutative transformations
        List<Node> transformedNodes = applyCommutativeTransformations(root);

        // Generate expressions from transformed nodes
        for (Node node : transformedNodes) {
            results.add(generateExpression(node));
        }

        return results;
    }

    public Node parseExpression(String expression) {
        Stack<Node> stack = new Stack<>();
        Stack<Character> operators = new Stack<>();
        int n = expression.length();
        String varRegex = "(~?((\\(\\*+[a-z]\\))|(\\([a-z]\\))|([a-z]))(\\.f\\d+)*)";
        Pattern operandPattern = Pattern.compile(varRegex);
        Pattern ternaryPattern = Pattern.compile("~?\\(" + varRegex + "\\s+\\?\\s+" + varRegex + "\\s+:\\s+" + varRegex + "\\s+\\+\\s+\\d+\\)");

        for (int i = 0; i < n; i++) {
            char ch = expression.charAt(i);

            if (ch == ' ') {
                continue;
            } else if (Character.isLetter(ch) || ch == '~' || ch == '*' || ch == '(') {
                Matcher matcher = operandPattern.matcher(expression.substring(i));
                if (matcher.find()) {
                    String operand = matcher.group();
                    stack.push(new Node(operand));
                    i += operand.length() - 1;
                }
            } else if (GenComplexExpr.arithmeticOperators.contains(String.valueOf(ch))) {
                // Handle / and % with ternary expressions as a whole
                if ((ch == '/' || ch == '%')) {

                    i += 2;//blank

                    Matcher matcher = ternaryPattern.matcher(expression.substring(i));
                    if (matcher.find()) {
                        String operand = matcher.group();
                        stack.push(new Node(operand));
                        stack.push(applyOperator(ch, stack.pop(), stack.pop()));
                        i += operand.length() - 1;
                    }
                } else {
                    while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(ch)) {
                        stack.push(applyOperator(operators.pop(), stack.pop(), stack.pop()));
                    }
                    operators.push(ch);
                }
            }
        }

        while (!operators.isEmpty()) {
            stack.push(applyOperator(operators.pop(), stack.pop(), stack.pop()));
        }

        return stack.pop();
    }

    public int precedence(char operator) {
        switch (operator) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
            case '%':
                return 2;
            case '&':
            case '|':
            case '^':
                return 3;
            default:
                return -1;
        }
    }

    public Node applyOperator(char operator, Node b, Node a) {
        return new Node(String.valueOf(operator), a, b);
    }

    public List<Node> applyCommutativeTransformations(Node root) {
        List<Node> transformations = new ArrayList<>();
        transformations.add(root);
        if (root.operator != null && isCommutative(root.operator.charAt(0))) {
            Node swapped = new Node(root.operator, root.right, root.left);
            transformations.add(swapped);
        }

        if (root.left != null) {
            List<Node> leftTransformations = applyCommutativeTransformations(root.left);
            for (Node left : leftTransformations) {
                transformations.add(new Node(root.operator, left, root.right));
            }
        }

        if (root.right != null) {
            List<Node> rightTransformations = applyCommutativeTransformations(root.right);
            for (Node right : rightTransformations) {
                transformations.add(new Node(root.operator, root.left, right));
            }
        }

        return transformations;
    }

    public boolean isCommutative(char operator) {
        return operator == '+' || operator == '*' || operator == '&' || operator == '|' || operator == '^';
    }

    public String generateExpression(Node node) {
        if (node.left == null && node.right == null) {
            return node.value;
        }

        String left = generateExpression(node.left);
        String right = generateExpression(node.right);

        return "" + left + " " + node.operator + " " + right + "";
    }

    class Node {
        String value;
        String operator;
        Node left;
        Node right;

        Node(String value) {
            this.value = value;
        }

        Node(String operator, Node left, Node right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }
    }
}
