package org.springframework.asm;

/**
 * 有关基础块的输入和输出堆栈映射帧的信息.
 */
class Frame {

    /*
     * 帧以两步过程计算:
     * 在每个指令的访问期间, 通过模拟指令对该所谓的"输出帧"的先前状态的动作, 来更新当前基础块结束时的帧的状态.
     * 在visitMaxs中, 使用定点算法来计算每个基本块的"输入帧",
     * i.e. 基础块开头的堆栈映射帧, 从第一个基础块的输入帧开始 (从方法描述符计算),
     * 并通过使用先前计算的输出帧来计算其他块的输入状态.
     * 
     * 所有输出和输入帧都存储为整数数组.
     * 引用和数组类型由类型表的索引表示
     * (这与类的常量池不同, 以避免在池中添加不必要的常量 - 并非所有计算的帧最终都会存储在堆栈映射表中).
     * 这允许非常快速的类型比较.
     * 
     * 相对于基础块的输入帧计算输出堆栈映射帧, 输入帧在计算输出帧时尚不知道.
     * 因此, 必须能够表示抽象类型, 例如"输入帧本地中x位置的类型" 或 "输入帧堆栈顶部x位置的类型",
     * 或"输入帧中x位置的类型, y更多(或更少) 数组尺寸".
     * 这解释了输出帧中使用的相当复杂的类型格式.
     * 
     * 这种格式如下: DIM KIND VALUE (4, 4 和 24 位).
     * DIM 是有符号数的数组维度 (from -8 to 7).
     * KIND 是BASE, LOCAL 或 STACK.
     * BASE 用于与输入帧无关的类型.
     * LOCAL 用于与输入局部变量类型相关的类型.
     * STACK 用于与输入堆栈类型相关的类型.
     * VALUE 取决于 KIND.
     * 对于LOCAL类型, 它是输入局部变量类型中的索引.
     * 对于STACK类型, 它是相对于输入帧堆栈顶部的位置.
     * 对于BASE类型, 它可以是下面定义的常量之一, 也可以是 OBJECT 和 UNINITIALIZED类型, 类型表中的标签和索引.
     * 
     * 输出帧可以包含任何种类的类型, 以及正或负维度 (甚至是未分配的类型, 由0表示 - 它与任何有效的类型值都不对应).
     * 输入帧只能包含正维度或null维度的BASE类型.
     * 在所有情况下, 类型表仅包含内部类型名称 (禁止数组类型描述符 - 必须通过DIM字段表示维度).
     * 
     * LONG和DOUBLE类型总是通过使用两个插槽 (LONG + TOP 或 DOUBLE + TOP)表示, 用于局部变量类型以及操作数堆栈.
     * 这对于能够模拟DUPx_y指令是必要的, 如果类型始终由堆栈中的单个插槽表示, 则其效果将取决于实际类型值
     * (这是不可能的, 因为实际的类型值并不总是已知 - cf LOCAL 和 STACK 类型).
     */

    /**
     * 掩码, 以获取帧类型的维度.
     * 此维度是介于-8和7之间的有符号整数.
     */
    static final int DIM = 0xF0000000;

    /**
     * 要添加到类型以获取具有多一个维度的类型的常量.
     */
    static final int ARRAY_OF = 0x10000000;

    /**
     * 要添加到类型以获取具有少一个维度的类型的常量.
     */
    static final int ELEMENT_OF = 0xF0000000;

    /**
     * 掩码, 以获取帧类型的种类.
     */
    static final int KIND = 0xF000000;

    /**
     * 用于LOCAL和STACK类型的标志.
     * 表示如果此类型恰好是long或double类型 (在计算输入帧期间),
     * 那么必须将其设置为TOP, 因为该值的第二个字已被重用, 以在基础块中存储其他数据.
     * 因此, 第一个字不再存储有效的long值或double值.
     */
    static final int TOP_IF_LONG_OR_DOUBLE = 0x800000;

    /**
     * 掩码, 以获取帧类型的值.
     */
    static final int VALUE = 0x7FFFFF;

    /**
     * 掩码, 以获取基类型的种类.
     */
    static final int BASE_KIND = 0xFF00000;

    /**
     * 掩码, 以获取基类型的值.
     */
    static final int BASE_VALUE = 0xFFFFF;

    /**
     * 与输入堆栈映射帧无关的类型的种类.
     */
    static final int BASE = 0x1000000;

    /**
     * 基引用类型的基本种类.
     * 这些类型的BASE_VALUE是类型表的索引.
     */
    static final int OBJECT = BASE | 0x700000;

    /**
     * 未初始化的基类型的基础种类.
     * 类型表索引中此类型的BASE_VALUE (该索引处的Item包含指令偏移量和内部类名).
     */
    static final int UNINITIALIZED = BASE | 0x800000;

    /**
     * 与输入堆栈映射帧的局部变量类型相关的类型的种类.
     * 这些类型的值是局部变量索引.
     */
    private static final int LOCAL = 0x2000000;

    /**
     * 与输入堆栈映射帧的堆栈相关的类型的种类.
     * 这些类型的值是相对于该堆栈顶部的位置.
     */
    private static final int STACK = 0x3000000;

    /**
     * TOP类型. 这是一个BASE类型.
     */
    static final int TOP = BASE | 0;

    /**
     * BOOLEAN类型. 这是一种主要用于数组类型的BASE类型.
     */
    static final int BOOLEAN = BASE | 9;

    /**
     * BYTE类型. 这是一种主要用于数组类型的BASE类型.
     */
    static final int BYTE = BASE | 10;

    /**
     * CHAR类型. 这是一种主要用于数组类型的BASE类型.
     */
    static final int CHAR = BASE | 11;

    /**
     * SHORT类型. 这是一种主要用于数组类型的BASE类型.
     */
    static final int SHORT = BASE | 12;

    /**
     * INTEGER类型. 这是一个BASE类型.
     */
    static final int INTEGER = BASE | 1;

    /**
     * FLOAT类型. 这是一个BASE类型.
     */
    static final int FLOAT = BASE | 2;

    /**
     * DOUBLE类型. 这是一个BASE类型.
     */
    static final int DOUBLE = BASE | 3;

