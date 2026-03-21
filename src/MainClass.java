
import io.Input;
import io.Output;
import lexer.Lexer;
import parser.Parser;

public class MainClass {
    public static void main(String[] args) {
        if (false) {
            test();
            return;
        }
        Parser parser = Parser.getParser();
        Input.InputFunction();//parser记录函数
        parser.setLexer(new Lexer(Input.InputLine())); //parser读取待解析表达式
        Output.printExpr(parser.parseExpr()); //解析表达式并输出
    }

    public static void test() {
        Parser parser = Parser.getParser();
        parser.setLexer(new Lexer(Input.InputLine())); //parser读取待解析表达式
        System.out.println((parser.parseExpr().isFactor() ? "1" : "0"));
    }

}
/*
 *  新增求导因子
 *  新增f{n}(x)
 */