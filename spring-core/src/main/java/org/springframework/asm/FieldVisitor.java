package org.springframework.asm;

/**
 * 访问Java字段的访问器. 必须按以下顺序调用此类的方法:
 * ( <tt>visitAnnotation</tt> | <tt>visitTypeAnnotation</tt> | <tt>visitAttribute</tt> )* <tt>visitEnd</tt>.
 */
public abstract class FieldVisitor {

    /**
     * 此访问器实现的ASM API版本.
     * 该字段的值必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}之一.
     */
    protected final int api;

    /**
     * 此访问器必须委托方法调用的字段访问器. 可能是 null.
     */
    protected FieldVisitor fv;

    /**
     * @param api
     *            此访问器实现的ASM API版本.
     *            必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}之一.
     */
    public FieldVisitor(final int api) {
        this(api, null);
    }

    /**
     * @param api
     *            此访问器实现的ASM API版本.
     *            必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}之一.
     * @param fv
     *            此访问器必须委托方法调用的字段访问器. 可能是 null.
     */
    public FieldVisitor(final int api, final FieldVisitor fv) {
        if (api < Opcodes.ASM4 || api > Opcodes.ASM6) {
            throw new IllegalArgumentException();
        }
        this.api = api;
        this.fv = fv;
    }

    /**
     * 访问字段的注解.
     * 
     * @param desc
     *            注解类的类描述符.
     * @param visible
     *            <tt>true</tt>如果注解在运行时可见.
     * 
     * @return 访问注解值的访问器, 或<tt>null</tt>如果此访问器对访问此注解不感兴趣.
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (fv != null) {
            return fv.visitAnnotation(desc, visible);
        }
        return null;
    }

    /**
     * 访问字段的类型的注解.
     * 
     * @param typeRef
     *            对注解类型的引用.
     *            此类型引用的类型必须是{@link TypeReference#FIELD FIELD}. See {@link TypeReference}.
     * @param typePath
     *            'typeRef'中带注解的类型参数, 通配符绑定, 数组元素类型, 或静态内部类型的路径.
     *            如果注解将'typeRef'作为一个整体, 则可以是<tt>null</tt>.
     * @param desc
     *            注解类的类描述符.
     * @param visible
     *            <tt>true</tt>如果注解在运行时可见.
     * 
     * @return 访问注解值的访问器, 或<tt>null</tt>如果此访问器对访问此注解不感兴趣.
     */
    public AnnotationVisitor visitTypeAnnotation(int typeRef,
            TypePath typePath, String desc, boolean visible) {
		/* SPRING PATCH: REMOVED FOR COMPATIBILITY WITH CGLIB 3.1
        if (api < Opcodes.ASM5) {
            throw new RuntimeException();
        }
        */
        if (fv != null) {
            return fv.visitTypeAnnotation(typeRef, typePath, desc, visible);
        }
        return null;
    }

    /**
     * 访问该字段的非标准属性.
     * 
     * @param attr
     *            属性.
     */
    public void visitAttribute(Attribute attr) {
        if (fv != null) {
            fv.visitAttribute(attr);
        }
    }

    /**
     * 访问该字段的结尾.
     * 此方法是最后一个要调用的方法, 用于通知访问器已访问该字段的所有注解和属性.
     */
    public void visitEnd() {
        if (fv != null) {
            fv.visitEnd();
        }
    }
}
