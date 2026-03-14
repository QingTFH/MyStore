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

        for (int i = 0; i < n; i++) { //构建map，<函数名 -> 表达式>
            parser.setLexer(new Lexer(InputLine()));
            parser.parseFuncDef();
        }
    }


}
/*
输入更改：
    第一行变为int n
    接下来至n+1行：自定义函数f(x) = Expr
    第n+2行,待解析表达式
*/