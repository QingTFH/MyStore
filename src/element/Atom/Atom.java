package element.Atom;

import element.Expression;
import element.Number;

public interface Atom {
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    String toOutString(Number power);

    Expression derive(String var,Number exponent);

    Expression toExpr(Number power);
}
