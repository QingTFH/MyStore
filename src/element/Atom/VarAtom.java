package element.Atom;

import element.Expression;
import element.Number;
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
    public String toOutString(Number power) {
        if (power.equal(0)) {
            return "";
        }
        if (power.equal(1)) {
            return name;
        }
        return name + "^" + power.toOutString();
    }

    @Override
    public Expression derive(String var,Number exponent) { //其实应该返回Term类型，但是考虑到exp(inner)求导完是Expr类型，统一了
        if(name.equals(var)) { // dv(v^e)
            Number newPower = Number.add(exponent, ElementFactory.newNumber(-1));
            if (newPower.equal(0)) { // 直接返回系数，避免构造x^0
                return ElementFactory.newConstExpr(exponent);
            }
            return Expression.mult(
                    ElementFactory.newConstExpr(exponent),
                    ElementFactory.newVarExpr(name, newPower)); // e * var ^ (e-1)
        } else {
            return ElementFactory.newSpaceExpr();
        }
    }

    @Override
    public Expression toExpr(Number power) { // var^power
        return ElementFactory.newVarExpr(name,power);
    }
}
