package utity;

import java.util.List;

public class IfInLoop {
    private String condition;
    private int startLine;//in loop
    private int elseLine;
    private int endLine;//in loop
    private List<String> ifBody;
    private List<String> elseBody;

    public IfInLoop() {
    }

    public IfInLoop(String condition, int startLine) {
        this.condition = condition;
        this.startLine = startLine;
    }

    public IfInLoop(String condition, int startLine, int endLine, List<String> ifBody, List<String> elseBody) {
        this.condition = condition;
        this.startLine = startLine;
        this.endLine = endLine;
        this.ifBody = ifBody;
        this.elseBody = elseBody;
    }

    public IfInLoop(String condition, int startLine, int elseLine, int endLine, List<String> ifBody, List<String> elseBody) {
        this.condition = condition;
        this.startLine = startLine;
        this.elseLine = elseLine;
        this.endLine = endLine;
        this.ifBody = ifBody;
        this.elseBody = elseBody;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getElseLine() {
        return elseLine;
    }

    public void setElseLine(int elseLine) {
        this.elseLine = elseLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public List<String> getIfBody() {
        return ifBody;
    }

    public void setIfBody(List<String> ifBody) {
        this.ifBody = ifBody;
    }

    public List<String> getElseBody() {
        return elseBody;
    }

    public void setElseBody(List<String> elseBody) {
        this.elseBody = elseBody;
    }

}
