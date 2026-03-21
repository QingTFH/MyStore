package element;

import java.math.BigInteger;
import java.util.Objects;

public class mNumber {
    private final BigInteger num;

    mNumber(BigInteger num) {
        this.num = num;
    }

    public int toInt() {
        return num.intValueExact(); //爆int自动报错
    }

    /*-----常用值-----*/

    public static final mNumber ZERO = new mNumber(BigInteger.ZERO);
    public static final mNumber ONE = new mNumber(BigInteger.ONE);

    /*-----对外方法-----*/

    public mNumber negate() {
        return ElementFactory.newNumber(this.num.negate());
    }

    public boolean equal(int number) {
        return this.num.equals(BigInteger.valueOf(number));
    }

    public mNumber abs() {
        return new mNumber(this.num.abs());
    }

    public String toOutString() {
        return this.num.toString();
    }

    public int compareTo(mNumber mNumber) {
        return this.num.compareTo(mNumber.num);
    }

    public boolean gt(mNumber mNumber) { //大于
        return this.compareTo(mNumber) > 0;
    }

    public Expression toExpr() {
        return ElementFactory.newConstExpr(this);
    }

    @Override
    public boolean equals(Object o) { //TermKey能作为Map的key的条件
        if (this == o) { //同一个实例，或同为null
            return true;
        } else if (o == null || getClass() != o.getClass()) { //类不同
            return false;
        }
        return this.num.equals(((mNumber) o).num);   //类相同
    }

    @Override
    public int hashCode() {
        return Objects.hash(num);
    }

    /*-----静态方法-----*/

    public static mNumber add(mNumber num1, mNumber num2) {
        return ElementFactory.newNumber(num1.num.add(num2.num));
    }

    public static mNumber mult(mNumber n1, mNumber n2) {
        return ElementFactory.newNumber(n1.num.multiply(n2.num));
    }

}
