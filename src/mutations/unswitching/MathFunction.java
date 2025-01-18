package mutations.unswitching;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MathFunction {
    public String getFucOneVar(){
        List<String> mathList = new ArrayList<>();
        mathList.add("sin");
        mathList.add("cos");
        mathList.add("tan");

        mathList.add("asin");
        mathList.add("acos");
        mathList.add("atan");

        mathList.add("sinh");
        mathList.add("cosh");
        mathList.add("tanh");
        mathList.add("asinh");
        mathList.add("acosh");
        mathList.add("atanh");

        mathList.add("exp");
        mathList.add("log");
        mathList.add("log10");
        mathList.add("exp2");
        mathList.add("expm1");
        mathList.add("log2");

        mathList.add("sqrt");
        mathList.add("cbrt");
        mathList.add("abs");
        mathList.add("fabs");

        mathList.add("");

        Random random = new Random();

        return mathList.get(random.nextInt(mathList.size()));

    }

    public String getFucTwoVar(){
        List<String> mathList = new ArrayList<>();
        mathList.add("atan2");
        mathList.add("ldexp");
        mathList.add("pow");
        mathList.add("hypot");

        Random random = new Random();

        return mathList.get(random.nextInt(mathList.size()));
    }
}
