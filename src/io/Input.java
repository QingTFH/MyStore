package io;

import java.util.Scanner;
import lexer.Lexer;
import parser.Parser;

public class Input {
    private static final Scanner scanner = new Scanner(System.in);

    public static String InputLine() {
        return scanner.nextLine();
    }

    public static void InputMap() {
        Parser parser = Parser.getParser();
        int n = Integer.parseInt(scanner.nextLine());

        for (int i=0;i<n;i++) {
            //切割成 函数名 = 函数表达式，后者parseExpr
            parser.setLexer(new Lexer(InputLine()));
            parser.parseFuncDef();
        }//构建map，<函数名 -> 表达式>
    }



}
/*
输入更改：
    第一行变为int n
    接下来至n+1行：自定义函数f(x) = Expr
    第n+2行,待解析表达式
*/