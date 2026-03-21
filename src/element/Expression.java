package element;

import java.util.HashMap;
import java.util.Map;

public class Expression extends Element {
    /*  3 * x^7 * y^2 + 2 * ooNiuBi ^ 5
     *  表达式：若干个Term的和，
     *  存若干个项，如果项内名称和幂次都相同(即TermKey相同)则合并
     *  统一使用Expression做计算，Factor只作提取因子用
     */
    private final Map<TermSign, mNumber> keyMap; // <项签名TermSign -> 该项系数>

    Expression() {
        this.keyMap = new HashMap<>();
    }

    Expression(TermSign term, mNumber coe) {
        this.keyMap = new HashMap<>();
        keyMap.put(term, coe);
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

    @Override
    public int hashCode() {
        return keyMap.hashCode();
    }

    public Expression substitute(String varName, Expression arg) {
        //将该Expr的元varName 由 arg 代入
        Expression result = new Expression();
        for (Map.Entry<TermSign, mNumber> entry : keyMap.entrySet()) {
            Expression term = entry.getKey().
                    substitute(varName, arg); // 对这一项的TermSign进行代入
            term = Expression.mult(term,
                    ElementFactory.newConstExpr(
                            entry.getValue())); // 乘以系数
            result = Expression.add(result, term);
        }
        return result;
    }

    public Expression derive(String var) { //对一个多项式求导 -> 对每一项求导、结果乘以系数、再相加
        Expression ans = ElementFactory.newConstExpr(ElementFactory.newNumber(0));
        for (TermSign entry : this.keyMap.keySet()) {
            ans = Expression.add(ans,
                    Expression.mult(
                            entry.derive(var), keyMap.get(entry).toExpr())); // 单项式求导结果乘以系数，与ans相加
        }
        return ans;
    }

    /*-----转化工具-----*/

    @Override
    public Expression toExpression() {
        return this;
    }

    public mNumber toNumber() {
        if (keyMap.isEmpty()) {
            return mNumber.ZERO;
        }
        if (keyMap.size() != 1) {
            throw new RuntimeException("表达式中项不唯一，无法转换为Number");
        }
        Map.Entry<TermSign, mNumber> entry = keyMap.entrySet().iterator().next();
        if (!entry.getKey().isConst()) {
            throw new RuntimeException("表达式是变元项，无法转换为Number");
        }
        return entry.getValue();
    }

    /*-----判断工具-----*/

    public boolean isZero() {
        //Expr是空的
        return keyMap.isEmpty();
    }

    public boolean isFactor() {
        // Expr是Factor -> Expr是：数字 | 幂函数 | exp
        if (keyMap.size() == 1) { //只有一项的非系数部分及其系数
            TermSign key = keyMap.keySet().iterator().next(); //多项式中唯一的项签名
            return (key.isFactor() && keyMap.get(key).equal(1)) || key.isConst(); // 这个唯一的项是因子或常数
        }
        return false;
    }

    /*---------内部工具----------*/

    private void mergeTerm(TermSign key, mNumber coe) { //合并<key -> coe>
        if (!coe.equal(0)) {
            this.keyMap.merge(key, coe, mNumber::add);
            if (this.keyMap.get(key).equal(0)) {
                this.keyMap.remove(key);
            }
        }
    }

    private String TermToString(TermSign key) { //项 -> String
        StringBuilder sb = new StringBuilder();
        mNumber coe = keyMap.get(key); //该项的系数
        mNumber coeAbs = coe.abs();
        //打印符号
        if (!coe.gt(mNumber.ZERO)) { //coe<0
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
            mNumber coe1 = m1.keyMap.get(key1); //coe1 = m1的项的系数
            for (TermSign key2 : m2.keyMap.keySet()) { //key2 = m2的项
                mNumber coe2 = m2.keyMap.get(key2); //coe2 = m2的项的系数
                mNumber ansCoe = mNumber.mult(coe1, coe2); //ansCoe = coe1 * coe2
                TermSign ansKey = TermSign.mult(key1, key2); //ansKey = key1 * key2
                //ans += ansCoe * ansKey
                ans.mergeTerm(ansKey, ansCoe);
            }
        }
        return ans;
    }

    public static Expression pow(Expression base, mNumber exp) {
        Expression ans = ElementFactory.newSpaceExpr();
        ans.mergeTerm(ElementFactory.newSpaceTermSign(), mNumber.ONE); // ans = 1
        if (exp.equal(0)) {
            return ans;
        }
        for (mNumber i = mNumber.ZERO; i.compareTo(exp) < 0; i = mNumber.add(i, mNumber.ONE)) {
            ans = Expression.mult(ans, base);
        }
        return ans;
    }

}
