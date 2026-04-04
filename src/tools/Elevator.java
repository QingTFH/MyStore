package tools;

import com.oocourse.elevator1.PersonRequest;
import io.Output;
import main.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public class Elevator implements Runnable {
    /*
     * 一个电梯
     * 移动一层->检查该楼层请求的Map/List->处理请求(->移动一层)
     */

    private final Integer DIRECTION_UP = 1;
    private final Integer DIRECTION_DOWN = 2;
    private final Integer DIRECTION_NULL = 0;
    private final boolean DOOR_CLOSED = false;
    private final boolean DOOR_OPENED = true;

    private final int id; // 初始化为非法值
    private int weight = 0;

    private final Object lock = new Object(); // inTask & targetFloor
    private final HashMap<Integer,List<PersonRequest>> inTask
            = new HashMap<>(); // 申请进入电梯的任务
    private final HashMap<Integer,List<PersonRequest>> outTask
            = new HashMap<>(); // 申请离开电梯的任务

    private final TreeSet<Integer> targetFloor = new TreeSet<>(); // 目标楼层,包含：inTask的“From楼层” 和 outTask的"To楼层"

    private Integer pos = 1; // -4~-1 1~7 共7层 -> 压缩至-3~7 其中-3~0分别表示-4~-1层
    private Integer direction = DIRECTION_NULL;
    private boolean door = DOOR_CLOSED;

    public Elevator(int id) {
        this.id = id;

        //初始化list,防止null
        for(int i=-3; i<=7; i++) {
            List<PersonRequest> inList = new ArrayList<>();
            inTask.put(i,inList);
            List<PersonRequest> outList = new ArrayList<>();
            outTask.put(i,outList);
        }
    }

    public void run() {
        while(true) {
            try{
                // 判断是否还有后续任务
                // 无任务,结束
                if(checkEnd()) { // 任务结束
                    break;
                }
                // 还有后续任务
                synchronized (lock) {

                    //但是还未收到，等待
                    if (targetFloor.isEmpty() && !Config.scheduleFinished) {
                        lock.wait();
                    }
                    if(checkEnd()) {
                        break;
                    }

                    // 检测当前楼层是否是目标楼层
                    // 如果是，开门放行，关门
                    if(targetFloor.contains(pos)) {
                        openDoor();
                        goOut();
                        goIn(); // 内含checkWeight逻辑，保证电梯不超重
                        closeDoor();
                    }

                    // 移动一个楼层
                    move(); // 内置setDirection
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addInTask(PersonRequest request) throws InterruptedException {
        int fromFloor = Config.changeStringToFloor(request.getFromFloor());
        synchronized (lock) {
            List<PersonRequest> list = inTask.getOrDefault(fromFloor,new ArrayList<>());
            list.add(request);
            inTask.put(fromFloor,list);

            targetFloor.add(fromFloor);

            Output.printReceive(request.getPersonId(),id);

            lock.notifyAll();
        }
    }

    private void changeTask(PersonRequest request) {
        // 将 request 从 inTask 转移到 outTask，修正targetFloor
        int fromFloor = Config.changeStringToFloor(request.getFromFloor());
        int toFloor = Config.changeStringToFloor(request.getToFloor());

        synchronized (lock) {
            // 合法检查: 判断request是否在inTask中
            List<PersonRequest> listIn = inTask.get(fromFloor);
            if(listIn == null
                    || !listIn.contains(request)) {
                return;
            }

            // 将request从 inTask 转移到 outTask
            List<PersonRequest> listOut = outTask.getOrDefault(toFloor,new ArrayList<>());
            listIn.remove(request);
            listOut.add(request);
            outTask.put(toFloor,listOut);

            // 处理targetFloor
            targetFloor.remove(fromFloor);
            targetFloor.add(toFloor);
        }
    }

    private void freeOutTask(PersonRequest request) {
        // 将 request 从 outTask|targetFloor 移除
        int toFloor = Config.changeStringToFloor(request.getToFloor());
        List<PersonRequest> list = outTask.get(toFloor);
        if(list == null) {
            return;
        }

        list.remove(request);
        outTask.put(toFloor,list);

        targetFloor.remove(toFloor);
    }

    private void move() throws InterruptedException { // 只是“向上/下移动一层”
        setDirection();
        if(direction.equals(DIRECTION_NULL)) {
            return; // 快速失败
        }
        if(door == DOOR_OPENED) {
            closeDoor(); // ?
        }

        pos +=  direction.equals(DIRECTION_UP) ? 1 : -1 ;

        // 输出逻辑:先移动到位，再输出

        Thread.sleep(400); // 移动一层要0.4s,对应sleep(400)

        Output.printArrive(pos,id);
    }

    private void openDoor() {
        door = DOOR_OPENED;

        Output.printOpen(pos,id);
    }

    private void closeDoor() throws InterruptedException {
        // 补足时间
        Thread.sleep(400);
        door = DOOR_CLOSED;
        Output.printClose(pos,id);
    }

    private void goIn() throws InterruptedException {

        // 有请求：处理进入请求
        synchronized(lock) {
            // 检查这个楼层是否有进入请求
            List<PersonRequest> list = inTask.get(pos);

            // 无请求：快速失败
            if(list == null) {
                return;
            }

            List<PersonRequest> copy = new ArrayList<>(list);
            for(PersonRequest request : copy) {
                //遍历list: 这个request goIn，体重是否合法
                int wei = request.getWeight();

                //不合法：下一个request
                if(!checkWeight(wei+weight)) {
                    continue;
                }

                //合法： 允许进入电梯
                singleGoIn(request);
            }
        }

        //出循环时、重量合法
    }

    private void singleGoIn(PersonRequest request) {
        //判断这个request上电梯是否合法：
        int wei = request.getWeight();
        //不合法：快速失败
        if(!checkWeight(wei+weight)) {
            return;
        }

        //合法：进入电梯 inTask -> outTask,处理targetFloor
        changeTask(request); // 内置targetFloor处理
        weight += wei;

        Output.printGoIn(request.getPersonId(),pos,id);
    }

    private void goOut() {
        //判断当前楼层有无out请求
        List<PersonRequest> list = outTask.get(pos);

        //没有：快速失败
        if(list == null || list.isEmpty()) {
            return;
        }

        List<PersonRequest> copy = new ArrayList<>(list);
        //有：完成请求、输出逻辑、hashMap/treeSet逻辑
        for(PersonRequest request : copy) {
            singleGoOut(request);
        }
    }

    private void singleGoOut(PersonRequest request) {
        // 完成请求
        freeOutTask(request);
        weight -= request.getWeight();

        Output.printGoOut(request.getPersonId(),pos,id);
    }

    private boolean checkWeight(int weight) {
        return weight <= Config.ELEVATOR_MAX_WEIGHT;
    }


    private Integer getTarget() { // 获取当前方向上的目标楼层
        if(Objects.equals(direction, DIRECTION_UP)) {
            return targetFloor.ceiling(pos+1); // 需要排除本楼层吗？
        } else {
            return targetFloor.floor(pos-1);
        }
    }

    private void setDirection() {
        // 初始方向
        if(direction.equals(DIRECTION_NULL)) {
            direction = DIRECTION_UP;
        }

        // 判断当前方向上有无目标
        // 有: 保持方向
        if(getTarget() != null) {
            return;
        }
        // 无: 调转方向
        if(direction.equals(DIRECTION_UP)) {
            direction = DIRECTION_DOWN;
        } else {
            direction = DIRECTION_UP;
        }

        // 再次检查
        // 有: 保持方向
        if(getTarget() != null) {
            return;
        }
        // 无: 待机
        direction = DIRECTION_NULL;
    }

    private boolean checkEnd() {
        return targetFloor.isEmpty()
                && weight == 0
                && Config.scheduleFinished;
    }

    Object getlock() {
        return lock;
    }

}