    /**
     * LONG类型. 这是一个BASE类型.
     */
    static final int LONG = BASE | 4;

    /**
     * NULL类型. 这是一个BASE类型.
     */
    static final int NULL = BASE | 5;

    /**
     * UNINITIALIZED_THIS类型. 这是一个BASE类型.
     */
    static final int UNINITIALIZED_THIS = BASE | 6;

    /**
     * 堆栈大小变化对应于每个JVM指令.
     * 此堆栈变化等于指令生成的值的大小, 减去此指令消耗的值的大小.
     */
    static final int[] SIZE;

    /**
     * 计算与每个JVM指令对应的堆栈大小变化.
     */
    static {
        int i;
        int[] b = new int[202];
        String s = "EFFFFFFFFGGFFFGGFFFEEFGFGFEEEEEEEEEEEEEEEEEEEEDEDEDDDDD"
                + "CDCDEEEEEEEEEEEEEEEEEEEEBABABBBBDCFFFGGGEDCDCDCDCDCDCDCDCD"
                + "CDCEEEEDDDDDDDCDCDCEFEFDDEEFFDEDEEEBDDBBDDDDDDCCCCCCCCEFED"
                + "DDCDCDEEEEEEEEEEFEEEEEEDDEEDDEE";
        for (i = 0; i < b.length; ++i) {
            b[i] = s.charAt(i) - 'E';
        }
        SIZE = b;

        // 用于生成上述字符串的代码
        //
        // int NA = 0; // 不适用 (未使用的操作码, 或可变大小的操作码)
        //
        // b = new int[] {
        // 0, //NOP, // visitInsn
        // 1, //ACONST_NULL, // -
        // 1, //ICONST_M1, // -
        // 1, //ICONST_0, // -
        // 1, //ICONST_1, // -
        // 1, //ICONST_2, // -
        // 1, //ICONST_3, // -
        // 1, //ICONST_4, // -
        // 1, //ICONST_5, // -
        // 2, //LCONST_0, // -
        // 2, //LCONST_1, // -
        // 1, //FCONST_0, // -
        // 1, //FCONST_1, // -
        // 1, //FCONST_2, // -
        // 2, //DCONST_0, // -
        // 2, //DCONST_1, // -
        // 1, //BIPUSH, // visitIntInsn
        // 1, //SIPUSH, // -
        // 1, //LDC, // visitLdcInsn
        // NA, //LDC_W, // -
        // NA, //LDC2_W, // -
        // 1, //ILOAD, // visitVarInsn
        // 2, //LLOAD, // -
        // 1, //FLOAD, // -
        // 2, //DLOAD, // -
        // 1, //ALOAD, // -
        // NA, //ILOAD_0, // -
        // NA, //ILOAD_1, // -
        // NA, //ILOAD_2, // -
        // NA, //ILOAD_3, // -
        // NA, //LLOAD_0, // -
        // NA, //LLOAD_1, // -
        // NA, //LLOAD_2, // -
        // NA, //LLOAD_3, // -
        // NA, //FLOAD_0, // -
        // NA, //FLOAD_1, // -
        // NA, //FLOAD_2, // -
        // NA, //FLOAD_3, // -
        // NA, //DLOAD_0, // -
        // NA, //DLOAD_1, // -
        // NA, //DLOAD_2, // -
        // NA, //DLOAD_3, // -
        // NA, //ALOAD_0, // -
        // NA, //ALOAD_1, // -
        // NA, //ALOAD_2, // -
        // NA, //ALOAD_3, // -
        // -1, //IALOAD, // visitInsn
        // 0, //LALOAD, // -
        // -1, //FALOAD, // -
        // 0, //DALOAD, // -
        // -1, //AALOAD, // -
        // -1, //BALOAD, // -
        // -1, //CALOAD, // -
        // -1, //SALOAD, // -
        // -1, //ISTORE, // visitVarInsn
        // -2, //LSTORE, // -
        // -1, //FSTORE, // -
        // -2, //DSTORE, // -
        // -1, //ASTORE, // -
        // NA, //ISTORE_0, // -
        // NA, //ISTORE_1, // -
        // NA, //ISTORE_2, // -
        // NA, //ISTORE_3, // -
        // NA, //LSTORE_0, // -
        // NA, //LSTORE_1, // -
        // NA, //LSTORE_2, // -
        // NA, //LSTORE_3, // -
        // NA, //FSTORE_0, // -
        // NA, //FSTORE_1, // -
        // NA, //FSTORE_2, // -
        // NA, //FSTORE_3, // -
        // NA, //DSTORE_0, // -
        // NA, //DSTORE_1, // -
        // NA, //DSTORE_2, // -
        // NA, //DSTORE_3, // -
        // NA, //ASTORE_0, // -
        // NA, //ASTORE_1, // -
        // NA, //ASTORE_2, // -
        // NA, //ASTORE_3, // -
        // -3, //IASTORE, // visitInsn
        // -4, //LASTORE, // -
        // -3, //FASTORE, // -
        // -4, //DASTORE, // -
        // -3, //AASTORE, // -
        // -3, //BASTORE, // -
        // -3, //CASTORE, // -
        // -3, //SASTORE, // -
        // -1, //POP, // -
        // -2, //POP2, // -
        // 1, //DUP, // -
        // 1, //DUP_X1, // -
        // 1, //DUP_X2, // -
        // 2, //DUP2, // -
        // 2, //DUP2_X1, // -
        // 2, //DUP2_X2, // -
        // 0, //SWAP, // -
        // -1, //IADD, // -
        // -2, //LADD, // -
        // -1, //FADD, // -
        // -2, //DADD, // -
        // -1, //ISUB, // -
        // -2, //LSUB, // -
        // -1, //FSUB, // -
        // -2, //DSUB, // -
        // -1, //IMUL, // -
        // -2, //LMUL, // -
        // -1, //FMUL, // -
        // -2, //DMUL, // -
        // -1, //IDIV, // -
        // -2, //LDIV, // -
        // -1, //FDIV, // -
        // -2, //DDIV, // -
        // -1, //IREM, // -
        // -2, //LREM, // -
        // -1, //FREM, // -
        // -2, //DREM, // -
        // 0, //INEG, // -
        // 0, //LNEG, // -
        // 0, //FNEG, // -
        // 0, //DNEG, // -
        // -1, //ISHL, // -
        // -1, //LSHL, // -
        // -1, //ISHR, // -
        // -1, //LSHR, // -
        // -1, //IUSHR, // -
        // -1, //LUSHR, // -
        // -1, //IAND, // -
        // -2, //LAND, // -
        // -1, //IOR, // -
        // -2, //LOR, // -
        // -1, //IXOR, // -
        // -2, //LXOR, // -
        // 0, //IINC, // visitIincInsn
        // 1, //I2L, // visitInsn
        // 0, //I2F, // -
        // 1, //I2D, // -
        // -1, //L2I, // -
        // -1, //L2F, // -
        // 0, //L2D, // -
        // 0, //F2I, // -
        // 1, //F2L, // -
        // 1, //F2D, // -
        // -1, //D2I, // -
        // 0, //D2L, // -
        // -1, //D2F, // -
        // 0, //I2B, // -
        // 0, //I2C, // -
        // 0, //I2S, // -
        // -3, //LCMP, // -
        // -1, //FCMPL, // -
        // -1, //FCMPG, // -
        // -3, //DCMPL, // -
        // -3, //DCMPG, // -
        // -1, //IFEQ, // visitJumpInsn
        // -1, //IFNE, // -
        // -1, //IFLT, // -
        // -1, //IFGE, // -
        // -1, //IFGT, // -
        // -1, //IFLE, // -
        // -2, //IF_ICMPEQ, // -
        // -2, //IF_ICMPNE, // -
        // -2, //IF_ICMPLT, // -
        // -2, //IF_ICMPGE, // -
        // -2, //IF_ICMPGT, // -
        // -2, //IF_ICMPLE, // -
        // -2, //IF_ACMPEQ, // -
        // -2, //IF_ACMPNE, // -
        // 0, //GOTO, // -
        // 1, //JSR, // -
        // 0, //RET, // visitVarInsn
        // -1, //TABLESWITCH, // visiTableSwitchInsn
        // -1, //LOOKUPSWITCH, // visitLookupSwitch
        // -1, //IRETURN, // visitInsn
        // -2, //LRETURN, // -
        // -1, //FRETURN, // -
        // -2, //DRETURN, // -
        // -1, //ARETURN, // -
        // 0, //RETURN, // -
        // NA, //GETSTATIC, // visitFieldInsn
        // NA, //PUTSTATIC, // -
        // NA, //GETFIELD, // -
        // NA, //PUTFIELD, // -
        // NA, //INVOKEVIRTUAL, // visitMethodInsn
        // NA, //INVOKESPECIAL, // -
        // NA, //INVOKESTATIC, // -
        // NA, //INVOKEINTERFACE, // -
        // NA, //INVOKEDYNAMIC, // visitInvokeDynamicInsn
        // 1, //NEW, // visitTypeInsn
        // 0, //NEWARRAY, // visitIntInsn
        // 0, //ANEWARRAY, // visitTypeInsn
        // 0, //ARRAYLENGTH, // visitInsn
        // NA, //ATHROW, // -
        // 0, //CHECKCAST, // visitTypeInsn
        // 0, //INSTANCEOF, // -
        // -1, //MONITORENTER, // visitInsn
        // -1, //MONITOREXIT, // -
        // NA, //WIDE, // NOT VISITED
        // NA, //MULTIANEWARRAY, // visitMultiANewArrayInsn
        // -1, //IFNULL, // visitJumpInsn
        // -1, //IFNONNULL, // -
        // NA, //GOTO_W, // -
        // NA, //JSR_W, // -
        // };
        // for (i = 0; i < b.length; ++i) {
        // System.err.print((char)('E' + b[i]));
        // }
        // System.err.println();
    }

