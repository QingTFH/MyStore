package element.atom;

import element.Expression;
import element.MyNumber;
import element.ElementFactory;

public class VarAtom implements Atom {
    private final String name;

    public VarAtom(String name) {
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
        if (!(o instanceof VarAtom)) {
            return false;
        }
        VarAtom other = (VarAtom) o;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toOutString(MyNumber power) {
        if (power.equal(0)) {
            return "";
        }
        if (power.equal(1)) {
            return name;
        }
        return name + "^" + power.toOutString();
    }

    @Override
    public Expression derive(String var, MyNumber exponent) {
        if (name.equals(var)) { // dv(v^e)
            MyNumber newPower = MyNumber.add(exponent, ElementFactory.newNumber(-1));
            if (newPower.equal(0)) { // 直接返回系数，避免构造x^0
                return ElementFactory.newConstExpr(exponent);
            }
            return Expression.mult(
                    ElementFactory.newConstExpr(exponent),
                    ElementFactory.newVarExpr(name, newPower)); // e * var ^ (e-1)
        } else {
            return ElementFactory.newConstExpr(ElementFactory.newNumber(0));
        }
    }

    @Override
    public Expression toExpr(MyNumber power) { // var^power
        return ElementFactory.newVarExpr(name, power);
    }
}
