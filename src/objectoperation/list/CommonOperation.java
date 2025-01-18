package objectoperation.list;

import utity.AvailableVariable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CommonOperation {
    public static void copyStringList(List<String> newList, List<String> oldList){
        newList.clear();
        newList.addAll(oldList);
    }

    public static void copyAvaiableVarList(List<AvailableVariable> newList, List<AvailableVariable> oldList){
        newList.addAll(oldList);
    }

    //随机选取List中的某一个元素List
    public static List<List<String>> getRandomList(List<List<String>> llist, int total){
        List<List<String>> randomList = new ArrayList<List<String>>();
        int size = llist.size();
        Random random =  new Random();
        for(int i=0; i<total; i++) {
            int ran = random.nextInt(size);
            randomList.add(llist.get(ran));
        }
        return randomList;

    }

    public static void printList(List<String> list) {
        for(String l: list) {
            System.out.println(l);
        }
    }

    public static List<Integer> initialIntList(){
        List<Integer> list = new ArrayList<>();
        for(int i=0; i<1000; i++){
            list.add(i);
        }
        return list;
    }

    public static List<String> getListPart(List<String> list, int startLine, int endLine){
        List<String> partList = new ArrayList<String>();
        for(int i=1; i<=list.size(); i++) {
            if(i>=startLine && i<=endLine) {
                partList.add(list.get(i-1));
            }
        }
        return partList;
    }

    public static List<String> genInitialList(File file) {
        List<String> initialList = new ArrayList<String>();
        InputStreamReader ins;
        BufferedReader br = null;
        try {
            ins = new InputStreamReader(new FileInputStream(file));
            br = new BufferedReader(ins);
            String line = null;
            while((line = br.readLine()) != null) {
                initialList.add(line);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return initialList;
    }

}