    /**
     * 这些输入和输出堆栈映射帧对应的标签(i.e. 基础块).
     */
    Label owner;

    /**
     * 输入堆栈映射帧locals.
     */
    int[] inputLocals;

    /**
     * 输入堆栈映射帧堆栈.
     */
    int[] inputStack;

    /**
     * 输出堆栈映射帧locals.
     */
    private int[] outputLocals;

    /**
     * 输出堆栈映射帧堆栈.
     */
    private int[] outputStack;

    /**
     * 输出堆栈的相对大小.
     * 该字段的确切语义取决于所使用的算法.
     * 
     * 当仅计算最大堆栈大小时, 此字段是相对于输入堆栈顶部的输出堆栈的大小.
     * 
     * 完全计算堆栈映射帧时, 此字段是{@link #outputStack}中类型的实际数量.
     */
    int outputStackTop;

    /**
     * 在基础块中初始化的类型数量.
     */
    private int initializationCount;

    /**
     * 在基础块中初始化的类型.
     * UNINITIALIZED或UNINITIALIZED_THIS类型的构造函数调用, 必须在局部变量和操作数堆栈中, 替换此类型的<i>每个出现</i>.
     * 这在算法的第一阶段无法完成, 因为在此阶段, 局部变量和操作数堆栈未完全计算.
     * 因此, 有必要在基础块中存储调用的构造函数的类型, 以便在算法的第二阶段进行此替换, 此时帧是完全计算的.
     * 请注意, 此数组可以包含与输入locals或输入堆栈相关的类型 (请参阅下面的算法说明).
     */
    private int[] initializations;

