package element.key;

import element.Number;

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
        if (this == o) {
            return true;
        }
        if (!(o instanceof VarKey)) {
            return false;
        }
        VarKey other = (VarKey) o;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toOutString(Number power) {
        if (power.equal(0)) {
            return "";
        }
        if (power.equal(1)) {
            return name;
        }
        return name + "^" + power.toOutString();
    }
}
