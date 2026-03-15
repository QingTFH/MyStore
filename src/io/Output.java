package io;

import element.Expression;

public class Output {

    public static void printExpr(Expression a) {
        //优化点1：正项提前
        Output.printString(a.toOutString());
    }

    public static void printString(String a) {
        System.out.print(a);
    }

}
