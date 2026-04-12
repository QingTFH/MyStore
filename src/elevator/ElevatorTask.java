package elevator;

import com.oocourse.elevator2.MaintRequest;
import com.oocourse.elevator2.PersonRequest;
import io.Output;
import main.Config;
import main.Config.Direction;
import main.Shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ElevatorTask {
    /*一个电梯内部的任务管理器, 处理该电梯所有任务逻辑需要处理"每一层的请求"、处理请求后调度运行方向*/
    /* 除电梯线程外, 其他线程需要读写：task(写)、targetFloor(读) */

    private final BlockingQueue<PersonRequest> task =
            new LinkedBlockingQueue<>(); // 缓存队列，分配了但还没receive的任务
    private final HashMap<Integer, List<PersonRequest>> inTask =
            new HashMap<>(); // 申请进入电梯的任务
    private final HashMap<Integer, List<PersonRequest>> outTask =
            new HashMap<>(); // 申请离开电梯的任务
    private final TreeSet<Integer> targetFloor = new TreeSet<>();

    private final int elevatorId;
    private int weight = 0;

    private volatile boolean maintainFlag = false;

    private volatile boolean spaceFlag = true;

    // 目标楼层,包含：inTask的“From楼层” 和 outTask的"To楼层"

    ElevatorTask(int id) {
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

    synchronized void process(int floor, Direction direction, boolean simulate) {
        /* 处理当前楼层的所有进出请求,更新重量,刷新target */
        //处理离开请求
        goOut(floor, simulate);
        //处理进入请求: 只接受当前运动方向上的进入请求
        goIn(floor, direction, simulate);
        //刷新target
    }

    private void goIn(int floor, Direction direction, boolean simulate) {
        /* 处理当前楼层的进入请求,更新重量 */
        // 检修中, 跳过
        if (maintainFlag) {
            return;
        }
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
            singleGoIn(request, floor, simulate);
        }
    }

    private void singleGoIn(PersonRequest request, int floor, boolean simulate) {
        /* 接受inTask, 转移到outTask, 更新重量, 输出 */
        changeTask(request);
        weight += request.getWeight();
        if (!simulate) {
            Output.printGoIn(request.getPersonId(), floor, elevatorId);
        }
    }

    private void goOut(int floor, boolean simulate) {
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
            singleGoOut(request, floor, simulate);
        }
    }

    private void singleGoOut(PersonRequest request, int floor, boolean simulate) {
        /* 完成outTask, 更新重量, 输出 */
        weight -= request.getWeight();
        finishRequest(request);
        if (!simulate) {
            Output.printGoOutS(request.getPersonId(), floor, elevatorId);
        }
    }

    /* ----- 任务分派逻辑 ----- */

    synchronized void addTask(PersonRequest request) {
        /* 向任务列表中添加一个任务 request */
        task.add(request);
        notifyAll();
    }

    synchronized void receiveTask(boolean simulate) {
        /* receive所有任务, 加入inTask队列, 输出 */
        while (!task.isEmpty()) {
            PersonRequest request = task.poll();
            int floor = Config.changeStringToFloor(request.getFromFloor());
            inTask.get(floor).add(request);
            if (!simulate) {
                Output.printReceive(request.getPersonId(), elevatorId);
            }
            refreshSingleTargetFloor(floor);
        }
    }

    private void changeTask(PersonRequest request) {
        /* 将 request 从 inTask 转移到 outTask */
        int to = Config.changeStringToFloor(request.getToFloor());
        int from = Config.changeStringToFloor(request.getFromFloor());
        List<PersonRequest> inList = inTask.get(from);
        inList.remove(request);
        List<PersonRequest> outList = outTask.get(to);
        outList.add(request);

        refreshSingleTargetFloor(to);
        refreshSingleTargetFloor(from);
    }

    private void finishRequest(PersonRequest request) {
        /* 将请求从outTask中删除 */
        int to = Config.changeStringToFloor(request.getToFloor());
        List<PersonRequest> outList = outTask.get(to);
        outList.remove(request);
        refreshSingleTargetFloor(to);
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

        spaceFlag = targetFloor.isEmpty();
    }

    synchronized void getTask(boolean simulate) throws InterruptedException {
        // 当前没有任务，没有可接收任务，不在检修中，等待；
        // 有，receive

        while (task.isEmpty()
                && targetFloor.isEmpty()
                && !Shared.getShared().isScheduleEnd()
                && !maintainFlag) {
            Shared.getShared().elevatorFinish(elevatorId);
            wait();
        }

        receiveTask(simulate);
    }

    /* ----- 运动调控逻辑 ----- */

    synchronized Direction decideNextDirection(int curFloor, Direction curDir) {
        /* 根据当前楼层、当前方向和targetFloor, 确定下一次运行的方向 */
        Integer up = targetFloor.ceiling(curFloor + 1);
        Integer down = targetFloor.floor(curFloor - 1);

        // 无目标：空闲
        if (up == null && down == null) {
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

    /*----- 工具方法 -----*/

    private boolean canAddWeight(int addWeight) {
        return weight + addWeight <= Config.ELEVATOR_MAX_WEIGHT;
    }

    private Direction getRequestDir(PersonRequest request) {
        int to = Config.changeStringToFloor(request.getToFloor());
        int from = Config.changeStringToFloor(request.getFromFloor());
        return to > from ? Direction.UP : Direction.DOWN;
    }

    private boolean haveUp(int curFloor) {
        /* 判断当前楼层是否有up请求 */
        for (PersonRequest request : inTask.get(curFloor)) {
            if (getRequestDir(request) == Direction.UP) {
                return true;
            }
        }
        return false;
    }

    private boolean haveDown(int curFloor) {
        /* 判断当前楼层是否有down请求 */
        for (PersonRequest request : inTask.get(curFloor)) {
            if (getRequestDir(request) == Direction.DOWN) {
                return true;
            }
        }
        return false;
    }

    /*----- 电梯读写 -----*/

    synchronized boolean isTarget(int floor) {
        return targetFloor.contains(floor);
    }

    synchronized void setMaintain() {
        this.maintainFlag = true;
    }

    synchronized void rmMaintain() {
        this.maintainFlag = false;
        spaceFlag = targetFloor.isEmpty();
    }

    /*----- 外部读(可能是透传) -----*/

    boolean isSpace() {
        // 目前没有运行中的任务
        return spaceFlag;
    }

    /*----- 外部写(可能是透传) -----*/

    /*----- 检修 -----*/

    synchronized void allGoOut() {
        /* 将outTask全部修改成 [from F1 to toFloor] 回流到pendingTasks */

        for (int i = Config.ELEVATOR_FLOOR_MIN; i <= Config.ELEVATOR_FLOOR_MAX; i++) {
            List<PersonRequest> list = outTask.get(i);
            Iterator<PersonRequest> iterator = list.iterator();
            while (iterator.hasNext()) {
                PersonRequest request = iterator.next();
                weight -= request.getWeight();
                Shared.getShared().addPending(
                        new PersonRequest(
                                "F1",
                                request.getToFloor(),
                                request.getPersonId(),
                                request.getWeight()
                        )
                );
                Output.printGoOutF(request.getPersonId(), 1, elevatorId);
                iterator.remove();
            }
        }

        refreshTargetFloor();
    }

    synchronized void removeReceive() {
        /* 将inTask全部回流到pendingTasks */

        for (int i = Config.ELEVATOR_FLOOR_MIN; i <= Config.ELEVATOR_FLOOR_MAX; i++) {
            List<PersonRequest> list = inTask.get(i);
            Iterator<PersonRequest> iterator = list.iterator();
            while (iterator.hasNext()) {
                PersonRequest request = iterator.next();
                Shared.getShared().addPending(request);
                iterator.remove();
            }
        }

        refreshTargetFloor();
    }

    void workerIn(MaintRequest request) {
        Output.printGoIn(request.getWorkerId(), 1, elevatorId);
    }

    void workerOut(MaintRequest request) {
        Output.printGoOutS(request.getWorkerId(), 1, elevatorId);
    }

    ElevatorTask cloneForShadow() {
        ElevatorTask shadow = new ElevatorTask(elevatorId);
        synchronized (this) {
            shadow.weight = this.weight;
            shadow.maintainFlag = this.maintainFlag;
            // 深拷贝inTask/outTask
            for (int i = Config.ELEVATOR_FLOOR_MIN; i <= Config.ELEVATOR_FLOOR_MAX; i++) {
                shadow.inTask.get(i).addAll(this.inTask.get(i));
                shadow.outTask.get(i).addAll(this.outTask.get(i));
            }
            // 深拷贝task缓存
            shadow.task.addAll(this.task);
            shadow.targetFloor.addAll(this.targetFloor);
        }
        return shadow;
    }

}