    /**
     * 将此帧设置为给定值.
     *
     * @param cw
     *            此标签所属的ClassWriter.
     * @param nLocal
     *            局部变量的数量.
     * @param local
     *            局部变量类型.
     *            原始类型由{@link Opcodes#TOP}, {@link Opcodes#INTEGER},
     *            {@link Opcodes#FLOAT}, {@link Opcodes#LONG}, {@link Opcodes#DOUBLE},{@link Opcodes#NULL} or
     *            {@link Opcodes#UNINITIALIZED_THIS} (long和double由单个元素表示)表示.
     *            引用类型由String对象 (表示内部名称), 和Label对象的未初始化的类型表示
     *            (此标签指定创建此未初始化的值的NEW指令).
     * @param nStack
     *            操作数堆栈元素的数量.
     * @param stack
     *            操作数堆栈类型 (与"local"数组相同的格式).
     */
    final void set(ClassWriter cw, final int nLocal, final Object[] local,
            final int nStack, final Object[] stack) {
        int i = convert(cw, nLocal, local, inputLocals);
        while (i < local.length) {
            inputLocals[i++] = TOP;
        }
        int nStackTop = 0;
        for (int j = 0; j < nStack; ++j) {
            if (stack[j] == Opcodes.LONG || stack[j] == Opcodes.DOUBLE) {
                ++nStackTop;
            }
        }
        inputStack = new int[nStack + nStackTop];
        convert(cw, nStack, stack, inputStack);
        outputStackTop = 0;
        initializationCount = 0;
    }

    /**
     * 将类型从MethodWriter.visitFrame() 格式转换为Frame格式.
     *
     * @param cw
     *            此标签所属的ClassWriter.
     * @param nInput
     *            要转换的类型数量.
     * @param input
     *            要转换的类型.
     *            原始类型由{@link Opcodes#TOP}, {@link Opcodes#INTEGER},
     *            {@link Opcodes#FLOAT}, {@link Opcodes#LONG}, {@link Opcodes#DOUBLE},{@link Opcodes#NULL} or
     *            {@link Opcodes#UNINITIALIZED_THIS} (long和double由单个元素表示)表示.
     *            引用类型由String对象 (表示内部名称), 和Label对象的未初始化的类型表示
     *            (此标签指定创建此未初始化的值的NEW指令).
     * @param output
     *            用于存储转换后的类型.
     * 
     * @return 输出元素的数量.
     */
    private static int convert(ClassWriter cw, int nInput, Object[] input,
            int[] output) {
        int i = 0;
        for (int j = 0; j < nInput; ++j) {
            if (input[j] instanceof Integer) {
                output[i++] = BASE | ((Integer) input[j]).intValue();
                if (input[j] == Opcodes.LONG || input[j] == Opcodes.DOUBLE) {
                    output[i++] = TOP;
                }
            } else if (input[j] instanceof String) {
                output[i++] = type(cw, Type.getObjectType((String) input[j])
                        .getDescriptor());
            } else {
                output[i++] = UNINITIALIZED
                        | cw.addUninitializedType("",
                                ((Label) input[j]).position);
            }
        }
        return i;
    }

    /**
     * 将此帧设置为给定帧的值.
     * WARNING: 调用此方法后, 两个帧共享相同的数据结构.
     * 建议丢弃给定的帧f, 以避免意外的副作用.
     *
     * @param f
     *            新的帧的值.
     */
    final void set(final Frame f) {
        inputLocals = f.inputLocals;
        inputStack = f.inputStack;
        outputLocals = f.outputLocals;
        outputStack = f.outputStack;
        outputStackTop = f.outputStackTop;
        initializationCount = f.initializationCount;
        initializations = f.initializations;
    }

    /**
     * 返回给定索引处的输出帧局部变量类型.
     * 
     * @param local
     *            必须返回的局部变量的索引.
     * 
     * @return 给定索引处的输出帧局部变量类型.
     */
    private int get(final int local) {
        if (outputLocals == null || local >= outputLocals.length) {
            // 此局部变量从未在此基础块中分配, 因此它仍然等于输入帧中的值
            return LOCAL | local;
        } else {
            int type = outputLocals[local];
            if (type == 0) {
                // 此局部变量从未在此基础块中分配, 因此它仍然等于输入帧中的值
                type = outputLocals[local] = LOCAL | local;
            }
            return type;
        }
    }

    /**
     * 在给定索引处设置输出帧局部变量类型.
     * 
     * @param local
     *            必须设置的局部变量的索引.
     * @param type
     *            必须设置的局部变量的值.
     */
    private void set(final int local, final int type) {
        // 如有必要, 创建和/或调整输出局部变量数组的大小
        if (outputLocals == null) {
            outputLocals = new int[10];
        }
        int n = outputLocals.length;
        if (local >= n) {
            int[] t = new int[Math.max(local + 1, 2 * n)];
            System.arraycopy(outputLocals, 0, t, 0, n);
            outputLocals = t;
        }
        // 设置局部变量
        outputLocals[local] = type;
    }

    /**
     * 将新类型推送到输出帧堆栈.
     * 
     * @param type
     *            必须推送的类型.
     */
    private void push(final int type) {
        // 如有必要, 创建和/或调整输出局部变量数组的大小
        if (outputStack == null) {
            outputStack = new int[10];
        }
        int n = outputStack.length;
        if (outputStackTop >= n) {
            int[] t = new int[Math.max(outputStackTop + 1, 2 * n)];
            System.arraycopy(outputStack, 0, t, 0, n);
            outputStack = t;
        }
        // 推送输出堆栈上的类型
        outputStack[outputStackTop++] = type;
        // 如果需要, 更新输出堆栈达到的最大高度
        int top = owner.inputStackTop + outputStackTop;
        if (top > owner.outputStackMax) {
            owner.outputStackMax = top;
        }
    }

    /**
     * 将新类型推送到输出帧堆栈.
     * 
     * @param cw
     *            此标签所属的ClassWriter.
     * @param desc
     *            要推送的类型的描述符.
     *            也可以是方法描述符 (在这种情况下, 此方法将其返回类型推送到输出帧堆栈).
     */
    private void push(final ClassWriter cw, final String desc) {
        int type = type(cw, desc);
        if (type != 0) {
            push(type);
            if (type == LONG || type == DOUBLE) {
                push(TOP);
            }
        }
    }

