package element;

import element.key.Monomial;
import factory.ElementFactory;

import java.util.HashMap;
import java.util.Map;

public class Expression extends Element {
    /*  3 * x^7 * y^2 + 2 * ooNiuBi ^ 5
     *  表达式：若干个Term的和，
     *  存若干个项，如果项内名称和幂次都相同(即TermKey相同)则合并
     *  统一使用Expression做计算，Factor只作提取因子用
     */
    private final Map<TermSign, Number> keyMap; // <项签名TermSign -> 该项系数>

    public Expression() {
        this.keyMap = new HashMap<>();
    }

    /*---------对外方法----------*/

    public String toOutString() { //Expr -> String
        StringBuilder sb = new StringBuilder();
        for (TermSign key : keyMap.keySet()) { //该项的元
            sb.append(TermToString(key));
        }
        if (sb.length() == 0) { //sb为空
            sb.append("0");
        } else if (sb.charAt(0) == '+') {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    private String TermToString(TermSign key) { //项 -> String
        StringBuilder sb = new StringBuilder();
        Number coe = keyMap.get(key); //该项的系数
        Number coeAbs = coe.abs();
        //打印符号
        if (!coe.gt(Number.ZERO)) { //coe<0
            sb.append("-");
        } else { //coe>0
            sb.append("+");
        }

        //打印系数(绝对值):常数 | 变元且系数绝对值不为1
        if (key.isConst() ||
                (!key.isConst() && !coeAbs.equal(1))) {
            sb.append(coe.abs().toOutString());
        }

        //打印元:
        if (!key.isConst()) {
            if (!coeAbs.equal(1)) { //系数不为1
                sb.append("*");
            }
            sb.append(key.toOutString());
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return keyMap.hashCode();
    }

    @Override
    public Expression toExpression() {
        return this;
    }

    public void addFactor(Factor factor) { //Factor转Expr的入口
        Number coe = factor.getCoe(); //factor的系数
        if (!coe.equal(Number.ZERO)) {
            Map<Monomial, Number> newMap = new HashMap<>(); //factor对应的TermKey的map
            if (!factor.isConst()) { //factor是变元
                newMap.put(ElementFactory.newVarKey(factor.getVarName()), Number.ONE);
            }
            TermSign newKey = ElementFactory.newTermKey(newMap); //factor的TermKey
            this.mergeTerm(newKey, coe);
        }
    }

    public void addExpFactor(Expression inner) {
        Map<Monomial, Number> newMap = new HashMap<>();
        newMap.put(ElementFactory.newExpKey(inner), Number.ONE);
        TermSign newKey = ElementFactory.newTermKey(newMap);
        mergeTerm(newKey, Number.ONE);
    }

    public Number toNumber() {
        if (keyMap.isEmpty()) {
            return Number.ZERO;
        }
        if (keyMap.size() != 1) {
            throw new RuntimeException("表达式中项不唯一，无法转换为Number");
        }
        Map.Entry<TermSign, Number> entry = keyMap.entrySet().iterator().next();
        if (!entry.getKey().isConst()) {
            throw new RuntimeException("表达式是变元项，无法转换为Number");
        }
        return entry.getValue();
    }

    public Expression substitute(String varName, Expression arg) {
        //将该Expr的元varName 由 arg 代入
        Expression result = new Expression();
        for (Map.Entry<TermSign, Number> entry : keyMap.entrySet()) {
            TermSign key = entry.getKey();
            Number coe = entry.getValue();
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

    public boolean isFactor() {
        // Expr是Factor -> Expr是：数字 | 幂函数 | exp
        if (keyMap.size() == 1) {
            TermSign key = keyMap.keySet().iterator().next(); //多项式中唯一的项签名
            return key.isFactor() || key.isConst(); // 这个唯一的项是因子或常数
        }
        return false;
    }

    /*---------内部工具----------*/

    private void mergeTerm(TermSign key, Number coe) { //合并<key -> coe>
        if (!coe.equal(0)) {
            this.keyMap.merge(key, coe, Number::add);
            if (this.keyMap.get(key).equal(0)) {
                this.keyMap.remove(key);
            }
        }
    }

    /*---------静态方法----------*/

    public static Expression add(Expression e1, Expression e2) {
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

    public static Expression mult(Expression m1, Expression m2) {
        Expression ans = new Expression();
        for (TermSign key1 : m1.keyMap.keySet()) { //key1 = m1的项
            Number coe1 = m1.keyMap.get(key1); //coe1 = m1的项的系数
            for (TermSign key2 : m2.keyMap.keySet()) { //key2 = m2的项
                Number coe2 = m2.keyMap.get(key2); //coe2 = m2的项的系数
                Number ansCoe = Number.mult(coe1, coe2); //ansCoe = coe1 * coe2
                TermSign ansKey = TermSign.mult(key1, key2); //ansKey = key1 * key2
                //ans += ansCoe * ansKey
                ans.mergeTerm(ansKey, ansCoe);
            }
        }
        return ans;
    }

    public static Expression pow(Expression base, Number exp) {
        Expression ans = new Expression();
        Map<Monomial, Number> newMap = new HashMap<>();
        ans.mergeTerm(new TermSign(newMap), Number.ONE); // ans = 1
        if (exp.equal(0)) {
            return ans;
        }
        for (Number i = Number.ZERO; i.compareTo(exp) < 0; i = Number.add(i, Number.ONE)) {
            ans = Expression.mult(ans, base);
        }
        return ans;
    }

}
