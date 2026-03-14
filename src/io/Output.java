package io;

import element.Expression;

public class Output {

    public static void printExpr(Expression a) {
        Output.printString(a.toOutString());
    }

    public static void printString(String a) {
        System.out.print(a);
    }

}
