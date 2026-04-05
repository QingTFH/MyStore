package tools;

import com.oocourse.elevator1.PersonRequest;
import io.Output;
import main.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class Elevator implements Runnable {
    /*
     * 一个电梯
     * 检查该楼层请求的Map/List -> 处理请求 -> 移动一层
     */

    private enum Direction {
        UP, DOWN, NULL
    }

    private static final boolean DOOR_CLOSED = false;
    private static final boolean DOOR_OPENED = true;

    private final int id;
    private int weight = 0;
    private final HashMap<Integer, List<PersonRequest>> inTask
            = new HashMap<>(); // 申请进入电梯的任务
    private final HashMap<Integer, List<PersonRequest>> outTask
            = new HashMap<>(); // 申请离开电梯的任务
    private final TreeSet<Integer> targetFloor = new TreeSet<>();
    // 目标楼层,包含：inTask的“From楼层” 和 outTask的"To楼层"
    private Integer pos = Config.ELEVATOR_INITIAL_POS; // -4~-1 1~7 共7层 -> 压缩至-3~7 其中-3~0分别表示-4~-1层
    private Direction direction = Direction.NULL;
    private boolean door = DOOR_CLOSED;

    private final Object lock = new Object(); // inTask & targetFloor

    private long openTime;

    public Elevator(int id) {
        this.id = id;

        //初始化map中的list,防止null
        for (int i = -3; i <= 7; i++) {
            List<PersonRequest> inList = new ArrayList<>();
            inTask.put(i, inList);
            List<PersonRequest> outList = new ArrayList<>();
            outTask.put(i, outList);
        }
    }

    public void run() { // 尽量保持run内部简洁
        while (!isAllEnd()) {
            try {
                synchronized (lock) { // 有后续任务
                    refreshTargetFloor();
                    if (targetFloor.isEmpty() && !Config.isScheduleFinished()) { //还未收到，等待
                        lock.wait();
                        refreshTargetFloor();
                    }

                    if (isAllEnd()) { // 可能被scheduler结束时唤醒
                        break;
                    }

                    handleRequest(); // 处理本楼层的请求
                    move(); // 移动一个楼层
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void freeOutTask(PersonRequest request) {
        // 将 request 从 outTask | targetFloor 移除
        int toFloor = Config.changeStringToFloor(request.getToFloor());

        List<PersonRequest> list = outTask.get(toFloor);
        list.remove(request);

        refreshTargetSingleFloor(toFloor);
    }

    private void move() throws InterruptedException {
        // “移动一层”
        setDirection();
        if (direction == Direction.NULL) {
            return; // 快速失败
        }

        if (door == DOOR_OPENED) {
            closeDoor(); // 移动前必须关门
        }

        Thread.sleep(Config.ELEVATOR_MOVE_TIME); // 移动一层要0.4s,对应sleep(400)
        pos += direction == Direction.UP ? 1 : -1;
        Output.printArrive(pos, id); // 先消耗时间，再到位
    }

    private void openDoor() {
        door = DOOR_OPENED;
        Output.printOpen(pos, id);
        openTime = System.currentTimeMillis();
    }

    private void closeDoor() throws InterruptedException {
        // 补足时间
        long time = System.currentTimeMillis() - openTime;
        if (time < 400) {
            Thread.sleep(Config.ELEVATOR_CLOSED_MIN_TIME - time);
        }
        door = DOOR_CLOSED;
        Output.printClose(pos, id);
    }

    private void singleGoIn(PersonRequest request) {
        //判断这个request上电梯 重量是否合法：
        int wei = request.getWeight();
        //不合法：快速失败
        if (!canAddWeight(wei)) {
            return;
        }

        //合法：进入电梯 inTask -> outTask,处理targetFloor
        changeTask(request); // 内置targetFloor处理
        weight += wei;

        Output.printGoIn(request.getPersonId(), pos, id);
    }

    private void goOut() {
        //判断当前楼层有无out请求
        List<PersonRequest> list = outTask.get(pos);

        //没有：快速失败
        if (list == null || list.isEmpty()) {
            return;
        }

        List<PersonRequest> copy = new ArrayList<>(list);
        //有：完成请求、输出逻辑、hashMap/treeSet逻辑
        for (PersonRequest request : copy) {
            singleGoOut(request);
        }

        refreshTargetSingleFloor(pos);
    }

    private void singleGoOut(PersonRequest request) {
        // 完成请求
        freeOutTask(request);
        weight -= request.getWeight();

        Output.printGoOut(request.getPersonId(), pos, id);
    }

    private boolean canAddWeight(int addWeight) {
        return weight + addWeight <= Config.ELEVATOR_MAX_WEIGHT;
    }

    private void setDirection() {
        //根据getTarget的结果设置方向

        if (direction == Direction.NULL) {
            direction = Direction.UP;
        }

        Integer floor = getTarget();

        if (floor == null) {  // 无目标: 待机
            direction = Direction.NULL;
            return;
        }

        if (floor >= pos) {
            direction = Direction.UP;
        } else {
            direction = Direction.DOWN;
        }
    }

    private boolean isAllEnd() {
        return targetFloor.isEmpty()
                && weight == 0
                && Config.isScheduleFinished();
    }

    Object getLock() {
        return lock;
    }

    /* ----- 涉及locked成员的方法 -----*/

    private void handleRequest() throws InterruptedException {
        // 检测当前楼层是否是目标楼层
        // 如果是，开门放行，关门
        boolean stop;

        synchronized (lock) { // 快照stop
            stop = targetFloor.contains(pos);
        }

        if (stop) {
            openDoor();
            goOut();
            goIn();
            closeDoor();
        }
    }

    public void addInTask(PersonRequest request) throws InterruptedException {
        int fromFloor = Config.changeStringToFloor(request.getFromFloor());
        synchronized (lock) {
            List<PersonRequest> list = inTask.get(fromFloor);
            list.add(request);
            targetFloor.add(fromFloor);

            lock.notifyAll(); // 唤醒等待任务的电梯线程
        }
        Output.printReceive(request.getPersonId(), id);
    }

    private void goIn() {
        // 处理进入请求
        synchronized (lock) {
            // 检查这个楼层是否有进入请求
            List<PersonRequest> list = inTask.get(pos);

            // 无请求：快速失败
            if (list.isEmpty()) {
                return;
            }

            List<PersonRequest> copy = new ArrayList<>(list); // 更新前的list，避免边遍历边修改
            for (PersonRequest request : copy) {
                //判断这个request goIn，体重是否合法
                int wei = request.getWeight();

                //不合法：下一个request
                if (!canAddWeight(wei)) {
                    continue;
                }

                //合法： 允许进入电梯
                singleGoIn(request);
            }

            refreshTargetSingleFloor(pos); // 避免targe被误删除
        }
    }

    private void changeTask(PersonRequest request) {
        // 将 request 从 inTask 转移到 outTask，修正targetFloor
        int fromFloor = Config.changeStringToFloor(request.getFromFloor());
        int toFloor = Config.changeStringToFloor(request.getToFloor());

        synchronized (lock) {
            // 合法检查: 判断request是否在inTask中
            List<PersonRequest> listIn = inTask.get(fromFloor);
            if (listIn == null
                    || !listIn.contains(request)) {
                return;
            }

            // 将request从 inTask 删去
            listIn.remove(request);

            // 处理targetFloor
            targetFloor.remove(fromFloor);
            targetFloor.add(toFloor);
        }

        // 将request加入outTask
        List<PersonRequest> listOut = outTask.get(toFloor);
        listOut.add(request);
    }

    private Integer getTarget() { // 获取目标楼层
        // 目标楼层：当前方向上的下一个目标 | 非当前方向上的最近目标 | 空
        Integer ceil;
        Integer floor;
        synchronized (lock) {
            ceil = targetFloor.ceiling(pos + 1);
            floor = targetFloor.floor(pos - 1);
        }

        return direction == Direction.UP ? (ceil == null ? floor : ceil) :
                direction == Direction.DOWN ? (floor == null ? ceil : floor) :
                        null;
    }

    private void refreshTargetFloor() {
        // 完全刷新targetFloor
        for (int i = Config.ELEVATOR_FLOOR_MIN;
             i <= Config.ELEVATOR_FLOOR_MAX; i++) {
            refreshTargetSingleFloor(i);
        }
    }

    private void refreshTargetSingleFloor(int floor) {
        // 单层刷新targetFloor: 检查inTask和outTask中该楼层的list是否为空
        boolean space;
        synchronized (lock) {
            space = inTask.get(floor).isEmpty()
                    && outTask.get(floor).isEmpty();
            if (space) { // 皆空：删除
                targetFloor.remove(floor);
            } else { // 否则：加入
                targetFloor.add(floor);
            }
        }
    }

}
