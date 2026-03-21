package element.atom;

import element.Expression;
import element.MyNumber;
import element.ElementFactory;

public class ExpAtom extends TranscenAtom {

    public ExpAtom(Expression inner) {
        super(inner);
    }

    @Override
    public String toOutString(MyNumber power) {
        // power对ExpKey固定为1，幂次已乘入inner
        if (getInner().isFactor()) {
            return "exp(" + getInner().toOutString() + ")";
        }
        return "exp((" + getInner().toOutString() + "))";
    }

    @Override
    public Expression derive(String var, MyNumber power) {
        // dv(exp(inner)^power) = dv(exp(inner*power))
        // -> exp(power*inner) * dv(power*inner) -> p * exp(p*i) * dv(i)
        Expression expr = toExpr(power); // exp(p*i)
        expr = Expression.mult(Expression.mult(expr, power.toExpr()),
                getInner().derive(var)); //
        return expr;
    }

    @Override
    public Expression toExpr(MyNumber power) { // exp(inner*power)
        return ElementFactory.newTransExpr(Expression.mult(getInner(), power.toExpr()));
    }

}
