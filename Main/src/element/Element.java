package element;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.TreeSet;

public class Element {
    private HashMap<Integer, BigInteger> eles = new HashMap<>();//次数->系数
    private TreeSet<Integer> exps = new TreeSet<>((a, b) -> b - a);//降序排列

    public Element() {//初始化为空元素
    }

    protected HashMap<Integer, BigInteger> eles() {
        return this.eles;
    }

    protected TreeSet<Integer> exps() {
        return this.exps;
    }

    public void addElement(Element element) { //加Element
        for (Integer exp : element.exps) { //key是次数，value是系数
            this.add(element.eles.get(exp), exp);
        }
    }

    protected void add(BigInteger coe, int exp) { //加Factor
        this.eles.merge(exp, coe, BigInteger::add);
        exps.add(exp);
        if (eles.get(exp).compareTo(BigInteger.ZERO) == 0) { //系数等于0，删去
            eles.remove(exp);
            exps.remove(exp);
        }
    }

    public Element mult(Element element) {
        Element ans = new Element();//ans = 0
        if (this.eles().isEmpty() || element.eles().isEmpty()) { //Term = 0
            return ans;
        }
        for (Integer e1 : element.exps()) {
            BigInteger c1 = element.eles().get(e1);
            for (Integer e2 : this.exps()) {
                BigInteger c2 = this.eles().get(e2);
                ans.add(c1.multiply(c2), e1 + e2); //ans += c1*c2*x^(e1+e2);
            }
        }
        return ans;
    }

    public void print() { //按key从小到大输出
        int flag = 0;//输出次数
        for (Integer exp : this.exps()) {
            BigInteger coe = this.eles().get(exp);
            if (!isZero_BI(coe)) { //系数不为0，输出该项
                if (!isPos_BI(coe)) { //负号一定输出
                    System.out.print("-");
                } else if (flag != 0) { //不是第一项，前有加
                    System.out.print("+");
                }
                flag = printCE(exp, flag);
            }
        }
        if (flag == 0) {
            System.out.print(0);
        }
    }

    private int printCE(int exp, int flag) { //打印幂次为exp的项
        int newFlag = flag;
        BigInteger coe = this.eles().get(exp).abs();//当前输出系数的绝对值
        if (!isZero_BI(coe)) { //系数不为0
            newFlag++;
            if (exp != 0) { //变元
                if (!coe.equals(new BigInteger("1"))) { //系数不为+-1,输出coe*
                    System.out.print(coe + "*");
                }
                if (exp == 1) { //次数为1，不输出^exp
                    System.out.print("x");
                } else { //次数不为1
                    System.out.print("x^" + exp);
                }
            } else { //常元
                System.out.print(coe);
            }
        }
        return newFlag;
    }

    private boolean isPos_BI(BigInteger a) {
        return a.compareTo(BigInteger.ZERO) > 0;//a大于0，则compareTo返回1
    }

    private boolean isZero_BI(BigInteger a) {
        return a.compareTo(BigInteger.ZERO) == 0;
    }

    protected void clear() {
        this.eles.clear();
        this.exps.clear();
    }

    private void copyTo(Element newEle) {
        for (Integer exp : this.exps) {
            newEle.add(this.eles.get(exp), exp);
        }
    }

    public Term toTerm() {
        Term term = ElementFactory.newTerm();
        term.clear();
        this.copyTo(term);
        return term;
    }
}
