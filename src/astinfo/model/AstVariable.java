package astinfo.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AstVariable {
	protected String id;
	protected String name;
	protected String type;
	protected String kind;
	protected int declareLine;
	protected boolean isStructUnion = false;
	protected boolean isConst = false;
	protected boolean isGlobal = false;
	protected String type2 = "";
	protected boolean isInitialized = false;
	protected boolean isParmVar = false;
	protected boolean isUsed = false;
	public List<Integer> assignLine = new ArrayList<Integer>();
	public List<Integer> useLine = new ArrayList<Integer>();
	public Set<Integer> existUseLine = new HashSet<Integer>();
	
	public AstVariable() {
		// TODO Auto-generated constructor stub
	}
	
	public AstVariable(String id, String name, String type) {
		this.id = id;
		this.name = name;
		this.type = type.replaceAll("\\s+", " ");
		this.isStructUnion = judgeIsStructUnion(type);
	}
	
	public AstVariable(String id, String name, String type, boolean isConst) {
		this.id = id;
		this.name = name;
		this.type = type.replaceAll("\\s+", " ");
		this.isConst = isConst;
		this.isStructUnion = judgeIsStructUnion(type);
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public void setKind(String kind) {
		this.kind = kind;
	}
	public String getKind() {
		return kind;
	}
	public void setDeclareline(int declareLine) {
		this.declareLine = declareLine;
	}
	public int getDeclareLine() {
		return declareLine;
	}
	public void setIsStructUnion(boolean isStructUnion) {
		this.isStructUnion = isStructUnion;
	}
	public boolean getIsStructUnion() {
		return isStructUnion;
	}
	public void setIsConst(boolean isConst) {
		this.isConst = isConst;
	}
	public boolean getIsConst() {
		return this.isConst;
	}
	public void setIsGlobal(boolean isGlobal) {
		this.isGlobal = isGlobal;
	}
	public boolean getIsGlobal() {
		return isGlobal;
	}
	public void setIsInitialized(boolean isInitialized) {
		this.isInitialized = isInitialized;
	}
	public boolean getIsIntialized() {
		return isInitialized;
	}
	public void setIsParmVar(boolean isParmVar) {
		this.isParmVar = isParmVar;
	}
	public boolean getIsParmVar() {
		return isParmVar;
	}
	
	public void setType2(String type2) {
		this.type2 = type2;
	}
	
	public String getType2() {
		return this.type2;
	}
	public boolean getIsUsed() {
		return isUsed;
	}
	public void setIsUsed(boolean isUsed) {
		this.isUsed = isUsed;
	}
	
	public static boolean judgeIsStructUnion(String type) {
		if(type.contains("struct ") || type.contains("union ")) return true;
		return false;
	}
	
	public static String getVarKindByType(String type) {
		//根据type分为common, pointer, array
		String varKind = "common";
		String regexPointer1 = "^([_a-zA-Z]+[_a-zA-Z0-9\\s]*)\\s(\\*+)[_0-9a-zA_Z\\s]*$";	//非数组指针
		//String regexPointer2 = "^([_a-zA-Z]+[_a-zA-Z0-9\\s]*)\\s\\**(\\(\\*+\\))((\\[[0-9]+\\])+)$";
		String regexPointer2 = "^([_a-zA-Z]+[_a-zA-Z0-9\\s]*)\\s\\**(\\(\\*+\\))((\\[[0-9a-zA-Z_\\s\\*]*\\])+)";	//数组指针
		//String regexArray = "^([_a-zA-Z]+[_a-zA-Z0-9\\s]*)\\s\\**((\\[[0-9]+\\])+)$";
		//int *[] ; int[]; int [2 * n] 有空格
		String regexArray = "^([_a-zA-Z]+[_a-zA-Z0-9\\s]*)\\s*\\**((\\[[0-9a-zA-Z_\\s\\*]*\\])+)";
		Pattern pPointer1 = Pattern.compile(regexPointer1);
		Pattern pPointer2 = Pattern.compile(regexPointer2);
		Pattern pArray = Pattern.compile(regexArray);
		Matcher mPointer1 = pPointer1.matcher(type);
		Matcher mPointer2 = pPointer2.matcher(type);
		Matcher mArray = pArray.matcher(type);
		
		if(mPointer1.find() || mPointer2.find()) {
			varKind = "pointer";
//			System.out.println("pointer: " + type);
		}else if(mArray.find()) {
			varKind = "array";
			//System.out.println("Array: " + mArray.group(0));
			//System.out.println(mArray.group(1) + " " + mArray.group(2));
		}else varKind = "common";
		
		return varKind;
	}
	
	public static int getPointerLevel(String varkind, String vartype) {
		int level = 0;
		if(varkind.equals("array")) {
			
		}
		
		return level;
	}
	
}
