package common;

import mutate.minmax.SafeMathMacrosAnalysis;
import utity.SafeMathMacros;

import java.io.File;
import java.util.*;

public class PropertiesInfo {

    public final static String indexDir = "/home/sdu/Desktop";
    public final static String pointerAnalysisExport = "$LLVM_HOME/lib";
    public final static String csmithSanitizer = "clang";
//    public final static String testCompiler = "gcc";//这两个一定要是不一样的
    public final static String testSuiteDir = indexDir + "/RandomSuite";
    public final static String testSuiteInitialDir = indexDir + "/RandomSuite_Initial";
    public final static String testSuiteAssertDir = indexDir + "/RandomSuite_Assert";
    public final static String testSuiteStructDir = indexDir + "/RandomSuite_Struct";
    public final static String mutateOutermostDir = indexDir + "/mutate";
    public final static String mutateCseDir = mutateOutermostDir + "/cse";
    public final static String mutateStatuteDir = mutateOutermostDir + "/statute";
    public final static String mutateMinMaxDir = mutateOutermostDir + "/minmax";
    public final static String mutatePointerTarget = mutateOutermostDir + "/pt";

    public final static String mutateLoopIndexDir = mutateOutermostDir + "/loopIndex";
    public final static String mutateInlineDir = mutateOutermostDir + "/inline";
    public final static String mutateVectorDir = mutateOutermostDir + "/vector";
    public final static String inconResultIndexDir = indexDir + "/InconResult";
    public static String compilerType = "gcc"; //gcc | clang
    public final static int GenAndTest_ThreadCount = 15;
    public final static int Santizer_ThreadCount = 15;
    public final static int Reduced_ThreadCount = 4;

    public static final Map<String, SafeMathMacros> allSafeMathMacros = SafeMathMacrosAnalysis.getSafeMathMacrosMap();

    public static final List<String> minmaxFuncList = new ArrayList<>(){{
        add("long smin(long a, long b) { return a < b ? a : b; }");
        add("unsigned long umin(unsigned long a, unsigned long b) { return a < b ? a : b; }");
        add("long smax(long a, long b) { return a > b ? a : b; }");
        add("unsigned long umax(unsigned long a, unsigned long b) { return a > b ? a : b; }");
    }};

    public static final String forStmtPattern = "for\\s*\\(\\s*([a-zA-Z0-9_\\[\\]\\.]*)\\s*=\\s*\\(?[^;]*;" +
            "\\s*\\(?\\1\\s*[<>=!]+\\s*[^;]*;" +
            "\\s*([\\+\\-]{0,2}\\1\\s*[\\+\\-]{0,2}=?\\s*[^;]*)\\)";

    public final static HashMap<String, ArrayList<File>> hostMap = new HashMap<>(){{
        put("statute", new ArrayList<>(){{
                    add(new File(mutateStatuteDir));
                    add(new File(inconResultIndexDir + "/statute"));
                }}
        );
//        put("cse", new ArrayList<>(){{
//                    add(new File(mutateCseDir));
//                    add(new File(inconResultIndexDir + "/cse"));
//                }}
//        );
//        put("inline", new ArrayList<File>(){{
//                    add(new File(mutateInlineDir));
//                    add(new File(inconResultIndexDir + "/inline"));
//                }}
//        );
    }};

    public final static List<String> typeList = new ArrayList<>() {{
        add("int8_t");
        add("uint8_t");
        add("int16_t");
        add("uint16_t");
        add("int32_t");
        add("uint32_t");
        add("int64_t");
        add("uint64_t");

        add("char");
        add("unsigned char");
        add("short");
        add("unsigned short");
        add("int");
        add("unsigned int");
        add("long");
        add("unsigned long");
        add("long long");

        add("struct");
        add("union");
        add("enum");
        add("const");
        add("volatile");
    }};

    public final static Map<String, String> typeToSpecifier = new HashMap<>() {{
        put("int8_t", "%d");
        put("uint8_t", "%u");
        put("int16_t", "%d");
        put("uint16_t", "%u");
        put("int32_t", "%d");
        put("uint32_t", "%u");
        put("int64_t", "%ld");
        put("uint64_t", "%lu");

        put("char", "%c");
        put("unsigned char", "%c");
        put("short", "%hd");
        put("unsigned short", "%hu");
        put("int", "%d");
        put("unsigned int", "%u");
        put("long", "%ld");
        put("unsigned long", "%lu");
        put("long long", "%lld");
    }};

