package org.springframework.asm;

/**
 * 标签, 表示方法的字节码中的位置.
 * 标签用于 jump, goto, 和 switch 指令, 以及 try catch 块.
 * 标签指定紧随其后的<i>指令</i>.
 * 但请注意, 标签与其指定的指令之间可能存在其他元素 (例如其他标签, 堆栈映射帧, 行号等.).
 */
public class Label {

    /**
     * 指示此标签是否仅用于调试属性.
     * 这样的标签不是基础块, 跳转指令的目标, 或异常处理程序的开始.
     * 在控制流图分析算法中可以安全地忽略它 (用于优化目的).
     */
    static final int DEBUG = 1;

    /**
     * 指示此标签的位置是否已知.
     */
    static final int RESOLVED = 2;

    /**
     * 指示在调整指令大小后, 是否已更新此标签.
     */
    static final int RESIZED = 4;

    /**
     * 指示是否已在基础块堆栈中推送此基础块.
     * See {@link MethodWriter#visitMaxs visitMaxs}.
     */
    static final int PUSHED = 8;

    /**
     * 指示此标签是跳转指令的目标, 还是异常处理程序的开始.
     */
    static final int TARGET = 16;

    /**
     * 指示是否必须为此标签存储堆栈映射帧.
     */
    static final int STORE = 32;

    /**
     * 指示此标签是否对应于可访问的基础块.
     */
    static final int REACHABLE = 64;

    /**
     * 指示此基础块是否以JSR指令结束.
     */
    static final int JSR = 128;

    /**
     * 指示此基础块是否以RET指令结束.
     */
    static final int RET = 256;

    /**
     * 指示此基础块是否是子程序的开始.
     */
    static final int SUBROUTINE = 512;

    /**
     * 指示visitSubroutine(null, ...)调用是否访问过此子程序基础块.
     */
    static final int VISITED = 1024;

    /**
     * 指示 visitSubroutine(!null, ...) 调用是否访问过此子程序基础块.
     */
    static final int VISITED2 = 2048;

    /**
     * 用于将用户信息与标签关联的字段.
     * Warning: ASM树包使用此字段.
     * 要将它与ASM树包一起使用, 必须覆盖 {@code org.objectweb.asm.tree.MethodNode#getLabelNode}方法.
     */
    public Object info;

    /**
     * 指示此标签的状态.
     */
    int status;

    /**
     * 与此标签对应的行号, 如果已知.
     * 如果有多行, 则每行存储在单独的标签中, 所有行都通过其 next 字段链接
     * (这些链接是在ClassReader中创建的, 并在调用visitLabel之前删除, 因此这不会影响其余的代码).
     */
    int line;

    /**
     * 如果已知, 此标签在代码中的位置.
     */
    int position;

    /**
     * 此标签的前向引用数, 乘以2.
     */
    private int referenceCount;

    /**
     * 有关前向引用的信息.
     * 每个前向引用由该数组中的两个连续整数描述:
     * 第一个是包含前向引用的字节码指令的第一个字节的位置, 而第二个是前向引用本身的第一个字节的位置.
     * 实际上, 第一个整数的符号表示该引用是否使用2或4个字节, 其绝对值给出了字节码指令的位置.
     * 该数组还用作位集来存储基础块所属的子程序.
     * 在解析了所有前向引用之后, {@link MethodWriter#visitMaxs}需要此信息.
     * 因此, 相同的数组可以毫无问题地用于两种目的.
     */
    private int[] srcAndRefPositions;

    // ------------------------------------------------------------------------

