package objectoperation.structure;

//import AST_Information.model.LoopStatement;
import objectoperation.list.CommonOperation;
import utity.IfInLoop;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindInfoInLoop {

    public List<IfInLoop> findInfoInLoop(List<String> loopBlock){
        List<IfInLoop> ifList = new ArrayList<>();

        String s_if = "\\bif\\s*\\((.*)\\)\\s*\\{";
        String s_for = "\\bfor\\s*\\(.*\\)\\s*\\{";
        String s_else = "\\}\\s*else\\s*\\{";
        Pattern p_if = Pattern.compile(s_if);
        Pattern p_for = Pattern.compile(s_for);
        Pattern p_else = Pattern.compile(s_else);
        Matcher m_if;
        Matcher m_for;
        Matcher m_else;

        Stack<IfInLoop> ifStack = new Stack<>();
        Stack<String> stringStack = new Stack<String>();

        for(int i=0; i<loopBlock.size(); i++){
            String s = loopBlock.get(i);
            m_if = p_if.matcher(s);
            m_for = p_for.matcher(s);
            m_else = p_else.matcher(s);

            if(m_if.find()){
                IfInLoop newIf = new IfInLoop(m_if.group(1), i + 1);
                newIf.setElseLine(-1);
                ifStack.push(newIf);
                ifList.add(newIf);
                stringStack.push(s);
            }
            else if(m_for.find()){
                stringStack.push(s);
            }
            else if(m_else.find()){
                ifStack.peek().setElseLine(i + 1);
            }
            else if(s.trim().equals("}")){
                String popLine = stringStack.pop().trim();
                if(popLine.matches("\\bif\\s*\\(.*\\)\\s*\\{")){
                    ifStack.pop().setEndLine(i + 1);
                }
            }
        }

        for(IfInLoop singleIf: ifList){
            List<String> ifBody;
            List<String> elseBody;
            if(singleIf.getElseLine() != -1) {
                ifBody = CommonOperation.getListPart(loopBlock, singleIf.getStartLine() + 1, singleIf.getElseLine() - 1);
                elseBody = CommonOperation.getListPart(loopBlock, singleIf.getElseLine() + 1, singleIf.getEndLine() - 1 );
            }
            else{
                ifBody = CommonOperation.getListPart(loopBlock, singleIf.getStartLine() + 1, singleIf.getEndLine() - 1);
                elseBody = new ArrayList<>();
            }
            singleIf.setIfBody(ifBody);
            singleIf.setElseBody(elseBody);
        }

        return ifList;
    }

//    public Map<Integer, Integer> getInnerLoop(LoopStatement loop){
//        Map<Integer, Integer> map = new HashMap<>();
//        if(!loop.getLoopList().isEmpty()){
//            for(LoopStatement innerLoop: loop.getLoopList()){
//                map.put(innerLoop.getStartLine() - loop.getStartLine(), innerLoop.getEndLine() - loop.getStartLine());
//            }
//        }
//        return map;
//    }
//
//    public List<IfInLoop> findInfoInLoopSimple(List<String> loopBlock){
//        List<IfInLoop> ifList = new ArrayList<>();
//        boolean isFirstLevel = false;
//        boolean isElse = false;
//
//        String s_con = "\\bif\\s*\\((.*)\\)\\s*\\{";
//        Pattern p_con = Pattern.compile(s_con);
//        Matcher m_con;
//        List<String> ifBlock = new ArrayList<>();
//        List<String> elseBlock = new ArrayList<>();
//        Stack<String> ifStack = new Stack<>();
//        Stack<String> elseStack = new Stack();
//        int startLine = 0;
//        int endLine = 0;
//        String condition = "";
//
//        for(int i=0; i<loopBlock.size(); i++){
//            String s = loopBlock.get(i);
//            m_con = p_con.matcher(s);
//
//            if(isFirstLevel){
//                if(!isElse) {
//                    if (s.trim().equals("}")) {
//                        while (!ifStack.pop().trim().endsWith("{")) {
//                        }
//                    } else if (s.trim().matches("\\}\\s*else\\s*\\{")) {
//                        while (!ifStack.pop().trim().endsWith("{")) {
//                        }
//                        if (!ifStack.empty()) {
//                            ifStack.push("else {");
//                        } else {//进入else block
//                            isElse = true;
//                            elseStack.push("{");
//                        }
//                    } else {
//                        ifStack.push(s);
//                    }
//
//                    if (!ifStack.isEmpty()) {
//                        ifBlock.add(s);
//                    } else {
//                        if(!isElse) {
//                            isFirstLevel = false;
//                        }
//                    }
//                }else{
//                    if (s.trim().equals("}")) {
//                        while (!elseStack.pop().trim().endsWith("{")) {
//                        }
//                    } else if (s.trim().matches("\\}\\s*else\\s*\\{")) {
//                        while (!elseStack.pop().trim().endsWith("{")) {
//                        }
//                        if (!elseStack.empty()) {
//                            elseStack.push("else {");
//                        }
//                    } else {
//                        elseStack.push(s);
//                    }
//
//                    if (!elseStack.isEmpty()) {
//                        elseBlock.add(s);
//                    } else {
//                        isFirstLevel = false;
//                    }
//                }
//                if(!isFirstLevel){
//                    endLine = i + 1;
//                    ifList.add(new IfInLoop(condition, startLine, endLine, ifBlock, elseBlock));
//                }
//                continue;
//            }
//
//            if(m_con.find() && !isFirstLevel){
//                condition = m_con.group(1);
//                startLine = i + 1;
//                isFirstLevel = true;
//                ifStack.push("{");
//            }
//
//        }
//        return ifList;
//    }




//    public List<IfInLoop> findInfoInLoopSkipMore(List<String> loopBlock, LoopStatement loop){
//        Map<Integer, Integer> innerMap = getInnerLoop(loop);
//        List<IfInLoop> ifList = new ArrayList<>();
//        boolean isFirstLevel = false;
//        boolean isElse = false;
//        boolean isInnerLoop = false;
//
//        String s_con = "\\bif\\s*\\((.*)\\)\\s*\\{";
//        Pattern p_con = Pattern.compile(s_con);
//        Matcher m_con;
//        List<String> ifBlock = new ArrayList<>();
//        List<String> elseBlock = new ArrayList<>();
//        Stack<String> ifStack = new Stack<>();
//        Stack<String> elseStack = new Stack();
//        int startLine = 0;
//        int endLine = 0;
//        String condition = "";
//        int innerLoopStartLine = 0;
//        int innerLoopEndLine = 0;
//
//        for(int i=0; i<loopBlock.size(); i++){
//
//            if(!innerMap.isEmpty() && innerMap.containsKey(i)){ //inner loop first line
//                innerLoopStartLine = i;
//                innerLoopEndLine = innerMap.get(i);
//                isInnerLoop = true;
//                continue;
//            }
//            if(isInnerLoop){
//                if(i > innerLoopStartLine && i < innerLoopEndLine){
//                    continue;
//                }
//                else if(i == innerLoopEndLine){
//                    isInnerLoop = false;
//                    continue;
//                }
//            }
//
//            String s = loopBlock.get(i);
//            m_con = p_con.matcher(s);
//            if(isFirstLevel){
//                if(!isElse) {
//                    if (s.trim().equals("}")) {
//                        while (!ifStack.pop().trim().endsWith("{")) {
//                        }
//                    } else if (s.trim().matches("\\}\\s*else\\s*\\{")) {
//                        while (!ifStack.pop().trim().endsWith("{")) {
//                        }
//                        if (!ifStack.empty()) {
//                            ifStack.push("else {");
//                        } else {//进入else block
//                            isElse = true;
//                            elseStack.push("{");
//                        }
//                    } else {
//                        ifStack.push(s);
//                    }
//
//                    if (!ifStack.isEmpty()) {
//                        ifBlock.add(s);
//                    } else {
//                        if(!isElse) {
//                            isFirstLevel = false;
//                        }
//                    }
//                }else{
//                    if (s.trim().equals("}")) {
//                        while (!elseStack.pop().trim().endsWith("{")) {
//                        }
//                    } else if (s.trim().matches("\\}\\s*else\\s*\\{")) {
//                        while (!elseStack.pop().trim().endsWith("{")) {
//                        }
//                        if (!elseStack.empty()) {
//                            elseStack.push("else {");
//                        }
//                    } else {
//                        elseStack.push(s);
//                    }
//
//                    if (!elseStack.isEmpty()) {
//                        elseBlock.add(s);
//                    } else {
//                        isFirstLevel = false;
//                    }
//                }
//                if(!isFirstLevel){
//                    endLine = i + 1;
//                    ifList.add(new IfInLoop(condition, startLine, endLine, ifBlock, elseBlock));
//                }
//                continue;
//            }
//
//            if(m_con.find() && !isFirstLevel){
//                condition = m_con.group(1);
//                startLine = i + 1;
//                isFirstLevel = true;
//                ifStack.push("{");
//            }
//
//        }
//        return ifList;
//    }

}
