package mutate.minmax;

import java.math.BigInteger;

public class MinMaxResult {
    public BigInteger max;
    public BigInteger min;

    public MinMaxResult(){}

    public MinMaxResult(BigInteger max, BigInteger min) {
        this.max = max;
        this.min = min;
    }

    public boolean isEmpty() {
        return (max == null && min == null);
    }
}