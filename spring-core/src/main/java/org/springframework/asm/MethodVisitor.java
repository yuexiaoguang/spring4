package org.springframework.asm;

/**
 * 访问Java方法的访问器.
 * 必须按以下顺序调用此类的方法:
 * ( <tt>visitParameter</tt> )* [ <tt>visitAnnotationDefault</tt> ] ( <tt>visitAnnotation</tt> |
 * <tt>visitParameterAnnotation</tt> <tt>visitTypeAnnotation</tt> |
 * <tt>visitAttribute</tt> )* [ <tt>visitCode</tt> ( <tt>visitFrame</tt> |
 * <tt>visit<i>X</i>Insn</tt> | <tt>visitLabel</tt> |
 * <tt>visitInsnAnnotation</tt> | <tt>visitTryCatchBlock</tt> |
 * <tt>visitTryCatchAnnotation</tt> | <tt>visitLocalVariable</tt> |
 * <tt>visitLocalVariableAnnotation</tt> | <tt>visitLineNumber</tt> )*
 * <tt>visitMaxs</tt> ] <tt>visitEnd</tt>.
 * 
 * 此外, 必须按访问代码的字节码指令的顺序调用<tt>visit<i>X</i>Insn</tt> 和 <tt>visitLabel</tt>方法,
 * <tt>visitInsnAnnotation</tt>必须在带注解的指令<i>之后</i>调用,
 * <tt>visitTryCatchBlock</tt>必须在作为参数传递的标签被访问<i>之前</i>调用,
 * <tt>visitTryCatchBlockAnnotation</tt>必须在访问了相应的try catch块<i>之后</i>调用,
 * 而且<tt>visitLocalVariable</tt>, <tt>visitLocalVariableAnnotation</tt>和<tt>visitLineNumber</tt>方法
 * 必须在作为参数传递的标签被访问<i>之后</i>调用.
 */
public abstract class MethodVisitor {

    /**
     * 此访问器实现的ASM API版本.
     * 该字段的值必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}之一.
     */
    protected final int api;

    /**
     * 此访问器必须委托方法调用的方法访问器.
     * May be null.
     */
    protected MethodVisitor mv;

    /**
     * @param api
     *            此访问器实现的ASM API版本.
     *            必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}或 {@link Opcodes#ASM6}之一.
     */
    public MethodVisitor(final int api) {
        this(api, null);
    }

    /**
     * @param api
     *            此访问器实现的ASM API版本.
     *            必须是{@link Opcodes#ASM4}, {@link Opcodes#ASM5}或 {@link Opcodes#ASM6}之一.
     * @param mv
     *            此访问器必须委托方法调用的方法访问器. May be null.
     */
    public MethodVisitor(final int api, final MethodVisitor mv) {
        if (api < Opcodes.ASM4 || api > Opcodes.ASM6) {
            throw new IllegalArgumentException();
        }
        this.api = api;
        this.mv = mv;
    }

    // -------------------------------------------------------------------------
    // Parameters, annotations and non standard attributes
    // -------------------------------------------------------------------------

    /**
     * 访问此方法的参数.
     * 
     * @param name
     *            参数名称, 如果没有提供则为null.
     * @param access
     *            参数的访问标志, 只允许<tt>ACC_FINAL</tt>, <tt>ACC_SYNTHETIC</tt>, <tt>ACC_MANDATED</tt> (see {@link Opcodes}).
     */
    public void visitParameter(String name, int access) {
		/* SPRING PATCH: REMOVED FOR COMPATIBILITY WITH CGLIB 3.1
        if (api < Opcodes.ASM5) {
            throw new RuntimeException();
        }
        */
        if (mv != null) {
            mv.visitParameter(name, access);
        }
    }

    /**
     * 访问此注解接口方法的默认值.
     * 
     * @return 访问此注解接口方法的实际默认值的访问器,
     * 或<tt>null</tt>如果此访问器对访问此默认值不感兴趣.
     * 传递给此注解访问器的方法的'name'参数将被忽略.
     * 此外, 必须在此注解访问器上调用一个访问方法, 然后是visitEnd.
     */
    public AnnotationVisitor visitAnnotationDefault() {
        if (mv != null) {
            return mv.visitAnnotationDefault();
        }
        return null;
    }

