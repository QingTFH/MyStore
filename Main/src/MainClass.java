import element.Expr;

import java.util.Scanner;

public class MainClass {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);

        Expr expr = parser.parseExpr();
        expr.print();
    }

}
/*
oo_1
20260306
        parseTerm化简项:各个因子相乘，取第一个因子作为Element,与后面每个因子进行Element.mult计算，返回一个Element
            幂：若干个相同的因子相乘，同样parseTerm
        parseExpr化简表达式：各个项相加，逐步加入HashMap中，直到无

        先完成：不考虑括号、数字前符号，只化简表达式(✔)
        再考虑括号(✔)
        再考虑幂(✔)
        再考虑减号(✔)
        再考虑前导符号(✔)
        再考虑输出简洁(✔)
*/