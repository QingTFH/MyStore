
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
 *  修复bug?其实是优化性能:parseChoose中，对于不需要的因子，没必要展开；例如要返回D，则不应展开C，而是直接跳过
 */