package element.atom;

import element.Expression;

public abstract class TranscenAtom implements Atom {
    private final Expression inner;

    protected TranscenAtom(Expression inner) {
        this.inner = inner;
    }

    /*-----超越子的共通方法-----*/

    public Expression getInner() {
        return inner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TranscenAtom)) {
            return false;
        }
        TranscenAtom other = (TranscenAtom) o;
        return Expression.subtract(
                this.inner, other.inner).isZero();// 判断 this.inner - other.inner 是否恒等于0
    }

    @Override
    public int hashCode() {
        return inner.hashCode();
    }
}
