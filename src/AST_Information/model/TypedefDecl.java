package AST_Information.model;

public class TypedefDecl {
	String id;
	String name;
	String record_type;
	String record_id;
	String record_name;
	int startline;
	
	public TypedefDecl(String id, String name, int startline) {
		this.id = id;
		this.name = name;
		this.startline = startline;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}

	public void setReocrdType(String record_type) {
		this.record_type = record_type;
	}
	
	public String getRecordType() {
		return this.record_type;
	}
	
	public void setRecordId(String record_id) {
		this.record_id = record_id;
	}
	
	public String getRecordId() {
		return this.record_id;
	}
	
	public void setRecordName(String record_name) {
		this.record_name = record_name;
	}
	
	public String getRecordName() {
		return this.record_name;
	}
	
//	public void setEndline(int endline) {
//		this.endline = endline;
//	}
//	
//	public int getEndline() {
//		return this.endline;
//	}
}
