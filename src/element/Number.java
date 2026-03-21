package element;

import java.math.BigInteger;
import java.util.Objects;

public class Number {
    private final BigInteger num;

    Number(BigInteger num) {
        this.num = num;
    }

    public BigInteger getNum() {
        return num;
    }

    public int toInt() {
        return num.intValueExact(); //爆int自动报错
    }

    /*-----常用值-----*/

    public static final Number ZERO = new Number(BigInteger.ZERO);
    public static final Number ONE = new Number(BigInteger.ONE);

    /*-----对外方法-----*/

    public Number negate() {
        return ElementFactory.newNumber(this.num.negate());
    }

    public boolean equal(Number number) {
        return this.num.equals(number.num);
    }

    public boolean equal(int number) {
        return this.num.equals(BigInteger.valueOf(number));
    }

    public Number abs() {
        return new Number(this.num.abs());
    }

    public String toOutString() {
        return this.num.toString();
    }

    public int compareTo(Number number) {
        return this.num.compareTo(number.num);
    }

    public boolean gt(Number number) { //大于
        return this.compareTo(number) > 0;
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
        return this.num.equals(((Number) o).num);   //类相同
    }

    @Override
    public int hashCode() {
        return Objects.hash(num);
    }

    /*-----静态方法-----*/

    public static Number add(Number num1, Number num2) {
        return ElementFactory.newNumber(num1.num.add(num2.num));
    }

    public static Number mult(Number n1, Number n2) {
        return ElementFactory.newNumber(n1.num.multiply(n2.num));
    }

}