    /**
     * 返回给定类型的int编码.
     * 
     * @param cw
     *            此标签所属的ClassWriter.
     * @param desc
     *            类型的描述符.
     * 
     * @return 给定类型的int编码.
     */
    private static int type(final ClassWriter cw, final String desc) {
        String t;
        int index = desc.charAt(0) == '(' ? desc.indexOf(')') + 1 : 0;
        switch (desc.charAt(index)) {
        case 'V':
            return 0;
        case 'Z':
        case 'C':
        case 'B':
        case 'S':
        case 'I':
            return INTEGER;
        case 'F':
            return FLOAT;
        case 'J':
            return LONG;
        case 'D':
            return DOUBLE;
        case 'L':
            // 存储内部名称, 而不是描述符!
            t = desc.substring(index + 1, desc.length() - 1);
            return OBJECT | cw.addType(t);
            // case '[':
        default:
            // 提取维度和元素类型
            int data;
            int dims = index + 1;
            while (desc.charAt(dims) == '[') {
                ++dims;
            }
            switch (desc.charAt(dims)) {
            case 'Z':
                data = BOOLEAN;
                break;
            case 'C':
                data = CHAR;
                break;
            case 'B':
                data = BYTE;
                break;
            case 'S':
                data = SHORT;
                break;
            case 'I':
                data = INTEGER;
                break;
            case 'F':
                data = FLOAT;
                break;
            case 'J':
                data = LONG;
                break;
            case 'D':
                data = DOUBLE;
                break;
            // case 'L':
            default:
                // 存储内部名称, 而不是描述符
                t = desc.substring(dims + 1, desc.length() - 1);
                data = OBJECT | cw.addType(t);
            }
            return (dims - index) << 28 | data;
        }
    }

    /**
     * 从输出帧堆栈弹出一个类型并返回其值.
     * 
     * @return 从输出帧堆栈中弹出的类型.
     */
    private int pop() {
        if (outputStackTop > 0) {
            return outputStack[--outputStackTop];
        } else {
            // 如果输出帧堆栈为空, 则从输入堆栈弹出
            return STACK | -(--owner.inputStackTop);
        }
    }

    /**
     * 从输出帧堆栈中弹出给定数量的类型.
     * 
     * @param elements
     *            必须弹出的类型数量.
     */
    private void pop(final int elements) {
        if (outputStackTop >= elements) {
            outputStackTop -= elements;
        } else {
            // 如果要弹出的元素数量大于输出堆栈中的元素数量, 将其清除, 然后从输入堆栈中弹出其余元素.
            owner.inputStackTop -= elements - outputStackTop;
            outputStackTop = 0;
        }
    }

    /**
     * 从输出帧堆栈弹出一个类型.
     * 
     * @param desc
     *            要弹出的类型的描述符.
     *            也可以是方法描述符 (在这种情况下, 此方法弹出与方法参数对应的类型).
     */
    private void pop(final String desc) {
        char c = desc.charAt(0);
        if (c == '(') {
            pop((Type.getArgumentsAndReturnSizes(desc) >> 2) - 1);
        } else if (c == 'J' || c == 'D') {
            pop(2);
        } else {
            pop(1);
        }
    }

    /**
     * 将新类型添加到类型列表中, 在类型列表上调用基础块中构造函数.
     * 
     * @param var
     *            调用构造函数的类型.
     */
    private void init(final int var) {
        // 如有必要, 创建和/或调整初始化数组的大小
        if (initializations == null) {
            initializations = new int[2];
        }
        int n = initializations.length;
        if (initializationCount >= n) {
            int[] t = new int[Math.max(initializationCount + 1, 2 * n)];
            System.arraycopy(initializations, 0, t, 0, n);
            initializations = t;
        }
        // 存储要初始化的类型
        initializations[initializationCount++] = var;
    }

    /**
     * 如果给定类型是要在基础块中调用其构造函数的类型之一, 则用适当的类型替换给定类型.
     * 
     * @param cw
     *            此标签所属的ClassWriter.
     * @param t
     *            类型
     * 
     * @return t; 或者, 如果t是要在基础块中调用其构造函数的类型之一, 则是与此构造函数对应的类型.
     */
    private int init(final ClassWriter cw, final int t) {
        int s;
        if (t == UNINITIALIZED_THIS) {
            s = OBJECT | cw.addType(cw.thisName);
        } else if ((t & (DIM | BASE_KIND)) == UNINITIALIZED) {
            String type = cw.typeTable[t & BASE_VALUE].strVal1;
            s = OBJECT | cw.addType(type);
        } else {
            return t;
        }
        for (int j = 0; j < initializationCount; ++j) {
            int u = initializations[j];
            int dim = u & DIM;
            int kind = u & KIND;
            if (kind == LOCAL) {
                u = dim + inputLocals[u & VALUE];
            } else if (kind == STACK) {
                u = dim + inputStack[inputStack.length - (u & VALUE)];
            }
            if (t == u) {
                return s;
            }
        }
        return t;
    }

    /**
     * 从方法描述符初始化第一个基础块的输入帧.
     * 
     * @param cw
     *            此标签所属的ClassWriter.
     * @param access
     *            此标签所属方法的访问标志.
     * @param args
     *            此方法的形式参数类型.
     * @param maxLocals
     *            此方法的最大局部变量数.
     */
    final void initInputFrame(final ClassWriter cw, final int access,
            final Type[] args, final int maxLocals) {
        inputLocals = new int[maxLocals];
        inputStack = new int[0];
        int i = 0;
        if ((access & Opcodes.ACC_STATIC) == 0) {
            if ((access & MethodWriter.ACC_CONSTRUCTOR) == 0) {
                inputLocals[i++] = OBJECT | cw.addType(cw.thisName);
            } else {
                inputLocals[i++] = UNINITIALIZED_THIS;
            }
        }
        for (int j = 0; j < args.length; ++j) {
            int t = type(cw, args[j].getDescriptor());
            inputLocals[i++] = t;
            if (t == LONG || t == DOUBLE) {
                inputLocals[i++] = TOP;
            }
        }
        while (i < maxLocals) {
            inputLocals[i++] = TOP;
        }
    }

