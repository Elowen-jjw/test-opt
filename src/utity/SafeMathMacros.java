package utity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SafeMathMacros {
    private String functionName;
    private String returnType;
    private String opType;
    private String suTrans;
    private List<String> paramType;

    public SafeMathMacros(String functionName, String returnType, List<String> paramType) {
        this.functionName = functionName;
        this.returnType = returnType;
        this.opType = functionName.substring(0, functionName.indexOf("_"));
        Pattern r = Pattern.compile(".*(.+_.+)");
        Matcher m = r.matcher(functionName);
        this.suTrans = m.find() ? m.group(1) : functionName;
        this.paramType = paramType;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getOpType() {
        return opType;
    }

    public void setOpType(String opType) {
        this.opType = opType;
    }

    public String getSuTrans() {
        return suTrans;
    }

    public void setSuTrans(String suTrans) {
        this.suTrans = suTrans;
    }

    public List<String> getParamType() {
        return paramType;
    }

    public void setParamType(List<String> paramType) {
        this.paramType = paramType;
    }
}
