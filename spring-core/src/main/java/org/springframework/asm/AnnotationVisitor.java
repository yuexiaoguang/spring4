package org.springframework.asm;

/**
 * 访问Java注解的访问器.
 * 必须按以下顺序调用此类的方法:
 * ( <tt>visit</tt> | <tt>visitEnum</tt> | <tt>visitAnnotation</tt> | <tt>visitArray</tt> )* <tt>visitEnd</tt>.
 */
public abstract class AnnotationVisitor {

    /**
     * 此访问器实现的ASM API版本.
     * 该字段的值必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}之一.
     */
    protected final int api;

    /**
     * 此访问器必须委托方法调用的注解访问器.
     * 可能是 null.
     */
    protected AnnotationVisitor av;

    /**
     * @param api 此访问器实现的ASM API版本.
     * 必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}之一.
     */
    public AnnotationVisitor(final int api) {
        this(api, null);
    }

    /**
     * @param api 此访问器实现的ASM API版本.
     *            必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}之一.
     * @param av 此访问器必须委托方法调用的注解访问器. May be null.
     */
    public AnnotationVisitor(final int api, final AnnotationVisitor av) {
        if (api < Opcodes.ASM4 || api > Opcodes.ASM6) {
            throw new IllegalArgumentException();
        }
        this.api = api;
        this.av = av;
    }

    /**
     * 访问注解的原始值.
     * 
     * @param name 值名称.
     * @param value 实际值, 类型必须是{@link Byte}, {@link Boolean}, {@link Character}, {@link Short},
     *            {@link Integer} , {@link Long}, {@link Float}, {@link Double},
     *            {@link String}, OBJECT的{@link Type}, ARRAY sort.
     *            也可以byte数组, boolean, short, char, int, long, float, double值
     *            (相当于使用{@link #visitArray visitArray}并依次访问每个数组元素, 但更方便).
     */
    public void visit(String name, Object value) {
        if (av != null) {
            av.visit(name, value);
        }
    }

    /**
     * 访问注解的枚举值.
     * 
     * @param name 值的名称.
     * @param desc 枚举类的类描述符.
     * @param value 实际的枚举值.
     */
    public void visitEnum(String name, String desc, String value) {
        if (av != null) {
            av.visitEnum(name, desc, value);
        }
    }

    /**
     * 访问注解的嵌套注解值.
     * 
     * @param name 值的名称.
     * @param desc 嵌套的注解类的类描述符.
     * 
     * @return 访问实际的嵌套注解值的访问器, 或<tt>null</tt> 如果访问器对访问此嵌套注解不感兴趣.
     * <i>在调用此注解访问器的其他方法之前, 必须完全访问嵌套注解值</i>.
     */
    public AnnotationVisitor visitAnnotation(String name, String desc) {
        if (av != null) {
            return av.visitAnnotation(name, desc);
        }
        return null;
    }

    /**
     * 访问注解的数组值.
     * 请注意, 基本类型的数组 (例如 byte, boolean, short, char, int, long, float, double) 可以作为值传递给{@link #visit visit}.
     * 这就是{@link ClassReader}所做的.
     * 
     * @param name 值的名称.
     * @return 访问实际的数组值元素的访问器, 或<tt>null</tt>如果访问器对访问这些值不感兴趣.
     * 传递给此访问器方法的'name'参数将被忽略.
     * <i>在调用此注解访问器的其他方法之前, 必须访问所有数组值</i>.
     */
    public AnnotationVisitor visitArray(String name) {
        if (av != null) {
            return av.visitArray(name);
        }
        return null;
    }

    /**
     * 访问注解的结尾.
     */
    public void visitEnd() {
        if (av != null) {
            av.visitEnd();
        }
    }
}
