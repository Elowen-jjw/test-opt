package ObjectOperation.datatype;

import java.math.BigInteger;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.CommonInfoFromFile;

public class StringOperation {
	
	public static boolean isNumber(String str) {
		try {
			Integer.valueOf(str);
			return true;
		}
		catch(Exception e) {
			return false;
		}
	}
	
	public static String getRandomVarName() {
		String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		//String str = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	    Random random = new Random();
	    //int randomLength  = random.nextInt(1,6);
	    int randomLength = (int) (Math.random()*2 + 3);
	    StringBuffer sb = new StringBuffer();
	    for(int i = 0;i < randomLength;i++){
	    	int number = random.nextInt(52);	//
	    	//int number = random.nextInt(63);
	    	sb.append(str.charAt(number));
	    }
	    return sb.toString();
	}
	
	public static boolean containsVarName(String string, String varName) {
        if(string.length() < varName.length()) return false;
        // 正则表达式：匹配 varName 后面不跟数字
        String regex = CommonInfoFromFile.replaceRegex(varName) + "(?![0-9_])";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        return matcher.find();
    }
	

	public static String truncateFromLeft(String input) {
		StringBuilder result = new StringBuilder();
		int bracketCount = 0;
		boolean insideBrackets = false;
		int i;
		for (i = 0; i < input.length(); i++) {
			char c = input.charAt(i);

			if (c == '(') {
				insideBrackets = true;
				bracketCount++;
				result.append(c);
			} else if (c == ')') {
				if (insideBrackets) {
					bracketCount--;
					result.append(c);
					if (bracketCount == 0) {
						insideBrackets = false;
						if(input.charAt(i + 1) == '0') {
							result.append(input.charAt(i + 1));
							break;
						} else if(input.charAt(i + 1) == ']'){
							continue;
						}
					}
				} else {
					break;
				}
			} else if (!Character.isLetterOrDigit(c) && c != '_' && c != '[' && c != ']' && c != '&' && c != '.') {
				// 非字母数字或下划线，且不在括号内部，截断
				if (!insideBrackets) {
					break;
				} else {
					result.append(c);
				}
			} else {
				result.append(c);
			}
		}

		return result.toString();
	}

	// 从右侧开始遍历截断字符串
	public static String truncateFromRight(String input) {
		StringBuilder result = new StringBuilder();
		int bracketCount = 0;
		boolean insideBrackets = false;
		int i;
		for (i = input.length() - 1; i >= 0; i--) {
			char c = input.charAt(i);
			if (c == ')') {
				insideBrackets = true;
				bracketCount++;
				result.insert(0, c);
			} else if (c == '(') {
				if (insideBrackets) {
					bracketCount--;
					result.insert(0, c);
					if (bracketCount == 0) {
						break;
					}
				} else {
					break;
				}
			} else if (!Character.isLetterOrDigit(c) && c != '_' && c != '[' && c != ']' && c != '.') {
				if (!insideBrackets) {
					break;
				} else {
					result.insert(0, c);
				}
			} else {
				result.insert(0, c);
			}
		}
		if(i > 0){
			String lefted = input.substring(0, i);
			Matcher m_lefted = Pattern.compile("(__builtin_[a-z]+$)|(func_\\d+$)").matcher(lefted);
			if(m_lefted.find())
				return m_lefted.group() + result;
		}
		return result.toString();
	}

	public static int countStars(String input) {
		int count = 0;
		for (int i = 0; i < input.length(); i++) {
			if (input.charAt(i) == '*') {
				count++;
			}
		}
		return count;
	}

	public static int countLeadingSpaces(String input) {
		if (input == null || input.isEmpty()) {
			return 0; // 如果字符串为空或为null，返回0
		}

		int count = 0;
		for (char c : input.toCharArray()) {
			if (c == ' ') {
				count++; // 如果字符是空格，则计数器加一
			} else {
				break; // 一旦遇到第一个非空字符，停止计数
			}
		}
		return count; // 返回空格的数量
	}

	public static BigInteger convertToDecimal(String dataType, String dataString) {
		// 处理带括号的字符串
		if (dataString.startsWith("(") && dataString.endsWith(")")) {
			dataString = dataString.substring(1, dataString.length() - 1);
		}

		BigInteger result;

		// 根据数据类型进行相应的处理
		switch (dataType) {
			case "int32_t": case "int":
				result = BigInteger.valueOf(parseNumber(dataString).intValue());
				break;
			case "uint32_t": case "unsigned int":
				result = parseNumber(dataString).and(BigInteger.valueOf(0xFFFFFFFFL)); // 正确处理 uint32_t
				break;
			case "int64_t": case "long":
				result = parseNumber(dataString);
				break;
			case "uint64_t": case "unsigned long":
				result = parseNumber(dataString).and(BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL)); // 正确处理 uint64_t
				// 如果解析出来的数是负数，手动转换为无符号
				if (result.signum() < 0) {
					result = result.add(BigInteger.ONE.shiftLeft(64)); // 加上2^64
				}
				break;
			case "int8_t": case "char":
				result = BigInteger.valueOf(parseNumber(dataString).byteValue());
				break;
			case "uint8_t": case "unsigned char":
				result = BigInteger.valueOf(parseNumber(dataString).intValue() & 0xFF);
				break;
			case "int16_t": case "short":
				result = BigInteger.valueOf(parseNumber(dataString).shortValue());
				break;
			case "uint16_t": case "unsigned short":
				result = BigInteger.valueOf(parseNumber(dataString).intValue() & 0xFFFF);
				break;
			default:
				result = null;
		}

		return result;
	}

	private static BigInteger parseNumber(String dataString) {
		dataString = dataString.trim();
		// 去掉后缀 L, UL, ULL, LL
		dataString = dataString.replaceAll("(L|UL|ULL|LL)$", "");

		if (dataString.startsWith("0x")) { // 十六进制
			return new BigInteger(dataString.substring(2), 16);
		} else { // 默认整型
			return new BigInteger(dataString);
		}
	}

	public static void main(String[] args) {
		// 测试用例
		String[] testTypes = {"int32_t", "uint32_t", "int64_t", "uint64_t", "int8_t", "uint8_t", "int16_t", "uint16_t"};
		String[] testValues = {
				"2147483649",     // int32_t max
				"-2",   // uint32_t max
				"9223372036854775807", // int64_t max
				"-1", // uint64_t max
				"127",            // int8_t max
				"-1UL",          // uint8_t max
				"32767",          // int16_t max
				"-1UL"         // uint16_t max
		};

		for (int i = 0; i < testTypes.length; i++) {
			BigInteger result = convertToDecimal(testTypes[i], testValues[i]);
			System.out.println("DataType: " + testTypes[i] + ", Value: " + result);
		}
	}

}
