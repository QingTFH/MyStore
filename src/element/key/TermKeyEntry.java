package element.key;

public interface TermKeyEntry {
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    String toOutputString(int power);
}
