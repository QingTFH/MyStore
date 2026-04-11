package main;

import com.oocourse.elevator2.Request;
import elevator.Elevator;
import io.DebugOutput;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Shared {
    /* 线程间的共享资源 */
    /* 其他线程需要读取： */
    /* 其他线程需要写入： */

    private volatile boolean inputFinished = false;
    private volatile boolean scheduleFinished = false;

    private final LinkedList<Request> pendingTasks = new LinkedList<>(); // 未被receive的任务
    private final Object lockPendingTasks = new Object();

    private final List<Elevator> elevators = new ArrayList<>(Config.ELEVATOR_NUM);

    /*----- 单例模式 -----*/

    private static Shared shared = null;

    private Shared() {
        for (int i = 1; i <= Config.ELEVATOR_NUM; i++) {
            elevators.add(new Elevator(i));
        }
    }

    public void startElevators() {
        for (Elevator e : elevators) {
            new Thread(e).start();
        }
    }

    public static Shared getShared() {
        if (shared == null) {
            shared = new Shared();
        }
        return shared;
    }

    /*----- 共享资源调度 -----*/

    public void addPending(Request request) {
        synchronized (lockPendingTasks) {
            DebugOutput.addPending();
            pendingTasks.add(request);
            lockPendingTasks.notifyAll();
        }
    }

    public Request pollPending() throws InterruptedException {
        synchronized (lockPendingTasks) {
            while (pendingTasks.isEmpty()
                    && !shouldSchedulerEnd()) {
                lockPendingTasks.wait(); // 取不到任务，但是可能还有任务
            }
            return pendingTasks.poll(); // 如果依然是空的，会返回null-->AllFinished
        }
    }

    public Elevator getElevator(int id) { // 获得id号电梯
        return elevators.get(id - 1);
    }

    public List<Elevator> getElevatorList() {
        return elevators;
    }

    /*----- 结束判断 -----*/

    public void inputEnd() {
        inputFinished = true;
        synchronized (lockPendingTasks) {
            lockPendingTasks.notifyAll();
        }
    }

    public void elevatorFinish() {
        synchronized (lockPendingTasks) {
            lockPendingTasks.notifyAll();
        }
    }

    public boolean shouldSchedulerEnd() {
        // input结束 并且 所有电梯都没有可能吐出任务了
        if (!inputFinished) {
            return false;
        }
        for (Elevator e : elevators) {
            if (!e.isSpace() || e.isMaintain()) {
                return false;
            }
        }
        return true;
    }

    public void scheduleEnd() {
        scheduleFinished = true;
        for (Elevator e : shared.getElevatorList()) {
            synchronized (e.getTask()) {
                e.getTask().notifyAll();
            }
        }
    }

    public boolean isScheduleEnd() {
        return scheduleFinished;
    }

}
