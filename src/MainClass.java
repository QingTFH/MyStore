import element.Expression;
import factory.ElementFactory;
import io.Input;
import io.Output;
import lexer.Lexer;
import parser.Parser;

import java.math.BigInteger;

public class MainClass {
    public static void main(String[] args) {
        Parser parser = Parser.getParser();
        Input.InputFunction();//parser记录函数
        parser.setLexer(new Lexer(Input.InputLine())); //parser读取待解析表达式
        Output.printExpr(parser.parseExpr()); //解析表达式并输出
    }

    public static void test() {
        Parser parser = Parser.getParser();
        parser.setLexer(new Lexer(Input.InputLine())); //parser读取待解析表达式
        Expression a1 = parser.parseExpr();
        parser.setLexer(new Lexer(Input.InputLine())); //parser读取待解析表达式
        Expression a2 = parser.parseExpr();
        parser.setLexer(new Lexer(Input.InputLine())); //parser读取待解析表达式
        Expression a3 = parser.parseExpr();
        parser.setLexer(new Lexer(Input.InputLine())); //parser读取待解析表达式
        Expression a4 = parser.parseExpr();
        Expression e1 = Expression.mult(a1, a2);
        Expression e2 = Expression.mult(a3, Expression.mult(
                a4, ElementFactory.newFactor(new BigInteger("-1")).toExpression()));
        Output.printExpr(e1);
        System.out.println();
        Output.printExpr(e2);
        System.out.println();
        Output.printExpr(Expression.add(e1, e2));
    }

}
/*
oo_hw2
    新增：
        函数因子(归类于"变量因子"中)
            指数函数因子exp(y) [^a] a是指数,y是因子
            自定义函数因子f(x) = Expr(x) x是形参
        选择式因子(归类于"因子"中0 [(A == B) ? C : D]  A-D都是因子,

    此时 因子 -> 变量因子|常数因子|表达式因子|选择式因子
        变量因子 -> 幂函数|指数函数|自定义函数
    问题：
        1.如何识别并提取新因子
        2.如何在Expr中存储新因子，或 如何原地展开后存入表达式
        3.既然增加了非线性的e，后续是否会增加同类的sin,cos等，如何存储？
    方案：
        1.1选择式因子:
            提取：在parseFactor中遇到"["时，触发parseChoose
            实现：提取Factor A和Factor B A + -B，如果结果是0就返回提取的C 否则返回提取的D？不重要
        1.2函数因子:
            1.2.1 指数函数exp
                提取：依旧是parseFactor，如果提取到"变量名"="exp"，就进入指数函数的提取
                实际上exp(y)^n = exp(n*y),由于y是因子所以可以直接合并进系数里
            1.2.2 自定义函数f
                记录：<函数名String -> 函数体Function>
                获得：parseFunction
                提取：依旧是parseFactor，如果提取到"变量名"="f"，就进入指数函数的提取
*/