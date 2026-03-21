package element;

import element.atom.TranscenAtom;
import element.atom.ExpAtom;
import element.atom.VarAtom;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class ElementFactory {

    /*-----Expression-----*/

    public static Expression newSpaceExpr() { // expr = 0
        return new Expression();
    }

    public static Expression newConstExpr(MyNumber coe) { // Expr = coe + 0
        return new Expression(newSpaceTermSign(), coe);
    }

    public static Expression newVarExpr(String varName, MyNumber power) { // Expr = var^power + 0
        return new Expression(newAlgeTermSign(varName, power), newNumber(1));
    }

    public static Expression newTransExpr(Expression inner) { // Expr = exp(inner) + 0
        if (inner.isZero()) {
            return newConstExpr(newNumber(1));
        }
        return new Expression(newTransTermSign(inner), newNumber(1));
    }

    /*-----Term-----*/     //不应直接传入map，而只传入第一个因子，后续要再传入使用mult方法

    public static TermSign newSpaceTermSign() {
        return new TermSign(new HashMap<>(), new HashMap<>());
    }

    public static TermSign newAlgeTermSign(String varName, MyNumber power) {
        Map<VarAtom, MyNumber> algeMap = new HashMap<>();
        algeMap.put(newVarKey(varName), power);
        return new TermSign(algeMap, new HashMap<>());
    }

    public static TermSign newTransTermSign(Expression inner) {
        Map<TranscenAtom, MyNumber> transMap = new HashMap<>();
        transMap.put(newExpKey(inner), newNumber(1));
        return new TermSign(new HashMap<>(), transMap);
    }

    /*-----Atom-----*/

    public static ExpAtom newExpKey(Expression inner) {
        if (inner.isZero()) {
            throw new IllegalArgumentException("创建ExpKey时inner = 0，请降级为VarExp");
        }
        return new ExpAtom(inner);
    }

    public static VarAtom newVarKey(String varName) {
        return new VarAtom(varName);
    }

    /*-----Number-----*/

    public static MyNumber newNumber(BigInteger num) {
        return new MyNumber(num);
    }

    public static MyNumber newNumber(String num) {
        if (!num.matches("^[+-]?[0-9]+$")) {
            throw new IllegalArgumentException("生成Number时，初始化使用错误的String:" + num);
        }
        return new MyNumber(new BigInteger(num));
    }

    public static MyNumber newNumber(int num) {
        return new MyNumber(BigInteger.valueOf(num));
    }

}
