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
他妈的怎么改了巨量WA出来
发现haveUP和haveDOWN时，可能抛空指针--->电梯运行会超出边界--->setDirection有问题？
无论如何，重新梳理一遍LOOK策略的做法
1.如果当前有运动方向：
	1.保持方向运动，接受所有沿路楼层的同向请求，直到(前方没有请求 且 当前楼层没有同向请求)时转向
	2.移动后立刻转向
2.如果当前没有运动方向：
	- 获取任意一个targetFloor作为初始方向