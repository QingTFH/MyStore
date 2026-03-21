package element.Atom;

import element.Expression;
import element.mNumber;

public interface Atom {
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    String toOutString(mNumber power);

    Expression derive(String var, mNumber exponent);

    Expression toExpr(mNumber power);
}
