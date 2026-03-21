package element;

public abstract class Element {
    public abstract Expression toExpression();
    public abstract Expression derive(String var);
}
