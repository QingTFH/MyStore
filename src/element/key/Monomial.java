package element.key;

import element.Number;

public interface Monomial {
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    String toOutString(Number power);
}
