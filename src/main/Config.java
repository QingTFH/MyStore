package main;

import com.oocourse.elevator3.PersonRequest;

public class Config {

    public static final PersonRequest POISON
            = new PersonRequest("NONE","NONE",-1,-1);

    public static final boolean DEBUG = true;
    public static final int SHAFT_NUM = 6; // 电梯井数量
    public static final int ELEVATOR_INITIAL_POS = 1; // 电梯初始楼层
    public static final int ELEVATOR_FLOOR_MAX = 7; // 最高楼层
    public static final int ELEVATOR_FLOOR_MIN = -3; // 最低楼层
    public static final int ELEVATOR_MIDDLE_POS = 2; // 双轿厢中间楼层
    public static final int ELEVATOR_MAX_WEIGHT = 400; // 电梯载重 400kg
    public static final int ELEVATOR_MOVE_TIME = 400; // 电梯移动：400ms
    public static final int ELEVATOR_CLOSED_MIN_TIME = 400; // 关门最小延迟：400ms
    public static final int ELEVATOR_REP_MOVE_TIME = 200; // 检修时，移动花费时间
    public static final int ELEVATOR_REP_STOP_TIME = 1000; // 检修时，等待花费时间
    public static final int ELEVATOR_REP_MAXTIME = 7000; // 检修BEGIN - END最多花费时间

    public enum Direction {
        UP, DOWN, NULL
    }

    public static int changeStringToFloor(String a) { // -3~7
        int x;
        if (a.charAt(0) == 'B') {
            // B2
            x = a.charAt(1) - '0';  // -4~-1层用 -3 ~ 0 层表示
            x = 1 - x;
        } else {
            // F3
            x = a.charAt(1) - '0';
        }

        return x;
    } // 建立映射表

    public static String changeFloorToString(int floor) {
        if (floor > 0) {
            // 3 -> F3
            return "F" + floor;
        } else {
            // -3 -> B4 // 0 -> B1
            return "B" + (1 - floor);
        }
    }

}
