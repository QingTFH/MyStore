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