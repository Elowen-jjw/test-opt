package mutations.unrolling;

import java.util.List;

public class Number {
	private int number;
	private List<Integer> factor;
	private int remainder;
	
	public Number(int number, List<Integer> factor, int remainder) {
		this.number = number;
		this.factor = factor;
		this.remainder = remainder;
	}
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public List<Integer> getFactor() {
		return factor;
	}
	public void setFactor(List<Integer> factor) {
		this.factor = factor;
	}
	public int getRemainder() {
		return remainder;
	}
	public void setRemainder(int remainder) {
		this.remainder = remainder;
	}
}
