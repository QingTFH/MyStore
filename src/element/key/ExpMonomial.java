package element.key;

import element.Expression;
import element.Number;

public class ExpMonomial implements Monomial {
    private final Expression inner; // exp括号内化简后的Expr

    public ExpMonomial(Expression inner) {
        this.inner = inner;
    }

    public Expression getInner() {
        return inner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExpMonomial)) {
            return false;
        }
        ExpMonomial other = (ExpMonomial) o;
        // 判断 this.inner - other.inner 是否恒等于0
        return Expression.subtract(this.inner, other.inner).isZero();
    }

    @Override
    public int hashCode() {
        return inner.hashCode();
    }

    @Override
    public String toOutString(Number power) {
        // power对ExpKey固定为1，幂次已乘入inner
        // 优化点2：括号冗余
        return "exp((" + inner.toOutString() + "))";
    }
}
