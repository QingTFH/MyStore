import element.Factor;
import element.Term;
import element.Expr;
import element.ElementFactory;
import element.Element;

import java.util.Objects;

public class Parser {
    private final Lexer lexer;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    //1+2*3+x^3
    public Expr parseExpr() {
        Expr expr = ElementFactory.newExpr();
        String flag = "+";
        if (pos(lexer.peek())) { //第一个项前可能会右额外正负号
            flag = getflag();
        }
        Term term = parseTerm();
        term = term.mult(P_NFactor(flag));
        expr.addElement(term);

        while (pos(lexer.peek())) {
            flag = getflag();
            term = parseTerm();
            term = term.mult(P_NFactor(flag));
            expr.addElement(term);
        }
        return expr;//此时curToken为右括号后的符号，或终点
    }

    private Factor P_NFactor(String a) {
        if (pos(a)) {
            if (Objects.equals(a, "+")) {
                return ElementFactory.newFactor("1", 0);
            } else {
                return ElementFactory.newFactor("-1", 0);
            }
        }
        throw new IllegalArgumentException("判断正负Factor时出现非法符号" + a);
    }

    private String getflag() {
        //判断curToken的符号，后移一个curToken
        String flag;
        if (pos(lexer.peek())) {
            //第一个因子前可能会右额外正负号
            flag = lexer.peek();
            lexer.next();
            return flag;
        } else {
            throw new IllegalArgumentException("判断flag时出现非法符号" + lexer.peek());
        }
    }

    private Term parseTerm() {
        Term term = ElementFactory.newTerm();
        String flag = "+";
        if (pos(lexer.peek())) {
            //第一个因子前可能会右额外正负号
            flag = getflag();
        }
        term = term.mult(P_NFactor(flag));
        term = multParse(term, parseFactor());
        while (Objects.equals(lexer.peek(), "*")) {
            lexer.next();//curToken = *后面的因子
            term = multParse(term, parseFactor());//curToken = 因子后面的符号
        }
        return term;
    }

    private Term multParse(Term term, Element element) { //ans = term * element(^n)
        Term ans = ElementFactory.newTerm();
        if (Objects.equals(lexer.peek(), "^")) { //因子后有指数
            lexer.next(); //curToken = ^后面的数字
            Factor tmpExp = (Factor) parseFactor();
            int n = tmpExp.toInt(); //指数
            if (n == 0) { //ans = term * 1
                ans = term.mult(ElementFactory.newFactor("1", 0));
            } else {
                ans = term.mult(element);
                for (int i = 1; i < n; i++) {
                    ans = ans.mult(element);
                }
            }
        } else {
            //没有指数，直接乘即可
            ans = term.mult(element);
        }
        return ans;
    }

    private Element parseFactor() { //带符号整数，变量因子，表达式因子
        String fac = lexer.peek(); //fac = 'x','(',常量因子数字,常量因子符号"+","-"
        //System.out.println(fac);

        if (!(fac.matches("[+x(\\-]|[0-9]+"))) {
            throw new IllegalArgumentException("parseFactor时读入非法因子: " + fac);
        }
        lexer.next();

        if (Objects.equals(fac, "(")) { //表达式因子
            Expr tmpExpr = parseExpr();
            lexer.next();//略过右括号
            //System.out.println(lexer.peek());
            //tmpExpr.print();
            return tmpExpr;
        } else if (Objects.equals(fac, "x")) { //变元因子
            return ElementFactory.newFactor("1", 1);
        } else if (pos(fac)) { //常元因子的符号
            StringBuilder flag = new StringBuilder(fac);//符号打头
            flag.append(lexer.peek());//拼接数字
            lexer.next();
            return ElementFactory.newFactor(flag.toString(), 0);
        } else { //常元
            return ElementFactory.newFactor(fac, 0);
        }
    }

    private boolean pos(String a) {
        return Objects.equals(a, "+") || Objects.equals(a, "-");
    }
}
