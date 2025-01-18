package astinfo.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PointerVar extends AstVariable{
	protected String pointToKind;
	//pointer若malloc了内存空间，记录malloc所在行，以及malloc的大小,记录malloc后括号的字符串() malloc ();
	protected Map<Integer, String> mallocLineSize = new LinkedHashMap<Integer, String>();
	
	public PointerVar() {
		super();
	}
	
	public PointerVar(String id, String name, String type) {
		super(id, name,type);
		this.pointToKind = getPointToKindByType(type);
	}
	
	public PointerVar(String id, String name, String type, boolean isConst) {
		super(id, name,type, isConst);
		this.pointToKind = getPointToKindByType(type);
	}
	
	public void setPointToKind(String pointToKind) {
		this.pointToKind = pointToKind;
	}
	public String getPointToKind() {
		return pointToKind;
	}
	
	public static String getPointToKindByType(String type) {
		//根据type分为common, pointer, array
		String pointToKind = "common";
		String regexPointer1 = "^([_a-zA-Z]+[_a-zA-Z0-9\\s]*)\\s(\\*+)[_0-9a-zA-Z\\s]*$";
		String regexPointer2 = "^([_a-zA-Z]+[_a-zA-Z0-9\\s]*)\\s\\**(\\(\\*+\\))((\\[[0-9a-zA-Z\\s\\*]+\\])+)";
		Pattern pPointer1 = Pattern.compile(regexPointer1);
		Pattern pPointer2 = Pattern.compile(regexPointer2);
		Matcher mPointer1 = pPointer1.matcher(type);
		Matcher mPointer2 = pPointer2.matcher(type);
		
		if(mPointer1.find()) {
			String pointStr = mPointer1.group(2);
			if(pointStr.contains("**")) pointToKind = "pointer";
		}else if(mPointer2.find()) {
			if(mPointer2.group(2).contains("**")) {
//				System.out.println("pointer");
//				System.out.println(type);
				pointToKind = "pointer";
			}
			else pointToKind = "array";
		}
		return pointToKind;
	}
	
	public static String getLevelStar(String type) {
		String star = "";
		int start = type.indexOf('*');
		int end = type.lastIndexOf('*');
		if(start != -1) star = type.substring(start, end+1);
		while(type.indexOf('[') != -1) {
			star += "*";
			end = type.indexOf(']');
			type = type.substring(end);
		}
		
		return star;
	}
	
}
