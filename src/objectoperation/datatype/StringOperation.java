package objectoperation.datatype;

import java.util.Random;

public class StringOperation {
	
	public static boolean isNumber(String str) {
		try {
			Integer.valueOf(str);
			return true;
		}
		catch(Exception e) {
			return false;
		}
	}
	
	public static String getRandomVarName() {
		String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		//String str = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	    Random random = new Random();
	    //int randomLength  = random.nextInt(1,6);
	    int randomLength = (int) (Math.random()*2 + 3);
	    StringBuffer sb = new StringBuffer();
	    for(int i = 0;i < randomLength;i++){
	    	int number = random.nextInt(52);	//
	    	//int number = random.nextInt(63);
	    	sb.append(str.charAt(number));
	    }
	    return sb.toString();
	}
}
