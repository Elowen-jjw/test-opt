package ObjectOperation.datatype;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Data {

	public static Map<String, Integer> dataTypeMap = new HashMap<String, Integer>() {/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			put("signed char", 1);
			put("int8_t", 1);
			put("unsigned char", 2);
			put("uint8_t", 2);
			put("short",3);
			put("short int", 3);
			put("int16_t", 3);
			put("unsigned short", 4);
			put("unsigned short int", 4);
			put("uint16_t", 4);
	        put("int", 5);
	        put("int32_t", 5);
	        put("unsigned", 6);
	        put("unsigned int",6);
	        put("uint32_t", 6);
	        put("long",7);
	        put("long int",7);
	        put("int64_t",7);
	        put("unsigend long",8);
	        put("unsigend long int",8);
	        put("uint64_t",8);
	    }
	};
		
	public static String getMaxType(String ta, String tb) {
		if(Data.dataTypeMap.get(ta) > Data.dataTypeMap.get(tb)) {
			return ta;
		}else return tb;
	}

	public static String getMaxTypeInList(List<String> typeList){
		String maxType = "int8_t";
		for(String type: typeList){
			if(Data.dataTypeMap.get(type) > Data.dataTypeMap.get(maxType)) {
				maxType = type;
			}
		}
		return maxType;
	}
	
	public static int compareType(String ta, String tb) {
		int levela = Data.dataTypeMap.get(ta);
		int levelb = Data.dataTypeMap.get(tb);
		if(levela < levelb) {
			return -1;
		}else if(levela == levelb) {
			return 0;
		}else {
			return 1;
		}
	}

	public static String addPrintf(int lineNumber, String type, String printfExpression, String calculateExpression){
		String dataType = "";
		if(type.equals("char") || type.equals("signed char") || type.equals("int8_t")
				|| type.equals("short") || type.equals("signed short") || type.equals("signed short int") || type.equals("short int") || type.equals("int16_t")
				|| type.equals("int") || type.equals("signed int") || type.equals("signed") || type.equals("int32_t")){
			dataType = "d";
		}else if(type.equals("unsigned char") || type.equals("uint8_t")
				|| type.equals("unsigned short") || type.equals("unsigned short int") || type.equals("uint16_t")
				|| type.equals("unsigned int") || type.equals("unsigned") || type.equals("uint32_t")){
			dataType = "u";
		}else if(type.equals("long") || type.equals("signed long") || type.equals("long int")
				|| type.equals("signed long int")|| type.equals("int64_t")){
			dataType = "ld";
		}else if(type.equals("unsigned long") || type.equals("unsigned long int") || type.equals("uint64_t")){
			dataType = "lu";
		}
		String printfString = "printf(\"" + lineNumber +": " + printfExpression + " @ %" + dataType + "\\n\", " + calculateExpression + ");";
		String elseString = "printf(\"" + lineNumber +": " + printfExpression + " @ " + "NULL\\n\");";
		if(printfExpression.matches("\\(\\*+[glp]_\\d+.*\\)(\\.f\\d+)?")) {
			return generateNullCheck(printfExpression) + "{\n" + printfString + "\n} else {\n" +  elseString + "\n}";
		} else {
			return printfString;
		}
	}

	public static String generateNullCheck(String varName) {
		// 计算指针级别
		int pointerLevel = 0;
		for (char c : varName.toCharArray()) {
			if (c == '*') {
				pointerLevel++;
			}
		}

		// 生成 null 检查的字符串
		StringBuilder nullCheckBuilder = new StringBuilder("if (");
		String vName = varName.replaceAll("\\*|\\(|\\)|(\\.f\\d+)", "").trim();
//		String fname = "";
//		Matcher m = Pattern.compile("\\.f\\d+").matcher(varName);
//		if(m.find()){
//			fname = m.group();
//		}

		// 为每个指针级别添加 null 检查
		for (int i = 0; i < pointerLevel; i++) {
			// 逐级构造变量名
			nullCheckBuilder.append("(").append("*".repeat(i)).append(vName).append(")").append(" != NULL");
			if (i < pointerLevel - 1) {
				nullCheckBuilder.append(" && "); // 添加逻辑与
			}
		}

		nullCheckBuilder.append(")");

		return nullCheckBuilder.toString();
	}

	public static void main(String args[]){
		System.out.println(generateNullCheck("(***p_45)"));
	}
	
}
