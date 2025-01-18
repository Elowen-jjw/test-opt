package utity;

import java.util.List;
import java.util.Map;

public class CompilationInfo {
    String simpliedFilename;
    Map<String, String> specificConfig;
    Map<String, String> fullCommand;
    Map<String,String> performanceMap;
    Map<String, List<String>> outputListMap;

    public CompilationInfo() {
    }
    public CompilationInfo(Map<String, List<String>> outputListMap) {
        this.outputListMap = outputListMap;
    }

    public CompilationInfo(Map<String, String> specificConfig, Map<String, String> fullCommand, Map<String, List<String>> outputListMap) {
        this.specificConfig = specificConfig;
        this.fullCommand = fullCommand;
        this.outputListMap = outputListMap;
    }

    public CompilationInfo(Map<String, List<String>> outputListMap, Map<String, String> performanceMap) {
        this.outputListMap = outputListMap;
        this.performanceMap = performanceMap;
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

    public Map<String, String> getSpecificConfig() {
        return specificConfig;
    }

    public void setSpecificConfig(Map<String, String> specificConfig) {
        this.specificConfig = specificConfig;
    }

    public Map<String, String> getFullCommand() {
        return fullCommand;
    }

    public void setFullCommand(Map<String, String> fullCommand) {
        this.fullCommand = fullCommand;
    }
}
