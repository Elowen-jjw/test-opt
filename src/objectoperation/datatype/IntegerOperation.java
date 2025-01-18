package objectoperation.datatype;

import java.util.ArrayList;
import java.util.List;

public class IntegerOperation {

	public static void main(String args[]) {
		int[] ii = getTwoNums(1093476228);
		System.out.println(ii[0]);
		System.out.println(ii[1]);
	}
	//判断一个数是否是素数
	public static boolean prime(int n){
		if(n < 2){
			return false;
		}
		if(n == 2 || n == 3){
			return true;
		}else{
			int a = (int)Math.sqrt(n);
			for(int i = 2; i <= a ; i++){
				if(n % i == 0){
					return false;
				}
			}
			return true;
		}		
	}
	//找出(n/2~n)之内因数最多的合数
	public static int findComNum(int n) {
		int comNum = n-1;
		int maxCount = 0;
		for(int i=(n-1); i > (n/2); i--) {
			if(prime(i)) continue;
			else {
				List<Integer> factor = new ArrayList<Integer>();
				for(int j = 2; j <= (i/2); j++) {
					if(i % j == 0 ) {
						factor.add(j);
					}
				}
				if(factor.size() > maxCount) {
					maxCount = factor.size();
					comNum = i;
				}
			}
		}
		return comNum;
	}
	
	//d的i次根
	public static int sqrt(double d, double i) {
		i = 1/i;
		return (int)Math.pow(d, i);
	}
	
	//判断一个数是不是2^n n>0
	public static boolean isTwoPow(int n) {
		if((n > 0) && ((n & (n -1)) == 0)) {
			return true;
		}
		return false;
	}
	
	//如果一个数是2^n, 返回n
	public static int getTwoExp(int n) {
		int exp = 0;
		while(true){
			n = n / 2;
			exp++;
			if(n == 1){
				return exp;
			}
		}
	}
	
	//如果不是2^n,则寻找最近的num满足2^n 上下找
	public static int[] getTwoNums(int n) {
		int[] nums = new int[2];
		int count1 = 1;
		int count2 = 1;
		int tempN = n;
		while(!isTwoPow(--n)) {
			count1++;
		}
		while(!isTwoPow(++tempN)) {
			count2++;
		}
		if(count1 <= count2 || count2 < 0) {
			nums[1] = count1;
			nums[0] = getTwoExp(n);
		}
		else {
			nums[1] = -count2;
			nums[0] = getTwoExp(tempN);
		}
		return nums;
	}
	
}
