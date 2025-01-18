package utity;

public class BasicIV {
	private String name;
	private String type;
	private int num;
	private String op;
	private int lineNumberInBlock;
	private int addLineNumber;
	private String conName;

	//num和conName 任选其一就可以

	public BasicIV(String name, String type, int num, String op, int lineNumberInBlock, int addLineNumber) {
		this.name = name;
		this.type = type;
		this.num = num;
		this.op = op;
		this.lineNumberInBlock = lineNumberInBlock;
		this.addLineNumber = addLineNumber;
	}

	public int getLineNumberInBlock() {
		return lineNumberInBlock;
	}

	public void setLineNumberInBlock(int lineNumberInBlock) {
		this.lineNumberInBlock = lineNumberInBlock;
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


	public String getOp() {
		return op;
	}
	
	public void setOp(String op) {
		this.op = op;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getConName() {
		return conName;
	}

	public void setConName(String conName) {
		this.conName = conName;
	}

	public int getAddLineNumber() {
		return addLineNumber;
	}

	public void setAddLineNumber(int addLineNumber) {
		this.addLineNumber = addLineNumber;
	}
}
