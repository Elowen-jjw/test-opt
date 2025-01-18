package utity;

import java.util.List;
import java.util.Map;

public class CompilationInfo {
    String simpliedFilename;
    Map<String,String> outputChecksumMap;
    Map<String,String> performanceMap;
    Map<String, List<String>> outputListMap;

    public CompilationInfo() {
    }
    public CompilationInfo(Map<String, List<String>> outputListMap) {
        this.outputListMap = outputListMap;
    }

    public CompilationInfo(Map<String, List<String>> outputListMap, Map<String, String> performanceMap) {
        this.outputListMap = outputListMap;
        this.performanceMap = performanceMap;
    }

    public Map<String, String> getOutputChecksumMap() {
        return outputChecksumMap;
    }

    public void setOutputChecksumMap(Map<String, String> outputChecksumMap) {
        this.outputChecksumMap = outputChecksumMap;
    }

    public Map<String, String> getPerformanceMap() {
        return performanceMap;
    }

    public void setPerformanceMap(Map<String, String> performanceMap) {
        this.performanceMap = performanceMap;
    }

    public String getSimpliedFilename() {
        return simpliedFilename;
    }

    public void setSimpliedFilename(String simpliedFilename) {
        this.simpliedFilename = simpliedFilename;
    }

    public Map<String, List<String>> getOutputListMap() {
        return outputListMap;
    }

    public void setOutputListMap(Map<String, List<String>> outputListMap) {
        this.outputListMap = outputListMap;
    }
}
