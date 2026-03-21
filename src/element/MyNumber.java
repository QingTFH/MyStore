package element;

import java.math.BigInteger;
import java.util.Objects;

public class MyNumber {
    private final BigInteger num;

    MyNumber(BigInteger num) {
        this.num = num;
    }

    public int toInt() {
        return num.intValueExact(); //爆int自动报错
    }

    /*-----常用值-----*/

    public static final MyNumber ZERO = new MyNumber(BigInteger.ZERO);
    public static final MyNumber ONE = new MyNumber(BigInteger.ONE);

    /*-----对外方法-----*/

    public MyNumber negate() {
        return ElementFactory.newNumber(this.num.negate());
    }

    public boolean equal(int number) {
        return this.num.equals(BigInteger.valueOf(number));
    }

    public MyNumber abs() {
        return new MyNumber(this.num.abs());
    }

    public String toOutString() {
        return this.num.toString();
    }

    public int compareTo(MyNumber myNumber) {
        return this.num.compareTo(myNumber.num);
    }

    public boolean gt(MyNumber myNumber) { //大于
        return this.compareTo(myNumber) > 0;
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
        return this.num.equals(((MyNumber) o).num);   //类相同
    }

    @Override
    public int hashCode() {
        return Objects.hash(num);
    }

    /*-----静态方法-----*/

    public static MyNumber add(MyNumber num1, MyNumber num2) {
        return ElementFactory.newNumber(num1.num.add(num2.num));
    }

    public static MyNumber mult(MyNumber n1, MyNumber n2) {
        return ElementFactory.newNumber(n1.num.multiply(n2.num));
    }

}
