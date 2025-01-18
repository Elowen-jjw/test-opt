package mutate.minmax;

import common.PropertiesInfo;

import java.math.BigInteger;
import java.util.*;


public class SafeMathInfo {
    private String firstParam;
    private String secondParam;
    private Set<BigInteger> firstValue;
    private Set<BigInteger> secondValue;
    private String firstType;
    private String secondType;
    private String funcName;
    private Map<String, Set<BigInteger>> comparableMap;
    private int startColumn;
    private int endColumn;
    private int firstStartColumn;
    private int firstEndColumn;
    private int secondStartColumn;
    private int secondEndColumn;
    private List<String> firstReplacedMinMax;
    private List<String> secondReplacedMinMax;

    public SafeMathInfo(String funcName, String firstParam, String secondParam, int startColumn, int endColumn) {
        this.firstParam = firstParam;
        this.secondParam = secondParam;
        this.funcName = funcName;
        this.startColumn = startColumn;
        this.endColumn = endColumn;

        if(funcName.contains("minus")){
            this.firstStartColumn = startColumn + 1;
            this.firstEndColumn = endColumn - 1;
            this.secondStartColumn = 0;
            this.secondEndColumn = 0;
        } else {
            this.firstStartColumn = startColumn + 1;
            this.firstEndColumn = startColumn + 1 + firstParam.length();
            this.secondStartColumn = endColumn - 1 - secondParam.length();
            this.secondEndColumn = endColumn - 1;
        }
        List<String> typeList = PropertiesInfo.allSafeMathMacros.get(funcName.replace("safe_", "")).getParamType();
        this.firstType = typeList.get(0);
        if(!funcName.contains("minus")){
            this.secondType = typeList.get(0);
        } else {
            this.secondType = "";
        }

        this.firstValue = new TreeSet<>();
        this.secondValue = new TreeSet<>();
        this.comparableMap = new HashMap<>();
        this.firstReplacedMinMax = new ArrayList<>();
        this.secondReplacedMinMax = new ArrayList<>();
    }

    public String getFirstParam() {
        return firstParam;
    }

    public void setFirstParam(String firstParam) {
        this.firstParam = firstParam;
    }

    public String getSecondParam() {
        return secondParam;
    }

    public void setSecondParam(String secondParam) {
        this.secondParam = secondParam;
    }

    public String getFuncName() {
        return funcName;
    }

    public void setFuncName(String funcName) {
        this.funcName = funcName;
    }

    public Set<BigInteger> getFirstValue() {
        return firstValue;
    }

    public void setFirstValue(Set<BigInteger> firstValue) {
        this.firstValue = firstValue;
    }

    public Set<BigInteger> getSecondValue() {
        return secondValue;
    }

    public void setSecondValue(Set<BigInteger> secondValue) {
        this.secondValue = secondValue;
    }

    public String getFirstType() {
        return firstType;
    }

    public void setFirstType(String firstType) {
        this.firstType = firstType;
    }

    public String getSecondType() {
        return secondType;
    }

    public void setSecondType(String secondType) {
        this.secondType = secondType;
    }

    public Map<String, Set<BigInteger>> getComparableMap() {
        return comparableMap;
    }

    public void setComparableMap(Map<String, Set<BigInteger>> comparableMap) {
        this.comparableMap = comparableMap;
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

    public List<String> getFirstReplacedMinMax() {
        return firstReplacedMinMax;
    }

    public void setFirstReplacedMinMax(List<String> firstReplacedMinMax) {
        this.firstReplacedMinMax = firstReplacedMinMax;
    }

    public List<String> getSecondReplacedMinMax() {
        return secondReplacedMinMax;
    }

    public void setSecondReplacedMinMax(List<String> secondReplacedMinMax) {
        this.secondReplacedMinMax = secondReplacedMinMax;
    }

    public int getFirstStartColumn() {
        return firstStartColumn;
    }

    public void setFirstStartColumn(int firstStartColumn) {
        this.firstStartColumn = firstStartColumn;
    }

    public int getFirstEndColumn() {
        return firstEndColumn;
    }

    public void setFirstEndColumn(int firstEndColumn) {
        this.firstEndColumn = firstEndColumn;
    }

    public int getSecondStartColumn() {
        return secondStartColumn;
    }

    public void setSecondStartColumn(int secondStartColumn) {
        this.secondStartColumn = secondStartColumn;
    }

    public int getSecondEndColumn() {
        return secondEndColumn;
    }

    public void setSecondEndColumn(int secondEndColumn) {
        this.secondEndColumn = secondEndColumn;
    }
}
