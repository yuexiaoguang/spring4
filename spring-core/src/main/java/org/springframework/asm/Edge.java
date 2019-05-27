package org.springframework.asm;

/**
 * 方法体的控制流图中的边缘. See {@link Label Label}.
 */
class Edge {

    /**
     * 表示正常的控制流图边缘.
     */
    static final int NORMAL = 0;

    /**
     * 表示对应于异常处理器的控制流图边缘.
     * 更确切地说, {@link #info}严格为正的任何{@link Edge}都对应于异常处理器.
     * {@link #info}的实际值是{@link ClassWriter}类型表中捕获的异常的索引.
     */
    static final int EXCEPTION = 0x7FFFFFFF;

    /**
     * 有关此控制流图边缘的信息.
     * 如果使用{@link ClassWriter#COMPUTE_MAXS}, 则此字段是此边缘来自的基础块中的(相对)堆栈大小.
     * 此大小等于此边缘对应的"jump"指令的堆栈大小, 相对于原始基础块开头的堆栈大小.
     * 如果使用{@link ClassWriter#COMPUTE_FRAMES}, 则此字段是此控件流图边缘的类型 (i.e. NORMAL 或 EXCEPTION).
     */
    int info;

    /**
     * 此边缘来自的基础块的后继块.
     */
    Label successor;

    /**
     * 原始基础块的后继列表中的下一个边缘.
     * See {@link Label#successors successors}.
     */
    Edge next;
}
