package elevator;

import com.oocourse.elevator1.PersonRequest;
import io.Output;
import main.Config;
import main.Config.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ElevatorTask {
    /*
     * 一个电梯内部的任务管理器, 处理该电梯所有任务逻辑
     * 需要处理"每一层的请求"、处理请求后调度运行方向
     */

    private final BlockingQueue<PersonRequest> task = new LinkedBlockingQueue<>();

    private final HashMap<Integer, List<PersonRequest>> inTask
            = new HashMap<>(); // 申请进入电梯的任务
    private final HashMap<Integer, List<PersonRequest>> outTask
            = new HashMap<>(); // 申请离开电梯的任务
    private final TreeSet<Integer> targetFloor = new TreeSet<>();

    private final int elevatorId;
    private int weight = 0;

    // 目标楼层,包含：inTask的“From楼层” 和 outTask的"To楼层"

    public ElevatorTask(int id) {
        elevatorId = id;
        //初始化map中的list,防止null
        for (int i = Config.ELEVATOR_FLOOR_MIN;
             i <= Config.ELEVATOR_FLOOR_MAX;
             i++) {
            List<PersonRequest> inList = new ArrayList<>();
            inTask.put(i, inList);
            List<PersonRequest> outList = new ArrayList<>();
            outTask.put(i, outList);
        }
    }

    /* ----- 处理请求逻辑 ----- */

    public synchronized void process(int floor, Direction direction) {
        /* 处理当前楼层的所有进出请求,更新重量,刷新target */
        //处理离开请求
        goOut(floor);
        //处理进入请求: 只接受当前运动方向上的进入请求
        goIn(floor, direction);
        //刷新target
        refreshTargetFloor();
    }

    private void goIn(int floor, Direction direction) {
        /* 处理当前楼层的进入请求,更新重量 */
        // 检查这个楼层是否有进入请求
        List<PersonRequest> list = inTask.get(floor);

        // 无请求：快速失败
        if (list.isEmpty()) {
            return;
        }

        List<PersonRequest> copy = new ArrayList<>(list); // 更新前的list，避免边遍历边修改
        for (PersonRequest request : copy) {
            // 1. 需求的移动方向是否与当前运动方向相同 (如果电梯空闲，则可任意接受)
            // 2. 进入后体重是否合法
            Direction requestDir = getRequestDir(request);
            if (direction != Direction.NULL
                    && requestDir != direction) {
                continue;
            }
            if (!canAddWeight(request.getWeight())) {
                continue;
            }
            //合法：进入电梯
            singleGoIn(request, floor);
        }
    }

    private Direction getRequestDir(PersonRequest request) {
        int to = Config.changeStringToFloor(request.getToFloor());
        int from = Config.changeStringToFloor(request.getFromFloor());
        return to > from ? Direction.UP : Direction.DOWN;
    }

    private void singleGoIn(PersonRequest request, int floor) {
        /* 接受inTask, 转移到outTask, 更新重量, 输出 */
        changeTask(request);
        weight += request.getWeight();
        Output.printGoIn(request.getPersonId(), floor, elevatorId);
    }

    private void goOut(int floor) {
        /* 处理当前楼层的离开请求,更新重量 */
        // 判断当前楼层有无out请求
        List<PersonRequest> list = outTask.get(floor);

        // 没有：快速失败
        if (list.isEmpty()) {
            return;
        }

        // 有：离开电梯、输出
        List<PersonRequest> copy = new ArrayList<>(list);
        for (PersonRequest request : copy) {
            singleGoOut(request, floor);
        }
    }

    private void singleGoOut(PersonRequest request, int floor) {
        /* 完成outTask, 更新重量, 输出 */
        finishRequest(request);
        weight -= request.getWeight();
        Output.printGoOut(request.getPersonId(), floor, elevatorId);
    }

    /* ----- 任务分派逻辑 ----- */

    public synchronized void addTask(PersonRequest request) {
        /* 向任务列表中添加一个任务 request */
        task.add(request);
        receiveTask(); // 目前直接receive即可
    }

    public synchronized void receiveTask() {
        /* receive所有任务, 加入inTask队列, 输出 */
        while (!task.isEmpty()) {
            PersonRequest request = task.poll();
            int floor = Config.changeStringToFloor(request.getFromFloor());
            inTask.get(floor).add(request);
            Output.printReceive(request.getPersonId(), elevatorId);
            refreshTargetSingleFloor(floor);
        }
        notifyAll();
    }

    private void changeTask(PersonRequest request) {
        /* 将 request 从 inTask 转移到 outTask */
        int to = Config.changeStringToFloor(request.getToFloor());
        int from = Config.changeStringToFloor(request.getFromFloor());
        List<PersonRequest> inList = inTask.get(from);
        inList.remove(request);
        List<PersonRequest> outList = outTask.get(to);
        outList.add(request);
    }

    private void finishRequest(PersonRequest request) {
        /* 将请求从outTask中删除 */
        int to = Config.changeStringToFloor(request.getToFloor());
        List<PersonRequest> outList = outTask.get(to);
        outList.remove(request);
    }

    private boolean canAddWeight(int addWeight) {
        return weight + addWeight <= Config.ELEVATOR_MAX_WEIGHT;
    }

    private void refreshTargetFloor() {
        /* 完全更新targetFloor */
        for (int i = Config.ELEVATOR_FLOOR_MIN;
             i <= Config.ELEVATOR_FLOOR_MAX; i++) {
            refreshTargetSingleFloor(i);
        }
    }

    private void refreshTargetSingleFloor(int floor) {
        /* 单层更新targetFloor */
        // 检查inTask和outTask中该楼层的list是否为空
        boolean space;
        space = inTask.get(floor).isEmpty()
                && outTask.get(floor).isEmpty();
        if (space) { // 皆空：删除
            targetFloor.remove(floor);
        } else { // 否则：加入
            targetFloor.add(floor);
        }
    }

    public synchronized boolean isEnd() {
        // 所有任务结束
        refreshTargetFloor();
        return weight == 0
                && targetFloor.isEmpty()
                && Config.isScheduleFinished();
    }

    synchronized void waitForTask() throws InterruptedException {
        while (targetFloor.isEmpty()
                && weight == 0
                && !Config.isScheduleFinished()) {
            wait();
        }
        refreshTargetFloor();
    }

    /* ----- 运动调控逻辑 ----- */

    public synchronized Direction decideNextDirection(int curFloor, Direction curDir) {
        /* 根据当前楼层、当前方向和targetFloor, 确定下一次运行的方向 */
        Integer up = targetFloor.ceiling(curFloor + 1);
        Integer down = targetFloor.floor(curFloor - 1);

        // 无目标：空闲
        if (up == null && down == null && !haveUp(curFloor) && !haveDown(curFloor)) {
            return Direction.NULL;
        }

        // 当前方向为空：确定一个方向
        if (curDir == Direction.NULL) {
            if (up != null && down != null) {
                // 上下都有目标，选近的
                return (up - curFloor) <= (curFloor - down) ? Direction.UP : Direction.DOWN;
            } else if (up != null) {
                return Direction.UP;
            } else {
                return Direction.DOWN;
            }
        }

        // 当前方向不为空：转向/保持
        // 转向：当前方向上没有任务：targetFloor和当前楼层的请求都没有
        if (curDir == Direction.UP && up == null && !haveUp(curFloor)) {
            return Direction.DOWN;
        }
        if (curDir == Direction.DOWN && down == null && !haveDown(curFloor)) {
            return Direction.UP;
        }
        return curDir; // 保持
    }

    private synchronized boolean haveUp(int curFloor) {
        /* 判断当前楼层是否有up请求 */
        for (PersonRequest request : inTask.get(curFloor)) {
            if (getRequestDir(request) == Direction.UP) {
                return true;
            }
        }
        return false;
    }

    private synchronized boolean haveDown(int curFloor) {
        /* 判断当前楼层是否有down请求 */
        for (PersonRequest request : inTask.get(curFloor)) {
            if (getRequestDir(request) == Direction.DOWN) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isTarget(int floor) {
        return targetFloor.contains(floor);
    }

}
