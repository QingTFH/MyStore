package element;

import element.Atom.ExpAtom;
import element.Atom.Atom;
import element.Atom.TranscenAtom;
import element.Atom.VarAtom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TermSign {
    /*
     *   项签名： 项的非系数部分
     *   x^7 * y^2 * exp(inner)
     */

    private final Map<VarAtom, mNumber> algeMap; // <项中代数子的name -> 次数>
    private final Map<TranscenAtom, mNumber> transcenMap; // <项中超越子的key -> 次数>

    TermSign(Map<VarAtom, mNumber> algeMap,
             Map<TranscenAtom, mNumber> transcenMap) { // 包级访问
        this.algeMap = new HashMap<>(algeMap);
        this.transcenMap = new HashMap<>(transcenMap);
    }

    /*-----静态方法-----*/

    public static TermSign mult(TermSign t1, TermSign t2) {
        TermSign ans = ElementFactory.newSpaceTermSign();
        // 合并代数部分
        ans.algeMap.putAll(t1.algeMap);
        mergeAlge(ans.algeMap, t2.algeMap);
        // 合并超越部分
        ans.transcenMap.putAll(t1.transcenMap);
        mergeTrans(ans.transcenMap, t2.transcenMap);
        return ans;
    }

    private static void mergeAlge(Map<VarAtom, mNumber> base,
                                  Map<VarAtom, mNumber> other) {
        other.forEach((k, v) -> { // lambda表达式 代替 增强for循环
            base.merge(k, v, mNumber::add);
            if (base.get(k).equal(0)) {
                base.remove(k);
            }
        });
    }

    private static void mergeTrans(Map<TranscenAtom, mNumber> base,
                                   Map<TranscenAtom, mNumber> other) {
        for (Map.Entry<TranscenAtom, mNumber> entry : other.entrySet()) {
            if (entry.getKey() instanceof ExpAtom) {
                ExpAtom found = findExpKey(base);
                if (found != null) {
                    base.remove(found);
                    Expression newInner = Expression.add(
                            found.getInner(), entry.getKey().getInner());
                    if (!newInner.isZero()) {
                        base.put(ElementFactory.newExpKey(newInner), mNumber.ONE);
                    }
                } else {
                    base.put(entry.getKey(), mNumber.ONE);
                }
            } else {
                throw new IllegalArgumentException("TermSign合并transMap时出错");
            }
        }
    }

    private static ExpAtom findExpKey(Map<TranscenAtom, mNumber> ansMap) {
        ExpAtom found = null;
        for (TranscenAtom k : ansMap.keySet()) {
            if (k instanceof ExpAtom) {
                found = (ExpAtom) k;
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
        return Objects.equals(algeMap, ((TermSign) o).algeMap)
                && Objects.equals(transcenMap, ((TermSign) o).transcenMap);   //类相同
    }

    @Override
    public int hashCode() {
        return Objects.hash(algeMap) + Objects.hash(transcenMap);
    }

    /*-----对外方法-----*/

    public Expression substitute(String varName, Expression arg) { // 将该TermKey中的元 varName 替换成 arg
        Expression result = ElementFactory.newConstExpr(
                ElementFactory.newNumber(1)); // 从1开始连乘
        for (Map.Entry<VarAtom, mNumber> entry : algeMap.entrySet()) { // 代入代数侧
            VarAtom var = entry.getKey();
            mNumber power = entry.getValue();
            if (var != null) {
                if (var.getName().equals(varName)) { // 对多项式部分:var^n -> arg^n
                    result = Expression.mult(result, Expression.pow(arg, power));
                } else { // 其他变量名的VarAtom-power，原样保留,result *= var^power
                    result = Expression.mult(result, var.toExpr(power));
                }
            } else {
                throw new IllegalArgumentException("TermSign代入时出错");
            }
        }
        for (Map.Entry<TranscenAtom, mNumber> entry : transcenMap.entrySet()) {
            TranscenAtom key = entry.getKey();
            if (key instanceof ExpAtom) { // 对Exp部分:arg代入inner，生成newExp,result *= newExp
                result = Expression.mult(result,
                        ElementFactory.newTransExpr(
                                key.getInner().substitute(varName, arg)));
            } else {
                throw new IllegalArgumentException("TermSign代入时出错");
            }
        }
        return result;
    }

    public String toOutString() {
        if (algeMap.isEmpty() && transcenMap.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Map.Entry<VarAtom, mNumber> entry : algeMap.entrySet()) {
            if (!isFirst) {
                sb.append("*");
            }
            sb.append(entry.getKey().toOutString(entry.getValue()));
            isFirst = false;
        }
        for (Map.Entry<TranscenAtom, mNumber> entry : transcenMap.entrySet()) {
            if (!isFirst) {
                sb.append("*");
            }
            sb.append(entry.getKey().toOutString(entry.getValue()));
            isFirst = false;
        }
        return sb.toString();
    }

    public boolean isConst() {
        return this.algeMap.isEmpty()
                && this.transcenMap.isEmpty(); // 该项没有非系数部分
    }

    public boolean isFactor() {
        return (this.algeMap.size() + this.transcenMap.size() == 1); // 该项的非系数部分只有一个
    }

    public Expression derive(String var) { // 对单项式求导 -> 对第i个因子求导，执行size次，
        if(isConst()) {
            return ElementFactory.newConstExpr(
                    ElementFactory.newNumber(0));
        }

        Expression ans = ElementFactory.newSpaceExpr();
        List<Map.Entry<? extends Atom, mNumber>> entries = new ArrayList<>();
        entries.addAll(algeMap.entrySet());
        entries.addAll(transcenMap.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Atom atom = entries.get(i).getKey();
            mNumber exponent = entries.get(i).getValue();
            Expression derivative = atom.derive(var, exponent);
            if (derivative.isZero()) {
                continue;
            }
            Expression term = ElementFactory.newConstExpr(
                    ElementFactory.newNumber(1));
            for (int j = 0; j < entries.size(); j++) {
                if (i != j) {
                    term = Expression.mult(term,
                            entries.get(j).getKey().toExpr(entries.get(j).getValue()));
                }
            }
            ans = Expression.add(ans, Expression.mult(derivative, term));
        }
        return ans;
    }

}
