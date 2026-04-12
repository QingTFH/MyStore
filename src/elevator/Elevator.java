package elevator;

import com.oocourse.elevator2.MaintRequest;
import com.oocourse.elevator2.PersonRequest;
import io.DebugOutput;
import io.Output;
import main.Config;
import main.Config.Direction;
import main.Shared;

public class Elevator implements Runnable {
    /* 一个电梯; 检查该楼层请求的Map/List -> 处理请求 -> 移动一层 */
    /* 电梯是电梯, 线程是线程 */
    /* 其他线程需要读取：当前电梯状态、维护flag */
    /* 其他线程需要写入：维护flag */

    private static final boolean DOOR_CLOSED = false;
    private static final boolean DOOR_OPENED = true;

    private final int id;
    private Integer curFloor = Config.ELEVATOR_INITIAL_POS;
    private Direction direction = Direction.NULL;
    private boolean door = DOOR_CLOSED;
    private int moveCostTime = Config.ELEVATOR_MOVE_TIME;
    private long openTime; // 上一次开门的时间

    private final ElevatorTask task;

    private volatile MaintRequest maintainRequest = null;

    public Elevator(int id) {
        this.id = id;
        this.task = new ElevatorTask(id);
    }

    /*----- 任务主体 -----*/

    public void run() { // 尽量保持run内部简洁
        // 电梯应该结束的标志：scheduler结束
        while (true) {
            try {
                if (isMaintain()) { // 检修
                    maintain();
                }

                receiveTask(); // 如果目前没有任务了,等待任务
                if (isSpace()
                        && Shared.getShared().isScheduleEnd()) {
                    DebugOutput.elevatorEnd(id);
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

    private void process() throws InterruptedException {
        // 检测当前楼层是否是目标楼层
        // 如果是，开门,进出,关门
        if (task.isTarget(curFloor)) {
            openDoor();
            task.process(curFloor, direction, false);
            closeDoor();
        }
    }

    private void move() throws InterruptedException {
        // “移动一层”
        if (direction == Direction.NULL) {
            return; // 快速失败
        }

        closeDoor();
        Thread.sleep(moveCostTime); // 移动一层要0.4s,对应sleep(400)
        curFloor += direction == Direction.UP ? 1 : -1;
        Output.printArrive(curFloor, id); // 先消耗时间，再到位
        if (curFloor == Config.ELEVATOR_FLOOR_MIN || curFloor == Config.ELEVATOR_FLOOR_MAX) {
            direction = Direction.NULL;
        }
    }

    private void moveTo(int floor) throws InterruptedException {
        if (floor == curFloor) {
            return;
        }

        direction = floor > curFloor ? Direction.UP : Direction.DOWN;
        int n = Math.abs(floor - curFloor); // 需要移动的次数
        for (int i = 0; i < n; i++) {
            move();
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

    private void receiveTask() throws InterruptedException {
        task.getTask(false);
    }

    private void maintain() throws InterruptedException {
        /* repair主体 */

        // 移动到1楼，开门赶人，上工人，关门，输出
        moveTo(1);
        process(); // 先处理本楼层可能的out请求, 避免new出 1->1的请求
        openDoor();
        allGoOut();
        workerIn();
        closeDoor();
        Output.printMaintain1(id);

        // 解除所有receive
        removeReceive();

        // 检修1s，输出
        Thread.sleep(Config.ELEVATOR_REP_STOP_TIME);
        Output.printMaintain2(id);

        // 修改移动速度，移动到toFloor
        setMoveCostTime(Config.ELEVATOR_REP_MOVE_TIME);
        moveTo(Config.changeStringToFloor(maintainRequest.getToFloor()));

        // 移动到F1，恢复移动速度
        moveTo(1);
        setMoveCostTime(Config.ELEVATOR_MOVE_TIME);

        // 开门，下工人，关门
        openDoor();
        workerOut();
        closeDoor();
        Output.printMaintainEnd(id);

        // 通知task可以receive，maint = null
        maintainRequest = null;
        task.rmMaintain();
    }

    private void setMoveCostTime(int moveCostTime) {
        this.moveCostTime = moveCostTime;
    }

    private void allGoOut() {
        task.allGoOut();
    }

    private void removeReceive() {
        task.removeReceive();
    }

    private void workerIn() {
        task.workerIn(maintainRequest);
    }

    private void workerOut() {
        task.workerOut(maintainRequest);
    }

    /*----- 外部写 -----*/

    public void addTask(PersonRequest request) {
        task.addTask(request);
    }

    public void setMaintain(MaintRequest request) {
        /* 启动检修流程, 唤醒电梯 */
        maintainRequest = request;
        synchronized (task) {
            task.setMaintain();
            task.notifyAll();
        }
    }

    /*----- 外部读 -----*/

    public ElevatorTask getTask() {
        return this.task;
    }

    public boolean isSpace() { // 这个电梯目前没任务了
        return task.isSpace();
    }

    public boolean isMaintain() {
        return maintainRequest != null;
    }

    public ShadowElevator newShadow() {
        return new ShadowElevator(task.cloneForShadow(),
                curFloor,
                direction,
                door);
    }

    public int getId() {
        return id;
    }

}
