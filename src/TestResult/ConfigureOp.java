package TestResult;

import processtimer.ProcessTerminal;

import java.util.*;

public class ConfigureOp {

    public final static List<String[]> gccConfig = new ArrayList<>() {{
        add(new String[]{"-fno-tree-bit-ccp"});
        add(new String[]{"-fno-strict-aliasing", "-fwrapv"});
        add(new String[]{"-fno-tree-ccp", "-fno-tree-forwprop", "-fno-tree-fre"});
        add(new String[]{"-ftree-loop-distribute-patterns"});
        add(new String[]{"-ftree-vectorize"});
        add(new String[]{"-ftree-vrp"});
        add(new String[]{"-finline-small-functions -fpartial-inlining --param max-inline-insns-single=1 --param uninlined-function-insns=10000"});
        add(new String[]{"-flto", "-flto-partition=none"});
        add(new String[]{"-fomit-frame-pointer"});
        add(new String[]{"-funroll-loops -fpeel-loops -ftracer -finline-functions"});
    }};

    public final static Map<String, List<String[]>> clangConfig = new HashMap<>() {{
        put("--target=x86_64-linux-gnu", new ArrayList<>() {{
            add(new String[]{"-mllvm -inline-threshold=100000"});
            add(new String[]{"-mavx", "-mavx2", "-mfma"});
            add(new String[]{"-mcmodel=large"});
        }});
        put("--target=riscv64-linux-gnu", new ArrayList<>() {{
            add(new String[]{"-fno-strict-aliasing", "-flto -fuse-ld=lld", "-fwrapv"});
            add(new String[]{"-mllvm -force-vector-width=16 -mllvm -force-vector-interleave=2"});
        }});
    }};

    public static void main(String[] args) {
        // Print results
        for (String entry:getGccConfigs()) {
            System.out.println(entry);
//            String command = "cd /home/sdu/Desktop/InconResult/minmax/gcc_configs/output_inconsistent/random10623 && "
//                    + " gcc random10623_minmax_203.c -lm -w -Os " + entry + " && ./a.out";
//            List<String> execLines = ProcessTerminal.listNotMemCheck(command, "sh");
//            System.out.println(execLines);
        }
    }

    public static List<String> getClangConfigs(){
        List<String> result = getAllClangConfigs(clangConfig);
        return getRandomElements(result, 5);
    }

    public static List<String> getGccConfigs(){
        List<String> result = getAllGccConfigs();
        return getRandomElements(result, 5);
    }

    public static List<String> getAllGccConfigs(){
        List<String> result = new ArrayList<>();
        for (String[] array : gccConfig) {
            if (array.length == 1) {
                // 如果长度为1，直接加入列表
                result.add(array[0]);
            } else {
                // 生成组合并加入结果
                generateGccCombinations(array, result);
            }
        }
        return result;
    }

    public static List<String> getRandomElements(List<String> result, int numberOfElements) {
        // 先打乱列表
        Collections.shuffle(result);

        // 选择前 numberOfElements 个元素，确保不会超过列表大小
        return result.subList(0, Math.min(numberOfElements, result.size()));
    }

    public static List<String> getAllClangConfigs(Map<String, List<String[]>> clangConfig) {
        List<String> result = new ArrayList<>();

        for (Map.Entry<String, List<String[]>> entry : clangConfig.entrySet()) {
            String key = entry.getKey();
            List<String[]> arrays = entry.getValue();

            List<List<String>> combinations = new ArrayList<>();

            // Process each array
            for (String[] array : arrays) {
                if (array.length == 1) {
                    // If only one element, directly add it to the combinations
                    combinations.add(Collections.singletonList(array[0]));
                } else {
                    // Generate combinations for length > 1
                    List<String> currentCombinations = new ArrayList<>();
                    for (String s : array) {
                        currentCombinations.add(s);
                    }
                    List<List<String>> generatedCombinations = new ArrayList<>();
                    getAllClangConfigs(currentCombinations, 0, new ArrayList<>(), generatedCombinations);
                    combinations.addAll(generatedCombinations);
                }
            }

            // Construct final result with the key
            for (List<String> combination : combinations) {
                result.add(key + " " + String.join(" ", combination));
            }
        }

        return result;
    }

    private static void getAllClangConfigs(List<String> options, int index, List<String> currentCombination, List<List<String>> allCombinations) {
        if (index == options.size()) {
            allCombinations.add(new ArrayList<>(currentCombination));
            return;
        }

        currentCombination.add(options.get(index));
        getAllClangConfigs(options, index + 1, currentCombination, allCombinations);
        currentCombination.remove(currentCombination.size() - 1); // backtrack
        getAllClangConfigs(options, index + 1, currentCombination, allCombinations); // Explore without current index
    }



    private static void generateGccCombinations(String[] array, List<String> result) {
        int n = array.length;
        // 通过位掩码生成组合
        for (int i = 1; i < (1 << n); i++) { // 从 1 到 2^n - 1
            StringBuilder combination = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) > 0) { // 检查位
                    combination.append(array[j]).append(" ");
                }
            }
            result.add(combination.toString().trim()); // 将组合添加到结果中
        }
    }

}
