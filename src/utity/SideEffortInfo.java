package utity;

public class SideEffortInfo {
    private int lineNumber;
    private String leftValue;
    private String leftType;
    private String rightValue;
    private String rightType;
    private String operator;
    private String statement;
    private int startColumn;//相对于line.trim()
    private int endColumn;//相对于line.trim()
    private int rightStartColumn;
    private int rightEndColumn;
    private boolean isOutermost;

    public SideEffortInfo(int lineNumber, String leftValue, String leftType, String rightValue, String rightType, String operator, String statement, int startColumn, int endColumn, int rightStartColumn, int rightEndColumn) {
        this.lineNumber = lineNumber;
        this.leftValue = leftValue;
        this.leftType = leftType;
        this.rightValue = rightValue;
        this.rightType = rightType;
        this.operator = operator;
        this.statement = statement;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
        this.rightStartColumn = rightStartColumn;
        this.rightEndColumn = rightEndColumn;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(String leftValue) {
        this.leftValue = leftValue;
    }

    public String getLeftType() {
        return leftType;
    }

    public void setLeftType(String leftType) {
        this.leftType = leftType;
    }

    public String getRightType() {
        return rightType;
    }

    public void setRightType(String rightType) {
        this.rightType = rightType;
    }

    public String getRightValue() {
        return rightValue;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }
    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public void setStartColumn(int startColumn) {
        this.startColumn = startColumn;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public void setEndColumn(int endColumn) {
        this.endColumn = endColumn;
    }

    public int getRightStartColumn() {
        return rightStartColumn;
    }

    public void setRightStartColumn(int rightStartColumn) {
        this.rightStartColumn = rightStartColumn;
    }

    public int getRightEndColumn() {
        return rightEndColumn;
    }

    public void setRightEndColumn(int rightEndColumn) {
        this.rightEndColumn = rightEndColumn;
    }

    public boolean isOutermost() {
        return isOutermost;
    }

    public void setOutermost(boolean outermost) {
        isOutermost = outermost;
    }
}
