package io;

import com.oocourse.elevator2.TimableOutput;
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

    public static void printGoOutS(int personId, int pos, int elevatorId) {
        TimableOutput.println("OUT-S-" + personId + "-" +
                Config.changeFloorToString(pos) + "-" + elevatorId);
    }

    public static void printGoOutF(int personId, int pos, int elevatorId) {
        TimableOutput.println("OUT-F-" + personId + "-" +
                Config.changeFloorToString(pos) + "-" + elevatorId);
    }

    public static void printMaintain1(int elevatorId) {
        TimableOutput.println("MAINT1-BEGIN-" + elevatorId);
    }

    public static void printMaintain2(int elevatorId) {
        TimableOutput.println("MAINT2-BEGIN-" + elevatorId);
    }

    public static void printMaintainEnd(int elevatorId) {
        TimableOutput.println("MAINT-END-" + elevatorId);
    }

}
