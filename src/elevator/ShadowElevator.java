package elevator;

import com.oocourse.elevator2.PersonRequest;
import main.Config;

public class ShadowElevator {

    /* 克隆某个电梯当前的状态，模拟完成所有任务所需的时间 */

    private static final boolean DOOR_CLOSED = false;
    private static final boolean DOOR_OPENED = true;

    private Integer curFloor;
    private Config.Direction direction;
    private boolean door;
    private int moveCostTime = Config.ELEVATOR_MOVE_TIME;

    private final ElevatorTask task;

    public ShadowElevator(ElevatorTask elevatorTask,
                          int floor,
                          Config.Direction dir,
                          boolean doorState) {
        task = elevatorTask;
        curFloor = floor;
        direction = dir;
        door = doorState;
    }

    public int simulate() {
        task.receiveTask(true);

        int time = 0; // 完成“所有任务”所需的时间

        while (!task.isSpace()) { // 任务还没有做完
            // 沿用电梯的策略：处理请求->移动楼层
            setDirection();
            time += process(); // 处理本楼层的请求
            time += move(); // 移动一个楼层
        }

        return time;
    }

    public ShadowElevator addTask(PersonRequest request) { // 加入新的任务
        task.addTask(request);
        return this;
    }

    private void setDirection() {
        direction = task.decideNextDirection(curFloor, direction);
    }

    private int process() {
        // 检测当前楼层是否是目标楼层
        // 如果是，开门,进出,关门
        int time = 0;
        if (task.isTarget(curFloor)) {
            openDoor();
            task.process(curFloor, direction, true);
            time += closeDoor();
        }
        return time;
    }

    private int move() {
        // “移动一层”
        if (direction == Config.Direction.NULL) {
            return 0; // 快速失败
        }
        int time = 0;

        time += closeDoor();
        curFloor += direction == Config.Direction.UP ? 1 : -1;
        if (curFloor == Config.ELEVATOR_FLOOR_MIN || curFloor == Config.ELEVATOR_FLOOR_MAX) {
            direction = Config.Direction.NULL;
        }
        time += moveCostTime;
        return time;
    }

    private void openDoor() { // 开门不消耗时间，统一关门消耗
        if (door == DOOR_OPENED) {
            return;
        }
        door = DOOR_OPENED;
    }

    private int closeDoor() {
        if (door == DOOR_CLOSED) {
            return 0;
        }
        door = DOOR_CLOSED;
        return Config.ELEVATOR_CLOSED_MIN_TIME;
    }
}
