package org.springframework.asm;

/**
 * 有关在{@link ClassReader}中解析的类的信息.
 */
class Context {

    /**
     * 必须为此类解析的属性的原型.
     */
    Attribute[] attrs;

    /**
     * 用于解析此类的{@link ClassReader}选项标志.
     */
    int flags;

    /**
     * 用于读取字符串的缓冲区.
     */
    char[] buffer;

    /**
     * 每个bootstrap方法的起始索引.
     */
    int[] bootstrapMethods;

    /**
     * 当前正在解析的方法的访问标志.
     */
    int access;

    /**
     * 当前正在解析的方法的名称.
     */
    String name;

    /**
     * 当前正在解析的方法的描述符.
     */
    String desc;

    /**
     * 由当前正在解析的方法的字节码偏移进行索引的标签对象
     * (只有需要标签的字节码偏移量才有非null关联的Label对象).
     */
    Label[] labels;

    /**
     * 当前正在解析的类型注解的目标.
     */
    int typeRef;

    /**
     * 当前正在解析的类型注解的路径.
     */
    TypePath typePath;

    /**
     * 已解析的最新堆栈映射帧的偏移量.
     */
    int offset;

    /**
     * 当前正在解析的局部变量类型注解中, 与局部变量范围的开始相对应的标签.
     */
    Label[] start;

    /**
     * 当前正在解析的局部变量类型注解中, 与局部变量范围的结尾相对应的标签.
     */
    Label[] end;

    /**
     * 当前正在解析的局部变量类型注解中, 每个局部变量范围的局部变量索引.
     */
    int[] index;

    /**
     * 已解析的最新堆栈映射帧的编码.
     */
    int mode;

    /**
     * 已解析的最新堆栈映射帧中的本地数量.
     */
    int localCount;

    /**
     * 已解析的最新堆栈映射帧中的本地数量, 减去前一帧中的本地数量.
     */
    int localDiff;

    /**
     * 已解析的最新堆栈映射帧的本地值.
     */
    Object[] local;

    /**
     * 已解析的最新堆栈映射帧的堆栈大小.
     */
    int stackCount;

    /**
     * 已解析的最新堆栈映射帧的堆栈值.
     */
    Object[] stack;
}