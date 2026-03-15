package lexer;

public class Lexer {
    private int pos = 0;//curToken后一位
    private String curToken;
    private final String input;

    private String removeWhitespace(String s) { //去除空白字符
        return s.replaceAll("[ \\t]+", "");
    }

    public Lexer(String input) {
        this.input = removeWhitespace(input);

        //System.out.println(this.input);

        this.next();//初始化,curToken为第一个有效token
    }

    public String peek() { //获取信息
        return curToken == null ? "" : curToken;
    }

    private String getNumber() {
        StringBuilder num = new StringBuilder();
        while (pos < input.length()
                && Character.isDigit(input.charAt(pos))) {
            num.append(input.charAt(pos));
            pos++;
        }
        return num.toString();
    }

    private String getName() {
        StringBuilder var = new StringBuilder();
        while (pos < input.length()
                && Character.isLetter(input.charAt(pos))) {
            var.append(input.charAt(pos));
            pos++;
        }
        return var.toString();
    }

    public void next() { //只分辨是数字,变量名还是符号
        if (pos >= input.length()) {
            curToken = null;
            return;
        }

        if (Character.isDigit(input.charAt(pos))) { //数字
            curToken = getNumber();
        } else if (Character.isLetter(input.charAt(pos))) {  //变量名
            curToken = getName();
        } else { //符号
            curToken = String.valueOf(input.charAt(pos));
            pos++;
        }
    }

}
