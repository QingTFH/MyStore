package element;

import java.math.BigInteger;

public class ElementFactory {
    public static Expr newExpr() {
        return new Expr();
    }

    public static Term newTerm() {
        return new Term();
    }

    public static Factor newFactor(String m, int n) {
        return new Factor(new BigInteger(m), n);
    }

}
