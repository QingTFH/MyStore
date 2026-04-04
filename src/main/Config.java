package main;

import com.oocourse.elevator1.PersonRequest;

public class Config {

//    public static final PersonRequest LAST_REQUEST // 任务队列结束的标志
//            =  new PersonRequest("-10","-1",-1,-1,-1);

    public static final int ELEVATOR_NUM = 6; // 电梯数量
    public static final int ELEVATOR_INITIAL_POS = 1; // 电梯初始楼层
    public static final int ELEVATOR_MAX_WEIGHT = 400; // 电梯载重 400kg
    public static final int ELEVATOR_MOVE_TIME = 400; // 电梯移动：400ms

    public static volatile boolean inputFinished = false;
    public static volatile boolean scheduleFinished = false;

    public static int changeStringToFloor(String a) {
        int x;
        if(a.charAt(0) == 'B') {
            // B2
            x = a.charAt(1) - '0';  // -4~-1层用 -3 ~ 0 层表示
            x = 1-x;
        } else {
            // F3
            x = a.charAt(1) - '0';
        }

        return x;
    }

    public static String changeFloorToString(int floor) {
        if(floor > 0){
            // 3 -> F3
            return "F" + floor;
        } else {
            // -3 -> B4 // 0 -> B1
            return "B" + (1-floor);
        }
    }
}
