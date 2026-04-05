package io;

import com.oocourse.elevator1.ElevatorInput;
import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;
import main.Config;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class Input implements Runnable {

    private final BlockingQueue<PersonRequest> taskQueue;

    public Input(BlockingQueue<PersonRequest> taskQueue) {
        this.taskQueue = taskQueue; // 共享一个任务队列,input是“生产者”
    }

    @Override
    public void run() {
        try (ElevatorInput input = new ElevatorInput(System.in)) {
            while (true) {
                Request curRequest = input.nextRequest();
                if (curRequest == null) {
                    Config.inputFinished();
                    break;
                } else {
                    // a new valid request
                    if (curRequest instanceof PersonRequest) {
                        PersonRequest personRequest = (PersonRequest) curRequest;
                        try {
                            taskQueue.put(personRequest); // put方法阻塞添加任务
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
