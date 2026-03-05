import expression.Expr;
import expression.Term;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClass {
    private static final String patternTerm = "[0-9*]+";//项的正则表达式，禁止空项
    public static final Pattern re = Pattern.compile(patternTerm);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String inputExpr = scanner.next();

        Matcher matcher = re.matcher(inputExpr);//matcher成为inputExpr的，对应re中正则表达式的Matcher

        Expr expr = new Expr();
        while (matcher.find()) {//find:查找下一个匹配表达式的子串，移动到子串后并返回true，否则返回false，且将子串记入matcher.group(0)
            String termStr = matcher.group(0);//提取子串
            Term term = new Term(termStr);//子串->项
            expr.addTerm(term);//项添加
        }

        System.out.println(expr.toString());//原地变形
    }
    //怎么中缀转后缀？
}