    /**
     * 模拟输出堆栈帧上给定指令的操作.
     * 
     * @param opcode
     *            指令的操作码.
     * @param arg
     *            指令的操作数.
     * @param cw
     *            此标签所属的ClassWriter.
     * @param item
     *            指令的操作数.
     */
    void execute(final int opcode, final int arg, final ClassWriter cw,
            final Item item) {
        int t1, t2, t3, t4;
        switch (opcode) {
        case Opcodes.NOP:
        case Opcodes.INEG:
        case Opcodes.LNEG:
        case Opcodes.FNEG:
        case Opcodes.DNEG:
        case Opcodes.I2B:
        case Opcodes.I2C:
        case Opcodes.I2S:
        case Opcodes.GOTO:
        case Opcodes.RETURN:
            break;
        case Opcodes.ACONST_NULL:
            push(NULL);
            break;
        case Opcodes.ICONST_M1:
        case Opcodes.ICONST_0:
        case Opcodes.ICONST_1:
        case Opcodes.ICONST_2:
        case Opcodes.ICONST_3:
        case Opcodes.ICONST_4:
        case Opcodes.ICONST_5:
        case Opcodes.BIPUSH:
        case Opcodes.SIPUSH:
        case Opcodes.ILOAD:
            push(INTEGER);
            break;
        case Opcodes.LCONST_0:
        case Opcodes.LCONST_1:
        case Opcodes.LLOAD:
            push(LONG);
            push(TOP);
            break;
        case Opcodes.FCONST_0:
        case Opcodes.FCONST_1:
        case Opcodes.FCONST_2:
        case Opcodes.FLOAD:
            push(FLOAT);
            break;
        case Opcodes.DCONST_0:
        case Opcodes.DCONST_1:
        case Opcodes.DLOAD:
            push(DOUBLE);
            push(TOP);
            break;
        case Opcodes.LDC:
            switch (item.type) {
            case ClassWriter.INT:
                push(INTEGER);
                break;
            case ClassWriter.LONG:
                push(LONG);
                push(TOP);
                break;
            case ClassWriter.FLOAT:
                push(FLOAT);
                break;
            case ClassWriter.DOUBLE:
                push(DOUBLE);
                push(TOP);
                break;
            case ClassWriter.CLASS:
                push(OBJECT | cw.addType("java/lang/Class"));
                break;
            case ClassWriter.STR:
                push(OBJECT | cw.addType("java/lang/String"));
                break;
            case ClassWriter.MTYPE:
                push(OBJECT | cw.addType("java/lang/invoke/MethodType"));
                break;
            // case ClassWriter.HANDLE_BASE + [1..9]:
            default:
                push(OBJECT | cw.addType("java/lang/invoke/MethodHandle"));
            }
            break;
        case Opcodes.ALOAD:
            push(get(arg));
            break;
        case Opcodes.IALOAD:
        case Opcodes.BALOAD:
        case Opcodes.CALOAD:
        case Opcodes.SALOAD:
            pop(2);
            push(INTEGER);
            break;
        case Opcodes.LALOAD:
        case Opcodes.D2L:
            pop(2);
            push(LONG);
            push(TOP);
            break;
        case Opcodes.FALOAD:
            pop(2);
            push(FLOAT);
            break;
        case Opcodes.DALOAD:
        case Opcodes.L2D:
            pop(2);
            push(DOUBLE);
            push(TOP);
            break;
        case Opcodes.AALOAD:
            pop(1);
            t1 = pop();
            push(ELEMENT_OF + t1);
            break;
        case Opcodes.ISTORE:
        case Opcodes.FSTORE:
        case Opcodes.ASTORE:
            t1 = pop();
            set(arg, t1);
            if (arg > 0) {
                t2 = get(arg - 1);
                // if t2 is of kind STACK or LOCAL we cannot know its size!
                if (t2 == LONG || t2 == DOUBLE) {
                    set(arg - 1, TOP);
                } else if ((t2 & KIND) != BASE) {
                    set(arg - 1, t2 | TOP_IF_LONG_OR_DOUBLE);
                }
            }
            break;
        case Opcodes.LSTORE:
        case Opcodes.DSTORE:
            pop(1);
            t1 = pop();
            set(arg, t1);
            set(arg + 1, TOP);
            if (arg > 0) {
                t2 = get(arg - 1);
                // if t2 is of kind STACK or LOCAL we cannot know its size!
                if (t2 == LONG || t2 == DOUBLE) {
                    set(arg - 1, TOP);
                } else if ((t2 & KIND) != BASE) {
                    set(arg - 1, t2 | TOP_IF_LONG_OR_DOUBLE);
                }
            }
            break;
        case Opcodes.IASTORE:
        case Opcodes.BASTORE:
        case Opcodes.CASTORE:
        case Opcodes.SASTORE:
        case Opcodes.FASTORE:
        case Opcodes.AASTORE:
            pop(3);
            break;
        case Opcodes.LASTORE:
        case Opcodes.DASTORE:
            pop(4);
            break;
        case Opcodes.POP:
        case Opcodes.IFEQ:
        case Opcodes.IFNE:
        case Opcodes.IFLT:
        case Opcodes.IFGE:
        case Opcodes.IFGT:
        case Opcodes.IFLE:
        case Opcodes.IRETURN:
        case Opcodes.FRETURN:
        case Opcodes.ARETURN:
        case Opcodes.TABLESWITCH:
        case Opcodes.LOOKUPSWITCH:
        case Opcodes.ATHROW:
        case Opcodes.MONITORENTER:
        case Opcodes.MONITOREXIT:
        case Opcodes.IFNULL:
        case Opcodes.IFNONNULL:
            pop(1);
            break;
        case Opcodes.POP2:
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ICMPLT:
        case Opcodes.IF_ICMPGE:
        case Opcodes.IF_ICMPGT:
        case Opcodes.IF_ICMPLE:
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE:
        case Opcodes.LRETURN:
        case Opcodes.DRETURN:
            pop(2);
            break;
        case Opcodes.DUP:
            t1 = pop();
            push(t1);
            push(t1);
            break;
        case Opcodes.DUP_X1:
            t1 = pop();
            t2 = pop();
            push(t1);
            push(t2);
            push(t1);
            break;
        case Opcodes.DUP_X2:
            t1 = pop();
            t2 = pop();
            t3 = pop();
            push(t1);
            push(t3);
            push(t2);
            push(t1);
            break;
        case Opcodes.DUP2:
            t1 = pop();
            t2 = pop();
            push(t2);
            push(t1);
            push(t2);
            push(t1);
            break;
        case Opcodes.DUP2_X1:
            t1 = pop();
            t2 = pop();
            t3 = pop();
            push(t2);
            push(t1);
            push(t3);
            push(t2);
            push(t1);
            break;
        case Opcodes.DUP2_X2:
            t1 = pop();
            t2 = pop();
            t3 = pop();
            t4 = pop();
            push(t2);
            push(t1);
            push(t4);
            push(t3);
            push(t2);
            push(t1);
            break;
        case Opcodes.SWAP:
            t1 = pop();
            t2 = pop();
            push(t1);
            push(t2);
            break;
        case Opcodes.IADD:
        case Opcodes.ISUB:
        case Opcodes.IMUL:
        case Opcodes.IDIV:
        case Opcodes.IREM:
        case Opcodes.IAND:
        case Opcodes.IOR:
        case Opcodes.IXOR:
        case Opcodes.ISHL:
        case Opcodes.ISHR:
        case Opcodes.IUSHR:
        case Opcodes.L2I:
        case Opcodes.D2I:
        case Opcodes.FCMPL:
        case Opcodes.FCMPG:
            pop(2);
            push(INTEGER);
            break;
        case Opcodes.LADD:
        case Opcodes.LSUB:
        case Opcodes.LMUL:
        case Opcodes.LDIV:
        case Opcodes.LREM:
        case Opcodes.LAND:
        case Opcodes.LOR:
        case Opcodes.LXOR:
            pop(4);
            push(LONG);
            push(TOP);
            break;
        case Opcodes.FADD:
        case Opcodes.FSUB:
        case Opcodes.FMUL:
        case Opcodes.FDIV:
        case Opcodes.FREM:
        case Opcodes.L2F:
        case Opcodes.D2F:
            pop(2);
            push(FLOAT);
            break;
        case Opcodes.DADD:
        case Opcodes.DSUB:
        case Opcodes.DMUL:
        case Opcodes.DDIV:
        case Opcodes.DREM:
            pop(4);
            push(DOUBLE);
            push(TOP);
            break;
        case Opcodes.LSHL:
        case Opcodes.LSHR:
        case Opcodes.LUSHR:
            pop(3);
            push(LONG);
            push(TOP);
            break;
        case Opcodes.IINC:
            set(arg, INTEGER);
            break;
        case Opcodes.I2L:
        case Opcodes.F2L:
            pop(1);
            push(LONG);
            push(TOP);
            break;
        case Opcodes.I2F:
            pop(1);
            push(FLOAT);
            break;
        case Opcodes.I2D:
        case Opcodes.F2D:
            pop(1);
            push(DOUBLE);
            push(TOP);
            break;
        case Opcodes.F2I:
        case Opcodes.ARRAYLENGTH:
        case Opcodes.INSTANCEOF:
            pop(1);
            push(INTEGER);
            break;
        case Opcodes.LCMP:
        case Opcodes.DCMPL:
        case Opcodes.DCMPG:
            pop(4);
            push(INTEGER);
            break;
        case Opcodes.JSR:
        case Opcodes.RET:
            throw new RuntimeException(
                    "JSR/RET are not supported with computeFrames option");
        case Opcodes.GETSTATIC:
            push(cw, item.strVal3);
            break;
        case Opcodes.PUTSTATIC:
            pop(item.strVal3);
            break;
        case Opcodes.GETFIELD:
            pop(1);
            push(cw, item.strVal3);
            break;
        case Opcodes.PUTFIELD:
            pop(item.strVal3);
            pop();
            break;
        case Opcodes.INVOKEVIRTUAL:
        case Opcodes.INVOKESPECIAL:
        case Opcodes.INVOKESTATIC:
        case Opcodes.INVOKEINTERFACE:
            pop(item.strVal3);
            if (opcode != Opcodes.INVOKESTATIC) {
                t1 = pop();
                if (opcode == Opcodes.INVOKESPECIAL
                        && item.strVal2.charAt(0) == '<') {
                    init(t1);
                }
            }
            push(cw, item.strVal3);
            break;
        case Opcodes.INVOKEDYNAMIC:
            pop(item.strVal2);
            push(cw, item.strVal2);
            break;
        case Opcodes.NEW:
            push(UNINITIALIZED | cw.addUninitializedType(item.strVal1, arg));
            break;
        case Opcodes.NEWARRAY:
            pop();
            switch (arg) {
            case Opcodes.T_BOOLEAN:
                push(ARRAY_OF | BOOLEAN);
                break;
            case Opcodes.T_CHAR:
                push(ARRAY_OF | CHAR);
                break;
            case Opcodes.T_BYTE:
                push(ARRAY_OF | BYTE);
                break;
            case Opcodes.T_SHORT:
                push(ARRAY_OF | SHORT);
                break;
            case Opcodes.T_INT:
                push(ARRAY_OF | INTEGER);
                break;
            case Opcodes.T_FLOAT:
                push(ARRAY_OF | FLOAT);
                break;
            case Opcodes.T_DOUBLE:
                push(ARRAY_OF | DOUBLE);
                break;
            // case Opcodes.T_LONG:
            default:
                push(ARRAY_OF | LONG);
                break;
            }
            break;
        case Opcodes.ANEWARRAY:
            String s = item.strVal1;
            pop();
            if (s.charAt(0) == '[') {
                push(cw, '[' + s);
            } else {
                push(ARRAY_OF | OBJECT | cw.addType(s));
            }
            break;
        case Opcodes.CHECKCAST:
            s = item.strVal1;
            pop();
            if (s.charAt(0) == '[') {
                push(cw, s);
            } else {
                push(OBJECT | cw.addType(s));
            }
            break;
        // case Opcodes.MULTIANEWARRAY:
        default:
            pop(arg);
            push(cw, item.strVal1);
            break;
        }
    }

