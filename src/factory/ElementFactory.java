package factory;

import element.Expression;
import element.Factor;
import element.Number;
import element.TermKey;
import element.key.ExpKey;
import element.key.TermKeyEntry;
import element.key.VarKey;

import java.math.BigInteger;
import java.util.Map;

public class ElementFactory {

    /*-----Factor类-----*/

    public static Factor newFactor(BigInteger coe) { //常元,只有coe
        return newFactor(newNumber(coe));
    }

    public static Factor newFactor(int coe) {
        return newFactor(BigInteger.valueOf(coe));
    }

    public static Factor newFactor(Number coe) { //常元,只有coe
        return new Factor(coe, null);
    }

    public static Factor newFactor(String varName) { //变元,只有varName
        return new Factor(newNumber(BigInteger.ONE), varName);
    }

    /*-----Expression类-----*/

    public static Expression newExpr() {
        return new Expression();
    }

    public static Expression newExpExpr(Expression inner) { //封装inner成为exp(inner)
        Expression expr = new Expression();
        if (inner.isZero()) { // e^0 = 1
            return newFactor(BigInteger.ONE).toExpression();
        }
        expr.addExpFactor(inner); // e^inner
        return expr;
    }

    /*-----TermKey类-----*/

    public static TermKey newTermKey(Map<TermKeyEntry, Number> map) {
        return new TermKey(map);
    }

    public static TermKeyEntry newExpKey(Expression inner) {
        if (inner.isZero()) {
            throw new IllegalArgumentException("创建ExpKey时inner = 0，请降级为VarExp");
        }
        return new ExpKey(inner);
    }

    public static TermKeyEntry newVarKey(String varName) {
        return new VarKey(varName);
    }

    /*-----Number-----*/

    public static Number newNumber(BigInteger num) {
        return new Number(num);
    }

    public static Number newNumber(String num) {
        if (!num.matches("^[+-]?[0-9]+$")) {
            throw new IllegalArgumentException("生成Number时，初始化使用错误的String:" + num);
        }
        return new Number(new BigInteger(num));
    }

    public static Number newNumber(int num) {
        return new Number(BigInteger.valueOf(num));
    }

}
