package element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Termkey { /*
    可以认为是项的变元，记录变量名及其幂次
    x^7 * y^2
*/
    private final Map<String,Integer> map; // <变量名 -> 次数>

    public Termkey(Map<String,Integer> map) {
        this.map = Collections.unmodifiableMap(new HashMap<>(map));//先拷贝再不可变化
    }

    public static Termkey mult(Termkey t1,Termkey t2) { //两项的变元相乘,同名因子次数相加
        Map<String,Integer> ansMap = new HashMap<>(t1.map);
        for (String varName : t2.map.keySet()) {
            int exp = t2.map.get(varName);
            ansMap.merge(varName,exp,Integer::sum);
        }
        return new Termkey(ansMap);
    }

    public String toString() {
        if (map.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        List<Map.Entry<String,Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByKey());
        int flag = 0;
        for (Map.Entry<String,Integer> entry : list) {
            String varName = entry.getKey();
            int exp = entry.getValue();
            if (flag != 0) {
                sb.append("*");
            }
            sb.append(varName);
            if (exp != 1) {
                sb.append("^").append(exp);
            }
            flag++;
        }
        return sb.toString();
    }

    public boolean isConst() {
        return this.map.isEmpty();
    }

    @Override
    public boolean equals(Object o) { //Termkey能作为Map的key的条件
        if (this == o) { //同一个实例，或同为null
            return true;
        } else if (o == null || getClass() != o.getClass()) { //类不同
            return false;
        } else { //类相同
            return Objects.equals(map,((Termkey) o).map);
        }
    }

    @Override
    public int hashCode() { //Termkey能作为Map的key的条件
        return Objects.hash(map);
    }
}
