package AST_Information;

import AST_Information.model.IfStatement;
import AST_Information.model.LoopStatement;

import java.util.ArrayList;
import java.util.List;

public class AstStmtOperation {
	public static List<LoopStatement> getAllLoops(List<LoopStatement> outmostLoopList){
		List<LoopStatement> res = new ArrayList<LoopStatement>();
		for(LoopStatement stmt: outmostLoopList) {
			res.add(stmt);
			res.addAll(getAllLoops(stmt.getLoopList()));
		}
		return res;
	}
	
	public static List<IfStatement> getAllIfs(List<IfStatement> outmostIfList){
		List<IfStatement> res = new ArrayList<IfStatement>();
		for(IfStatement stmt: outmostIfList) {
			res.add(stmt);
			res.addAll(getAllIfs(stmt.getIfList()));
		}
		return res;
	}
}
