package astinfo.model;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArrayVar extends AstVariable {
	protected String eleCnt;
	protected String elementType;
	protected String elementKind;
	//存放变量id；判断时如果找不到id对应的变量，则说明该元素的值是立即数
	protected ArrayList<String> elementIdList = new ArrayList<String>();
	
	public ArrayVar() {
		super();
	}
	public ArrayVar(String id, String name, String type) {
		super(id, name, type);
		this.elementType = getEleTypeByType(type);
		this.elementKind = getEleKindByType(type);
		//this.length = getArrayLengthByType(type);
		this.eleCnt = getEleCntBySizeof(name, type);
	}
	
	public ArrayVar(String id, String name, String type, boolean isConst) {
		super(id, name, type, isConst);
		this.elementType = getEleTypeByType(type);
		this.elementKind = getEleKindByType(type);
		//this.length = getArrayLengthByType(type);
		this.eleCnt = getEleCntBySizeof(name, type);
	}
	
	public void setEleCnt(String eleCnt) {
		this.eleCnt = eleCnt;
	}
	public String getEleCnt() {
		return eleCnt;
	}
	public void setElementType(String elementType) {
		this.elementType = elementType;
	}
	public String getElementType() {
		return elementType;
	}
	public void setElementKind(String elementKind) {
		this.elementKind = elementKind;
	}
	public String getElementKind() {
		return elementKind;
	}
	public void setElementIdList(ArrayList<String> elementIdList) {
		this.elementIdList = elementIdList;
	}
	public ArrayList<String> getElementIdList() {
		return elementIdList;
	}
	
	public static String getEleTypeByType(String type) {
		String eleType = null;
		int endIndex = type.indexOf('[');
		if(type.charAt(endIndex-1) == ' ') endIndex--;
		eleType = type.substring(0, endIndex);
		return eleType;
	}
	
	public static String getEleKindByType(String type) {
		String elementKind = "common";
		//根据type分为common, pointer, array
		String regexPointerArray = "^([_a-zA-Z]+[_a-zA-Z0-9\\s]*)\\s(\\*+)((\\[[0-9a-zA-Z_\\s\\*]*\\])+)";	//int *arr[2 * b];
		Pattern pPointerArray = Pattern.compile(regexPointerArray);
		Matcher mPointerArray = pPointerArray.matcher(type);
		
		if(mPointerArray.find()) {
			elementKind = "pointer";
			//System.out.println("PointerArray: " + mPointerArray.groupCount() + " "  + mPointerArray.group(0));
			//System.out.println(mPointerArray.group(1) + " " + mPointerArray.group(2) + " " + mPointerArray.group(3));
		}
		return elementKind;
	}
	
	public static String getEleCntBySizeof(String varname, String type) {
		String length = "sizeof(" + varname + ")/sizeof(" + varname + getAddressZero(type) + ")";
		return length;
	}
	
	public static String getAddressZero(String type) {
		//int *[2][3] =》 int *rename[2][3];
		int begin = type.indexOf('[');
		int end = type.lastIndexOf(']')+1;
		//截[2][3],变 [0][0]]
		String address = type.substring(begin, end);
		address = address.replaceAll("\\[[0-9a-zA-Z_\\s\\*]*\\]", "\\[0\\]");
		return address;
	}
	
	/*
	public static String getArrayLengthByType(String type) {
		String length = "";
		String regexArrayLength = "^[_a-zA-Z]+[_a-zA-Z0-9\\s]*\\s\\**((\\[[0-9a-zA-Z_]*\\])+)$";
		Pattern pArray = Pattern.compile(regexArrayLength);
		Matcher mArray = pArray.matcher(type);
		int i = 0;
		if(mArray.find()) {
			String array = mArray.group(1);
			String regexNum = "\\[([0-9a-zA-Z_]+)\\]";
			Pattern pNum = Pattern.compile(regexNum);
			Matcher mNum = pNum.matcher(array);
			while(mNum.find()) {
				if(i!=0) length += "*";
				length += mNum.group(1);
				i++;
			}
		}
		return length;
	}
	*/
	
	/*
	 public static int getArrayLengthByType(String type) {
		int length = 1;
		String regexArrayLength = "^[_a-zA-Z]+[_a-zA-Z0-9\\s]*\\s\\**((\\[[0-9]+\\])+)$";
		Pattern pArray = Pattern.compile(regexArrayLength);
		Matcher mArray = pArray.matcher(type);
		
		if(mArray.find()) {
			String array = mArray.group(1);
			String regexNum = "\\[([0-9]+)\\]";
			Pattern pNum = Pattern.compile(regexNum);
			Matcher mNum = pNum.matcher(array);
			while(mNum.find()) {
				int num = Integer.parseInt(mNum.group(1));
				length = length * num;
			}
		}
		return length;
	}
	 */
	
}
