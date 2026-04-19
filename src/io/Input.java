package io;

import com.oocourse.elevator3.ElevatorInput;
import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.Request;
import main.Shared;

import java.io.IOException;

public class Input implements Runnable {

    public Input() {
    }

    @Override
    public void run() {
        try (ElevatorInput input = new ElevatorInput(System.in)) {
            Shared shared = Shared.getShared();
            while (true) {
                Request curRequest = input.nextRequest();

                if (curRequest == null) { // input结束
                    shared.inputEnd();
                    DebugOutput.inputEnd();
                    break;
                }

                shared.addPending(curRequest);
                if(curRequest instanceof PersonRequest) {
                    Shared.getShared().addRequest();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