    public final static Map<String, String> commonToStandardType = new HashMap<>(){{
        put("char", "int8_t");
        put("unsigned char", "uint8_t");
        put("short", "int16_t");
        put("unsigned short", "uint16_t");
        put("int", "int32_t");
        put("unsigned int", "uint32_t");
        put("long", "int64_t");
        put("unsigned long", "uint64_t");
    }};

    public static final Map<String, List<String>> operatorReplacements = new HashMap<>(){{
        put("<=", Arrays.asList("!(#a# > #b#)", "#a# == #b# || #a# < #b#", "!(#a# != #b#) || #a# < #b#"));
        put("<", Arrays.asList("!(#a# >= #b#)"));
        put(">=", Arrays.asList("!(#a# < #b#)", "#a# == #b# || !(#a# < #b#)"));
        put(">", Arrays.asList("!(#a# <= #b#)"));
        put("==", Arrays.asList("!(#a# ^ #b#)", "(#a# ^ #b#) == 0", "!(#a# != #b#)", "~(~#a# | ~#b#)", "(#a# - #b#) == 0", "((#a# - #b#) | (#b# - #a#)) == 0", "!((#a# - #b#) | (#b# - #a#))"));
        put("!=", Arrays.asList("#a# ^ #b#", "(#a# - #b#) != 0", "(#a# ^ #b#) != 0", "(#a# < #b#) || (#a# > #b#)", "!(#a# == #b#)", "((#a# - #b#) | (#b# - #a#)) != 0"));
    }};

    public final static Map<Integer, String> specificVarName = new HashMap<>() {{
        put(0, "a");
        put(1, "b");
        put(2, "c");
        put(3, "d");
        put(4, "e");
        put(5, "f");
        put(6, "g");
        put(7, "h");
    }};

    public final static String indexPattern = "(?:\\[(?:(?:[ijklmn]*\\d*)|(?:\\(?[glp]_\\d+(?:\\[\\d+\\])*(?:.f\\d+)*\\s*[\\+\\*-]?\\s*\\d*\\)?)|(?:\\(?\\(\\*+[glp]_\\d+(?:\\[\\d+\\])*\\)(?:.f\\d+)*\\s*[\\+\\-\\*]?\\s*\\d*\\)?))\\])*";
    public final static String notHaveBraceVarRegex = "(?:[glp]_\\d+" + indexPattern + "(?:\\s*(?:->|\\.)\\s*f\\d+)?" + indexPattern + ")";
    public final static String haveBraceVarRegex = "(?:\\(\\*+[glp]_\\d+" + indexPattern + "\\)(?:\\s*(?:->|\\.)\\s*f\\d+)?" + indexPattern + ")";
    public final static String rightRegex = "((?:0[xX][0-9a-fA-F]+U?[Ll]*)|(?:[0-9]+U?[Ll]*)|(?:\\(-?\\d+\\))|(?:\\(void\\s*\\*+\\)[^;\\{\\}\\)]*)|(?:func_\\d+\\(.*\\))|(?:safe_\\w+\\(.+\\))|(?:[^;\\{\\}]+))";

    public final static Map<String, String> gccOpConfig = new HashMap<>(){{
        put("cse", "-fcse-follow-jumps -frerun-cse-after-loop -fgcse -fgcse-las -fgcse-after-reload -ffunction-cse -fgcse-lm -fgcse-sm -ftree-cselim " +
                "-fno-thread-jumps -fno-forward-propagate -flive-range-shrinkage " +
                "-fno-tree-loop-distribute-patterns -fno-crossjumping -fno-ipa-cp-clone");
        put("inline", "-finline -finline-atomics -fno-inline-functions -finline-functions-called-once -fno-inline-small-functions -fno-early-inlining -fno-partial-inlining -fno-indirect-inlining " +
                "-fno-ipa-sra -fno-ipa-vrp -fno-ipa-ra -fno-ipa-pta -fno-ipa-icf-variables -fno-ipa-icf-functions -fno-ipa-icf -fno-ipa-cp-clone -fno-ipa-cp -fno-ipa-bit-cp " +
                "-fno-thread-jumps -fno-forward-propagate -flive-range-shrinkage");
    }};


    public final static Map<String, String> clangOpConfig = new HashMap<>(){{
        put("cse", "-mllvm -polly");
        put("inline", "-mllvm -polly");
    }};


    public static List<String> selectRandomOptions(String options, int count) {
        List<String> optionList = Arrays.asList(options.split("\\s+"));
        Collections.shuffle(optionList);
        return new ArrayList<>(optionList.subList(0, Math.min(count, optionList.size())));
    }

}
