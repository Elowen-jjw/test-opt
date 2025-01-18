package mutate.minmax;

import java.util.*;

public class ParamReplacedOp {
    public void generateReplacements(Map<Integer, List<SafeMathInfo>> safeMathMap, List<String> initialFileList, Map<Integer, List<String>> mutateMap) {
        for (Integer i : safeMathMap.keySet()) {
            List<String> results = new ArrayList<>();
            String currentLine = initialFileList.get(i - 1);
            List<AllReplacementsInfo> allReplacements = new ArrayList<>();

            for (SafeMathInfo func : safeMathMap.get(i)) {
                if(!func.getFirstReplacedMinMax().isEmpty())
                    allReplacements.add(new AllReplacementsInfo(func.getFirstStartColumn(), func.getFirstEndColumn(), func.getFirstReplacedMinMax()));
                if(!func.getSecondReplacedMinMax().isEmpty())
                    allReplacements.add(new AllReplacementsInfo(func.getSecondStartColumn(), func.getSecondEndColumn(), func.getSecondReplacedMinMax()));
            }

            if(allReplacements.isEmpty()) continue;

            // 存储所有组合的结果
            List<List<ReplacementInfo>> allCombinations = new ArrayList<>();

            long totalCombinations = 1;
            boolean isOverThousand = false;
            for (AllReplacementsInfo info : allReplacements) {
                totalCombinations *= info.replacement.size();
                if (totalCombinations > 1000) {
                    isOverThousand = true;
                    break;
                }
            }

            if(isOverThousand){
//                System.out.println(i + "starts to generate limited combinations in the same line....");
                generateLimitedCombinations(allReplacements, new ArrayList<>(), allCombinations);
            } else {
//                System.out.println(i + "starts to generate combinations in the same line....");
                generateCombinations(allReplacements, new ArrayList<>(), allCombinations);
            }

            // 生成同一行所有可能的组合
            if(!allCombinations.isEmpty()) {
//                System.out.println("同一行的组合数：" + allCombinations.size());
                for (List<ReplacementInfo> combination : allCombinations) {
//                    System.out.println("Combination:");
//                    for (ReplacementInfo replacementInfo : combination) {
//                        System.out.println("Start=" + replacementInfo.startColumn +
//                                ", End=" + replacementInfo.endColumn +
//                                ", \n1=" + initialFileList.get(i-1).substring(replacementInfo.startColumn, replacementInfo.endColumn) +
//                                ", \n2='" + replacementInfo.replacement + "'");
//                    }
//                    System.out.println();
                    String replacedLine = applyReplacements(currentLine, combination);
                    if(!replacedLine.isEmpty())
                        results.add(replacedLine);
                }
            }
            if(!results.isEmpty()) {
                mutateMap.put(i, results);
            }
        }
    }

    public void generateLimitedCombinations(List<AllReplacementsInfo> allReplacements, List<ReplacementInfo> currentCombination, List<List<ReplacementInfo>> allCombinations) {
        currentCombination.clear(); // 清空当前组合
        // 进行随机选择其余的组合
        for (int k = 0; k < 1000; k++) {
            for (int j = 0; j < allReplacements.size(); j++) {
                AllReplacementsInfo replacementInfo = allReplacements.get(j);
                int randomIndex = new Random().nextInt(replacementInfo.replacement.size());
                currentCombination.add(new ReplacementInfo(replacementInfo.startColumn, replacementInfo.endColumn, replacementInfo.replacement.get(randomIndex)));
            }
            allCombinations.add(new ArrayList<>(currentCombination)); // 存储当前组合
            currentCombination.clear(); // 清空当前组合以准备下一个组合
        }
    }

