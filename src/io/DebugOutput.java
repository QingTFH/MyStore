package io;

import com.oocourse.elevator3.Request;
import main.Config;

public class DebugOutput {

    public static void exception(String a) {
        if (Config.DEBUG) {
            System.out.println("-----------Exception :: " + a + " !------------");
        }
    }

    public static void cantDo() {
        if (Config.DEBUG) {
            System.out.println("zan wei wan gong!!!!!!!!!!");
        }
    }

    public static void inputEnd() {
        if (Config.DEBUG) {
            System.out.println("InputThread End");
        }
    }

    public static void addPending() {
        if (Config.DEBUG) {
            System.out.println("Input Task, Shared::add Pending");
        }
    }

    public static void schedulerEnd() {
        if (Config.DEBUG) {
            System.out.println("scheduler thread end");
        }
    }

    public static void dispatchTask(int id) {
        if (Config.DEBUG) {
            System.out.println("Task schedule to id:" + id);
        }
    }

    public static void dispatchTask(int eid, int shaftId) {
        if (Config.DEBUG) {
            System.out.println("Shaft " + shaftId + " schedule to id:" + eid);
        }
    }

    public static void elevatorEnd(int id) {
        if (Config.DEBUG) {
            System.out.println("elevator end " + id);
        }
    }

    public static void simulateWin(int id, int time) {
        if (Config.DEBUG) {
            System.out.println(id + " elevatorSimulate winOFtime " + time);
        }
    }

    public static void simulate(int id, int time) {
        if (Config.DEBUG) {
            System.out.println(id + " id simulate" + time);
        }
    }

    public static void elevatorSpace(int id) {
        if (Config.DEBUG) {
            System.out.println(id + " elevator is space ");
        }
    }

    public static void waitForPoll() {
        if (Config.DEBUG) {
            System.out.println("wait for poll");
        }
    }

    public static void newShadow(int id) {
        if (Config.DEBUG) {
            System.out.println(id + " id elevator new shadow");
        }
    }

    public static void getOtherRequest(Request request) {
        if (Config.DEBUG) {
            System.out.println("Other Request Class: " + request.getClass().toString());
        }
    }

    public static void shaftEnd(int id) {
        if (Config.DEBUG) {
            System.out.println(id + " Shaft End");
        }
    }

    public static void receiveUpdate(int id) {
        if (Config.DEBUG) {
            System.out.println(id + " shaft Receive Update");
        }
    }

    public static void receiveRecycle(int id) {
        if (Config.DEBUG) {
            System.out.println(id + " shaft Receive Recycle");
        }
    }

    public static void elevatorStart(int id) {
        if (Config.DEBUG) {
            System.out.println(id + " elevator Start");
        }
    }

}
