import element.Factor;
import element.Expression;
import element.ElementFactory;

import java.math.BigInteger;
import java.util.Objects;

public class Parser {
    private final Lexer lexer;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    public Expression parseExpr() {
        Expression ansExpr = ElementFactory.newExpr(); //ansExpr = 0
        String flag = "+";
        if (pOs(lexer.peek())) { //第一个项前可能会右额外正负号
            flag = getFlag();
        }
        Expression term = pOnFactor(flag).toExpression(); //term = +1 | -1
        term = Expression.mult(term,parseTerm()); //term = 符号 * parseTerm()
        ansExpr = Expression.add(ansExpr,term); //ansExpr += term

        while (pOs(lexer.peek())) { //向后解析
            term = pOnFactor(getFlag()).toExpression(); //term = +1 | -1
            term = Expression.mult(term,parseTerm()); // 符号 * parseTerm
            ansExpr = Expression.add(ansExpr,term);
        }
        return ansExpr; //此时curToken为右括号后的符号，或终点
    }

    private Factor pOnFactor(String a) { //通过a构筑正负因子
        if (pOs(a)) {
            if (Objects.equals(a, "+")) { // +1
                return ElementFactory.newFactor(BigInteger.ONE);
            } else { // -1
                return ElementFactory.newFactor(new BigInteger("-1"));
            }
        }
        throw new IllegalArgumentException("判断正负Factor时出现非法符号" + a);
    }

    private String getFlag() {
        //判断curToken的符号，后移一个curToken
        String flag;
        if (pOs(lexer.peek())) {
            //第一个因子前可能会右额外正负号
            flag = lexer.peek();
            lexer.next();
            return flag;
        } else {
            throw new IllegalArgumentException("判断flag时出现非法符号" + lexer.peek());
        }
    }

    private Expression parseTerm() {
        String flag = "+";
        if (pOs(lexer.peek())) { //第一个因子前可能会右额外正负号
            flag = getFlag();
        }
        Expression term = pOnFactor(flag).toExpression(); //以正负因子作为初始值 +1 或 -1
        term = multParse(term, parseFactor());
        while (Objects.equals(lexer.peek(), "*")) {
            lexer.next();//curToken = *后面的因子
            term = multParse(term, parseFactor()); //curToken = 因子后面的符号
        }
        return term;
    }

    private Expression multParse(Expression term, Expression factor) { //解析乘：一边解析一边乘
        //term是项本体，factor是打包后的因子
        Expression ans;
        if (Objects.equals(lexer.peek(), "^")) { //因子后有指数
            lexer.next(); //curToken = ^后面的数字
            Expression tmpExp = parseFactor();
            int n = tmpExp.toInt(); //指数
            if (n == 0) { //ans = term * 1
                ans = Expression.mult(term,ElementFactory.newFactor(BigInteger.ONE).toExpression());
            } else { // ans = term * factor ^ n
                ans = Expression.mult(term,factor);
                for (int i = 1; i < n; i++) {
                    ans = Expression.mult(ans,factor);
                }
            }
        } else {
            //没有指数，直接乘即可
            ans = Expression.mult(term,factor);
        }
        return ans;
    }

    private Expression parseFactor() { //带符号整数，变量因子，表达式因子
        String fac = lexer.peek(); //fac = '字母','(',常量因子数字,常量因子符号"+","-"
        if (!(fac.matches("[+(\\-]|[a-z0-9]+"))) { //允许读入数字，字母，+,-,(
            throw new IllegalArgumentException("parseFactor时读入非法因子: " + fac);
        }
        lexer.next();

        if (Objects.equals(fac, "(")) { //表达式因子
            Expression expr = parseExpr(); //解析表达式
            lexer.next(); //略过右括号
            return expr;
        } else if (fac.matches("[a-z]+")) { //变元因子
            return ElementFactory.newFactor(fac).toExpression();
        } else if (pOs(fac)) { //常量因子的符号
            String flag = fac + lexer.peek(); //符号拼接数字
            lexer.next();
            return ElementFactory.newFactor(new BigInteger(flag)).toExpression();
        } else { //常元
            return ElementFactory.newFactor(new BigInteger(fac)).toExpression();
        }
    }

    private boolean pOs(String a) { //positive or negative
        return Objects.equals(a, "+") || Objects.equals(a, "-");
    }
}
