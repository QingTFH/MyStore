package element;

import java.math.BigInteger;

public class ElementFactory {

    public static Expression newExpr() {
        return new Expression();
    }

    public static Factor newFactor(BigInteger coe) { //常元,只有coe
        return new Factor(coe,null);
    }

    public static Factor newFactor(String varName) { //变元,只有varName
        return new Factor(BigInteger.ONE, varName);
    }

}
