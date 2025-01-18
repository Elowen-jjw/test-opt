package mutate.pointer_assignment;

import java.util.*;

public class GenComposition {
    public static void generateCombinations(Map<Integer, Set<String>> conditionMap,
                                            List<Map<Integer, String>> resultList,
                                            Map<Integer, String> currentCombination,
                                            List<Integer> keys, int keyIndex) {
        // 递归终止条件，当遍历完所有key
        if (keyIndex == keys.size()) {
            resultList.add(new HashMap<>(currentCombination));
            return;
        }

        // 获取当前key对应的set
        int key = keys.get(keyIndex);
        Set<String> values = conditionMap.get(key);

        // 遍历当前key的所有值，并递归处理下一个key
        for (String value : values) {
            currentCombination.put(key, value);
            generateCombinations(conditionMap, resultList, currentCombination, keys, keyIndex + 1);
            currentCombination.remove(key);  // 回溯
        }
    }

    // 随机生成一个组合
    public static Map<Integer, String> generateRandomCombination(Map<Integer, Set<String>> conditionMap) {
        Map<Integer, String> randomCombination = new HashMap<>();
        Random random = new Random();

        for (Map.Entry<Integer, Set<String>> entry : conditionMap.entrySet()) {
            Integer key = entry.getKey();
            Set<String> values = entry.getValue();
            // 从set中随机选取一个元素
            int randomIndex = random.nextInt(values.size());
            String randomValue = new ArrayList<>(values).get(randomIndex);
            randomCombination.put(key, randomValue);
        }

        return randomCombination;
    }

    public static List<Map<Integer, String>> getCombinations(Map<Integer, Set<String>> conditionMap, int maxComSum) {
        List<Map<Integer, String>> resultList = new ArrayList<>();
        List<Integer> keys = new ArrayList<>(conditionMap.keySet());
        Map<Integer, String> currentCombination = new HashMap<>();

        // 首先计算所有组合的数量
        int totalCombinations = 1;
        for (Set<String> values : conditionMap.values()) {
            totalCombinations *= values.size();
            // 如果组合数量超过500，直接采用随机组合的方式
            if (totalCombinations > maxComSum) {
                resultList.add(generateRandomCombination(conditionMap));
                return resultList;  // 返回一个随机组合
            }
        }

        // 如果组合数量 <= 500，则生成所有组合
        generateCombinations(conditionMap, resultList, currentCombination, keys, 0);
        return resultList;
    }
}
