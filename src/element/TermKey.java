package element;

import element.key.ExpKey;
import element.key.TermKeyEntry;
import element.key.VarKey;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TermKey { /*
    可以认为是项的变元，记录变量名及其幂次
    x^7 * y^2
*/
    private final Map<TermKeyEntry,Integer> map; // <变量名 -> 次数>

    public TermKey(Map<TermKeyEntry,Integer> map) {
        this.map = Collections.unmodifiableMap(new HashMap<>(map));//先拷贝再不可变化
    }

    public static TermKey mult(TermKey t1, TermKey t2) {
        //两项的x^m*x^n=x^m+n
        //两项的exp(A)*exp(B)=exp(A+B)
        Map<TermKeyEntry,Integer> ansMap = new HashMap<>(t1.map);
        for (TermKeyEntry key : t2.map.keySet()) {
            if (key instanceof ExpKey) { // 找ans是否有ExpKey，有则合并inner
                ExpKey found = findExpKey(ansMap);
                if (found != null) { //ans中有exp
                    ansMap.remove(found); //删除原有的ExpKey
                    Expression newInner = Expression.add(
                            found.getInner(), ((ExpKey) key).getInner()
                    );
                    ansMap.put(new ExpKey(newInner), 1); //指数函数处理后次数exponent都为1
                } else {
                    ansMap.put(key, 1);
                }
            } else if(key instanceof VarKey) {
                int exponent = t2.map.get(key);
                ansMap.merge(key,exponent,Integer::sum);
            } else {
                throw new IllegalArgumentException("TermKey合并时出错");
            }
        }
        return new TermKey(ansMap);
    }

    private static ExpKey findExpKey(Map<TermKeyEntry,Integer> ansMap) {
        ExpKey found = null;
        for (TermKeyEntry k : ansMap.keySet()) {
            if (k instanceof ExpKey) {
                found = (ExpKey) k;
                break;
            }
        }
        return found;
    }

    public String toString() {
        if (map.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int flag = 0;
        for (Map.Entry<TermKeyEntry, Integer> entry : map.entrySet()) {
            if (flag != 0) {
                sb.append("*");
            }
            sb.append(entry.getKey().toOutputString(entry.getValue()));
            flag++;
        }
        return sb.toString();
    }

    public boolean isConst() {
        return this.map.isEmpty();
    }

    /*-----接口-----*/

    @Override
    public boolean equals(Object o) { //Termkey能作为Map的key的条件
        if (this == o) { //同一个实例，或同为null
            return true;
        } else if (o == null || getClass() != o.getClass()) { //类不同
            return false;
        }
        return Objects.equals(map,((TermKey) o).map);   //类相同
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    public Expression substitute(String varName, Expression arg) {
        //将该TermKey中的元 varName 替换成 arg
        Expression result = ElementFactory.newFactor(BigInteger.ONE).toExpression(); // 从1开始连乘

        for (Map.Entry<TermKeyEntry, Integer> entry : map.entrySet()) {
            TermKeyEntry key = entry.getKey();
            int power = entry.getValue();

            if (key instanceof VarKey) {
                //对多项式部分，x^n -> arg^n，
                if(((VarKey) key).getName().equals(varName)) {
                    result = Expression.mult(result, Expression.pow(arg, power));
                } else { // 其他变量名的VarKey，原样保留,result *= keep
                    Expression keep;
                    Factor f = ElementFactory.newFactor(((VarKey) key).getName());
                    keep = Expression.pow(f.toExpression(), power);
                    result = Expression.mult(result, keep);
                }
            } else if (key instanceof ExpKey){
                // 对ExpKey的inner也需要substitute
                Expression newExpKey;
                Expression newInner = ((ExpKey) key).getInner().substitute(varName, arg); //递归处理Exp中的Expr
                newExpKey = ElementFactory.newExpExpr(newInner);
                result = Expression.mult(result, newExpKey);
            } else {
                throw new IllegalArgumentException("TermKey代入时出错");
            }
        }
        return result;
    }
}
