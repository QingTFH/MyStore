import element.Expression;

import java.util.Scanner;

public class MainClass {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);

        Expression expr = parser.parseExpr();
        expr.print();
    }

}
/*
oo_2
20260308
    这里是地狱吗
*/