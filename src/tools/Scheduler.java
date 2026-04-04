package tools;

import com.oocourse.elevator1.PersonRequest;
import main.Config;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Scheduler implements Runnable{
    /*
     * 电梯调度、任务分配系统
     */

    final BlockingQueue<PersonRequest> taskQueue; // 共享任务队列
    final List<Elevator> elevators; // 该System可调度的电梯

    public Scheduler(BlockingQueue<PersonRequest> taskQueue, List<Elevator> elevators) {
        this.taskQueue = taskQueue;
        this.elevators = elevators;
    }

    @Override
    public void run() {
        Config.scheduleFinished = false;

        while(true) {
            try {
                if(taskQueue.isEmpty()
                        && Config.inputFinished) { // 任务结束
                    Config.scheduleFinished = true;
                    //System.out.println("scheduler end");
                    for(Elevator e : elevators) {
                        synchronized (e.getlock()) {
                            e.getlock().notifyAll();
                        }
                    }

                    break;
                }

                PersonRequest request = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                if(request == null) { // 每100ms检查一次队列和inputFinished
                    continue;
                }

                //分派任务
                int id = request.getElevatorId();
                Elevator elevator = elevators.get(id-1); // 位置在id-1
                elevator.addInTask(request);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
