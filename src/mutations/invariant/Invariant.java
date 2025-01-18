package mutations.invariant;


import common.MuProcessException;
import objectoperation.datatype.Data;
import objectoperation.list.RandomAndCheck;
import utity.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
//import java.util.Set;
//import java.util.HashSet;


public class Invariant {
	File file;
	int startLine;
	int endLine;

	String[] assignOperator = {"+=", "-=", "*=", "="};
	String[] binaryOperator = {"+", "-", "*", "/","%","|", "&", "^", ">>", "<<"};

	ProcessingBlock initialBlock;
	List<ProcessingBlock> transformedBlockList;
	List<AvailableVariable> avarListCopy;
	List<InitialAndTransBlock> newItaList = new ArrayList<>();

	List<String> expMotionBeforeLoop;
	List<String> initAddInLoop;
	List<String> transAddInLoop;
	List<AvailableVariable> Invs;
	AvailableVariable var;

	public boolean isTrans;

	int randomIndex;

	public void invariant(FixedStuff fs){
		this.file = fs.getInitialFile();
		this.startLine = fs.getStartLine();
		this.endLine = fs.getEndLine();

		avarListCopy = new ArrayList<>();
		isTrans = true;

		for(AvailableVariable avar: fs.getAvarList()) {
			if(!avar.getIsConst()) {
				avarListCopy.add(avar);
			}
		}

		for(InitialAndTransBlock ita: fs.getIatList()){
			while(avarListCopy != null && avarListCopy.size() >= 3 && isTrans) {
				randomIndex = 0;
				RandomAndCheck rc = new RandomAndCheck();
				Invs = rc.getRandomAvailableVarChange(avarListCopy, 2);
				var = rc.getRandomAvailableVarChange(avarListCopy, 1).get(0);
				Trans(ita, "initial");
			}
		}
		if(newItaList.isEmpty()){
			isTrans = false;
			return;
		}
		fs.setIatList(newItaList);
	}

