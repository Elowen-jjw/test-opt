package astinfo.model;

public class CommonVar extends AstVariable{

	public CommonVar() {
		super();
	}
	
	public CommonVar(String id, String name, String type) {
		super(id, name,type);
	}
	
	public CommonVar(String id, String name, String type, boolean isConst) {
		super(id, name, type, isConst);
	}
	
	
}
