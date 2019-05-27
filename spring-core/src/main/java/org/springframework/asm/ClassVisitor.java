package org.springframework.asm;

/**
 * 访问Java类的访问器.
 * 必须按以下顺序调用此类的方法:
 * <tt>visit</tt> [ <tt>visitSource</tt> ] [ <tt>visitModule</tt> ][ <tt>visitOuterClass</tt> ]
 * ( <tt>visitAnnotation</tt> | <tt>visitTypeAnnotation</tt> | <tt>visitAttribute</tt> )*
 * ( <tt>visitInnerClass</tt> | <tt>visitField</tt> | <tt>visitMethod</tt> )* <tt>visitEnd</tt>.
 */
public abstract class ClassVisitor {

    /**
     * 此访问器实现的ASM API版本.
     * 该字段的值必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}之一.
     */
    protected final int api;

    /**
     * 此访问器必须委托方法调用的类访问器. 可能是null.
     */
    protected ClassVisitor cv;

    /**
     * @param api
     *            此访问器实现的ASM API版本.
     *            必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}之一.
     */
    public ClassVisitor(final int api) {
        this(api, null);
    }

    /**
     * @param api
     *            此访问器实现的ASM API版本.
     *            必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}之一.
     * @param cv
     *            此访问器必须委托方法调用的类访问器. May be null.
     */
    public ClassVisitor(final int api, final ClassVisitor cv) {
        if (api < Opcodes.ASM4 || api > Opcodes.ASM6) {
            throw new IllegalArgumentException();
        }
        this.api = api;
        this.cv = cv;
    }

