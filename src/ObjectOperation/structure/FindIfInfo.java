package ObjectOperation.structure;

import ObjectOperation.list.CommonOperation;
import utity.IfInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindIfInfo {
    public List<IfInfo> findAllIfInfo(List<String> block){
        List<IfInfo> ifList = new ArrayList<>();

        String s_func = ".*func_[0-9]+\\s*\\(.*\\)\\s*\\{";
        String s_if = "\\bif\\s*\\((.*)\\)\\s*\\{";
        String s_for = "\\bfor\\s*\\(.*\\)\\s*\\{";
        String s_else = "\\}\\s*else\\s*\\{";
        Pattern p_func = Pattern.compile(s_func);
        Pattern p_if = Pattern.compile(s_if);
        Pattern p_for = Pattern.compile(s_for);
        Pattern p_else = Pattern.compile(s_else);
        Matcher m_func;
        Matcher m_if;
        Matcher m_for;
        Matcher m_else;

        Stack<IfInfo> ifStack = new Stack<>();
        Stack<String> stringStack = new Stack<String>();

        for(int i=0; i < block.size(); i++){
            String s = block.get(i);
            m_func = p_func.matcher(s);
            m_if = p_if.matcher(s);
            m_for = p_for.matcher(s);
            m_else = p_else.matcher(s);

            if(m_if.find()){
                IfInfo newIf = new IfInfo(m_if.group(1), i + 1);
                newIf.setElseLine(-1);
                ifStack.push(newIf);
                ifList.add(newIf);
                stringStack.push(s);
            }
            else if(m_for.find() || m_func.find() || s.trim().equals("int main(void) {")){
                stringStack.push(s);
            }
            else if(m_else.find()){
                ifStack.peek().setElseLine(i + 1);
            }
            else if(s.trim().equals("}")){
                String popLine = stringStack.pop().trim();
                if(popLine.matches("\\s*if\\s*\\(.*\\)\\s*\\{.*")){
                    ifStack.pop().setEndLine(i + 1);
                }
            }
        }

        return getIfInfos(block, ifList);
    }

    public List<IfInfo> findOutermostIfInfo(List<String> block){
        List<IfInfo> ifList = new ArrayList<>();

        String s_func = ".*func_[0-9]+\\s*\\(.*\\)\\s*\\{";
        String s_if = "\\bif\\s*\\((.*)\\)\\s*\\{";
        String s_for = "\\bfor\\s*\\(.*\\)\\s*\\{";
        String s_else = "\\}\\s*else\\s*\\{";
        Pattern p_func = Pattern.compile(s_func);
        Pattern p_if = Pattern.compile(s_if);
        Pattern p_for = Pattern.compile(s_for);
        Pattern p_else = Pattern.compile(s_else);
        Matcher m_func;
        Matcher m_if;
        Matcher m_for;
        Matcher m_else;

        Stack<IfInfo> ifStack = new Stack<>();
        Stack<String> stringStack = new Stack<String>();

        for(int i=0; i<block.size(); i++){
            String s = block.get(i);
            m_func = p_func.matcher(s);
            m_if = p_if.matcher(s);
            m_for = p_for.matcher(s);
            m_else = p_else.matcher(s);

            if(m_if.find()){
                IfInfo newIf = new IfInfo(m_if.group(1), i + 1);
                newIf.setElseLine(-1);
                ifStack.push(newIf);
                stringStack.push(s);
            }
            else if(m_for.find() || m_func.find() || s.trim().equals("int main(void) {")){
                stringStack.push(s);
            }
            else if(m_else.find()){
                ifStack.peek().setElseLine(i + 1);
            }
            else if(s.trim().equals("}")){
                String popLine = stringStack.pop().trim();
                if(popLine.matches("\\bif\\s*\\(.*\\)\\s*\\{")){
                    IfInfo newIf = ifStack.pop();
                    if(!isMatchInStack("\\bif\\s*\\(.*\\)\\s*\\{", stringStack)) {
                        newIf.setEndLine(i + 1);
                        ifList.add(newIf);
                    } else{
                    }
                }
            }
        }

        return getIfInfos(block, ifList);
    }

    private List<IfInfo> getIfInfos(List<String> block, List<IfInfo> ifList) {
        for(IfInfo singleIf: ifList){
            List<String> ifBody;
            List<String> elseBody;
            if(singleIf.getElseLine() != -1) {
                ifBody = CommonOperation.getListPart(block, singleIf.getStartLine() + 1, singleIf.getElseLine() - 1);
                elseBody = CommonOperation.getListPart(block, singleIf.getElseLine() + 1, singleIf.getEndLine() - 1 );
            }
            else{
                ifBody = CommonOperation.getListPart(block, singleIf.getStartLine() + 1, singleIf.getEndLine() - 1);
                elseBody = new ArrayList<>();
            }
            singleIf.setIfBody(ifBody);
            singleIf.setElseBody(elseBody);
        }

        return ifList;
    }

    public boolean isMatchInStack(String regex, Stack<String> stringStack){
        Pattern pattern = Pattern.compile(regex);
        for (String line : stringStack) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }
}
