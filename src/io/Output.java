package io;

import element.Expression;

public class Output {

    public static void printExpr(Expression a) { // 可优化点1：正项提前
        Output.printString(a.toOutString());
        Output.printString("\n");
    }

    public static void printString(String a) {
        System.out.print(a);
    }

}
