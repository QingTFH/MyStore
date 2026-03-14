package io;

import element.Expression;
import element.TermKey;

import java.math.BigInteger;
import java.util.Map;

public class Output {

    public static void printExpr(Expression a) {
        Output.printString(a.toOutString());
    }

    public static void printString(String a) {
        System.out.print(a);
    }

}