	public void Trans(InitialAndTransBlock ita, String removedType){
		randomIndex++;
		if(randomIndex > 10){
			System.out.println("generate invalid... skip");
			return;
		}
		List<String> currentAssignOp = new ArrayList<>();
		List<String> currentBinaryOp = new ArrayList<>();
		if(removedType.equals("initial")){
			currentAssignOp = Arrays.asList(assignOperator);
			currentBinaryOp = Arrays.asList(binaryOperator);
		}else if(removedType.equals("shift")){
			currentAssignOp = Arrays.asList(assignOperator);
			for(String s: binaryOperator){
				if(!s.trim().equals("<<") && !s.trim().equals(">>")){
					currentBinaryOp.add(s.trim());
				}
			}
		}else if(removedType.equals("overflow")){
			for(String s: assignOperator){
				if(!s.trim().equals("*=")){
					currentAssignOp.add(s.trim());
				}
			}
			for(String s: binaryOperator){
				if(!s.trim().equals("*")){
					currentBinaryOp.add(s.trim());
				}
			}
		}

		InitialAndTransBlock newIta = new InitialAndTransBlock(ita);//backups
		initialBlock = newIta.getInitialBlock();
		transformedBlockList = newIta.getTransformedBlockList();

		expMotionBeforeLoop = new ArrayList<String>();
		initAddInLoop = new ArrayList<String>();
		transAddInLoop = new ArrayList<String>();
		AvailableVariable inv1, inv2;
		inv1 = Invs.get(0);
		inv2 = Invs.get(1);
		String exp1, exp2, exp3;
		String assignOp = getRandomOperator(currentAssignOp);	//+= -= *=,=
		String op1 = getRandomOperator(currentBinaryOp);
		String op2 = getRandomOperator(currentBinaryOp);
		String op3 = getRandomOperator(currentBinaryOp);
		String op4 = getRandomOperator(currentBinaryOp);
		Random random = new Random();

		//generate exp, format: var = (inv1 op1 inv2) op2 (random(inv1,inv2) op3 var) op4 var
		if(op1.equals("/") || op1.equals("%")) {
			exp1 = "(" + inv1.getValue() + op1 + "(" + inv2.getValue() + "?" + inv2.getValue() + ":" + (random.nextInt(0, Integer.MAX_VALUE)+1) + "))";
		}else{
			exp1 = "(" + inv1.getValue() + op1 + inv2.getValue() + ")";
		}
		AvailableVariable randomInv = random.nextBoolean()? inv1:inv2;
		if(op3.equals("/") || op3.equals("%")) {
			exp2 = "(" + randomInv.getValue() + op3 + "(" + var.getValue() + "?" + var.getValue() + ":" + (random.nextInt(0, Integer.MAX_VALUE)+1) + "))";
		}else {
			exp2 = "(" + randomInv.getValue() + op3 + var.getValue() + ")";
		}
		if(op4.equals("/") || op4.equals("%")) {
			exp3 = "(" + var.getValue() + "?" + var.getValue() + ":" + (random.nextInt(0, Integer.MAX_VALUE)+1) + ")";
		}else {
			exp3 = var.getValue();
		}
		if(op2.equals("/") || op2.equals("%")){
			exp2 = "(" + exp2 + "?" + exp2 + ":" + (random.nextInt(0, Integer.MAX_VALUE)+1) + ")";
		}

		String motionVar = genRandomName();
		String maxType = Data.getMaxType(inv1.getType(), inv2.getType());
		if(op1.equals("/") || op1.equals("%")) maxType = Data.getMaxType(maxType, "int");

		initAddInLoop.add("//add expression: init");
		initAddInLoop.add( var.getValue() + assignOp + exp1 + op2 + exp2 + op4 + exp3 + ";");
		modifyInitial();

		List<String> ubList = new ArrayList<>();
		ubList.add("shift");
		if(MuProcessException.isHaveUB(file, startLine, endLine, initialBlock, ubList)){
			System.out.println(randomIndex + ": generate shift exponent.....");
			Trans(ita, "shift");
			return;
		}

		ubList.clear();
		ubList.add("overflow");
		if(MuProcessException.isHaveUB(file, startLine, endLine, initialBlock, ubList)){
			System.out.println(randomIndex + ": generate overflow.....");
			Trans(ita, "overflow");
			return;
		}

		expMotionBeforeLoop.add("//add expresstion: motion");
		expMotionBeforeLoop.add(maxType + " " + motionVar + " = " + exp1 + ";");
		transAddInLoop.add("//add expression: trans");
		transAddInLoop.add(var.getValue() + assignOp + motionVar + op2 + exp2 + op4 + exp3 + ";");

		for(ProcessingBlock singleTrans: transformedBlockList){
			modifyTransformed(singleTrans);
		}

		newItaList.add(newIta);
	}

	public void modifyInitial(){
		//initialBlock.setBlockList();
		initialBlock.getBlockList().addAll(1, initAddInLoop);
	}

	public void modifyTransformed(ProcessingBlock singleTrans){
		//singleTrans.setBlockList();
		singleTrans.setAddLineBoforeHeader(expMotionBeforeLoop);
		singleTrans.getBlockList().addAll(1,transAddInLoop);
	}

	private String getRandomOperator(List<String> operator) {
		Random random = new Random();
		return operator.get(random.nextInt(operator.size()));
	}

	private String getRandomOperatorNotShift(String[] operator) {
		Random random = new Random();
		return operator[random.nextInt(operator.length-2)];
	}

	public String genRandomName() {
		String str="_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		//String str = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random = new Random();
		//int randomLength  = random.nextInt(1,6);
		StringBuffer name = new StringBuffer();
		int randomLength = (int) (Math.random()*2 + 3);
		for(int i = 0;i < randomLength;i++){
			int number = random.nextInt(53);	//
			name.append(str.charAt(number));
		}

		return name.toString();
	}

}