    /**
     * 将给定基础块的输入帧与该基础块的输入和输出帧合并.
     * 如果此操作更改了给定标签的输入帧, 则返回<tt>true</tt>.
     * 
     * @param cw
     *            此标签所属的ClassWriter.
     * @param frame
     *            必须更新其输入帧的基础块.
     * @param edge
     *            这个标签和'label'之间{@link Edge}的种类.
     *            See {@link Edge#info}.
     * 
     * @return <tt>true</tt>如果通过此操作更改了给定标签的输入帧.
     */
    final boolean merge(final ClassWriter cw, final Frame frame, final int edge) {
        boolean changed = false;
        int i, s, dim, kind, t;

        int nLocal = inputLocals.length;
        int nStack = inputStack.length;
        if (frame.inputLocals == null) {
            frame.inputLocals = new int[nLocal];
            changed = true;
        }

        for (i = 0; i < nLocal; ++i) {
            if (outputLocals != null && i < outputLocals.length) {
                s = outputLocals[i];
                if (s == 0) {
                    t = inputLocals[i];
                } else {
                    dim = s & DIM;
                    kind = s & KIND;
                    if (kind == BASE) {
                        t = s;
                    } else {
                        if (kind == LOCAL) {
                            t = dim + inputLocals[s & VALUE];
                        } else {
                            t = dim + inputStack[nStack - (s & VALUE)];
                        }
                        if ((s & TOP_IF_LONG_OR_DOUBLE) != 0
                                && (t == LONG || t == DOUBLE)) {
                            t = TOP;
                        }
                    }
                }
            } else {
                t = inputLocals[i];
            }
            if (initializations != null) {
                t = init(cw, t);
            }
            changed |= merge(cw, t, frame.inputLocals, i);
        }

        if (edge > 0) {
            for (i = 0; i < nLocal; ++i) {
                t = inputLocals[i];
                changed |= merge(cw, t, frame.inputLocals, i);
            }
            if (frame.inputStack == null) {
                frame.inputStack = new int[1];
                changed = true;
            }
            changed |= merge(cw, edge, frame.inputStack, 0);
            return changed;
        }

        int nInputStack = inputStack.length + owner.inputStackTop;
        if (frame.inputStack == null) {
            frame.inputStack = new int[nInputStack + outputStackTop];
            changed = true;
        }

        for (i = 0; i < nInputStack; ++i) {
            t = inputStack[i];
            if (initializations != null) {
                t = init(cw, t);
            }
            changed |= merge(cw, t, frame.inputStack, i);
        }
        for (i = 0; i < outputStackTop; ++i) {
            s = outputStack[i];
            dim = s & DIM;
            kind = s & KIND;
            if (kind == BASE) {
                t = s;
            } else {
                if (kind == LOCAL) {
                    t = dim + inputLocals[s & VALUE];
                } else {
                    t = dim + inputStack[nStack - (s & VALUE)];
                }
                if ((s & TOP_IF_LONG_OR_DOUBLE) != 0
                        && (t == LONG || t == DOUBLE)) {
                    t = TOP;
                }
            }
            if (initializations != null) {
                t = init(cw, t);
            }
            changed |= merge(cw, t, frame.inputStack, nInputStack + i);
        }
        return changed;
    }

