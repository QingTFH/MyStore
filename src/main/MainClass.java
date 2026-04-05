package main;

import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.TimableOutput;
import io.Input;
import tools.Elevator;
import tools.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainClass {

    public static void main(String[] args) {
        TimableOutput.initStartTimestamp(); // 初始化时间戳

        BlockingQueue<PersonRequest> taskQueue
                = new LinkedBlockingQueue<>(); // 初始化任务共享队列

        Thread input = new Thread(new Input(taskQueue)); // 初始化输入线程

        List<Elevator> elevators = new ArrayList<>(Config.ELEVATOR_NUM); // 初始化电梯组、启动对应线程
        for (int i = 0; i < Config.ELEVATOR_NUM; i++) {
            Elevator elevator = new Elevator(i + 1);
            elevators.add(elevator);
            new Thread(elevator).start();
        }

        new Thread(new Scheduler(taskQueue, elevators)).start(); // 初始化调度系统

        input.start(); // 启动输入线程，开始操作。
    }
}
