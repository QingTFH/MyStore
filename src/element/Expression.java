package element;

import element.key.ExpKey;
import element.key.TermKeyEntry;
import element.key.VarKey;
import io.Output;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class Expression extends Element { /*
    3 * x^7 * y^2 + 2 * ooNiuBi ^ 5
    表达式：若干个Term的和，
    存若干个项，如果项内名称和幂次都相同(即TermKey相同)则合并

    统一使用Expression做计算，Factor只作提取因子用
*/
    private final Map<TermKey, BigInteger> keyMap; //< 项特征 -> 系数 >

    public Expression() {
        this.keyMap = new HashMap<>();
    }

    public void print() { //打印
        Output.printKeyMap(this.keyMap);
    }

    @Override
    public Expression toExpression() {
        return this;
    }

    public void addFactor(Factor factor) { //Factor转Expr的入口
        BigInteger coe = factor.getCoe(); //factor的系数
        if (!coe.equals(BigInteger.ZERO)) {
            Map<TermKeyEntry,Integer> newMap = new HashMap<>(); //factor对应的TermKey的map
            if (!factor.isConst()) { //factor是变元
                newMap.put(new VarKey(factor.getVarName()),1);
            }
            TermKey newKey = new TermKey(newMap); //factor的TermKey
            this.mergeTerm(newKey,coe);
        }
    }

    public void addExpFactor(Expression inner) {
        Map<TermKeyEntry, Integer> newMap = new HashMap<>();
        newMap.put(new ExpKey(inner), 1);
        TermKey newKey = new TermKey(newMap);
        mergeTerm(newKey, BigInteger.ONE);
    }

    private void mergeTerm(TermKey key, BigInteger coe) { //合并key -> coe
        if (!coe.equals(BigInteger.ZERO)) {
            this.keyMap.merge(key,coe,BigInteger::add);
            if (this.keyMap.get(key).equals(BigInteger.ZERO)) {
                this.keyMap.remove(key);
            }
        }
    }

    public int toInt() {
        if (keyMap.isEmpty()) {
            return 0;
        }
        if (keyMap.size() != 1) {
            this.print();
            throw new RuntimeException("表达式中项不唯一，无法转换为int");
        }
        Map.Entry<TermKey, BigInteger> entry = keyMap.entrySet().iterator().next();
        if (!entry.getKey().isConst()) {
            this.print();
            throw new RuntimeException("表达式是变元项，无法转换为int");
        }
        return entry.getValue().intValueExact();
    }

    public Expression substitute(String varName, Expression arg) {
        //将该Expr的元varName 由 arg 代入
        Expression result = new Expression();
        for (Map.Entry<TermKey, BigInteger> entry : keyMap.entrySet()) {
            TermKey key = entry.getKey();
            BigInteger coe = entry.getValue();
            // 对这一项的TermKey进行代入
            Expression term = key.substitute(varName, arg);
            // 乘以系数
            Expression coeExpr = ElementFactory.newFactor(coe).toExpression();
            term = Expression.mult(term, coeExpr);
            result = Expression.add(result, term);
        }
        return result;
    }

    public boolean isZero() {
        //Expr是空的
        return keyMap.isEmpty();
    }

    @Override
    public int hashCode() {
        return keyMap.hashCode();
    }

    /*---------静态方法----------*/

    public static Expression add(Expression e1,Expression e2) {
        Expression ans = new Expression();
        e1.keyMap.forEach(ans::mergeTerm);
        e2.keyMap.forEach(ans::mergeTerm);
        return ans;
    }

    public static Expression subtract(Expression e1, Expression e2) {
        //e1 - e2
        Expression ans = new Expression();
        e1.keyMap.forEach(ans::mergeTerm);
        e2.keyMap.forEach((key, coe) -> ans.mergeTerm(key, coe.negate()));
        return ans;
    }

    public static Expression mult(Expression m1,Expression m2) {
        Expression ans = new Expression();
        for (TermKey key1 : m1.keyMap.keySet()) { //key1 = m1的项
            BigInteger coe1 = m1.keyMap.get(key1); //coe1 = m1的项的系数
            for (TermKey key2 : m2.keyMap.keySet()) { //key2 = m2的项
                BigInteger coe2 = m2.keyMap.get(key2); //coe2 = m2的项的系数
                BigInteger ansCoe = coe1.multiply(coe2); //ansCoe = coe1 * coe2
                TermKey ansKey = TermKey.mult(key1,key2); //ansKey = key1 * key2
                //ans += ansCoe * ansKey
                ans.mergeTerm(ansKey,ansCoe);
            }
        }
        return ans;
    }

    public static Expression pow(Expression base, int exp) {
        Expression ans = new Expression();
        Map<TermKeyEntry, Integer> newMap = new HashMap<>();
        if (exp == 0) {
            ans.mergeTerm(new TermKey(newMap), BigInteger.ONE); // 返回1
            return ans;
        }
        ans.mergeTerm(new TermKey(newMap), BigInteger.ONE); // ans = 1
        for (int i = 0; i < exp; i++) {
            ans = Expression.mult(ans, base);
        }
        return ans;
    }

}
