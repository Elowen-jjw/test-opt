package astinfo.model;

public class FieldVar {
	String id;
	String name;
	String type;
	String kind;
	int declareLine;
	boolean isReferenced = false;
	boolean isStructUnion = false;
	boolean isConst = false;
	StructUnionBlock parentStructUnion = null;
	
	public FieldVar(String id, String name, String type) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.kind = AstVariable.getVarKindByType(type);
		this.isStructUnion = AstVariable.judgeIsStructUnion(type);
	}
	
	public FieldVar(String id, String name, String type, boolean isConst) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.isConst = isConst;
		this.kind = AstVariable.getVarKindByType(type);
		this.isStructUnion = AstVariable.judgeIsStructUnion(type);
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return this.type;
	}
	
	public void setKind(String kind) {
		this.kind = kind;
	}
	
	public String getKind() {
		return this.kind;
	}
	
	public void setDeclareline(int declareLine) {
		this.declareLine = declareLine;
	}
	
	public int getDeclareLine() {
		return declareLine;
	}
	
	public void setIsReferenced(boolean isReferenced) {
		this.isReferenced = isReferenced;
	}
	
	public boolean getIsReferenced() {
		return this.isReferenced;
	}
	
	public void setIsStructUnion(boolean isStructUnion) {
		this.isStructUnion = isStructUnion;
	}
	
	public boolean getIsStructUnion() {
		return this.isStructUnion;
	}
	
	public void setIsConst(boolean isConst) {
		this.isConst = isConst;
	}
	
	public boolean getIsConst() {
		return this.isConst;
	}
	public void setParentStructUnion(StructUnionBlock parentStructUnion) {
		this.parentStructUnion = parentStructUnion;
	}
	
	public StructUnionBlock getParentStructUnion() {
		return parentStructUnion;
	}
	
}
