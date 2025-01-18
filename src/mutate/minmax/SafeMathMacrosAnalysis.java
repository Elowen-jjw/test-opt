package mutate.minmax;

import ObjectOperation.datatype.StringOperation;
import common.PropertiesInfo;
import utity.SafeMathMacros;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SafeMathMacrosAnalysis {

    public static Map<String, SafeMathMacros> extractSafeMathMacros(String filePath) {
        Map<String, SafeMathMacros> macrosMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            String nextLine = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Check for the STATIC keyword
                if (line.startsWith("STATIC")) {
                    nextLine = br.readLine(); // Read the next line

                    if (nextLine != null) {
                        nextLine = nextLine.trim(); // Trim the next line

                        // Define regex patterns for matching
                        String returnTypePattern = "STATIC\\s*([^\\s]*)";
                        String functionNamePattern = "FUNC_NAME\\((\\w+)\\)\\((.*)LOG_INDEX\\)";

                        // Match the return type
                        Matcher returnTypeMatcher = Pattern.compile(returnTypePattern).matcher(line);
                        if (returnTypeMatcher.find()) {
                            String returnType = returnTypeMatcher.group(1);

                            // Match the function name
                            Matcher functionNameMatcher = Pattern.compile(functionNamePattern).matcher(nextLine);
                            if (functionNameMatcher.find()) {
                                String functionName = functionNameMatcher.group(1);
                                String[] paramPart = functionNameMatcher.group(2).split(",");
                                List<String> paramType = new ArrayList<>();
                                for(String param: paramPart){
                                    paramType.add(param.trim().substring(0, param.trim().lastIndexOf(" ")));
                                }

                                // Create SafeMath object
                                SafeMathMacros safeMathMacros = new SafeMathMacros(functionName, returnType, paramType);
                                macrosMap.put(functionName, safeMathMacros);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return macrosMap;
    }

    public static Map<String, SafeMathMacros> getSafeMathMacrosMap(){
        String safeMathPath = PropertiesInfo.indexDir + "/safe_math.h";
        return extractSafeMathMacros(safeMathPath);
    }

    public static void main(String[] args) {
        String safeMathPath = PropertiesInfo.indexDir + "/safe_math.h";
        Map<String, SafeMathMacros> functions = extractSafeMathMacros(safeMathPath);

        // Print the extracted functions
        for (String functionName : functions.keySet()) {
            SafeMathMacros function = functions.get(functionName);
            System.out.printf(function.getFunctionName()
                    + " "
                    + function.getReturnType()
                    + " "
                    + function.getSuTrans()
                    + " "
                    + function.getOpType()
                    + " ");
            for(String type: function.getParamType())
                System.out.printf(type + " ");
            System.out.println();
        }

    }

    public static void findSafeMath(String line, boolean isRestart, int count){
        String safeMathPattern = "(safe_\\w+)(\\([^;]+)";
        Pattern safeMathPatternCompiled = Pattern.compile(safeMathPattern);
        Matcher safeMathMatcher = safeMathPatternCompiled.matcher(line.trim());
        if (safeMathMatcher.find()) {
        	if(isRestart) System.out.printf(count + ": ");
            String safemathName = safeMathMatcher.group(1).trim();
            String safeMathPart = safeMathMatcher.group(2).trim();
            String truncatedPart = StringOperation.truncateFromLeft(safeMathPart);
            System.out.printf(safemathName + " -> ");
            findSafeMath(truncatedPart, false, count);
            findSafeMath(safeMathPart.replace(truncatedPart, ""), true, count);
            System.out.println();
        }
    }
}
