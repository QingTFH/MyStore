package elevator;

import com.oocourse.elevator3.MaintRequest;
import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.RecycleRequest;
import com.oocourse.elevator3.Request;
import com.oocourse.elevator3.UpdateRequest;
import io.DebugOutput;
import io.Output;
import main.Config;
import main.Config.Direction;
import main.Shared;
import tools.RequestFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Elevator implements Runnable {
    /* 一个电梯; 检查该楼层请求的Map/List -> 处理请求 -> 移动一层 */
    /* 电梯是电梯, 线程是线程 */
    /* 其他线程需要读取：当前电梯状态、维护flag */
    /* 其他线程需要写入：维护flag */

    private static final boolean DOOR_CLOSED = false;
    private static final boolean DOOR_OPENED = true;

    private final int id;
    private final boolean isMain;
    private Integer curFloor = Config.ELEVATOR_INITIAL_POS;
    private Direction direction = Direction.NULL;
    private boolean door = DOOR_CLOSED;
    private int weight = 0;
    private int moveCostTime = Config.ELEVATOR_MOVE_TIME;
    private int maxFloor = Config.ELEVATOR_FLOOR_MAX;
    private int minFloor = Config.ELEVATOR_FLOOR_MIN;
    private long openTime; // 上一次开门的时间

    private final BlockingQueue<Request> buffer =
            new LinkedBlockingQueue<>();
    // 第三级盘子, 任务缓存; 生产者: shaft; 消费者: elevator(elevatorTask)
    private final HashMap<Integer, List<PersonRequest>> inTask =
            new HashMap<>(); // 申请进入电梯的任务
    private final HashMap<Integer, List<PersonRequest>> outTask =
            new HashMap<>(); // 申请离开电梯的任务
    private final TreeSet<Integer> targetFloor = new TreeSet<>();

    private volatile MaintRequest maintainRequest = null;
    private volatile boolean updateFlag = false;
    private volatile boolean recycleFlag = false;
    private volatile boolean endFlag = false; // 允许结束标志
    private volatile boolean doubleFlag = false; // 双轿厢模式

    private final ElevatorTask task;
    private Shared shared;
    private Shaft shaft;

    public Elevator(int id) {
        this.id = id;
        this.task = new ElevatorTask(false);
        isMain = (id <= 6);
        for (int i = Config.ELEVATOR_FLOOR_MIN; i <= Config.ELEVATOR_FLOOR_MAX; i++) {
            List<PersonRequest> inList = new ArrayList<>();
            inTask.put(i, inList);
            List<PersonRequest> outList = new ArrayList<>();
            outTask.put(i, outList);
        }
    }

    /*----- 任务主体 -----*/

    public void run() { // 尽量保持run内部简洁
        // 电梯应该结束的标志：scheduler结束
        DebugOutput.elevatorStart(id);
        shared = Shared.getShared();
        shaft = shared.getShaft(isMain ? id : id - 6);
        while (true) {
            try {
                if (maintainRequest != null) { // 检修
                    maintain();
                }
                if (updateFlag) {
                    startDouble();
                }
                if (recycleFlag) {
                    endDouble();
                }
                task.clearBuffer(); // 如果目前没有任务了, 会等待任务
                if (targetFloor.isEmpty() && endFlag && !recycleFlag) {
                    break;
                }
                direction = task.decideNextDirection();
                process(); // 处理本楼层的请求
                move(); // 移动一个楼层
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void move() throws InterruptedException {
        if (direction == Direction.NULL) {
            if (!doubleFlag || !(curFloor == 2)) {
                return; // 快速失败
            }
            direction = (isMain) ? Direction.UP : Direction.DOWN; // 主轿厢向上
        }
        closeDoor();
        final int prevFloor = curFloor;
        Thread.sleep(moveCostTime);
        if (curFloor + (direction == Direction.UP ? 1 : -1) == 2) { // 需要进入F2, 释放锁在输出前
            lockSharedFloor();
        }
        curFloor = curFloor + (direction == Direction.UP ? 1 : -1);
        Output.printArrive(curFloor, id); // 先消耗时间，再到位
        if (prevFloor == 2) { // 需要离开F2, 释放锁在输出后
            unlockSharedFloor();
        }
        if (curFloor == Config.ELEVATOR_FLOOR_MIN || curFloor == Config.ELEVATOR_FLOOR_MAX) {
            direction = Direction.NULL;
        }
    }

    private void unlockSharedFloor() {
        Object lock = shaft.getFloorLock();
        synchronized (lock) {
            shaft.resetF2Busy();
            lock.notifyAll();
        }
    }

    private void lockSharedFloor() throws InterruptedException {
        Object lock = shaft.getFloorLock();
        synchronized (lock) {
            while (shaft.isF2Busy()) {
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
        Thread.sleep(Math.max(0, Config.ELEVATOR_CLOSED_MIN_TIME -
                (System.currentTimeMillis() - openTime)));
        door = DOOR_CLOSED;
        Output.printClose(curFloor, id);
    }

    private void process() throws InterruptedException {
        if (task.isTarget(curFloor)) {
            openDoor();
            task.goOut();
            if (maintainRequest == null) {
                task.goIn();
            }
            closeDoor();
        }
    }

    public void addTask(Request request) {
        buffer.add(request);
    }

    private void maintain() throws InterruptedException {
        moveTo(1);
        process(); // 先处理本楼层可能的out请求, 避免new出 1->1的请求
        allGoOut();
        Output.printGoIn(maintainRequest.getWorkerId(), 1, id);
        closeDoor();
        Output.printMaintain1(id);
        task.removeReceive();
        Thread.sleep(Config.ELEVATOR_REP_STOP_TIME);
        Output.printMaintain2(id);
        setMoveCostTime(Config.ELEVATOR_REP_MOVE_TIME);
        moveTo(Config.changeStringToFloor(maintainRequest.getToFloor()));
        moveTo(1);
        setMoveCostTime(Config.ELEVATOR_MOVE_TIME);
        openDoor();
        Output.printGoOutS(maintainRequest.getWorkerId(), 1, id);
        closeDoor();
        Output.printMaintainEnd(id);
        maintainRequest = null;
        shared.finishRequest();
    }

    private void setMoveCostTime(int moveCostTime) {
        this.moveCostTime = moveCostTime;
    }

    private void allGoOut() {
        openDoor();
        task.allGoOut();
    }

    void setAsMain() throws InterruptedException { // 进入双轿厢状态
        moveTo(3);
        process(); // 先处理本楼层可能的out请求, 避免new出 1->1的请求
        allGoOut(); // 此处可能会导致额外的开门
        closeDoor();
        Output.updateBegin(id);
        long timeBegin = System.currentTimeMillis();
        task.removeReceive();
        long timeEnd = System.currentTimeMillis();
        Thread.sleep(1010 - (timeEnd - timeBegin));
        Output.updateEnd(id);
        Shared.getShared().getShaft(id).updateEnd();
    }

    void mainOver() {
        minFloor = Config.ELEVATOR_FLOOR_MIN;
        maxFloor = Config.ELEVATOR_FLOOR_MAX;
        doubleFlag = false;
    }

    void deputyOver() throws InterruptedException {
        moveTo(1);
        process(); // 先处理本楼层可能的out请求, 避免new出 1->1的请求
        allGoOut(); // 此处可能会导致额外的开门
        closeDoor();
        Output.recycleBegin(id);
        long timeBegin = System.currentTimeMillis();
        task.removeReceive();
        long timeEnd = System.currentTimeMillis();
        Thread.sleep(1010 - (timeEnd - timeBegin));
        doubleFlag = false;
        Output.recycleEnd(id);
        Shared.getShared().getShaft(id - Config.SHAFT_NUM).recycleEnd();
    }

    private void startDouble() throws InterruptedException {
        doubleFlag = true;
        task.changeLimitedFloor();
        if (id <= Config.SHAFT_NUM) {
            setAsMain();
        }
        updateFlag = false;
    }

    void endDouble() throws InterruptedException {
        if (id <= Config.SHAFT_NUM) {
            mainOver();
        } else {
            deputyOver();
        }
        recycleFlag = false;
    }

    private class ElevatorTask {
        private final boolean simulate;

        ElevatorTask(boolean simulate) {
            this.simulate = simulate;
        }

        private void goIn() {
            /* 处理当前楼层的进入请求,更新重量 */
            List<PersonRequest> list = inTask.get(curFloor);
            if (list.isEmpty()) {
                return;
            }
            List<PersonRequest> copy = new ArrayList<>(list); // 更新前的list，避免边遍历边修改
            for (PersonRequest request : copy) {
                if ((direction == Direction.NULL || getRequestDir(request) == direction)
                        && canAddWeight(request.getWeight())) {
                    changeTask(request);
                    weight += request.getWeight();
                    if (!simulate) {
                        Output.printGoIn(request.getPersonId(), curFloor, id);
                    }
                }
            }
        }

        private void goOut() {
            List<PersonRequest> list = outTask.get(curFloor);
            if (list.isEmpty()) {
                return;
            }
            Iterator<PersonRequest> iterator = list.iterator();
            while (iterator.hasNext()) {
                PersonRequest request = iterator.next();
                iterator.remove();
                if (curFloor == Config.changeStringToFloor(request.getToFloor())) {
                    singleOutSuccess(request); // out-S
                    continue;
                }
                singleOutFailure(request); // out-F
            }
            refreshSingleTargetFloor(curFloor);
        }

        private synchronized void singleOutSuccess(PersonRequest request) {
            weight -= request.getWeight();
            if (!simulate) {
                Output.printGoOutS(request.getPersonId(), curFloor, id);
                Shared.getShared().finishRequest();
            }
        }

        private synchronized void singleOutFailure(PersonRequest request) {
            weight -= request.getWeight();
            if (!simulate) {
                Output.printGoOutF(request.getPersonId(), curFloor, id);
            }
            Shared.getShared().addPending(RequestFactory.newPersonRequest(curFloor, request));
        }

        synchronized void clearBuffer() throws InterruptedException {
            if (buffer.isEmpty() && targetFloor.isEmpty()) { // 进入空闲状态
                scheduleTask(buffer.take());
            }
            while (!buffer.isEmpty()) { // 清空buffer
                scheduleTask(buffer.poll());
            }
        }

        private synchronized void scheduleTask(Request request) {
            if (request instanceof PersonRequest) {
                if (request == Config.POISON) { // 遇到结束标志
                    endFlag = true;
                    return;
                }
                receiveSingleRequest((PersonRequest) request);
            } else if (request instanceof MaintRequest) {
                maintainRequest = (MaintRequest) request;
            } else if (request instanceof UpdateRequest) {
                updateFlag = true;
            } else if (request instanceof RecycleRequest) {
                recycleFlag = true;
            }
        }

        synchronized void receiveSingleRequest(PersonRequest request) {
            int floor = Config.changeStringToFloor(request.getFromFloor());
            inTask.get(floor).add(request);
            if (!simulate) {
                Output.printReceive(request.getPersonId(), id);
            }
            refreshSingleTargetFloor(floor);
        }

        private void changeTask(PersonRequest request) {
            int to = Config.changeStringToFloor(request.getToFloor());
            int from = Config.changeStringToFloor(request.getFromFloor());
            to = Math.max(minFloor, Math.min(maxFloor, to)); // 将to限定在区间内

            inTask.get(from).remove(request);
            outTask.get(to).add(request);

            refreshSingleTargetFloor(to);
            refreshSingleTargetFloor(from);
        }

        private void refreshTargetFloor() {
            /* 完全更新targetFloor */
            for (int i = Config.ELEVATOR_FLOOR_MIN;
                 i <= Config.ELEVATOR_FLOOR_MAX; i++) {
                refreshSingleTargetFloor(i);
            }
        }

        private void refreshSingleTargetFloor(int floor) {
            /* 单层更新targetFloor */
            // 检查inTask和outTask中该楼层的list是否为空
            if (inTask.get(floor).isEmpty()
                    && outTask.get(floor).isEmpty()) { // 皆空：删除
                targetFloor.remove(floor);
            } else { // 否则：加入
                targetFloor.add(floor);
            }
        }

        synchronized Direction decideNextDirection() {
            /* 根据当前楼层、当前方向和targetFloor, 确定下一次运行的方向 */
            if (doubleFlag && curFloor == 2) {
                if (isMain) {
                    return Direction.UP;
                }
                return Direction.DOWN;
            }

            Integer up = targetFloor.ceiling(curFloor + 1);
            Integer down = targetFloor.floor(curFloor - 1);
            if (up == null && down == null) {
                return Direction.NULL;
            }

            if (direction == Direction.NULL) { // 当前方向为空：确定一个方向
                if (up != null && down != null) { // 上下都有目标，选近的
                    return (up - curFloor) <= (curFloor - down) ? Direction.UP : Direction.DOWN;
                } else if (up != null) {
                    return Direction.UP;
                } else {
                    return Direction.DOWN;
                }
            }
            if (direction == Direction.UP && up == null && !haveDir(Direction.UP)) {
                return Direction.DOWN;
            }
            if (direction == Direction.DOWN && down == null && !haveDir(Direction.DOWN)) {
                return Direction.UP;
            }
            return direction; // 保持
        }

        private boolean canAddWeight(int addWeight) {
            return weight + addWeight <= Config.ELEVATOR_MAX_WEIGHT;
        }

        private Direction getRequestDir(PersonRequest request) {
            int to = Config.changeStringToFloor(request.getToFloor());
            int from = Config.changeStringToFloor(request.getFromFloor());
            return to > from ? Direction.UP : Direction.DOWN;
        }

        private boolean haveDir(Direction dir) {
            for (PersonRequest request : inTask.get(curFloor)) {
                if (getRequestDir(request) == dir) {
                    return true;
                }
            }
            return false;
        }

        synchronized boolean isTarget(int floor) {
            return targetFloor.contains(floor);
        }

        synchronized void allGoOut() {
            goOut(); // 先处理本楼层可能的out-S
            for (int i = Config.ELEVATOR_FLOOR_MIN; i <= Config.ELEVATOR_FLOOR_MAX; i++) {
                List<PersonRequest> list = outTask.get(i);
                Iterator<PersonRequest> iterator = list.iterator();
                while (iterator.hasNext()) {
                    singleOutFailure(iterator.next());
                    iterator.remove();
                }
            }
            refreshTargetFloor();
        }

        synchronized void removeReceive() {
            for (int i = Config.ELEVATOR_FLOOR_MIN; i <= Config.ELEVATOR_FLOOR_MAX; i++) {
                List<PersonRequest> list = inTask.get(i);
                Iterator<PersonRequest> iterator = list.iterator();
                while (iterator.hasNext()) {
                    Shared.getShared().addPending(iterator.next());
                    iterator.remove();
                }
            }
            while (!buffer.isEmpty()) {
                Shared.getShared().addPending(buffer.poll());
            }

            refreshTargetFloor();
        }

        void changeLimitedFloor() {
            if (isMain) {
                minFloor = 2; // 最低层为F2
            } else {
                maxFloor = 2; // 最高层为F2
            }
        }
    }
}
