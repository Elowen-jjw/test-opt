package ObjectOperation.datatype;

import ObjectOperation.list.CommonOperation;

import java.io.File;
import java.util.List;
import java.util.regex.*;
import java.math.BigInteger;

public class ConstantExtractor {

    public static void main(String[] args) {
        // 示例程序代码，包含各种格式的常量
        File file = new File("/home/sdu/Desktop/random11108" + ".c");
        List<String> initialFileList = CommonOperation.genInitialList(file);
        int count = 0;
        for(String codeSnippet: initialFileList) {
            count++;
            if(!isHaveOp(codeSnippet)) continue;
            // 使用正则表达式提取所有常量
            String regex = "(?<=\\W)(0x)?\\-?[0-9a-fA-F]+U?L{1,2}(?=\\W)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(codeSnippet);
            // 遍历所有匹配到的常量
            while (matcher.find()) {
                String constant = matcher.group();
                System.out.println(initialFileList.get(count - 1));
                System.out.println(count + "  Original constant: " + constant);

                // 将常量转换为 10 进制，并判断其类型范围
                String decimalValue = convertToDecimal(constant);
                String typeRange = getTypeRange(decimalValue, constant);

                // 输出结果
                System.out.printf("Decimal: %s, Type Range: %s\n", decimalValue, typeRange);
            }
        }
    }

    /**
     * 将匹配到的常量字符串转换为 10 进制形式
     * @param constant 常量字符串
     * @return 10 进制表示的常量字符串
     */
    private static String convertToDecimal(String constant) {
        BigInteger decimalValue;

        // 判断是否为十六进制格式 (0x 或 0X 开头)
        if (constant.startsWith("0x") || constant.startsWith("0X")) {
            // 去掉 "0x" 或 "0X" 前缀，并转换为 10 进制
            decimalValue = new BigInteger(constant.substring(2).replaceAll("[ULul]", ""), 16);
        } else {
            // 普通十进制数字
            decimalValue = new BigInteger(constant.replaceAll("[ULul]", ""));
        }

        return decimalValue.toString();
    }

    /**
     * 判断常量属于哪种数据类型的范围
     * @param decimalValue 10 进制表示的常量字符串
     * @param originalConstant 原始常量字符串（判断符号）
     * @return 该常量适合的最小数据类型范围
     */
    private static String getTypeRange(String decimalValue, String originalConstant) {
        BigInteger value = new BigInteger(decimalValue);

        // 根据原始常量是否带有 'U' 判断是否为无符号类型
        boolean isUnsigned = originalConstant.toUpperCase().contains("U");

        // 根据原始常量是否带有 'L' 判断是否为 long 类型
        boolean isLong = originalConstant.toUpperCase().contains("L");

        // 判断常量的最小数据类型范围
        if (!isUnsigned && fitsInRange(value, BigInteger.valueOf(Byte.MIN_VALUE), BigInteger.valueOf(Byte.MAX_VALUE))) {
            return "byte (signed)";
        } else if (!isUnsigned && fitsInRange(value, BigInteger.valueOf(Short.MIN_VALUE), BigInteger.valueOf(Short.MAX_VALUE))) {
            return "short (signed)";
        } else if (!isUnsigned && fitsInRange(value, BigInteger.valueOf(Integer.MIN_VALUE), BigInteger.valueOf(Integer.MAX_VALUE))) {
            return "int (signed)";
        } else if (!isUnsigned && fitsInRange(value, BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE))) {
            return "long (signed)";
        } else if (isUnsigned && fitsInRange(value, BigInteger.ZERO, BigInteger.valueOf(255))) {
            return "unsigned byte";
        } else if (isUnsigned && fitsInRange(value, BigInteger.ZERO, BigInteger.valueOf(65535))) {
            return "unsigned short";
        } else if (isUnsigned && fitsInRange(value, BigInteger.ZERO, new BigInteger("4294967295"))) {
            return "unsigned int";
        } else if (isUnsigned && fitsInRange(value, BigInteger.ZERO, new BigInteger("18446744073709551615"))) {
            return "unsigned long";
        } else if (!isUnsigned && fitsInRange(value, BigInteger.valueOf(Long.MIN_VALUE), new BigInteger("18446744073709551615"))) {
            return "BigInteger (signed or unsigned)";
        } else {
            return "Out of range";
        }
    }

    /**
     * 判断一个 BigInteger 值是否在指定的范围内
     * @param value 待判断的 BigInteger 值
     * @param min 范围的最小值
     * @param max 范围的最大值
     * @return 如果 value 在 [min, max] 范围内，则返回 true；否则返回 false
     */
    private static boolean fitsInRange(BigInteger value, BigInteger min, BigInteger max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    public static boolean isHaveOp(String line){
        return line.contains("+=") || line.contains("-=") ||
                line.contains("^=") || line.contains("&=") ||
                line.contains("*=") || line.contains(">>=") ||
                line.contains("<<=") || line.contains("/=") ||
                line.contains("%=") ||line.contains("|=");
    }
}
