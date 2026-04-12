20260410_v4
修正电梯运行的顺序：
原：setDirection -> process -> move
现：process -> setDirection -> move，move后如果碰壁立刻设置方向为NULL
每到一层，先更新targetFloor表，再进行setD，否则可能会按照旧的targetFloor进行setD

v5
其实电梯移动完后就应该立刻根据targetFloor和当前楼层“有哪些请求”来设定自己的方向了：如果有同向请求：维持；否则，转向，或者NULL
修正电梯运行的顺序：
v4：process -> setDirection -> move，move后如果碰壁立刻设置方向为NULL
v5：setDirection -> process -> move
setDirection加入了对“当前楼层是否有同向任务”的判断

v6
电梯边界越界

hw6
v1:重构并增加了shared
设计难点：	在pollPending时，先持有pending的锁，再while-wait，判断条件有 "电梯没有运行结束"，需要持有elevatorTask的锁
			在elevatorTask释放receive的任务时，需要先持有自己elevatorTask的锁，再把任务放回pending--->需要持有pending的锁
		两种持有方式相反，可能造成死锁
	思路：将 "电梯没有运行结束"这一只读条件去除锁化，使用volatile spaceFlag表示elevatorTask是否空闲(运行结束)，每次造成targetFloor变动时读取
缓存：Elevator elevator; // 均匀分配方法
                        do {
                            elevator = shared.getElevator(cnt);
                            cnt = (cnt) % Config.ELEVATOR_NUM + 1;
                        } while (elevator.isMaintain()); // 如果在检修，换下一台
                        // 如果都在检修怎么办?
                        elevator.addTask((PersonRequest) request);
                        DebugOutput.dispatchTask(cnt);