package parser;

import element.Factor;
import element.Expression;
import element.Number;
import factory.ElementFactory;
import lexer.Lexer;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Objects;

public class Parser {
    private static Parser parser = null;
    private final HashMap<String, Function> funcs = new HashMap<>(); // <函数名 -> 函数体>

    private Lexer lexer;

    private Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    public static Parser getParser() {
        if (parser == null) {
            parser = new Parser(null);
        }
        return parser;
    }

    public void setLexer(Lexer lexer) {
        this.lexer = lexer;
    }

    public Expression parseExpr() {
        Expression ansExpr = ElementFactory.newExpr(); // ansExpr = 0
        String flag = "+";
        if (isPlusOrSub(lexer.peek())) { // 第一个项前可能会右额外正负号
            flag = getFlag();
        }
        Expression term = SignFactor(flag).toExpression(); // term = +1 | -1
        term = Expression.mult(term,parseTerm()); // term = 符号 * parseTerm()
        ansExpr = Expression.add(ansExpr,term); // ansExpr += term

        while (isPlusOrSub(lexer.peek())) { // 向后解析
            term = SignFactor(getFlag()).toExpression(); // term = +1 | -1
            term = Expression.mult(term,parseTerm()); // 符号 * parseTerm
            ansExpr = Expression.add(ansExpr,term);
        }
        return ansExpr; //此时curToken为右括号后的符号，或终点
    }

    private Factor SignFactor(String a) { // 通过a构筑正负因子
        if (isPlusOrSub(a)) {
            if (Objects.equals(a, "+")) { // +1
                return ElementFactory.newFactor(BigInteger.ONE);
            } else { // -1
                return ElementFactory.newFactor(new BigInteger("-1"));
            }
        }
        throw new IllegalArgumentException("判断正负Factor时出现非法符号" + a);
    }

    private String getFlag() { //判断curToken的符号，后移一个curToken
        String flag;
        if (isPlusOrSub(lexer.peek())) {
            flag = lexer.peek();
            lexer.next();
            return flag;
        } else {
            throw new IllegalArgumentException("判断flag时出现非法符号" + lexer.peek());
        }
    }

    private Expression parseTerm() {
        String flag = "+";
        if (isPlusOrSub(lexer.peek())) { // 第一个因子前可能会右额外正负号
            flag = getFlag();
        }
        Expression term = SignFactor(flag).toExpression(); // 以正负因子作为初始值 +1 或 -1
        term = Expression.mult(term, parseFactor());
        while (Objects.equals(lexer.peek(), "*")) {
            lexer.next(); //curToken = *后面的因子
            term = Expression.mult(term, parseFactor()); // curToken = 因子后面的符号
        }
        return term;
    }

    private Expression parseFactor() {
        /* 带符号整数，变量因子，表达式因子 */

        String fac = lexer.peek(); // fac = '字母','(',常量因子数字,常量因子符号"+","-"

        if (!(fac.
                matches("[+(\\[\\-]|[a-z0-9]+"))) { //允许读入数字，字母，+,-,(
            throw new IllegalArgumentException("parseFactor时读入非法因子: " + fac);
        }
        lexer.next(); // curToken = fac后一位

        Expression ans;
        if (Objects.equals(fac, "(")) { // 表达式因子
            ans = parseExprFactor();
        } else if (Objects.equals(fac, "[")) { // 选择式因子,curToken = "("
            ans = parseChoose();
        } else if (fac.matches("[a-z]+")) { // 一串字母
            if ((Objects.equals(fac, "f"))) { // 函数，后续可能要遍历funcs
                ans = parseFunction();
            } else if ((Objects.equals(fac, "exp"))) { // exp
                ans = parseExp();
            } else { // 变元因子
                ans = parseVarFactor(fac);
            }
        } else if (isPlusOrSub(fac)) { // 常量因子的符号
            ans = parseSignWithNumber(fac);
        } else { // 常量因子
            ans = parseNumber(fac);
        } // 这时lexer.peek是Factor后的符号

        if (Objects.equals(lexer.peek(), "^")) {
            lexer.next();
            Number n = parseFactor().toNumber();
            ans = Expression.pow(ans, n);
        }

        return ans;
    }

