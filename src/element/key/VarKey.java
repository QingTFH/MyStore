package element.key;

public class VarKey implements TermKeyEntry {
    private final String name;

    public VarKey(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VarKey)) return false;
        VarKey other = (VarKey) o;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toOutString(int power) {
        if (power == 0) return "";
        if (power == 1) return "x";
        return "x^" + power;
    }
}
