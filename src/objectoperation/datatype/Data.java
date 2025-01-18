package objectoperation.datatype;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
}
