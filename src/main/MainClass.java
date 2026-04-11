package main;

import com.oocourse.elevator2.TimableOutput;
import io.Input;
import tools.Scheduler;

public class MainClass {

    public static void main(String[] args) {
        TimableOutput.initStartTimestamp(); // 初始化时间戳
        Shared shared = Shared.getShared(); // 初始化共享资源
        Thread input = new Thread(new Input()); // 初始化输入线程
        new Thread(new Scheduler()).start(); // 初始化调度系统
        shared.startElevators(); // 启动电梯线程
        input.start();// 启动输入线程
    }
}
