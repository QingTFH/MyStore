package tools;

import com.oocourse.elevator2.MaintRequest;
import com.oocourse.elevator2.PersonRequest;
import com.oocourse.elevator2.Request;
import elevator.Elevator;
import io.DebugOutput;
import main.Config;
import main.Shared;

public class Scheduler implements Runnable {
    /* 电梯调度、任务分配系统 */

    private int cnt = 1; // 1~6

    public Scheduler() {
    }

    @Override
    public void run() {
        // scheduler应该结束的标志：input结束，并且电梯没有任务了
        Shared shared = Shared.getShared();
        while (true) {
            try {
                Request request = shared.pollPending();
                if (request == null) { // AllFinished,任务结束
                    shared.scheduleEnd();
                    DebugOutput.schedulerEnd();
                    break;
                }

                //分派任务
                if (request instanceof MaintRequest) {
                    shared.getElevator(
                                    ((MaintRequest) request).getElevatorId())
                            .setMaintain((MaintRequest) request); // 位置在id-1
                } else { // personRequest，暂时使用均匀分配

                    Elevator elevator = null;
                    int costTime = Integer.MAX_VALUE;
                    for (int id = 1; id <= Config.ELEVATOR_NUM; id++) {
                        Elevator e = shared.getElevator(id);
                        int time = e.newShadow()
                                .addTask((PersonRequest) request)
                                .simulate(); // 模拟完成时间
                        DebugOutput.simulate(id, time);
                        if (time < costTime) {
                            costTime = time;
                            elevator = e;
                            DebugOutput.simulateWin(id, time);
                        }
                    }

                    if (elevator == null) {
                        elevator = shared.getElevator(cnt);
                        cnt = (cnt % Config.ELEVATOR_NUM) + 1;
                    }

                    elevator.addTask((PersonRequest) request);
                    DebugOutput.dispatchTask(elevator.getId());

                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
