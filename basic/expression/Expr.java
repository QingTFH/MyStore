package expression;

import java.util.ArrayList;

public class Expr {
    private ArrayList<Term> terms = new ArrayList<>();//表达式的项们

    public void addTerm(Term term) {
        terms.add(term);
    }

    @Override
    public String toString() {
        if (terms.size()==1) {//默认无空表达式了?
            return terms.get(0).toString();//第一个项->只有一个项
        } else {
            StringBuilder sb = new StringBuilder();//可变字符串类
            sb.append(terms.get(0));//追加
            sb.append(" ");
            sb.append(terms.get(1));
            sb.append(" ");
            sb.append("+");
            for (int i = 2; i < terms.size(); i++) {
                sb.append(" ");
                sb.append(terms.get(i));
                sb.append(" ");
                sb.append("+");
            }
            return sb.toString();
        }
    }//返回该表达式的后缀表达式
}