    /*
     * 控制流和数据流图分析算法的字段 (用于计算最大堆栈大小或堆栈映射帧).
     * 控制流程图包含每个"基础块"的一个节点, 和每个"jump"从一个基础块到另一个基础块的一个边缘.
     * 每个节点(i.e., 每个基础块) 由对应于该基础块的第一个指令的Label对象表示.
     * 每个节点还将其后继者列表存储在图中, 作为Edge对象的链接列表.
     *
     * 用于计算最大堆栈大小或堆栈映射帧的控制流分析算法类似, 并使用两个步骤.
     * 第一步, 在每条指令访问期间, 在每个基础块的末尾构建有关局部变量状态和操作数堆栈的信息, 称为"输出帧",
     * <i>相对于</i> 基础块开头的帧状态, 称为"输入帧", 在此步骤中<i>未知</i>.
     * 第二步, 在{@link MethodWriter#visitMaxs}中, 是一个定点算法, 用于计算每个基础块的输入帧的信息,
     * 从第一个基础块的输入状态 (从方法签名中得知), 并且使用先前计算的相对输出帧.
     *
     * 用于计算最大堆栈大小的算法仅计算相对输出和绝对输入堆栈高度,
     * 而用于计算堆栈映射帧的算法计算相对输出帧和绝对输入帧.
     */

    /**
     * 相对于输入堆栈的输出堆栈的开始.
     * 该字段的确切语义取决于所使用的算法.
     *
     * 仅计算最大堆栈大小时, 此字段是输入堆栈中的元素数量.
     *
     * 当完全计算堆栈映射帧时, 该字段是第一个输出堆栈元素相对于输入堆栈顶部的偏移量.
     * 此偏移量始终为负或 null. null 偏移量意味着必须将输出堆栈附加到输入堆栈.
     * -n 偏移量意味着前n个输出堆栈元素必须替换顶部前n个输入堆栈元素, 并且其他元素必须附加到输入堆栈.
     */
    int inputStackTop;

    /**
     * 输出堆栈达到的最大高度, 相对于输入堆栈的顶部.
     * 此最大值始终为正或null.
     */
    int outputStackMax;

    /**
     * 有关此基础块的输入和输出堆栈映射帧的信息.
     * 仅当使用{@link ClassWriter#COMPUTE_FRAMES}选项时才使用此字段.
     */
    Frame frame;

    /**
     * 这个标签的后继者, 按他们被访问的顺序.
     * 此链表不包括仅用于调试信息的标签.
     * 如果使用{@link ClassWriter#COMPUTE_FRAMES}选项, 则此外, 它不包含表示相同字节码位置的连续标签
     * (在这种情况下, 只有第一个标签出现在此列表中).
     */
    Label successor;

    /**
     * 控制流图中此节点的后继者.
     * 这些后继存储在{@link Edge Edge}对象的链表中, 通过{@link Edge#next}字段相互链接.
     */
    Edge successors;

    /**
     * 基础块堆栈中的下一个基础块.
     * 该堆栈用于控制流分析算法的第二步中使用的定点算法的主循环中.
     * 它也用于{@link #visitSubroutine}以避免使用递归方法, 并在ClassReader中临时存储标签的多个源代码行.
     */
    Label next;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public Label() {
    }

    // ------------------------------------------------------------------------
    // Methods to compute offsets and to manage forward references
    // ------------------------------------------------------------------------

    /**
     * 返回与此标签对应的偏移量.
     * 此偏移量是从方法的字节码开始计算的.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     *
     * @return 与此标签对应的偏移量.
     * @throws IllegalStateException
     *             如果此标签尚未解析.
     */
    public int getOffset() {
        if ((status & RESOLVED) == 0) {
            throw new IllegalStateException(
                    "Label offset position has not been resolved yet");
        }
        return position;
    }

    /**
     * 在方法的字节码中放入对此标签的引用.
     * 如果已知标签的位置, 则计算并直接写入偏移量.
     * 否则, 将写入null偏移量, 并为此标签声明新的前向引用.
     *
     * @param owner
     *            调用此方法的代码编写器.
     * @param out
     *            方法的字节码.
     * @param source
     *            包含此标签的字节码指令的第一个字节的位置.
     * @param wideOffset
     *            <tt>true</tt>如果引用必须以4个字节存储,
     *            或<tt>false</tt> 如果它必须以2个字节存储.
     * 
     * @throws IllegalArgumentException
     *             如果此标签尚未由给定的代码编写器创建.
     */
    void put(final MethodWriter owner, final ByteVector out, final int source,
            final boolean wideOffset) {
        if ((status & RESOLVED) == 0) {
            if (wideOffset) {
                addReference(-1 - source, out.length);
                out.putInt(-1);
            } else {
                addReference(source, out.length);
                out.putShort(-1);
            }
        } else {
            if (wideOffset) {
                out.putInt(position - source);
            } else {
                out.putShort(position - source);
            }
        }
    }

