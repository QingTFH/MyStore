package io;

import main.Config;

public class DebugOutput {

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

    public static void elevatorEnd(int id) {
        if (Config.DEBUG) {
            System.out.println("elevator end " + id);
        }
    }

}