    public void generateCombinations(List<AllReplacementsInfo> allReplacements, List<ReplacementInfo> currentCombination, List<List<ReplacementInfo>> allCombinations) {
        if (currentCombination.size() == allReplacements.size()) {
            if (!currentCombination.isEmpty()) {
                allCombinations.add(new ArrayList<>(currentCombination));
            }
            return;
        }

        int index = currentCombination.size();
        AllReplacementsInfo replacementInfo = allReplacements.get(index);

        // Shuffle replacements to get random combinations
        List<String> shuffledReplacements = new ArrayList<>(replacementInfo.replacement);
        Collections.shuffle(shuffledReplacements);

        // 遍历打乱后的替换
        for (String replacement : shuffledReplacements) {
            currentCombination.add(new ReplacementInfo(replacementInfo.startColumn, replacementInfo.endColumn, replacement));
            generateCombinations(allReplacements, currentCombination, allCombinations);
            currentCombination.remove(currentCombination.size() - 1); // backtrack
        }
    }



    public String applyReplacements(String initialLine, List<ReplacementInfo> combination) {
        combination.sort(new Comparator<ReplacementInfo>() {
            @Override
            public int compare(ReplacementInfo o1, ReplacementInfo o2) {
                // Check for inclusion
                if (o1.startColumn >= o2.startColumn && o1.endColumn <= o2.endColumn) {
                    return -1; // o1 is contained in o2
                } else if (o2.startColumn >= o1.startColumn && o2.endColumn <= o1.endColumn) {
                    return 1; // o2 is contained in o1
                } else {
                    return Integer.compare(o1.startColumn, o2.startColumn); // Sort by startColumn
                }
            }
        });

        List<ReplacementInfo> finalReplacements = new ArrayList<>();

        for (int j = 0; j < combination.size(); j++) {
            ReplacementInfo current = combination.get(j);
            boolean isIncluded = false;
            for(int k = j + 1; k < combination.size(); k++){
                ReplacementInfo next = combination.get(k);
                if (current.startColumn >= next.startColumn && current.endColumn <= next.endColumn) {
                    isIncluded = true;
//                    System.out.println("把: " + next.replacement);
//                    System.out.println("中的: " +  initialLine.substring(current.startColumn, current.endColumn));
//                    System.out.println("替换成: " + current.replacement);
                    next.replacement = next.replacement.substring(0, next.replacement.indexOf(",") + 2)
                            + initialLine.substring(next.startColumn, current.startColumn) + current.replacement
                            + initialLine.substring(current.endColumn, next.endColumn) + ")";
//                    next.replacement = next.replacement.replace(initialLine.substring(current.startColumn, current.endColumn), current.replacement);
//                    System.out.println("替换之后为: " + next.replacement);
                    break;
                }
            }
            if(!isIncluded)
                finalReplacements.add(current);
        }

//        for (ReplacementInfo replacementInfo: finalReplacements) {
//            System.out.println("-------------");
//            System.out.println("Start=" + replacementInfo.startColumn +
//                    ", End=" + replacementInfo.endColumn +
//                    ", Replacement='" + replacementInfo.replacement + "'");
//        }

        StringBuilder result = new StringBuilder(initialLine);
        int offset = 0;

        for (ReplacementInfo replacement : finalReplacements) {
            int start = replacement.startColumn + offset;
            int end = replacement.endColumn + offset;

            result.replace(start, end, replacement.replacement);
            offset += replacement.replacement.length() - (end - start);
        }

//        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
//        System.out.println(result);
//        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");

        return result.toString();
    }

    class AllReplacementsInfo {
        int startColumn;
        int endColumn;
        List<String> replacement;

        AllReplacementsInfo(int startColumn, int endColumn, List<String> replacement) {
            this.startColumn = startColumn;
            this.endColumn = endColumn;
            this.replacement = replacement;
        }
    }

    class ReplacementInfo {
        int startColumn;
        int endColumn;
        String replacement;

        ReplacementInfo(int startColumn, int endColumn, String replacement) {
            this.startColumn = startColumn;
            this.endColumn = endColumn;
            this.replacement = replacement;
        }
    }
}