    /**
     * 添加此标签的前向引用.
     * 必须仅为真正的前向引用调用此方法, i.e. 仅在此标签尚未解析时才调用.
     * 对于后向引用, 必须直接计算和存储引用的偏移量.
     *
     * @param sourcePosition
     *            引用指令的位置.
     *            该位置将用于计算此前向引用的偏移量.
     * @param referencePosition
     *            必须存储此前向引用的偏移量的位置.
     */
    private void addReference(final int sourcePosition,
            final int referencePosition) {
        if (srcAndRefPositions == null) {
            srcAndRefPositions = new int[6];
        }
        if (referenceCount >= srcAndRefPositions.length) {
            int[] a = new int[srcAndRefPositions.length + 6];
            System.arraycopy(srcAndRefPositions, 0, a, 0,
                    srcAndRefPositions.length);
            srcAndRefPositions = a;
        }
        srcAndRefPositions[referenceCount++] = sourcePosition;
        srcAndRefPositions[referenceCount++] = referencePosition;
    }

    /**
     * 解析对此标签的所有前向引用.
     * 当将此标签添加到方法的字节码时, i.e. 当其位置已知时, 必须调用此方法.
     * 此方法通过先前添加到此标签的每个前向引用填充字节码中剩余的空白.
     *
     * @param owner
     *            调用此方法的代码编写器.
     * @param position
     *            这个标签在字节码中的位置.
     * @param data
     *            方法的字节码.
     * 
     * @return <tt>true</tt> 如果留给此标签的空白太小而无法存储偏移量.
     * 在这种情况下, 使用无符号的两个字节偏移量将相应的跳转指令替换为伪指令 (使用未使用的操作码).
     * 在ClassReader中, 这些伪指令将被标准字节码指令替换为具有更宽的偏移 (4个字节而不是2个字节).
     * @throws IllegalArgumentException
     *             如果此标签已经解析, 或者它尚未由给定的代码编写器创建.
     */
    boolean resolve(final MethodWriter owner, final int position,
            final byte[] data) {
        boolean needUpdate = false;
        this.status |= RESOLVED;
        this.position = position;
        int i = 0;
        while (i < referenceCount) {
            int source = srcAndRefPositions[i++];
            int reference = srcAndRefPositions[i++];
            int offset;
            if (source >= 0) {
                offset = position - source;
                if (offset < Short.MIN_VALUE || offset > Short.MAX_VALUE) {
                    /*
                     * 更改跳转指令的操作码, 以便以后能够找到它 (see resizeInstructions in MethodWriter).
                     * 这些临时操作码类似于跳转指令操作码, 但2字节偏移量是无符号的
                     * (并且因此可以表示0到65535之间的值, 这就足够了, 因为方法的大小限制为65535字节).
                     */
                    int opcode = data[reference - 1] & 0xFF;
                    if (opcode <= Opcodes.JSR) {
                        // changes IFEQ ... JSR to opcodes 202 to 217
                        data[reference - 1] = (byte) (opcode + 49);
                    } else {
                        // changes IFNULL and IFNONNULL to opcodes 218 and 219
                        data[reference - 1] = (byte) (opcode + 20);
                    }
                    needUpdate = true;
                }
                data[reference++] = (byte) (offset >>> 8);
                data[reference] = (byte) offset;
            } else {
                offset = position + source + 1;
                data[reference++] = (byte) (offset >>> 24);
                data[reference++] = (byte) (offset >>> 16);
                data[reference++] = (byte) (offset >>> 8);
                data[reference] = (byte) offset;
            }
        }
        return needUpdate;
    }

