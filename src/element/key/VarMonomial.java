package element.key;

import element.Number;

public class VarMonomial implements Monomial {
    private final String name;

    public VarMonomial(String name) {
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
        if (!(o instanceof VarMonomial)) {
            return false;
        }
        VarMonomial other = (VarMonomial) o;
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
