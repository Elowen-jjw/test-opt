package AST_Information.model;

import java.util.ArrayList;
import java.util.List;

public class StructUnionBlock {
	protected String id;
	protected String name;
	protected String block_type;
	protected int startLine;
	protected int endLine;
	protected int astStartLine;
	protected int astEndLine;
	
	List<FieldVar> childFields = new ArrayList<FieldVar>();
	List<StructUnionBlock> childStructUnion = new ArrayList<StructUnionBlock>();
	StructUnionBlock parentStructUnion = null;
	
	public StructUnionBlock(String id, String name, String block_type) {
		this.id = id;
		this.name = name;
		this.block_type = block_type;
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
	
	public void setBlockType(String block_type) {
		this.block_type = block_type;
	}
	
	public String getBlockType() {
		return block_type;
	}
	
	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}
	
	public int getStartLine() {
		return startLine;
	}
	
	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}
	
	public int getEndLine() {
		return endLine;
	}
	
	public void setAstStartLine(int astStartLine) {
		this.astStartLine = astStartLine;
	}
	
	public int getAstStartLine() {
		return this.astStartLine;
	}
	
	public void setAstEndLine(int astEndLine) {
		this.astEndLine = astEndLine;
	}
	
	public int getAstEndLine() {
		return this.astEndLine;
	}
	
	public void setChildField(List<FieldVar> childFields) {
		this.childFields = childFields;
	}
	
	public List<FieldVar> getChildField() {
		return childFields;
	}
	
	public void setChildStructUnion(List<StructUnionBlock> childStructUnion) {
		this.childStructUnion = childStructUnion;
	}
	
	public List<StructUnionBlock> getChildStructUnion(){
		return childStructUnion;
	}
	
	public void setParentStructUnion(StructUnionBlock parentStructUnion) {
		this.parentStructUnion = parentStructUnion;
	}
	
	public StructUnionBlock getParentStructUnion(){
		return parentStructUnion;
	}
}