    /**
     * 返回此标签所属系列的第一个标签.
     * 对于独立的标签或一系列连续标签中的第一个标签, 此方法返回标签本身.
     * 对于其他标签, 它返回该系列的第一个标签.
     *
     * @return 该标签所属系列的第一个标签.
     */
    Label getFirst() {
        return !ClassReader.FRAMES || frame == null ? this : frame.owner;
    }

    // ------------------------------------------------------------------------
    // Methods related to subroutines
    // ------------------------------------------------------------------------

    /**
     * 返回true, 表示此基础块属于给定的子程序.
     *
     * @param id
     *            子程序id.
     * 
     * @return true 表示此基础块属于给定的子程序.
     */
    boolean inSubroutine(final long id) {
        if ((status & Label.VISITED) != 0) {
            return (srcAndRefPositions[(int) (id >>> 32)] & (int) id) != 0;
        }
        return false;
    }

    /**
     * 如果此基础块和给定的块属于公共子程序, 则返回true.
     *
     * @param block
     *            另一个基础块.
     * 
     * @return true 如果此基础块和给定的块属于公共子程序.
     */
    boolean inSameSubroutine(final Label block) {
        if ((status & VISITED) == 0 || (block.status & VISITED) == 0) {
            return false;
        }
        for (int i = 0; i < srcAndRefPositions.length; ++i) {
            if ((srcAndRefPositions[i] & block.srcAndRefPositions[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 标记此基础块属于给定的子程序.
     *
     * @param id
     *            子程序id.
     * @param nbSubroutines
     *            方法中子程序的总数.
     */
    void addToSubroutine(final long id, final int nbSubroutines) {
        if ((status & VISITED) == 0) {
            status |= VISITED;
            srcAndRefPositions = new int[nbSubroutines / 32 + 1];
        }
        srcAndRefPositions[(int) (id >>> 32)] |= (int) id;
    }

    /**
     * 查找属于给定子程序的基础块, 并将这些块标记为属于此子程序.
     * 此方法遵循控制流图, 以查找可从当前块访问的所有块, 而不遵循任何JSR目标.
     *
     * @param JSR
     *            跳转到此子程序的JSR块.
     *            如果此JSR不为null, 则将其添加到子程序中找到的RET块的后继块中.
     * @param id
     *            此子程序的id.
     * @param nbSubroutines
     *            方法中子程序的总数.
     */
    void visitSubroutine(final Label JSR, final long id, final int nbSubroutines) {
        // 用户管理的标签堆栈, 以避免使用递归方法 (递归可能导致非常大的方法的堆栈溢出)
        Label stack = this;
        while (stack != null) {
            // 从堆栈中删除标签l
            Label l = stack;
            stack = l.next;
            l.next = null;

            if (JSR != null) {
                if ((l.status & VISITED2) != 0) {
                    continue;
                }
                l.status |= VISITED2;
                // 如果它是RET块, 则将JSR添加到l的后继中
                if ((l.status & RET) != 0) {
                    if (!l.inSameSubroutine(JSR)) {
                        Edge e = new Edge();
                        e.info = l.inputStackTop;
                        e.successor = JSR.successors.successor;
                        e.next = l.successors;
                        l.successors = e;
                    }
                }
            } else {
                // 如果l块已经属于子程序 'id', 继续
                if (l.inSubroutine(id)) {
                    continue;
                }
                // 将l块标记为属于子程序 'id'
                l.addToSubroutine(id, nbSubroutines);
            }
            // 除了JSR目标之外, 在堆栈上推送l的每个后继
            Edge e = l.successors;
            while (e != null) {
                // 如果l块是一个JSR块, 那么 'l.successors.next'将导致JSR目标(see {@link #visitJumpInsn}), 因此必须不遵循
                if ((l.status & Label.JSR) == 0 || e != l.successors.next) {
                    // 如果尚未添加, 则将e.successor推送到堆栈上
                    if (e.successor.next == null) {
                        e.successor.next = stack;
                        stack = e.successor;
                    }
                }
                e = e.next;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Overriden Object methods
    // ------------------------------------------------------------------------

    @Override
    public String toString() {
        return "L" + System.identityHashCode(this);
    }
}
