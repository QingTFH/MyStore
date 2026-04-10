package elevator;

import com.oocourse.elevator1.PersonRequest;
import io.Output;
import main.Config;
import main.Config.Direction;

public class Elevator implements Runnable {
    /*
     * 一个电梯
     * 检查该楼层请求的Map/List -> 处理请求 -> 移动一层
     */

    private static final boolean DOOR_CLOSED = false;
    private static final boolean DOOR_OPENED = true;

    private final int id;
    private Integer curFloor = Config.ELEVATOR_INITIAL_POS;
    // -4~-1 1~7 共7层 -> 压缩至-3~7 其中-3~0分别表示-4~-1层
    private Direction direction = Direction.NULL;
    private boolean door = DOOR_CLOSED;
    private long openTime;

    private final ElevatorTask task;

    public Elevator(int id) {
        this.id = id;
        this.task = new ElevatorTask(id);
    }

    public void run() { // 尽量保持run内部简洁
        while (!isEnd()) {
            try {
                // 如果目前没有任务了,等待任务
                waitForTask();

                if (isEnd()) {
                    break;
                }

                // 否则：处理请求->移动楼层
                setDirection();
                process(); // 处理本楼层的请求
                move(); // 移动一个楼层

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void addTask(PersonRequest request) {
        task.addTask(request);
    }

    private void process() throws InterruptedException {
        // 检测当前楼层是否是目标楼层
        // 如果是，开门,进出,关门
        if (task.isTarget(curFloor)) {
            openDoor();
            task.process(curFloor, direction);
            closeDoor();
        }
    }

    private void move() throws InterruptedException {
        // “移动一层”
        if (direction == Direction.NULL) {
            return; // 快速失败
        }

        closeDoor();

        Thread.sleep(Config.ELEVATOR_MOVE_TIME); // 移动一层要0.4s,对应sleep(400)
        curFloor += direction == Direction.UP ? 1 : -1;
        Output.printArrive(curFloor, id); // 先消耗时间，再到位
        if (curFloor == Config.ELEVATOR_FLOOR_MIN || curFloor == Config.ELEVATOR_FLOOR_MAX) {
            direction = Direction.NULL;
        }
    }

    private void openDoor() {
        if (door == DOOR_OPENED) {
            return;
        }
        door = DOOR_OPENED;
        Output.printOpen(curFloor, id);
        openTime = System.currentTimeMillis();
    }

    private void closeDoor() throws InterruptedException {
        if (door == DOOR_CLOSED) {
            return;
        }
        // 补足时间
        long time = System.currentTimeMillis() - openTime;
        if (time < 400) {
            Thread.sleep(Config.ELEVATOR_CLOSED_MIN_TIME - time);
        }
        door = DOOR_CLOSED;
        Output.printClose(curFloor, id);
    }

    private void setDirection() {
        direction = task.decideNextDirection(curFloor, direction);
    }

    private boolean isEnd() {
        return task.isEnd();
    }

    private void waitForTask() throws InterruptedException {
        task.waitForTask();
    }

    public ElevatorTask getTask() {
        return this.task;
    }
}
