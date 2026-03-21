package element.atom;

import element.Expression;
import element.MyNumber;

public interface Atom {
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    String toOutString(MyNumber power);

    Expression derive(String var, MyNumber exponent);

    Expression toExpr(MyNumber power);
}
