package tools;

import com.oocourse.elevator3.MaintRequest;
import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.RecycleRequest;
import com.oocourse.elevator3.Request;
import com.oocourse.elevator3.UpdateRequest;
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
        while (true) {
            try {
                Request request = Shared.getShared().pollPending();

                if (request == null) { // AllFinished,任务结束
                    end();
                    break;
                }

                // 根据request类型, 分派任务
                dispatch(request);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void end() { // scheduler应该结束的标志：input结束，并且电梯没有任务了
        Shared.getShared().scheduleEnd();
        DebugOutput.schedulerEnd();
    }

    private void dispatch(Request request) {
        switch (request.getClass().getSimpleName()){
            case("MaintRequest") :
            case("UpdateRequest") :
            case("RecycleRequest") :
                dispatchToShaft(request,
                        (getShaftId(request)));
                break;
            case("PersonRequest") :
                dispatchPersonRequest((PersonRequest) request);
                break;
            default:
                DebugOutput.getOtherRequest(request);
                break;
        }
    }

    private int getShaftId(Request request) {
        if (request instanceof MaintRequest) {
            return ((MaintRequest) request).getElevatorId();
        } else if (request instanceof UpdateRequest) {
            return ((UpdateRequest) request).getElevatorId();
        } else if (request instanceof RecycleRequest) {
            return ((RecycleRequest) request).getElevatorId() - Config.SHAFT_NUM;
        }
        return 0;
    }

    private void dispatchToShaft(Request request,int elevatorId) {
        Shared.getShared()
                .getShaft(elevatorId)
                .addTask(request);
    }

    private void dispatchPersonRequest(PersonRequest request) {
        int mode = 3;
        switch (mode) {
            case(1) : {
//        Elevator elevator = getBestElevator(request); // 影子电梯
//        elevator.addTask(request);
//        DebugOutput.dispatchTask(elevator.getId());
            }

            case(2) : { // 均匀分配
                Shared.getShared()
                        .getShaft(cnt)
                        .addTask(request);
                cnt = (cnt) % Config.SHAFT_NUM + 1;
                DebugOutput.dispatchTask(cnt);
            } break;

            case(3) : { // 固定给1号电梯分配
                Shared.getShared()
                        .getShaft(1)
                        .addTask(request);
                DebugOutput.dispatchTask(1);
            } break;

        }





    }

//    private Elevator getBestElevator(PersonRequest request) {
//        Shared shared = Shared.getShared();
//        Elevator elevator = null;
//        int costTime = Integer.MAX_VALUE;
//
//        for (int id = 1; id <= Config.SHAFT_NUM; id++) {
//            Elevator e = shared.getElevator(id);
//            int time = e.newShadow()
//                    .addTask(request)
//                    .simulate(); // 模拟完成时间
//            DebugOutput.simulate(id, time);
//            if (time < costTime) {
//                costTime = time;
//                elevator = e;
//                DebugOutput.simulateWin(id, time);
//            }
//        }
//
//        if (elevator == null) { // 避免出异常，此时均匀分配
//            elevator = shared.getElevator(cnt);
//            cnt = (cnt % Config.SHAFT_NUM) + 1;
//        }
//
//        return elevator;
//    }


}
