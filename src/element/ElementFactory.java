package element;

import element.key.ExpKey;
import element.key.TermKeyEntry;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

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

    public static Expression newExpExpr(Expression inner) { //封装inner成为exp(inner)
        Expression expr = new Expression();
        expr.addExpFactor(inner);
        return expr;
    }
}
