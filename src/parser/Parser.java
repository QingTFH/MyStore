package parser;

import element.Expression;
import element.Number;
import element.ElementFactory;
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

    /*-----EXPR-----*/

    public Expression parseExpr() {
        Expression ansExpr = ElementFactory.newSpaceExpr(); // ansExpr = 0
        String flag = "+";
        if (isPlusOrSub(lexer.peek())) { // 第一个项前可能会右额外正负号
            flag = getFlag();
        }
        Expression term = SignBase(flag); // term = +1 | -1
        term = Expression.mult(term,parseTerm()); // term = 符号 * parseTerm()
        ansExpr = Expression.add(ansExpr,term); // ansExpr += term

        while (isPlusOrSub(lexer.peek())) { // 向后解析
            term = SignBase(getFlag()); // term = +1 | -1
            term = Expression.mult(term,parseTerm()); // 符号 * parseTerm
            ansExpr = Expression.add(ansExpr,term);
        }
        return ansExpr; //此时curToken为右括号后的符号，或终点
    }

    /*-----TERM-----*/

    private Expression parseTerm() {
        String flag = "+";
        if (isPlusOrSub(lexer.peek())) { // 第一个因子前可能会右额外正负号
            flag = getFlag();
        }
        Expression term = SignBase(flag).toExpression(); // 以正负因子作为初始值 +1 或 -1
        term = Expression.mult(term, parseFactor());
        while (Objects.equals(lexer.peek(), "*")) {
            lexer.next(); //curToken = *后面的因子
            term = Expression.mult(term, parseFactor()); // curToken = 因子后面的符号
        }
        return term;
    }

    /*-----FACTOR-----*/

    private Expression parseFactor() {
        /* 带符号整数，变量因子，表达式因子 */
        //要求进入前Factor完整，返回后Factor完全消除
        String fac = lexer.peek(); // fac = '字母','(',常量因子数字,常量因子符号"+","-"
        if (!(fac.
                matches("[+(\\[\\-]|[a-z0-9]+"))) { //允许读入数字，字母，+,-,(
            throw new IllegalArgumentException("parseFactor时读入非法因子: " + fac);
        }
        Expression ans = parseFactorByType(fac);
        if (Objects.equals(lexer.peek(), "^")) {
            lexer.next();
            Number n = parseFactor().toNumber();
            ans = Expression.pow(ans, n);
        }
        return ans;
    }

    private Expression parseFactorByType(String fac) {
        Expression ans;
        //分发前，保持了factor的完整性
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
                ans = parseVarFactor();
            }
        } else if (isPlusOrSub(fac)) { // 常量因子的符号
            ans = parseSignWithNumber();
        } else { // 常量因子
            ans = parseNumber();
        } // 这时lexer.peek是Factor后的符号
        return ans;
    }

    private Expression parseExprFactor() {
        /* 待解析: "(Expr)" */
        lexer.next(); // 略过左括号
        Expression expr = parseExpr(); // 解析表达式
        lexer.next(); // 略过右括号
        return expr;
    }

    private Expression parseChoose() {
        /* 待解析: "[(A==B)?C:D]" */
        lexer.next(); // 消费 "["
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

    private Expression parseFunction() {
        /* 待解析: f(arg) */
        lexer.next(); // 消费"f"
        lexer.next(); // 消费"("，peek = 实参第一个token
        Expression arg = parseExpr(); //预防性编程....
        lexer.next(); // 消费")"
        Function f = funcs.get("f");
        return f.apply(arg);
    }

    private Expression parseExp() {
        /* 待解析: exp(inner)  */
        lexer.next(); // 消费"exp"
        lexer.next(); // 消费"("，peek = inner第一个token
        Expression inner = parseExpr();
        lexer.next(); // 消费")"
        return ElementFactory.newTransExpr(inner);
    }

    private Expression parseVarFactor() {
        /* 待解析: "varName" */
        Expression ans = ElementFactory.newVarExpr(lexer.peek(), Number.ONE);
        lexer.next();
        return ans;
    }

    private Expression parseSignWithNumber() {
        /* 待解析: "+Number" */
        String sign = lexer.peek(); // 当前符号
        lexer.next();
        String num = lexer.peek(); // 当前数字
        lexer.next();
        String number = sign + num; // 符号拼接数字
        return ElementFactory.newNumber(new BigInteger(number)).toExpr();
    }

    private Expression parseNumber() {
        /* 待解析: "Number" */
        String num = lexer.peek(); // 当前数字
        lexer.next();
        return ElementFactory.newNumber(new BigInteger(num)).toExpr();
    }

    /*-----FUNCTION-----*/

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

    /*-----SKIP-----*/

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

    /*-----内部工具-----*/

    private Expression SignBase(String a) { // 通过a构筑正负因子
        if (!isPlusOrSub(a)) {
            throw new IllegalArgumentException("判断正负Factor时出现非法符号" + a);
        }
        if (Objects.equals(a, "+")) {
            return ElementFactory.newConstExpr(Number.ONE); // +1
        } else {
            return ElementFactory.newConstExpr(Number.ONE.negate()); // -1
        }
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

    private boolean isPlusOrSub(String a) { //加号或减号
        return Objects.equals(a, "+") || Objects.equals(a, "-");
    }

}
