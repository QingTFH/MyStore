package main;

import com.oocourse.elevator3.Request;
import elevator.Shaft;
import io.DebugOutput;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Shared {
    /* 线程间的共享资源 */

    private final AtomicInteger remainingRequest = new AtomicInteger(0);

    private volatile boolean inputFinished = false;

    private final LinkedList<Request> pendingTasks = new LinkedList<>(); // 未被receive的任务
    private final Object lockPendingTasks = new Object();

    private final List<Shaft> shafts = new ArrayList<>(Config.SHAFT_NUM);

    /*----- 单例模式 -----*/

    private static Shared shared = null;

    private Shared() {
        for (int id = 1; id <= Config.SHAFT_NUM; id++) {
            shafts.add(new Shaft(id));
        }
    }

    public void startShaft() {
        for (Shaft shaft : shafts) {
            new Thread(shaft).start();
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
                DebugOutput.waitForPoll();
                lockPendingTasks.wait(); // 取不到任务，但是可能还有任务
            }
            return pendingTasks.poll(); // 如果依然是空的，会返回null-->AllFinished
        }
    }

    public Shaft getShaft(int id) {
        return shafts.get(id - 1);
    }

    /*----- 结束判断 -----*/

    public void inputEnd() { // 第一级盘子的生产者不唯一，不适合用毒丸，需要用flag + notify
        inputFinished = true;
        synchronized (lockPendingTasks) {
            lockPendingTasks.notifyAll();
        }
    }

    public boolean shouldSchedulerEnd() { // input结束 并且 所有任务都完成
        return inputFinished && isAllRequestFinish();
    }

    public void scheduleEnd() {
        for (Shaft shaft : shafts) {
            shaft.addTask(Config.POISON);
        }
    }

    public void addRequest() {
        remainingRequest.addAndGet(1);
    }

    public void finishRequest() {
        remainingRequest.addAndGet(-1);
        synchronized (lockPendingTasks) {
            lockPendingTasks.notifyAll();
        }
    }

    public boolean isAllRequestFinish() {
        return remainingRequest.get() == 0;
    }

}
