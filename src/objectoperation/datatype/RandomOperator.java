package objectoperation.datatype;

import java.util.*;

public class RandomOperator {
    public List<Integer> operatorIndexComList = new ArrayList<>();
    public List<List<Integer>> allCom = new ArrayList<>();

    public void combination(List<Integer> nums) {
        if(null == nums || nums.isEmpty()) {
            allCom.add(new ArrayList<>(operatorIndexComList));
            return;
        }
        for(Integer n:nums) {
            operatorIndexComList.add(n);
            List<Integer> newNums = new ArrayList<Integer>();
            for(Integer nn: nums){
                if(!Objects.equals(nn, n))
                    newNums.add(nn);
            }
            combination(newNums);
            operatorIndexComList.remove(operatorIndexComList.size() - 1);
        }
    }
}
