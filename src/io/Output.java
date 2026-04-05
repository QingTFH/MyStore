package io;

import com.oocourse.elevator1.TimableOutput;
import main.Config;

public class Output {

    public static void printReceive(int personId, int elevatorId) {
        TimableOutput.println("RECEIVE-" + personId + "-" + elevatorId);
    }

    public static void printArrive(int pos, int elevatorId) {
        TimableOutput.println("ARRIVE-" +
                Config.changeFloorToString(pos) + "-" + elevatorId);
    }

    public static void printOpen(int pos, int elevatorId) {
        TimableOutput.println("OPEN-" +
                Config.changeFloorToString(pos) + "-" + elevatorId);
    }

    public static void printClose(int pos, int elevatorId) {
        TimableOutput.println("CLOSE-" +
                Config.changeFloorToString(pos) + "-" + elevatorId);
    }

    public static void printGoIn(int personId, int pos, int elevatorId) {
        TimableOutput.println("IN-" + personId + "-" +
                Config.changeFloorToString(pos) + "-" + elevatorId);
    }

    public static void printGoOut(int personId, int pos, int elevatorId) {
        TimableOutput.println("OUT-S-" + personId + "-" +
                Config.changeFloorToString(pos) + "-" + elevatorId);
    }

}
