import io.Input;

public class MainClass {
    public static void main(String[] args) {
        Lexer lexer = new Lexer(Input.InputNextString());
        Parser parser = new Parser(lexer);

        parser.parseExpr().print();
    }

}
/*
oo_2
20260308
    这里是地狱吗
*/