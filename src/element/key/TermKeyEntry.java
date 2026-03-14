package element.key;

public interface TermKeyEntry {
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    String toOutString(int power);
}
