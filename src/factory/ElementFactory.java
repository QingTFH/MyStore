package factory;

import element.Expression;
import element.Factor;
import element.TermKey;
import element.key.ExpKey;
import element.key.TermKeyEntry;
import element.key.VarKey;

import java.math.BigInteger;
import java.util.Map;

public class ElementFactory {

    /*-----Factor类-----*/

    public static Factor newFactor(BigInteger coe) { //常元,只有coe
        return new Factor(coe,null);
    }

    public static Factor newFactor(String varName) { //变元,只有varName
        return new Factor(BigInteger.ONE, varName);
    }

    /*-----Expression类-----*/

    public static Expression newExpr() {
        return new Expression();
    }

    public static Expression newExpExpr(Expression inner) { //封装inner成为exp(inner)
        Expression expr = new Expression();
        if(inner.isZero()) { // e^0 = 1
            return newFactor(BigInteger.ONE).toExpression();
        }
        expr.addExpFactor(inner); // e^inner
        return expr;
    }

    /*-----TermKey类-----*/

    public static TermKey newTermKey(Map<TermKeyEntry,Integer> map) {
        return new TermKey(map);
    }

    public static TermKeyEntry newExpKey(Expression inner) {
        if(inner.isZero()) {
            throw new IllegalArgumentException("创建ExpKey时inner = 0，请降级为VarExp");
        }
        return new ExpKey(inner);
    }

    public static TermKeyEntry newVarKey(String varName) {
        return new VarKey(varName);
    }
}
