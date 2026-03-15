package parser;

import element.Expression;

public class Function {
    private final String funcName; // 函数名
    private final String paramName; // 形参名：x
    private final Expression body; // 函数体：x^2+1

    public Function(String funcName, String paramName, Expression body) {
        this.funcName = funcName;
        this.paramName = paramName;
        this.body = body;
    }

    public Expression apply(Expression arg) {
        return body.substitute(paramName, arg);
    }

    public String getFuncName() {
        return funcName;
    }
}
