package element;

import java.math.BigInteger;

public class Factor extends Element {
    public Factor(BigInteger coe, int exp) {
        this.add(coe, exp);
    }

    public int toInt() {
        return (this.eles().getOrDefault(0, BigInteger.ZERO).intValueExact());
    }
}
