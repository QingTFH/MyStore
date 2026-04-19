package elevator;

import com.oocourse.elevator3.MaintRequest;
import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.Request;
import io.DebugOutput;
import main.Config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Shaft implements Runnable {
    /* 电梯井：
     * 1. 控制双轿厢模式的启动和关闭
     * 2. 接受scheduler分发的任务(二级盘子), 分发给具体轿厢(三级盘子);
     * 3. 向scheduler提供当前电梯的评分(持有降级、统一)
     */

    private static final int MAIN = 1;
    private static final int DEPUTY = 2;

    private final BlockingQueue<Request> pending = new LinkedBlockingQueue<>();
    // 第二级盘子, 生产者: scheduler; 消费者: shaft

    private final int id;
    private final Elevator mainElevator;
    private final Elevator deputyElevator;
    private boolean doubleFlag = false;
    private final Object floorLock = new Object();
    private volatile boolean f2Busy = false;

    public Shaft(int id) {
        this.id = id;
        mainElevator = new Elevator(id);
        deputyElevator = new Elevator(id + Config.SHAFT_NUM);
    }

    @Override
    public void run() {
        startElevator(MAIN);
        Request request;
        try { // 获取任务->检查结束->根据任务类型操作
            while (true) {
                request = getTask();

                if (request == Config.POISON) {
                    break;
                }

                schedule(request);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            end();
        }

    }

    private void end() {
        /* 收尾工作 */
        DebugOutput.shaftEnd(id);
        endElevator(MAIN);
        endElevator(DEPUTY);
    }

    private void startElevator(int i) {
        if (i == MAIN) {
            new Thread(mainElevator).start();
        } else if (i == DEPUTY) {
            new Thread(deputyElevator).start();
        }
    }

    private void endElevator(int i ) {
        if (i == MAIN) {
            mainElevator.addTask(Config.POISON);
        } else if (i == DEPUTY) {
            deputyElevator.addTask(Config.POISON);
        }
    }

    public void addTask(Request request) {
        /* 向第二级盘子里增加任务 */
        pending.add(request);
    }

    private Request getTask() throws InterruptedException {
        /* 从第二级盘子获得任务 */
        return pending.take(); // 调度器可以用take, 但是电梯只能用poll
    }

    private void schedule(Request request) throws InterruptedException {
        /* 依据任务类型, 调度所需功能 */
        switch (request.getClass().getSimpleName()) {
            case ("MaintRequest"):
                mainElevator.setMaintain((MaintRequest) request);
                break;
            case ("UpdateRequest"):
                try {
                    setDouble();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            case ("RecycleRequest"):
                endDouble();
                break;
            case ("PersonRequest"):
                dispatch((PersonRequest) request);
                break;
            default:
                DebugOutput.getOtherRequest(request);
                break;
        }
    }

    private void dispatch(PersonRequest request) {
        /* 根据任务需求，分配到指定电梯的三级盘子 */
        if(!doubleFlag) { // 无需协作
            mainElevator.addTask(request);
            return;
        }

        int fromFloor = Config.changeStringToFloor(request.getFromFloor());
        int toFloor = Config.changeStringToFloor(request.getToFloor());

        boolean assignToMain = (fromFloor > 2)
                || (fromFloor == 2 && toFloor > 2);

        if(assignToMain) { // main
            mainElevator.addTask(request);
        } else {
            deputyElevator.addTask(request);
        }

    }

    private void setDouble() throws InterruptedException {
        /* 启动双轿厢模式 */
        DebugOutput.receiveUpdate(id);

        // 轿厢改造
        mainElevator.setAsMain();

        // 启动副轿厢
        deputyElevator.setAsDeputy();
        startElevator(DEPUTY);

        doubleFlag = true;
    }

    private void endDouble() throws InterruptedException {
        /* 结束双轿厢模式 */
        DebugOutput.receiveRecycle(id);

        // 副轿厢改造
        deputyElevator.deputyOver();
        endElevator(DEPUTY);

        // 副轿厢结束
        mainElevator.mainOver();

        doubleFlag = false;
    }

    Object getFloorLock() {
        return floorLock;
    }

    boolean isF2Busy() {
        return f2Busy;
    }

    void setF2Busy() {
        f2Busy = true;
    }

    void resetF2Busy() {
        f2Busy = false;
    }

}