    /**
     * 访问此方法的注解.
     * 
     * @param desc
     *            注解类的类描述符.
     * @param visible
     *            <tt>true</tt>如果注解在运行时可见.
     * 
     * @return 访问注解值的访问器, 或<tt>null</tt>如果此访问器对访问此注解不感兴趣.
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (mv != null) {
            return mv.visitAnnotation(desc, visible);
        }
        return null;
    }

    /**
     * 访问方法签名中类型的注解.
     * 
     * @param typeRef
     *            对注解类型的引用.
     *            此类型引用的类型必须是{@link TypeReference#METHOD_TYPE_PARAMETER METHOD_TYPE_PARAMETER},
     *            {@link TypeReference#METHOD_TYPE_PARAMETER_BOUND METHOD_TYPE_PARAMETER_BOUND},
     *            {@link TypeReference#METHOD_RETURN METHOD_RETURN}, {@link TypeReference#METHOD_RECEIVER METHOD_RECEIVER},
     *            {@link TypeReference#METHOD_FORMAL_PARAMETER METHOD_FORMAL_PARAMETER}或 {@link TypeReference#THROWS THROWS}.
     *            See {@link TypeReference}.
     * @param typePath
     *            'typeRef'中带注解的类型参数, 通配符绑定, 数组元素类型, 或静态内部类型的路径.
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
        if (mv != null) {
            return mv.visitTypeAnnotation(typeRef, typePath, desc, visible);
        }
        return null;
    }

    /**
     * 访问此方法的参数的注解.
     * 
     * @param parameter
     *            参数索引.
     * @param desc
     *            注解类的类描述符.
     * @param visible
     *            <tt>true</tt>如果注解在运行时可见.
     * 
     * @return 访问注解值的访问器, 或<tt>null</tt>如果此访问器对访问此注解不感兴趣.
     */
    public AnnotationVisitor visitParameterAnnotation(int parameter,
            String desc, boolean visible) {
        if (mv != null) {
            return mv.visitParameterAnnotation(parameter, desc, visible);
        }
        return null;
    }

    /**
     * 访问此方法的非标准属性.
     * 
     * @param attr
     *            属性.
     */
    public void visitAttribute(Attribute attr) {
        if (mv != null) {
            mv.visitAttribute(attr);
        }
    }

    /**
     * 开始访问方法的代码 (i.e. 非抽象方法).
     */
    public void visitCode() {
        if (mv != null) {
            mv.visitCode();
        }
    }

