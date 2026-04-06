package main;

public class Config {

    public static final int ELEVATOR_NUM = 6; // 电梯数量
    public static final int ELEVATOR_INITIAL_POS = 1; // 电梯初始楼层
    public static final int ELEVATOR_MAX_WEIGHT = 400; // 电梯载重 400kg
    public static final int ELEVATOR_MOVE_TIME = 400; // 电梯移动：400ms
    public static final int ELEVATOR_CLOSED_MIN_TIME = 400; // 关门最小延迟：400ms
    public static final int ELEVATOR_FLOOR_MAX = 7;
    public static final int ELEVATOR_FLOOR_MIN = -3;

    private static volatile boolean inputFinished = false;
    private static volatile boolean scheduleFinished = false;
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

    public static void inputFinished() {
        inputFinished = true;
    }

    public static boolean isInputFinished() {
        return inputFinished;
    }

    public static void scheduleFinished() {
        scheduleFinished = true;
    }

    public static boolean isScheduleFinished() {
        return scheduleFinished;
    }
}
