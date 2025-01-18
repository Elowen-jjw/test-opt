package mutate.minmax;

import ObjectOperation.datatype.StringOperation;
import common.PropertiesInfo;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SafeMathExtractor {

    public Map<Integer, List<SafeMathInfo>> getAllSafeMathCall(List<String> initialFileList){
        Map<Integer, List<SafeMathInfo>> safeMathMap = new TreeMap<>();
        int count = 0;
        for(String line: initialFileList){
            count++;
            List<SafeMathInfo> safeMathInfoList = new ArrayList<>();
            analyzeSafeMath(line, safeMathInfoList, StringOperation.countLeadingSpaces(line));
            if(!safeMathInfoList.isEmpty()){
                safeMathMap.put(count, safeMathInfoList);
            }
        }
        sortSafeMathInfo(safeMathMap);
        return safeMathMap;
    }
    public void analyzeSafeMath(String s, List<SafeMathInfo> safeMathInfoList, int baseIndex){
        Matcher m_for = Pattern.compile(PropertiesInfo.forStmtPattern).matcher(s);
        if(m_for.find()) return;
        String safeMathPattern = "(safe_\\w+)(\\([^;]+)";
        Pattern safeMathPatternCompiled = Pattern.compile(safeMathPattern);

        Matcher safeMathMatcher = safeMathPatternCompiled.matcher(s.trim());
        if (safeMathMatcher.find()) {
            String safemathName = safeMathMatcher.group(1);
            String safeMathPart = safeMathMatcher.group(2);
            String truncatedPart = StringOperation.truncateFromLeft(safeMathPart);

            int startColumn = baseIndex + safeMathMatcher.start(2); // 原始s的基准位置加上匹配的部分
            int endColumn = startColumn + truncatedPart.length(); // 获取结束位置
            analyzeSafeMath(safeMathPart, safeMathInfoList, startColumn);

            String insidePart = truncatedPart.substring(1, truncatedPart.length() - 1).trim();
//            System.out.println("" + safemathName + " -> " + truncatedPart);
            if(safemathName.contains("minus")){
                safeMathInfoList.add(new SafeMathInfo(safemathName, insidePart, "", startColumn, endColumn));
            } else {
                if(insidePart.charAt(0) != '(' && !insidePart.startsWith("func_")){
                    int seperatedIndex = insidePart.indexOf(",");
                    safeMathInfoList.add(new SafeMathInfo(safemathName,
                            insidePart.substring(0, seperatedIndex).trim(),
                            insidePart.substring(seperatedIndex + 1).trim(), startColumn, endColumn));
                } else if(insidePart.charAt(insidePart.length() - 1) != ')') {
                    int seperatedIndex = insidePart.lastIndexOf(",");
                    safeMathInfoList.add(new SafeMathInfo(safemathName,
                            insidePart.substring(0, seperatedIndex).trim(),
                            insidePart.substring(seperatedIndex + 1).trim(), startColumn, endColumn));
                } else {
                    String firstParam = StringOperation.truncateFromLeft(insidePart);
                    String secondParam = StringOperation.truncateFromRight(insidePart);
                    safeMathInfoList.add(new SafeMathInfo(safemathName, firstParam, secondParam, startColumn, endColumn));
                }
            }
        }
    }

    public void sortSafeMathInfo(Map<Integer, List<SafeMathInfo>> safeMathMap) {
        for (List<SafeMathInfo> list : safeMathMap.values()) {
            Collections.sort(list, new Comparator<SafeMathInfo>() {
                @Override
                public int compare(SafeMathInfo o1, SafeMathInfo o2) {
                    // 先检查是否包含
                    if (o1.getStartColumn() >= o2.getStartColumn() && o1.getEndColumn() <= o2.getEndColumn()) {
                        // o1 被 o2 包含，o1 优先
                        return -1; // 返回负数使 o2 排到后面
                    } else if (o2.getStartColumn() >= o1.getStartColumn() && o2.getEndColumn() <= o1.getEndColumn()) {
                        // o2 被 o1 包含，o2 优先
                        return 1; // 返回正数使 o1 排到后面
                    } else {
                        // 否则按 startColumn 升序排序
                        return Integer.compare(o1.getStartColumn(), o2.getStartColumn());
                    }
                }
            });
        }
    }
}
