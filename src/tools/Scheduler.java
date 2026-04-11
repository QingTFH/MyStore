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
                    Elevator elevator = shared.getElevator(
                            ((MaintRequest) request).getElevatorId()); // 位置在id-1
                    elevator.setMaintain((MaintRequest) request);
                } else { // personRequest，暂时使用均匀分配
                    Elevator elevator;
                    do {
                        elevator = shared.getElevator(cnt);
                        cnt = (cnt) % Config.ELEVATOR_NUM + 1;
                    } while (elevator.isMaintain()); // 如果在检修，换下一台
                    // 如果都在检修怎么办?
                    elevator.addTask((PersonRequest) request);
                    DebugOutput.dispatchTask(cnt);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
