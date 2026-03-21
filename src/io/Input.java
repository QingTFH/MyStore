package io;

import java.util.Scanner;

import lexer.Lexer;
import parser.Parser;

public class Input {
    private static final Scanner scanner = new Scanner(System.in);

    public static String InputLine() {
        return scanner.nextLine();
    }

    public static void InputFunction() {
        Parser parser = Parser.getParser();
        int n = Integer.parseInt(scanner.nextLine());

        for (int i = 0; i < n; i++) { // 读取非递归函数
            parser.setLexer(new Lexer(InputLine()));
            parser.parseFuncDef();
        }

        int m = Integer.parseInt(scanner.nextLine());

        for (int i = 0; i < m; i++) { // 读取递归函数
            for (int j = 0; j < 2; j++) {
                parser.setLexer(new Lexer(InputLine()));
                parser.parseRecuFuncDefO();
            }
            parser.setLexer(new Lexer(InputLine()));
            parser.parseRecuFuncDefN();
        }
    }

}
/*
输入更改：
    第一行变为int n
    接下来至n+1行：自定义函数f(x) = Expr
    第n+2行,待解析表达式
*/