    /**
     * 访问该类的header.
     * 
     * @param version
     *            类版本.
     * @param access
     *            类的访问标志 (see {@link Opcodes}).
     *            此参数还指示是否已弃用该类.
     * @param name
     *            类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     * @param signature
     *            类的签名.
     *            可以是<tt>null</tt>, 如果类不是泛型类, 并且不扩展或实现泛型类或接口.
     * @param superName
     *            超类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     *            对于接口, 超类是{@link Object}. 可能是<tt>null</tt>, 但仅适用于{@link Object}类.
     * @param interfaces
     *            类的接口的内部名称 (see {@link Type#getInternalName() getInternalName}).
     *            可能是<tt>null</tt>.
     */
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        if (cv != null) {
            cv.visit(version, access, name, signature, superName, interfaces);
        }
    }

    /**
     * 访问类的源.
     * 
     * @param source
     *            从中编译类的源文件的名称.
     *            May be <tt>null</tt>.
     * @param debug
     *            额外的调试信息, 用于计算类的源元素和已编译元素之间的对应关系.
     *            May be <tt>null</tt>.
     */
    public void visitSource(String source, String debug) {
        if (cv != null) {
            cv.visitSource(source, debug);
        }
    }

    /**
     * 访问该类对应的模块.
     * 
     * @param name
     *            模块名称
     * @param access
     *            模块标志, {@code ACC_OPEN}, {@code ACC_SYNTHETIC}, {@code ACC_MANDATED}其中之一.
     * @param version
     *            模块版本或null.
     *            
     * @return 访问模块值的访问器, 或<tt>null</tt> 如果访问器对访问此模块不感兴趣.
     */
    public ModuleVisitor visitModule(String name, int access, String version) {
        if (api < Opcodes.ASM6) {
            throw new RuntimeException();
        }
        if (cv != null) {
            return cv.visitModule(name, access, version);
        }
        return null;
    }

    /**
     * 访问该类的封闭类. 仅当类具有封闭类时, 才必须调用此方法.
     * 
     * @param owner
     *            类的封闭类的内部名称.
     * @param name
     *            包含该类的方法的名称, 或<tt>null</tt> 如果该类未包含在其封闭类的方法中.
     * @param desc
     *            包含该类的方法的描述符, 或<tt>null</tt> 如果该类未包含在其封闭类的方法中.
     */
    public void visitOuterClass(String owner, String name, String desc) {
        if (cv != null) {
            cv.visitOuterClass(owner, name, desc);
        }
    }

    /**
     * 访问该类的注解.
     * 
     * @param desc
     *            注解类的类描述符.
     * @param visible
     *            <tt>true</tt>如果注解在运行时可见.
     * 
     * @return 访问注解值的访问器, 或<tt>null</tt> 如果此访问器对访问此注解不感兴趣.
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (cv != null) {
            return cv.visitAnnotation(desc, visible);
        }
        return null;
    }

    /**
     * 访问类签名中类型的注解.
     * 
     * @param typeRef
     *            对注解类型的引用.
     *            此类型引用的类型必须是{@link TypeReference#CLASS_TYPE_PARAMETER CLASS_TYPE_PARAMETER},
     *            {@link TypeReference#CLASS_TYPE_PARAMETER_BOUND CLASS_TYPE_PARAMETER_BOUND} 或
     *            {@link TypeReference#CLASS_EXTENDS CLASS_EXTENDS}. See {@link TypeReference}.
     * @param typePath
     *            'typeRef'中带注解的类型参数, 通配符绑定, 数组元素类型, 或静态内部类型.
     *            如果注解将'typeRef'作为一个整体, 则可能是<tt>null</tt>.
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
        if (cv != null) {
            return cv.visitTypeAnnotation(typeRef, typePath, desc, visible);
        }
        return null;
    }

    /**
     * 访问该类的非标准属性.
     * 
     * @param attr
     *            属性.
     */
    public void visitAttribute(Attribute attr) {
        if (cv != null) {
            cv.visitAttribute(attr);
        }
    }

    /**
     * 访问有关内部类的信息. 这个内部类不一定是被访问类的成员.
     * 
     * @param name
     *            内部类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     * @param outerName
     *            内部类所属的类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     *            对于非成员类, 可能<tt>null</tt>.
     * @param innerName
     *            封闭类中内部类的(简单)名称. 对于匿名内部类, 可能是<tt>null</tt>.
     * @param access
     *            最初在封闭类中声明的内部类的访问标志.
     */
    public void visitInnerClass(String name, String outerName,
            String innerName, int access) {
        if (cv != null) {
            cv.visitInnerClass(name, outerName, innerName, access);
        }
    }

    /**
     * 访问类的字段.
     * 
     * @param access
     *            该字段的访问标志 (see {@link Opcodes}).
     *            此参数还指示该字段是合成的或是已弃用.
     * @param name
     *            字段的名称.
     * @param desc
     *            字段的描述符 (see {@link Type Type}).
     * @param signature
     *            字段的签名. 如果字段的类型不使用泛型类型, 则可能是<tt>null</tt>.
     * @param value
     *            字段的初始值.如果字段没有初始值, 则可以是<tt>null</tt>.
     *            此参数必须是{@link Integer}, {@link Float}, {@link Long},
     *            {@link Double}或 {@link String}
     *            (分别对应<tt>int</tt>, <tt>float</tt>, <tt>long</tt>, <tt>String</tt>字段).
     *            <i>此参数仅用于静态字段</i>.
     *            对于非静态字段, 它的值被忽略, 非静态字段必须通过构造函数或方法中的字节码指令进行初始化.
     * 
     * @return 访问字段注解和属性的访问器, 或<tt>null</tt>如果此类访问器对访问这些注解和属性不感兴趣.
     */
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {
        if (cv != null) {
            return cv.visitField(access, name, desc, signature, value);
        }
        return null;
    }

    /**
     * 访问该类的方法.
     * 每次调用此方法时<i>必须</i>返回一个新的{@link MethodVisitor}实例 (或<tt>null</tt>),
     * i.e., 它不应该返回先前返回的访问器.
     * 
     * @param access
     *            方法的访问标志 (see {@link Opcodes}).
     *            此参数还指示方法是合成的或是已弃用.
     * @param name
     *            方法的名称.
     * @param desc
     *            方法的描述符 (see {@link Type Type}).
     * @param signature
     *            方法的签名.
     *            如果方法参数, 返回类型和异常不使用泛型类型, 则可能是<tt>null</tt>.
     * @param exceptions
     *            方法的异常类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     *            可能是<tt>null</tt>.
     * 
     * @return 访问该方法的字节代码的对象, 或<tt>null</tt>如果此类访问器对访问此方法的代码不感兴趣.
     */
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        if (cv != null) {
            return cv.visitMethod(access, name, desc, signature, exceptions);
        }
        return null;
    }

    /**
     * 访问此类的结尾.
     * 此方法是最后一个要调用的方法, 用于通知访问器已访问该类的所有字段和方法.
     */
    public void visitEnd() {
        if (cv != null) {
            cv.visitEnd();
        }
    }
}
