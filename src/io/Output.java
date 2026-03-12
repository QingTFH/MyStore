package io;

import element.Termkey;

import java.math.BigInteger;
import java.util.Map;

public class Output {

    public static void printKeyMap(Map<Termkey, BigInteger> map) {
        int flag = 0; //打印次数
        for (Termkey key : map.keySet()) { //该项的元
            BigInteger coe = map.get(key); //该项的系数
            //打印符号
            if (coe.compareTo(BigInteger.ZERO) < 0) { //coe<0
                System.out.print("-");
            } else if (coe.compareTo(BigInteger.ZERO) > 0 && flag != 0) { //coe>0 且 不是第一项
                System.out.print("+");
            }

            //打印系数(绝对值):常数 | 变元且系数不为1
            if (key.isConst() || (!key.isConst() && !coe.abs().equals(BigInteger.ONE))) {
                System.out.print(coe.abs());
            }

            //打印元:
            if (!key.isConst()) {
                if (!coe.abs().equals(BigInteger.ONE)) { //系数不为1
                    System.out.print("*");
                }
                System.out.print(key); //自动调用toString
            }
            flag++;
        }
        if (flag == 0) {
            System.out.print("0");
        }
    }

}
