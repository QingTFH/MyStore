package element;

import factory.ElementFactory;

import java.math.BigInteger;

public class Factor extends Element { /*
    x | y | ooNiuBi | +3 | -2 | 003
    Factor存数值、变元名称()
*/
    private final BigInteger coe; //系数
    private final String varName; //变量名

    public Factor(BigInteger coe, String varName) {
        this.coe = coe;
        this.varName = varName;
    }

    public BigInteger getCoe() {
        return this.coe;
    }

    public String getVarName() {
        return this.varName;
    }

    public boolean isConst() {
        return this.varName == null;
    }

    public int toInt() {
        return (this.coe.intValueExact());
    }

    public Expression toExpression() {
        Expression expr = ElementFactory.newExpr();
        expr.addFactor(this);
        return expr;
    }
}
