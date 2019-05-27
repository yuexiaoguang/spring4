package org.springframework.asm;

/**
 * 有关方法"current"指令的输入堆栈映射帧的信息.
 * 这是作为仅包含一条指令的"基础块"的Frame子类实现的.
 */
class CurrentFrame extends Frame {
 
    /**
     * 将此CurrentFrame设置为下一个"current"指令的输入堆栈映射帧,
     * i.e. 在给定的指令之后的指令.
     * 假设在调用此方法时, 该对象的值是在执行给定指令之前的堆栈映射帧状态.
     */
    @Override
    void execute(int opcode, int arg, ClassWriter cw, Item item) {
        super.execute(opcode, arg, cw, item);
        Frame successor = new Frame();
        merge(cw, successor, 0);
        set(successor);
        owner.inputStackTop = 0;
    }
}