    /**
     * 访问局部变量和操作数堆栈元素的当前状态.
     * 这个方法必须(*)在任何指令<b>i</b> <i>之前</i>调用, 该指令遵循无条件分支指令,
     * 如 GOTO 或 THROW, 即跳转指令的目标, 或者异常处理程序块的开始.
     * 被访问类型必须在执行<b>i</b><i>之前</i>描述局部变量和操作数堆栈元素.<br>
     * <br>
     * (*) 这仅适用于版本大于或等于{@link Opcodes#V1_6 V1_6}的类. <br>
     * <br>
     * 方法的帧必须以扩展形式或压缩形式给出
     * (所有帧必须使用相同的格式, i.e. 不得在单个方法中混合扩展和压缩帧):
     * <ul>
     * <li>在扩展形式中, 所有帧都必须具有F_NEW类型.</li>
     * <li>在压缩形式中, 帧基本上是来自前一帧状态的"deltas":
     * <ul>
     * <li>{@link Opcodes#F_SAME} 表示具有与前一帧完全相同的局部变量和具有空堆栈的帧.</li>
     * <li>{@link Opcodes#F_SAME1} 表示具有与前一帧完全相同的局部变量, 并且在堆栈上具有单个值的帧
     * (<code>nStack</code>是 1, 而且<code>stack[0]</code>包含堆栈项类型的值).</li>
     * <li>{@link Opcodes#F_APPEND} 表示当前局部变量与前一帧的局部变量完全相同的帧, 除了定义了其他局部变量
     * (<code>nLocal</code>是 1, 2 或 3, 而且<code>local</code>元素包含表示添加的类型的值).</li>
     * <li>{@link Opcodes#F_CHOP} 表示当前局部变量与前一帧的局部变量完全相同的帧, 除了最后1-3个局部变量不存在, 并且具有空堆栈
     * (<code>nLocals</code>是 1, 2 或 3).</li>
     * <li>{@link Opcodes#F_FULL}表示完整的帧数据.</li>
     * </ul>
     * </li>
     * </ul>
     * <br>
     * 在这两种情况下, 对应于方法的参数和访问标志的第一个帧是隐式的, 不可访问.
     * 此外, 在相同的代码位置访问两个或更多帧是非法的 (i.e., 在两次调用visitFrame之间必须访问至少一条指令).
     * 
     * @param type
     *            此堆栈映射帧的类型.
     *            对于扩展帧, 必须是{@link Opcodes#F_NEW};
     *            或者对于压缩帧, 是{@link Opcodes#F_FULL}, {@link Opcodes#F_APPEND},
     *            {@link Opcodes#F_CHOP}, {@link Opcodes#F_SAME}或
     *            {@link Opcodes#F_APPEND}, {@link Opcodes#F_SAME1}.
     * @param nLocal
     *            被访问的帧中的局部变量数.
     * @param local
     *            此帧中的局部变量类型.
     *            不得修改此数组.
     *            原始类型由
     *            {@link Opcodes#TOP}, {@link Opcodes#INTEGER},
     *            {@link Opcodes#FLOAT}, {@link Opcodes#LONG},
     *            {@link Opcodes#DOUBLE},{@link Opcodes#NULL}或
     *            {@link Opcodes#UNINITIALIZED_THIS} (long和double由单个元素表示)表示.
     *            引用类型由String对象 (表示内部名称)表示,
     *            而未初始化的类型由Label对象表示 (此标签指定创建此未初始化的值的NEW指令).
     * @param nStack
     *            被访问的帧中操作数堆栈元素的数量.
     * @param stack
     *            此帧中的操作数堆栈类型. This array must not be modified.
     *            其内容与"local"数组具有相同的格式.
     * 
     * @throws IllegalStateException
     *             如果一帧正好在另一帧之后被访问, 两者之间没有任何指令
     *             (除非这个帧是一个 Opcodes#F_SAME帧, 在这种情况下它会被静默忽略).
     */
    public void visitFrame(int type, int nLocal, Object[] local, int nStack,
            Object[] stack) {
        if (mv != null) {
            mv.visitFrame(type, nLocal, local, nStack, stack);
        }
    }

    // -------------------------------------------------------------------------
    // Normal instructions
    // -------------------------------------------------------------------------

    /**
     * 访问零操作数指令.
     * 
     * @param opcode
     *            要访问的指令的操作码.
     *            操作码可以是 NOP, ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1,
     *            ICONST_2, ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1,
     *            FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, IALOAD,
     *            LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD,
     *            IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE,
     *            SASTORE, POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1,
     *            DUP2_X2, SWAP, IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB,
     *            IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM,
     *            FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL, LSHL, ISHR, LSHR,
     *            IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR, I2L, I2F, I2D,
     *            L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S,
     *            LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IRETURN, LRETURN, FRETURN,
     *            DRETURN, ARETURN, RETURN, ARRAYLENGTH, ATHROW, MONITORENTER,
     *            或 MONITOREXIT.
     */
    public void visitInsn(int opcode) {
        if (mv != null) {
            mv.visitInsn(opcode);
        }
    }

