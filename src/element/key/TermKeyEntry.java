package element.key;

import element.Number;

public interface TermKeyEntry {
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    String toOutString(Number power);
}
