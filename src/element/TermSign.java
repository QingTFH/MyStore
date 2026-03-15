package element;

import element.key.ExpMonomial;
import element.key.Monomial;
import element.key.VarMonomial;
import factory.ElementFactory;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TermSign {
    /*
    *   项的非系数部分
    *   x^7 * y^2 * exp(inner)
    */

    private final Map<Monomial, Number> map; // <单项式的key -> 次数>

    public TermSign(Map<Monomial, Number> map) {
        this.map = Collections.unmodifiableMap(new HashMap<>(map));//先拷贝再不可变化
    }

    /*-----静态方法-----*/

    public static TermSign mult(TermSign t1, TermSign t2) {
        //两项的x^m*x^n=x^m+n
        //两项的exp(A)*exp(B)=exp(A+B)
        Map<Monomial, Number> ansMap = new HashMap<>(t1.map);
        for (Monomial key : t2.map.keySet()) {
            if (key instanceof ExpMonomial) {
                //exp
                ExpMonomial found = findExpKey(ansMap); // 找ans是否有ExpKey，有则合并inner;
                if (found != null) { //ans中有exp
                    ansMap.remove(found); //删除原有的ExpKey
                    Expression newInner = Expression.add(
                            found.getInner(), ((ExpMonomial) key).getInner()
                    );  //newExpKey的inner
                    if (!newInner.isZero()) { // 不是e^0
                        ansMap.put(
                                ElementFactory.newExpKey(newInner), Number.ONE);
                        //指数函数处理后次数exponent都为1
                    }
                } else {
                    ansMap.put(key, Number.ONE);
                }
            } else if (key instanceof VarMonomial) {
                //var
                Number exponent = t2.map.get(key);
                ansMap.merge(key, exponent, Number::add);
            } else {
                throw new IllegalArgumentException("TermKey合并时出错");
            }
        }
        return ElementFactory.newTermKey(ansMap);
    }

    private static ExpMonomial findExpKey(Map<Monomial, Number> ansMap) {
        ExpMonomial found = null;
        for (Monomial k : ansMap.keySet()) {
            if (k instanceof ExpMonomial) {
                found = (ExpMonomial) k;
                break;
            }
        }
        return found;
    }

    /*-----接口-----*/

    @Override
    public boolean equals(Object o) { //TermKey能作为Map的key的条件
        if (this == o) { //同一个实例，或同为null
            return true;
        } else if (o == null || getClass() != o.getClass()) { //类不同
            return false;
        }
        return Objects.equals(map, ((TermSign) o).map);   //类相同
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    /*-----对外方法-----*/

    public Expression substitute(String varName, Expression arg) {
        //将该TermKey中的元 varName 替换成 arg
        Expression result = ElementFactory.newFactor(BigInteger.ONE).toExpression(); // 从1开始连乘

        for (Map.Entry<Monomial, Number> entry : map.entrySet()) {
            Monomial key = entry.getKey();
            Number power = entry.getValue();

            if (key instanceof VarMonomial) {
                //对多项式部分，x^n -> arg^n，
                if (((VarMonomial) key).getName().equals(varName)) {
                    result = Expression.mult(result, Expression.pow(arg, power));
                } else { // 其他变量名的VarKey，原样保留,result *= keep
                    Expression keep;
                    Factor f = ElementFactory.newFactor(((VarMonomial) key).getName());
                    keep = Expression.pow(f.toExpression(), power);
                    result = Expression.mult(result, keep);
                }
            } else if (key instanceof ExpMonomial) {
                // 对ExpKey的inner也需要substitute
                Expression newExpKey;
                Expression newInner = ((ExpMonomial) key).getInner().
                        substitute(varName, arg); //代入exp(inner)中的形参
                newExpKey = ElementFactory.newExpExpr(newInner);
                result = Expression.mult(result, newExpKey);
            } else {
                throw new IllegalArgumentException("TermKey代入时出错");
            }
        }
        return result;
    }

    public String toOutString() {
        if (map.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Map.Entry<Monomial, Number> entry : map.entrySet()) {
            if (!isFirst) {
                sb.append("*");
            }
            sb.append(entry.getKey().toOutString(entry.getValue()));
            isFirst = false;
        }
        return sb.toString();
    }

    public boolean isConst() {
        return this.map.isEmpty();
    }

}