    /**
     * 使用单个int操作数访问指令.
     * 
     * @param opcode
     *            要访问的指令的操作码. 此操作码是 BIPUSH, SIPUSH 或 NEWARRAY.
     * @param operand
     *            要访问的指令的操作数.<br>
     *            当操作码是 BIPUSH时, 操作数值应该在 Byte.MIN_VALUE 和 Byte.MAX_VALUE之间.<br>
     *            当操作码是 SIPUSH时, 操作数值应该在 Short.MIN_VALUE 和 Short.MAX_VALUE之间.<br>
     *            当操作码是 NEWARRAY时, 操作数值应该是
     *            {@link Opcodes#T_BOOLEAN}, {@link Opcodes#T_CHAR},
     *            {@link Opcodes#T_FLOAT}, {@link Opcodes#T_DOUBLE},
     *            {@link Opcodes#T_BYTE}, {@link Opcodes#T_SHORT},
     *            {@link Opcodes#T_INT} 或 {@link Opcodes#T_LONG}之一.
     */
    public void visitIntInsn(int opcode, int operand) {
        if (mv != null) {
            mv.visitIntInsn(opcode, operand);
        }
    }

    /**
     * 访问局部变量指令.
     * 局部变量指令是加载或存储局部变量值的指令.
     * 
     * @param opcode
     *            要访问的局部变量指令的操作码.
     *            此操作码是 ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE 或 RET.
     * @param var
     *            要访问的指令的操作数. 此操作数是局部变量的索引.
     */
    public void visitVarInsn(int opcode, int var) {
        if (mv != null) {
            mv.visitVarInsn(opcode, var);
        }
    }

    /**
     * 访问类型指令.
     * 类型指令是将类的内部名称作为参数的指令.
     * 
     * @param opcode
     *            要访问的类型指令的操作码.
     *            此操作码是 NEW, ANEWARRAY, CHECKCAST 或 INSTANCEOF.
     * @param type
     *            要访问的指令的操作数.
     *            此操作数必须是对象或数组类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     */
    public void visitTypeInsn(int opcode, String type) {
        if (mv != null) {
            mv.visitTypeInsn(opcode, type);
        }
    }