    /**
     * 将给定类型数组中给定索引处的类型与给定类型合并.
     * 如果此操作已修改类型数组, 则返回<tt>true</tt>.
     * 
     * @param cw
     *            此标签所属的ClassWriter.
     * @param t
     *            必须合并类型数组元素的类型.
     * @param types
     *            类型数组.
     * @param index
     *            必须在'types'中合并的类型的索引.
     * 
     * @return <tt>true</tt>如果类型数组已被此操作修改.
     */
    private static boolean merge(final ClassWriter cw, int t,
            final int[] types, final int index) {
        int u = types[index];
        if (u == t) {
            // 如果类型相等, merge(u,t)=u, 因此没有变化
            return false;
        }
        if ((t & ~DIM) == NULL) {
            if (u == NULL) {
                return false;
            }
            t = NULL;
        }
        if (u == 0) {
            // 如果从未分配过 types[index], merge(u,t)=t
            types[index] = t;
            return true;
        }
        int v;
        if ((u & BASE_KIND) == OBJECT || (u & DIM) != 0) {
            // 如果 u 是任何维度的引用类型
            if (t == NULL) {
                // 如果 t 是 NULL 类型, merge(u,t)=u, 因此没有变化
                return false;
            } else if ((t & (DIM | BASE_KIND)) == (u & (DIM | BASE_KIND))) {
                // 如果 t 和 u 具有相同的维度和相同的基础类型
                if ((u & BASE_KIND) == OBJECT) {
                    // 如果 t 也是引用类型, 并且如果 u 和 t 具有相同的维度 merge(u,t) = dim(t) | u 和 t 的元素类型的共同父类
                    v = (t & DIM) | OBJECT
                            | cw.getMergedType(t & BASE_VALUE, u & BASE_VALUE);
                } else {
                    // 如果 u 和 t 是数组类型, 但没有相同的元素类型, merge(u,t) = dim(u) - 1 | java/lang/Object
                    int vdim = ELEMENT_OF + (u & DIM);
                    v = vdim | OBJECT | cw.addType("java/lang/Object");
                }
            } else if ((t & BASE_KIND) == OBJECT || (t & DIM) != 0) {
                // 如果 t 是任何其他引用或数组类型, 则合并类型为 min(udim, tdim) | java/lang/Object,
            	// 其中 udim 是 u 的数组维度, 如果u是具有基本元素类型的数组类型, 则减去1 (类似于 tdim).
                int tdim = (((t & DIM) == 0 || (t & BASE_KIND) == OBJECT) ? 0
                        : ELEMENT_OF) + (t & DIM);
                int udim = (((u & DIM) == 0 || (u & BASE_KIND) == OBJECT) ? 0
                        : ELEMENT_OF) + (u & DIM);
                v = Math.min(tdim, udim) | OBJECT
                        | cw.addType("java/lang/Object");
            } else {
                // 如果 t 是任何其他类型, merge(u,t)=TOP
                v = TOP;
            }
        } else if (u == NULL) {
            // 如果 u 是NULL类型, merge(u,t)=t, 或者如果t不是引用类型, 则为TOP
            v = (t & BASE_KIND) == OBJECT || (t & DIM) != 0 ? t : TOP;
        } else {
            // 如果 u 是任何其他类型, merge(u,t)=TOP 无论 t 是什么
            v = TOP;
        }
        if (u != v) {
            types[index] = v;
            return true;
        }
        return false;
    }
}
