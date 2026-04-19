package elevator;

import com.oocourse.elevator3.MaintRequest;
import com.oocourse.elevator3.PersonRequest;
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
    private volatile boolean doubleFlag = false;

    public Elevator(int id) {
        this.id = id;
        this.task = new ElevatorTask(id);
    }

    /*----- 任务主体 -----*/

    public void run() { // 尽量保持run内部简洁
        // 电梯应该结束的标志：scheduler结束
        DebugOutput.elevatorStart(id);
        while (true) {
            try {
                if (isMaintain()) { // 检修
                    maintain();
                }

                receiveTask(); // 如果目前没有任务了, 会等待任务
                if (isSpace()
                        && isEnd()) {
                    end();
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

    private void end() {
        DebugOutput.elevatorEnd(id);
    }

    /* ----- 运动逻辑 ----- */

    private void move() throws InterruptedException {
        // “移动一层”
        // 如果进入公共区域, 需要获得
        // 如果离开公共区域, 需要释放

        if (direction == Direction.NULL) {
            if(!doubleFlag || !(curFloor == 2)) {
                return; // 快速失败
            }

            // 双轿厢模式下, process后如果在F2, 需要立刻移动退出F2
            // 设定方向以退出F2
            direction = (id <= Config.SHAFT_NUM) ?
                    Direction.UP : Direction.DOWN; // 主轿厢向上
        }

        closeDoor();
        int prevFloor = curFloor;
        int nextFloor = curFloor + (direction == Direction.UP ? 1 : -1);
        Thread.sleep(moveCostTime);

        if(doubleFlag && nextFloor == 2) { // 需要进入F2, 释放锁在输出前
            lockSharedFloor();
        }

        curFloor = nextFloor;
        Output.printArrive(curFloor, id); // 先消耗时间，再到位

        if(doubleFlag && prevFloor == 2) { // 需要离开F2, 释放锁在输出后
            unlockSharedFloor();
        }

        if (curFloor == Config.ELEVATOR_FLOOR_MIN || curFloor == Config.ELEVATOR_FLOOR_MAX) {
            direction = Direction.NULL;
        }
    }

    private void unlockSharedFloor() {
        // 解锁F2
        Shaft shaft = Shared.getShared().getShaft(id <= 6
                ? id : id-Config.SHAFT_NUM);
        Object lock = shaft.getFloorLock();
        synchronized (lock) {
            shaft.resetF2Busy();
            lock.notifyAll();
        }
    }

    private void lockSharedFloor() throws InterruptedException {
        // 锁F2
        Shaft shaft = Shared.getShared().getShaft(id <= 6
                ? id : id-Config.SHAFT_NUM);
        Object lock = shaft.getFloorLock();
        synchronized (lock) {
            while(shaft.isF2Busy()) {
                lock.wait();
            }
            shaft.setF2Busy();
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

    /*---------- 任务逻辑 ----------*/

    private void process() throws InterruptedException {
        // 检测当前楼层是否是目标楼层
        // 如果是，开门,进出,关门
        if (task.isTarget(curFloor)) {
            openDoor();
            task.process(curFloor, direction, false);
            closeDoor();
        }
    }

    public void addTask(PersonRequest request) {
        task.addTask(request);
    }

    private void receiveTask() throws InterruptedException {
        task.receiveTask(false);
    }

    public boolean isSpace() { // 这个电梯目前没任务了
        return task.isSpace();
    }

    /*---------- maintain ----------*/

    public void setMaintain(MaintRequest request) {
        /* 启动检修流程, 唤醒电梯 */
        maintainRequest = request;
        synchronized (task) {
            task.setMaintain();
            task.notifyAll();
        }
    }

    private void maintain() throws InterruptedException {
        /* repair主体 */

        // 移动到1楼，开门赶人，上工人，关门，输出
        moveTo(1);
        process(); // 先处理本楼层可能的out请求, 避免new出 1->1的请求
        // 此处可能会导致额外的开门
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

    private void allGoOut(){
        openDoor();
        task.allGoOut(curFloor);
    }

    private void removeReceive() {
        task.removeReceive();
    }

    private void workerIn() {
        openDoor();
        task.workerIn(maintainRequest);
    }

    private void workerOut() {
        task.workerOut(maintainRequest);
    }

    public boolean isMaintain() {
        return maintainRequest != null;
    }

    public ShadowElevator newShadow() {
        DebugOutput.newShadow(getId());
        return new ShadowElevator(task.cloneForShadow(),
                curFloor,
                direction,
                door,
                isMaintain());
    }

    public int getId() {
        return id;
    }

    private boolean isEnd() {
        return task.isEnd();
    }

    /*---------- double ----------*/

    void setAsMain() throws InterruptedException { // 进入双轿厢状态
        // 移动到F3, 所有人下电梯, 输出UPDATE-BEGIN:t1
        moveTo(3);
        process(); // 先处理本楼层可能的out请求, 避免new出 1->1的请求
        allGoOut(); // 此处可能会导致额外的开门
        closeDoor();
        Output.updateBegin(id);
        long timeBegin = System.currentTimeMillis();

        // UPDATE: 取消所有receive, 进行改造, 等待至少1s(-t1)
        removeReceive();
        task.changeLimitedFloor();
        long timeEnd = System.currentTimeMillis();
        Thread.sleep(1010-(timeEnd-timeBegin));

        // UPDATE-END: 备用轿厢在F1, 主轿厢在F3, 进入双轿厢模式
        doubleFlag = true;
        Output.updateEnd(id);
    }

    void setAsDeputy() {
        doubleFlag = true;
        task.changeLimitedFloor();
    }

    void mainOver() {
        task.resetLimitedFloor();
        doubleFlag = false;
    }

    void deputyOver() throws InterruptedException {
        moveTo(1);
        process(); // 先处理本楼层可能的out请求, 避免new出 1->1的请求
        allGoOut(); // 此处可能会导致额外的开门
        closeDoor();
        Output.recycleBegin(id);
        long timeBegin = System.currentTimeMillis();

        // UPDATE: 取消所有receive, 进行改造, 等待至少1s(-t1)
        removeReceive();
        long timeEnd = System.currentTimeMillis();
        Thread.sleep(1010-(timeEnd-timeBegin));

        doubleFlag = false;
        Output.recycleEnd(id);
    }

}