    /**
     * 访问字段指令.
     * 字段指令是加载或存储对象字段值的指令.
     * 
     * @param opcode
     *            要访问的类型指令的操作码.
     *            此操作码是 GETSTATIC, PUTSTATIC, GETFIELD 或 PUTFIELD.
     * @param owner
     *            字段所有者类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     * @param name
     *            该字段的名称.
     * @param desc
     *            字段的描述符 (see {@link Type Type}).
     */
    public void visitFieldInsn(int opcode, String owner, String name,
            String desc) {
        if (mv != null) {
            mv.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    /**
     * 访问方法指令.
     * 方法指令是调用方法的指令.
     * 
     * @param opcode
     *            要访问的类型指令的操作码.
     *            此操作码是 INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC 或 INVOKEINTERFACE.
     * @param owner
     *            方法所有者类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     * @param name
     *            方法的名称.
     * @param desc
     *            方法的描述符 (see {@link Type Type}).
     */
    @Deprecated
    public void visitMethodInsn(int opcode, String owner, String name,
            String desc) {
        if (api >= Opcodes.ASM5) {
            boolean itf = opcode == Opcodes.INVOKEINTERFACE;
            visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        if (mv != null) {
            mv.visitMethodInsn(opcode, owner, name, desc);
        }
    }

    /**
     * 访问方法指令.
     * 方法指令是调用方法的指令.
     * 
     * @param opcode
     *            要访问的类型指令的操作码.
     *            此操作码是 INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC 或 INVOKEINTERFACE.
     * @param owner
     *            方法所有者类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     * @param name
     *            方法的名称.
     * @param desc
     *            方法的描述符 (see {@link Type Type}).
     * @param itf
     *            方法的所有者类是否是接口.
     */
    public void visitMethodInsn(int opcode, String owner, String name,
            String desc, boolean itf) {
        if (api < Opcodes.ASM5) {
            if (itf != (opcode == Opcodes.INVOKEINTERFACE)) {
                throw new IllegalArgumentException(
                        "INVOKESPECIAL/STATIC on interfaces require ASM 5");
            }
            visitMethodInsn(opcode, owner, name, desc);
            return;
        }
        if (mv != null) {
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    /**
     * 访问invokedynamic指令.
     * 
     * @param name
     *            方法的名称.
     * @param desc
     *            方法的描述符 (see {@link Type Type}).
     * @param bsm
     *            引导方法.
     * @param bsmArgs
     *            引导方法常量参数.
     *            每个参数必须是{@link Integer}, {@link Float}, {@link Long},
     *            {@link Double}, {@link String}, {@link Type} 或 {@link Handle}值.
     *            允许此方法修改数组的内容, 以便调用者可以预期此数组可能会更改.
     */
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
            Object... bsmArgs) {
        if (mv != null) {
            mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
    }

    /**
     * 访问跳转指令.
     * 跳转指令是可以跳转到另一条指令的指令.
     * 
     * @param opcode
     *            要访问的类型指令的操作码.
     *            此操作码是 IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
     *            IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
     *            IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL 或 IFNONNULL.
     * @param label
     *            要访问的指令的操作数.
     *            该操作数是一个标签, 用于指定跳转指令可以跳转到的指令.
     */
    public void visitJumpInsn(int opcode, Label label) {
        if (mv != null) {
            mv.visitJumpInsn(opcode, label);
        }
    }

    /**
     * 访问标签. 标签指定将在其之后访问的指令.
     * 
     * @param label
     *            {@link Label Label}对象.
     */
    public void visitLabel(Label label) {
        if (mv != null) {
            mv.visitLabel(label);
        }
    }

    // -------------------------------------------------------------------------
    // Special instructions
    // -------------------------------------------------------------------------

    /**
     * 访问LDC指令.
     * 请注意, 可以在Java虚拟机的未来版本中添加新的常量类型.
     * 为了轻松检测新的常量类型, 此方法的实现应检查意外的常量类型, 如下所示:
     * 
     * <pre>
     * if (cst instanceof Integer) {
     *     // ...
     * } else if (cst instanceof Float) {
     *     // ...
     * } else if (cst instanceof Long) {
     *     // ...
     * } else if (cst instanceof Double) {
     *     // ...
     * } else if (cst instanceof String) {
     *     // ...
     * } else if (cst instanceof Type) {
     *     int sort = ((Type) cst).getSort();
     *     if (sort == Type.OBJECT) {
     *         // ...
     *     } else if (sort == Type.ARRAY) {
     *         // ...
     *     } else if (sort == Type.METHOD) {
     *         // ...
     *     } else {
     *         // throw an exception
     *     }
     * } else if (cst instanceof Handle) {
     *     // ...
     * } else {
     *     // throw an exception
     * }
     * </pre>
     * 
     * @param cst
     *            要在堆栈上加载的常量.
     *            此参数必须是一个非 null的 {@link Integer}, {@link Float}, {@link Long},
     *            {@link Double}, {@link String},
     *            OBJECT的{@link Type}或<tt>.class</tt>常量的ARRAY类型, 用于版本为 49.0的类,
     *            METHOD类型的{@link Type}或 MethodType 和 MethodHandle常量的{@link Handle}, 用于版本为 51.0的类.
     */
    public void visitLdcInsn(Object cst) {
        if (mv != null) {
            mv.visitLdcInsn(cst);
        }
    }

    /**
     * 访问IINC指令.
     * 
     * @param var
     *            要递增的局部变量的索引.
     * @param increment
     *            用于增加局部变量的数量.
     */
    public void visitIincInsn(int var, int increment) {
        if (mv != null) {
            mv.visitIincInsn(var, increment);
        }
    }

    /**
     * 访问TABLESWITCH指令.
     * 
     * @param min
     *            最小键值.
     * @param max
     *            最大键值.
     * @param dflt
     *            默认处理程序块的起始.
     * @param labels
     *            处理程序块的起始. <tt>labels[i]</tt>是<tt>min + i</tt>键的处理程序块的起始.
     */
    public void visitTableSwitchInsn(int min, int max, Label dflt,
            Label... labels) {
        if (mv != null) {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
    }

    /**
     * 访问LOOKUPSWITCH 指令.
     * 
     * @param dflt
     *            默认处理程序块的起始.
     * @param keys
     *            键的值.
     * @param labels
     *            处理程序块的起始. <tt>labels[i]</tt>是<tt>keys[i]</tt>键的处理程序块的起始.
     */
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        if (mv != null) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
    }

    /**
     * 访问 MULTIANEWARRAY 指令.
     * 
     * @param desc
     *            数组类型描述符 (see {@link Type Type}).
     * @param dims
     *            要分配的数组的维数.
     */
    public void visitMultiANewArrayInsn(String desc, int dims) {
        if (mv != null) {
            mv.visitMultiANewArrayInsn(desc, dims);
        }
    }

    /**
     * 访问指令上的注解.
     * 必须在带注解的指令<i>之后</i>调用此方法. 对于同一指令, 可以多次调用它.
     * 
     * @param typeRef
     *            对带注解的类型的引用.
     *            此类型引用的类型必须是{@link TypeReference#INSTANCEOF INSTANCEOF},
     *            {@link TypeReference#NEW NEW}, {@link TypeReference#CONSTRUCTOR_REFERENCE CONSTRUCTOR_REFERENCE},
     *            {@link TypeReference#METHOD_REFERENCE METHOD_REFERENCE}, {@link TypeReference#CAST CAST},
     *            {@link TypeReference#CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT},
     *            {@link TypeReference#METHOD_INVOCATION_TYPE_ARGUMENT METHOD_INVOCATION_TYPE_ARGUMENT},
     *            {@link TypeReference#CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT},
     *            或{@link TypeReference#METHOD_REFERENCE_TYPE_ARGUMENT METHOD_REFERENCE_TYPE_ARGUMENT}.
     *            See {@link TypeReference}.
     * @param typePath
     *            'typeRef'中带注解的类型参数, 通配符绑定, 数组元素类型, 或静态内部类型的路径.
     *            如果注解将'typeRef'作为一个整体, 则可能是<tt>null</tt>.
     * @param desc
     *            注解类的类描述符.
     * @param visible
     *            <tt>true</tt>如果注解在运行时可见.
     * 
     * @return 访问注解值的访问器, 或<tt>null</tt>如果此访问器对访问此注解不感兴趣.
     */
    public AnnotationVisitor visitInsnAnnotation(int typeRef,
            TypePath typePath, String desc, boolean visible) {
		/* SPRING PATCH: REMOVED FOR COMPATIBILITY WITH CGLIB 3.1
        if (api < Opcodes.ASM5) {
            throw new RuntimeException();
        }
        */
        if (mv != null) {
            return mv.visitInsnAnnotation(typeRef, typePath, desc, visible);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Exceptions table entries, debug information, max stack and max locals
    // -------------------------------------------------------------------------

    /**
     * 访问try catch块.
     * 
     * @param start
     *            异常处理程序范围的起始 (包括).
     * @param end
     *            异常处理程序范围的结尾 (不包括).
     * @param handler
     *            异常处理程序代码的起始.
     * @param type
     *            处理程序处理的异常类型的内部名称, 或<tt>null</tt>以捕获任何异常(对于"finally"块).
     * 
     * @throws IllegalArgumentException
     *             如果此访问器已访问过其中一个标签 (通过{@link #visitLabel visitLabel}方法).
     */
    public void visitTryCatchBlock(Label start, Label end, Label handler,
            String type) {
        if (mv != null) {
            mv.visitTryCatchBlock(start, end, handler, type);
        }
    }

    /**
     * 访问异常处理程序类型的注解.
     * 对于带注解的异常处理程序, 必须在{@link #visitTryCatchBlock} <i>之后</i>调用此方法.
     * 对于同一个异常处理程序, 可以多次调用它.
     * 
     * @param typeRef
     *            对带注解的类型的引用.
     *            此类型引用的类型必须是{@link TypeReference#EXCEPTION_PARAMETER EXCEPTION_PARAMETER}.
     *            See {@link TypeReference}.
     * @param typePath
     *            'typeRef'中带注解的类型参数, 通配符绑定, 数组元素类型, 或静态内部类型的路径.
     *            如果注解将'typeRef'作为一个整体, 则可能是<tt>null</tt>.
     * @param desc
     *            注解类的类描述符.
     * @param visible
     *            <tt>true</tt>如果注解在运行时可见.
     * 
     * @return 访问注解值的访问器, 或<tt>null</tt>如果此访问器对访问此注解不感兴趣.
     */
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef,
            TypePath typePath, String desc, boolean visible) {
		/* SPRING PATCH: REMOVED FOR COMPATIBILITY WITH CGLIB 3.1
        if (api < Opcodes.ASM5) {
            throw new RuntimeException();
        }
        */
        if (mv != null) {
            return mv.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
        }
        return null;
    }

    /**
     * 访问局部变量声明.
     * 
     * @param name
     *            局部变量的名称.
     * @param desc
     *            此局部变量的类型描述符.
     * @param signature
     *            此局部变量的类型签名.
     *            如果局部变量类型不使用泛型类型, 则可能是<tt>null</tt>.
     * @param start
     *            对应于此局部变量范围的第一个指令 (包括).
     * @param end
     *            对应于此局部变量范围的最后一条指令 (不包括).
     * @param index
     *            局部变量的索引.
     * 
     * @throws IllegalArgumentException
     *             如果此访问器尚未访问其中一个标签 (通过{@link #visitLabel visitLabel}方法).
     */
    public void visitLocalVariable(String name, String desc, String signature,
            Label start, Label end, int index) {
        if (mv != null) {
            mv.visitLocalVariable(name, desc, signature, start, end, index);
        }
    }

    /**
     * 访问本地变量类型的注解.
     * 
     * @param typeRef
     *            对带注解的类型的引用.
     *            此类型引用的类型必须是{@link TypeReference#LOCAL_VARIABLE LOCAL_VARIABLE}
     *            或 {@link TypeReference#RESOURCE_VARIABLE RESOURCE_VARIABLE}. See {@link TypeReference}.
     * @param typePath
     *            'typeRef'中带注解的类型参数, 通配符绑定, 数组元素类型, 或静态内部类型的路径.
     *            如果注解将'typeRef'作为一个整体, 则可能是<tt>null</tt>.
     * @param start
     *            对应于构成此局部变量范围的连续范围的第一个指令 (包括).
     * @param end
     *            对应于构成此局部变量范围的连续范围的最后一条指令 (不包括).
     *            此数组必须与'start'数组具有相同的大小.
     * @param index
     *            每个范围内的局部变量的索引.
     *            此数组必须与'start'数组具有相同的大小.
     * @param desc
     *            注解类的类描述符.
     * @param visible
     *            <tt>true</tt>如果注解在运行时可见.
     * 
     * @return 访问注解值的访问器, 或<tt>null</tt>如果此访问器对访问此注解不感兴趣.
     */
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
            TypePath typePath, Label[] start, Label[] end, int[] index,
            String desc, boolean visible) {
		/* SPRING PATCH: REMOVED FOR COMPATIBILITY WITH CGLIB 3.1
        if (api < Opcodes.ASM5) {
            throw new RuntimeException();
        }
        */
        if (mv != null) {
            return mv.visitLocalVariableAnnotation(typeRef, typePath, start,
                    end, index, desc, visible);
        }
        return null;
    }

    /**
     * 访问行号声明.
     * 
     * @param line
     *            行号. 此编号指的是编译的类的源文件.
     * @param start
     *            与该行号对应的第一条指令.
     * 
     * @throws IllegalArgumentException
     *             如果此访问器尚未访问<tt>start</tt> (通过{@link #visitLabel visitLabel}方法).
     */
    public void visitLineNumber(int line, Label start) {
        if (mv != null) {
            mv.visitLineNumber(line, start);
        }
    }

    /**
     * 访问方法的最大堆栈大小和最大局部变量数.
     * 
     * @param maxStack
     *            方法的最大堆栈大小.
     * @param maxLocals
     *            方法的最大局部变量数.
     */
    public void visitMaxs(int maxStack, int maxLocals) {
        if (mv != null) {
            mv.visitMaxs(maxStack, maxLocals);
        }
    }

    /**
     * 访问方法的结尾.
     * 此方法是最后一个要调用的方法, 用于通知访问器已访问该方法的所有注解和属性.
     */
    public void visitEnd() {
        if (mv != null) {
            mv.visitEnd();
        }
    }
}