    private void skipFactor() {
        //跳过一整个Factor->常数、带符号常数、幂函数、函数调用、选择式、表达式因子
        String fac = lexer.peek();
        if (Objects.equals(fac, "(")) { // 表达式
            skipBalanced(fac);
            skipExponent();
        } else if (Objects.equals(fac, "[")) { // 选择式
            skipBalanced(fac);
        } else if (Objects.equals(fac, "exp")) {
            lexer.next(); //后续是(inner)
            skipBalanced("(");
            skipExponent();
        } else if (Objects.equals(fac, "f")) {
            lexer.next(); //后续是(arg)
            skipBalanced("(");
        } else {
            // x、常数、带符号常数，可能有^n
            lexer.next();
            skipExponent();
        }
    }

    private void skipBalanced(String open) {
        // 对于用括号包裹的因子，消费完匹配的"(inner)" 或者 "[inner]"
        lexer.next();
        String close = Objects.equals(open, "(") ? ")" : "]";
        int depth = 1;
        while (depth > 0) {
            String tok = lexer.peek();
            lexer.next();
            if (tok.equals(open)) {
                depth++;
            }
            else if (tok.equals(close)) {
                depth--;
            }
        } //跳出循环后，curToken = close的next
    }

    private void skipExponent() {
        //消费"^Number"
        if (Objects.equals(lexer.peek(), "^")) {
            lexer.next(); // 消费 "^"
            lexer.next(); // 消费 指数数字
        }
    }

    private Expression parseExprFactor() {
        Expression expr = parseExpr(); // 解析表达式
        lexer.next(); // 略过右括号
        return expr;
    }

    private Expression parseVarFactor(String varName) {
        return ElementFactory.newFactor(varName).toExpression();
    }

    private Expression parseSignWithNumber(String sign) {
        String num = lexer.peek(); // 当前数字
        lexer.next(); // 掠过当前数字
        String number = sign + num; // 符号拼接数字
        return ElementFactory.newFactor(new BigInteger(number)).toExpression();
    }

    private Expression parseNumber(String num) {
        return ElementFactory.newFactor(new BigInteger(num)).toExpression();
    }

    private Expression parseChoose() {
        /* 待解析: "(A==B)?C:D]" */
        lexer.next(); // 消费 "("
        Expression a = parseFactor(); // 之后peek是"="和"="
        lexer.next();
        lexer.next();
        Expression b = parseFactor(); // 之后peek是")"
        Expression diff = Expression.subtract(a, b);
        lexer.next(); // 消费 ")"
        lexer.next(); // 消费 "?"
        Expression ans;
        if (diff.isZero()) {
            ans = parseFactor(); // parseC
            lexer.next(); // 消费 ":"
            skipFactor(); // 跳过D 后续是"]"
        } else {
            skipFactor(); // 跳过C，后续是 " :D] "
            lexer.next(); // 消费 ":"
            ans = parseFactor(); // parseD
        }
        lexer.next(); // 消费 "]",来到ChooseFactor后的第一个token
        return ans;
    }

    private Expression parseExp() {
        //待解析 (A) A是因子
        lexer.next(); // 消费"("，peek = inner第一个token
        Expression inner = parseFactor();
        lexer.next(); // 消费")"
        return ElementFactory.newExpExpr(inner);
    }

    private Expression parseFunction() {
        // 待解析 (A)
        lexer.next(); // 消费"("，peek = 实参第一个token
        Expression arg = parseFactor();
        lexer.next(); // 消费")"
        Function f = funcs.get("f");
        return f.apply(arg);
    }

    private boolean isPlusOrSub(String a) { //加号或减号
        return Objects.equals(a, "+") || Objects.equals(a, "-");
    }

    public void parseFuncDef() {
        //已知该行为 "f(x)=Expr",curToken = "f"
        final String funcName = lexer.peek();
        lexer.next(); // peek = "("
        lexer.next(); // peek = "x"
        final String varName = lexer.peek();
        lexer.next(); // peek = ")"
        lexer.next(); // peek = "="
        lexer.next(); // peek = Expression第一个token
        Expression body = parseExpr();
        funcs.put(funcName, new Function(funcName, varName, body));
    }
}
