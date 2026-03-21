package parser;

import element.ElementFactory;
import element.Expression;

import java.util.ArrayList;
import java.util.List;

public class RecuFunction {
    /*  Recurrence Function
     *  f{n}(x) = coe1*f{n-1}(arg1)+coe2*f{n-2}(arg2) [+ extra]
     */
    List<Expression> memo = new ArrayList<>();
    String varName = "x";
    Expression coe1;
    Expression coe2;
    Expression arg1;
    Expression arg2;
    Expression extra;

    RecuFunction() {}

    void setFx(int x,Expression f) {
        while (memo.size() <= x) {
            memo.add(null); // 先扩容
        }
        memo.set(x, f);
    }

    void setOther(Expression coe1,
                 Expression coe2,
                 Expression arg1,
                 Expression arg2,
                 Expression extra) {
        this.coe1 = coe1;
        this.coe2 = coe2;
        this.arg1 = arg1; // 需要深克隆吗？应该是不用
        this.arg2 = arg2;

        if(extra != null) {
            this.extra = extra;
        } else {
            this.extra = ElementFactory.newSpaceExpr();
        }
    }

    public Expression apply(int n,Expression arg) {
        while(n>=memo.size()){ // 可取到的位置是memo[size-1]
            expand(); // 递推得到memo[n]
        }
        return memo.get(n).substitute("x",arg);
    }

    private void expand()  {
        //向后递推一个函数
        int n = memo.size(); // 本次要递推的位置
        if(n<2) {
            throw new RuntimeException("RecuFunction 递推时 缺少前置条件:size = " + n);
        }
        Expression f1 = Expression.mult(coe1,memo.get(n-1).substitute(varName,arg1));
        Expression f2 = Expression.mult(coe2,memo.get(n-2).substitute(varName,arg2));
        Expression fN = Expression.add(f1,f2);
        memo.add(Expression.add(fN,extra));
    }

}
