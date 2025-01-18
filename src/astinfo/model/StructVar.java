package astinfo.model;

public class StructVar extends AstVariable{
	
	public StructVar() {
		super();
	}
	
	public StructVar(String id, String name, String type) {
		super(id, name,type);
	}
	
	public StructVar(String id, String name, String type, boolean isConst) {
		super(id, name, type, isConst);
	}
}
