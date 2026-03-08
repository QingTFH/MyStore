package element;

import java.math.BigInteger;

public class Term extends Element {

    public Term() {
        super();
        this.add(BigInteger.ONE, 0);//初始化为1*x^0
    }

    public Term mult(Element element) {
        Element ans = super.mult(element);
        return ans.toTerm();
    }
}
