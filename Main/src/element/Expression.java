package element;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class Expression extends Element { /*
    3 * x^7 * y^2 + 2 * ooNiuBi ^ 5
    表达式：若干个Term的和，
    存若干个项，如果项内名称和幂次都相同(即Termkey相同)则合并

    统一使用Expression做计算，Factor只作提取因子用
*/
    private final Map<Termkey, BigInteger> keyMap; //< 项特征 -> 系数 >

    public Expression() {
        this.keyMap = new HashMap<>();
    }

    public void addFactor(Factor factor) { //Factor转Expr的入口
        BigInteger coe = factor.getCoe(); //factor的系数
        if (!coe.equals(BigInteger.ZERO)) {
            Map<String,Integer> newMap = new HashMap<>(); //factor对应的Termkey的map
            if (!factor.isConst()) { //factor是变元
                newMap.put(factor.getVarName(),1);
            }
            Termkey newKey = new Termkey(newMap); //factor的Termkey
            this.mergeTerm(newKey,coe);
        }
    }

    public void mergeTerm(Termkey key,BigInteger coe) { //合并key -> coe
        if (!coe.equals(BigInteger.ZERO)) {
            this.keyMap.merge(key,coe,BigInteger::add);
            if (this.keyMap.get(key).equals(BigInteger.ZERO)) {
                this.keyMap.remove(key);
            }
        }
    }

    public static Expression add(Expression e1,Expression e2) {
        Expression ans = new Expression();
        e1.keyMap.forEach(ans::mergeTerm);
        e2.keyMap.forEach(ans::mergeTerm);
        return ans;
    }

    public static Expression mult(Expression m1,Expression m2) {
        Expression ans = new Expression();
        for (Termkey key1 : m1.keyMap.keySet()) { //key1 = m1的项
            BigInteger coe1 = m1.keyMap.get(key1); //coe1 = m1的项的系数
            for (Termkey key2 : m2.keyMap.keySet()) { //key2 = m2的项
                BigInteger coe2 = m2.keyMap.get(key2); //coe2 = m2的项的系数
                BigInteger ansCoe = coe1.multiply(coe2); //ansCoe = coe1 * coe2
                Termkey ansKey = Termkey.mult(key1,key2); //ansKey = key1 * key2
                //ans += ansCoe * ansKey
                ans.mergeTerm(ansKey,ansCoe);
            }
        }
        return ans;
    }

    public void print() { //打印
        //3*x^2 + 5*y^3 + z
        int flag = 0; //打印次数
        for (Termkey key : this.keyMap.keySet()) { //该项的元
            BigInteger coe = this.keyMap.get(key); //该项的系数
            //打印符号
            if (coe.compareTo(BigInteger.ZERO) < 0) { //coe<0
                System.out.print("-");
            } else if (coe.compareTo(BigInteger.ZERO) > 0 && flag != 0) { //coe>0 且 不是第一项
                System.out.print("+");
            }

            //打印系数(绝对值):常数 | 变元且系数不为1
            if (key.isConst() || (!key.isConst() && !coe.abs().equals(BigInteger.ONE))) {
                System.out.print(coe.abs());
            }

            //打印元:
            if (!key.isConst()) {
                if (!coe.abs().equals(BigInteger.ONE)) { //系数不为1
                    System.out.print("*");
                }
                System.out.print(key); //自动调用toString
            }
            flag++;
        }
        if (flag == 0) {
            System.out.print("0");
        }
    }

    @Override
    public Expression toExpression() {
        return this;
    }

    public int toInt() {
        if (keyMap.isEmpty()) {
            return 0;
        }
        if (keyMap.size() != 1) {
            this.print();
            throw new RuntimeException("表达式中项不唯一，无法转换为int");
        }
        Map.Entry<Termkey, BigInteger> entry = keyMap.entrySet().iterator().next();
        if (!entry.getKey().isConst()) {
            this.print();
            throw new RuntimeException("表达式是变元项，无法转换为int");
        }
        return entry.getValue().intValueExact();
    }
}
