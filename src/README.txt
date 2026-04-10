20260410_v4
修正电梯运行的顺序：
原：setDirection -> process -> move
现：process -> setDirection -> move，move后如果碰壁立刻设置方向为NULL
每到一层，先更新targetFloor表，再进行setD，否则可能会按照旧的targetFloor进行后续移动