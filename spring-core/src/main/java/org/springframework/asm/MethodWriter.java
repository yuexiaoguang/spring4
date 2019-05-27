package org.springframework.asm;

/**
 * 以字节码形式生成方法的{@link MethodVisitor}.
 * 该类的每个访问方法将对应于被访问的指令的字节码附加到字节向量, 按照调用这些方法的顺序.
 */
class MethodWriter extends MethodVisitor {

    /**
     * 用于表示构造函数的伪访问标志.
     */
    static final int ACC_CONSTRUCTOR = 0x80000;

    /**
     * 与前一个堆栈映射帧具有完全相同的局部变量的帧, 并且堆栈项的数量为零.
     */
    static final int SAME_FRAME = 0; // to 63 (0-3f)

    /**
     * 与前一个堆栈映射帧具有完全相同的局部变量的帧, 并且堆栈项的数量为1
     */
    static final int SAME_LOCALS_1_STACK_ITEM_FRAME = 64; // to 127 (40-7f)

    /**
     * 保留供将来使用
     */
    static final int RESERVED = 128;

    /**
     * 与前一个堆栈映射帧具有完全相同的局部变量的帧, 并且堆栈项的数量为1.
     * 偏移量大于63;
     */
    static final int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247; // f7

    /**
     * 当前局部变量与前一帧中的局部变量相同的帧, 除了缺少k个最后的局部变量.
     * k的值由公式 251-frame_type给出.
     */
    static final int CHOP_FRAME = 248; // to 250 (f8-fA)

    /**
     * 与前一个堆栈映射帧具有完全相同的局部变量的帧, 并且堆栈项的数量为零.
     * 偏移量大于 63;
     */
    static final int SAME_FRAME_EXTENDED = 251; // fb

    /**
     * 当前局部变量与前一帧中的局部变量相同的帧, 除了定义了k个其他局部变量.
     * k的值由公式 frame_type-251 给出.
     */
    static final int APPEND_FRAME = 252; // to 254 // fc-fe

    /**
     * 完全的帧
     */
    static final int FULL_FRAME = 255; // ff

    /**
     * 指示必须从头开始重新计算堆栈映射帧.
     * 在这种情况下, 最大堆栈大小和局部变量的数量也从头开始重新计算.
     */
    static final int FRAMES = 0;

    /**
     * 指示必须计算F_INSERT类型的堆栈映射帧.
     * 其他帧不会计算或重新计算.
     * 它们都应该是F_NEW类型, 并且应该足以计算F_INSERT帧的内容, 以及F_NEW和F_INSERT帧之间的字节码指令
     * - 并且不知道类型层次结构 (根据F_INSERT的定义).
     */
    static final int INSERTED_FRAMES = 1;

    /**
     * 指示必须自动计算最大堆栈大小和局部变量数.
     */
    static final int MAXS = 2;

    /**
     * 表示不必自动计算任何内容.
     */
    static final int NOTHING = 3;

    /**
     * 必须添加此方法的类编写器.
     */
    final ClassWriter cw;

    /**
     * 此方法的访问标志.
     */
    private int access;

    /**
     * 包含此方法名称的常量池项的索引.
     */
    private final int name;

    /**
     * 包含此方法描述符的常量池项的索引.
     */
    private final int desc;

    /**
     * 此方法的描述符.
     */
    private final String descriptor;

    /**
     * 此方法的签名.
     */
    String signature;

    /**
     * 如果不为零, 则表示必须从在<code>cw.cr</code>中与此writer的关联的ClassReader复制此方法的代码.
     * 更准确地说, 该字段给出了从<code>cw.cr.b</code>复制的第一个字节的索引.
     */
    int classReaderOffset;

    /**
     * 如果不为零, 则表示必须从在<code>cw.cr</code>中与此writer的关联的ClassReader复制此方法的代码.
     * 更准确地说, 该字段给出了从<code>cw.cr.b</code>复制的字节数.
     */
    int classReaderLength;

    /**
     * 此方法可以抛出的异常数.
     */
    int exceptionCount;

    /**
     * 此方法可以抛出的异常.
     * 更确切地说, 此数组包含常量池项的索引, 这些项包含这些异常类的内部名称.
     */
    int[] exceptions;

    /**
     * 此方法的注解默认属性. May be <tt>null</tt>.
     */
    private ByteVector annd;

    /**
     * 此方法的运行时可见注解. May be <tt>null</tt>.
     */
    private AnnotationWriter anns;

    /**
     * 此方法的运行时不可见注解. May be <tt>null</tt>.
     */
    private AnnotationWriter ianns;

    /**
     * 此方法的运行时可见类型注解. May be <tt>null</tt>.
     */
    private AnnotationWriter tanns;

    /**
     * 此方法的运行时不可见类型注解. May be <tt>null</tt>.
     */
    private AnnotationWriter itanns;

    /**
     * 此方法的运行时可见参数注解. May be <tt>null</tt>.
     */
    private AnnotationWriter[] panns;

    /**
     * 此方法的运行时不可见参数注解. May be <tt>null</tt>.
     */
    private AnnotationWriter[] ipanns;

    /**
     * 此方法的合成参数数量.
     */
    private int synthetics;

    /**
     * 该方法的非标准属性.
     */
    private Attribute attrs;

    /**
     * 此方法的字节码.
     */
    private ByteVector code = new ByteVector();

    /**
     * 此方法的最大堆栈大小.
     */
    private int maxStack;

    /**
     * 此方法的最大局部变量数.
     */
    private int maxLocals;

    /**
     * 当前堆栈映射帧中的局部变量数.
     */
    private int currentLocals;

    /**
     * StackMapTable属性中的堆栈映射帧数.
     */
    int frameCount;

    /**
     * StackMapTable属性.
     */
    private ByteVector stackMap;

    /**
     * StackMapTable属性中写入的最后一帧的偏移量.
     */
    private int previousFrameOffset;

    /**
     * StackMapTable属性中写入的最后一个帧.
     */
    private int[] previousFrame;

    /**
     * 当前堆栈映射帧.
     * 第一个元素包含帧对应的指令的偏移量, 第二个元素是局部变量的数量, 第三个元素是堆栈元素的数量.
     * 局部变量从索引3开始, 后跟操作数堆栈值.
     * 总结: frame[0] = offset, frame[1] = nLocal, frame[2] = nStack, frame[3] = nLocal.
     * 所有类型都编码为整数, 格式与{@link Label}中使用的格式相同, 但仅限于BASE类型.
     */
    private int[] frame;

    /**
     * 异常处理程序列表中的元素数.
     */
    private int handlerCount;

    /**
     * 异常处理程序列表中的第一个元素.
     */
    private Handler firstHandler;

    /**
     * 异常处理程序列表中的最后一个元素.
     */
    private Handler lastHandler;

    /**
     * MethodParameters属性中的条目数.
     */
    private int methodParametersCount;

    /**
     * MethodParameters属性.
     */
    private ByteVector methodParameters;

    /**
     * LocalVariableTable属性中的条目数.
     */
    private int localVarCount;

    /**
     * LocalVariableTable属性.
     */
    private ByteVector localVar;

    /**
     * LocalVariableTypeTable属性中的条目数.
     */
    private int localVarTypeCount;

    /**
     * LocalVariableTypeTable属性.
     */
    private ByteVector localVarType;

    /**
     * LineNumberTable属性中的条目数.
     */
    private int lineNumberCount;

    /**
     * LineNumberTable属性.
     */
    private ByteVector lineNumber;

    /**
     * 上次访问指令的起始偏移量.
     */
    private int lastCodeOffset;

    /**
     * 代码的运行时可见类型注解. May be <tt>null</tt>.
     */
    private AnnotationWriter ctanns;

    /**
     * 代码的运行时不可见类型注解. May be <tt>null</tt>.
     */
    private AnnotationWriter ictanns;

    /**
     * 方法代码的非标准属性.
     */
    private Attribute cattrs;

    /**
     * 此方法中的子程序的数量.
     */
    private int subroutines;

    // ------------------------------------------------------------------------

    /*
     * 控制流图分析算法的字段 (用于计算最大堆栈大小).
     * 控制流程图包含每个"基础块"一个节点, 并且每个"jump"一个边缘, 从一个基础块到另一个基础块.
     * 每个节点(i.e., 每个基础块) 由对应于该基础块的第一个指令的Label对象表示.
     * 每个节点还将其后继者列表存储在图中, 作为Edge对象的链表.
     */

    /**
     * 表示必须自动计算的内容.
     */
    private final int compute;

    /**
     * 标签列表.
     * 此列表是方法中的基础块列表,
     * i.e. 由{@link Label#successor}字段相互链接的Label对象列表,
     * 按{@link MethodVisitor#visitLabel}访问它们的顺序, 从第一个基础块开始.
     */
    private Label labels;

    /**
     * 上一个基础块.
     */
    private Label previousBlock;

    /**
     * 当前的基础块.
     */
    private Label currentBlock;

    /**
     * 上次访问指令后的(相对)堆栈大小.
     * 此大小相对于当前基础块的起始,
     * i.e., 上次访问指令后的真实堆栈大小等于当前基础块的{@link Label#inputStackTop beginStackSize}加上<tt>stackSize</tt>.
     */
    private int stackSize;

    /**
     * 上次访问指令后的(相对)最大堆栈大小.
     * 此大小相对于当前基础块的起始,
     * i.e., 上次访问指令后的真实最大堆栈大小等于当前基础块的{@link Label#inputStackTop beginStackSize}加上<tt>stackSize</tt>.
     */
    private int maxStackSize;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * @param cw
     *            必须添加方法的类编写器.
     * @param access
     *            方法的访问标志 (see {@link Opcodes}).
     * @param name
     *            方法的名称.
     * @param desc
     *            方法的描述符 (see {@link Type}).
     * @param signature
     *            方法的签名. May be <tt>null</tt>.
     * @param exceptions
     *            方法异常的内部名称. May be <tt>null</tt>.
     * @param compute
     *            表示必须自动计算的内容 (see #compute).
     */
    MethodWriter(final ClassWriter cw, final int access, final String name,
            final String desc, final String signature,
            final String[] exceptions, final int compute) {
        super(Opcodes.ASM6);
        if (cw.firstMethod == null) {
            cw.firstMethod = this;
        } else {
            cw.lastMethod.mv = this;
        }
        cw.lastMethod = this;
        this.cw = cw;
        this.access = access;
        if ("<init>".equals(name)) {
            this.access |= ACC_CONSTRUCTOR;
        }
        this.name = cw.newUTF8(name);
        this.desc = cw.newUTF8(desc);
        this.descriptor = desc;
        if (ClassReader.SIGNATURES) {
            this.signature = signature;
        }
        if (exceptions != null && exceptions.length > 0) {
            exceptionCount = exceptions.length;
            this.exceptions = new int[exceptionCount];
            for (int i = 0; i < exceptionCount; ++i) {
                this.exceptions[i] = cw.newClass(exceptions[i]);
            }
        }
        this.compute = compute;
        if (compute != NOTHING) {
            // updates maxLocals
            int size = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
            if ((access & Opcodes.ACC_STATIC) != 0) {
                --size;
            }
            maxLocals = size;
            currentLocals = size;
            // 创建并访问第一个基础块的标签
            labels = new Label();
            labels.status |= Label.PUSHED;
            visitLabel(labels);
        }
    }

    // ------------------------------------------------------------------------
    // Implementation of the MethodVisitor abstract class
    // ------------------------------------------------------------------------

    @Override
    public void visitParameter(String name, int access) {
        if (methodParameters == null) {
            methodParameters = new ByteVector();
        }
        ++methodParametersCount;
        methodParameters.putShort((name == null) ? 0 : cw.newUTF8(name))
                .putShort(access);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        annd = new ByteVector();
        return new AnnotationWriter(cw, false, annd, null, 0);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc,
            final boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // 写入类型, 并为值计数保留空间
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv, 2);
        if (visible) {
            aw.next = anns;
            anns = aw;
        } else {
            aw.next = ianns;
            ianns = aw;
        }
        return aw;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(final int typeRef,
            final TypePath typePath, final String desc, final boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write target_type and target_info
        AnnotationWriter.putTarget(typeRef, typePath, bv);
        // 写入类型, 并为值计数保留空间
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv,
                bv.length - 2);
        if (visible) {
            aw.next = tanns;
            tanns = aw;
        } else {
            aw.next = itanns;
            itanns = aw;
        }
        return aw;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter,
            final String desc, final boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        if ("Ljava/lang/Synthetic;".equals(desc)) {
            // 合成参数的javac中的错误的解决方法
            // see ClassReader.readParameterAnnotations
            synthetics = Math.max(synthetics, parameter + 1);
            return new AnnotationWriter(cw, false, bv, null, 0);
        }
        // 写入类型, 并为值计数保留空间
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv, 2);
        if (visible) {
            if (panns == null) {
                panns = new AnnotationWriter[Type.getArgumentTypes(descriptor).length];
            }
            aw.next = panns[parameter];
            panns[parameter] = aw;
        } else {
            if (ipanns == null) {
                ipanns = new AnnotationWriter[Type.getArgumentTypes(descriptor).length];
            }
            aw.next = ipanns[parameter];
            ipanns[parameter] = aw;
        }
        return aw;
    }

    @Override
    public void visitAttribute(final Attribute attr) {
        if (attr.isCodeAttribute()) {
            attr.next = cattrs;
            cattrs = attr;
        } else {
            attr.next = attrs;
            attrs = attr;
        }
    }

    @Override
    public void visitCode() {
    }

    @Override
    public void visitFrame(final int type, final int nLocal,
            final Object[] local, final int nStack, final Object[] stack) {
        if (!ClassReader.FRAMES || compute == FRAMES) {
            return;
        }

        if (compute == INSERTED_FRAMES) {
            if (currentBlock.frame == null) {
                // 对于隐式的第一个帧, 这应该只发生一次
                // (如果使用EXPAND_ASM_INSNS选项, 则在ClassReader中显式访问).
                currentBlock.frame = new CurrentFrame();
                currentBlock.frame.owner = currentBlock;
                currentBlock.frame.initInputFrame(cw, access,
                        Type.getArgumentTypes(descriptor), nLocal);
                visitImplicitFirstFrame();
            } else {
                if (type == Opcodes.F_NEW) {
                    currentBlock.frame.set(cw, nLocal, local, nStack, stack);
                } else {
                    // 在这种情况下，假设类型等于F_INSERT, currentBlock.frame包含当前指令的堆栈映射帧,
                    // 从最后一个F_NEW帧和字节码指令之间计算出来 (通过调用 CurrentFrame#execute).
                }
                visitFrame(currentBlock.frame);
            }
        } else if (type == Opcodes.F_NEW) {
            if (previousFrame == null) {
                visitImplicitFirstFrame();
            }
            currentLocals = nLocal;
            int frameIndex = startFrame(code.length, nLocal, nStack);
            for (int i = 0; i < nLocal; ++i) {
                if (local[i] instanceof String) {
                    frame[frameIndex++] = Frame.OBJECT
                            | cw.addType((String) local[i]);
                } else if (local[i] instanceof Integer) {
                    frame[frameIndex++] = ((Integer) local[i]).intValue();
                } else {
                    frame[frameIndex++] = Frame.UNINITIALIZED
                            | cw.addUninitializedType("",
                                    ((Label) local[i]).position);
                }
            }
            for (int i = 0; i < nStack; ++i) {
                if (stack[i] instanceof String) {
                    frame[frameIndex++] = Frame.OBJECT
                            | cw.addType((String) stack[i]);
                } else if (stack[i] instanceof Integer) {
                    frame[frameIndex++] = ((Integer) stack[i]).intValue();
                } else {
                    frame[frameIndex++] = Frame.UNINITIALIZED
                            | cw.addUninitializedType("",
                                    ((Label) stack[i]).position);
                }
            }
            endFrame();
        } else {
            int delta;
            if (stackMap == null) {
                stackMap = new ByteVector();
                delta = code.length;
            } else {
                delta = code.length - previousFrameOffset - 1;
                if (delta < 0) {
                    if (type == Opcodes.F_SAME) {
                        return;
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }

            switch (type) {
            case Opcodes.F_FULL:
                currentLocals = nLocal;
                stackMap.putByte(FULL_FRAME).putShort(delta).putShort(nLocal);
                for (int i = 0; i < nLocal; ++i) {
                    writeFrameType(local[i]);
                }
                stackMap.putShort(nStack);
                for (int i = 0; i < nStack; ++i) {
                    writeFrameType(stack[i]);
                }
                break;
            case Opcodes.F_APPEND:
                currentLocals += nLocal;
                stackMap.putByte(SAME_FRAME_EXTENDED + nLocal).putShort(delta);
                for (int i = 0; i < nLocal; ++i) {
                    writeFrameType(local[i]);
                }
                break;
            case Opcodes.F_CHOP:
                currentLocals -= nLocal;
                stackMap.putByte(SAME_FRAME_EXTENDED - nLocal).putShort(delta);
                break;
            case Opcodes.F_SAME:
                if (delta < 64) {
                    stackMap.putByte(delta);
                } else {
                    stackMap.putByte(SAME_FRAME_EXTENDED).putShort(delta);
                }
                break;
            case Opcodes.F_SAME1:
                if (delta < 64) {
                    stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
                } else {
                    stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
                            .putShort(delta);
                }
                writeFrameType(stack[0]);
                break;
            }

            previousFrameOffset = code.length;
            ++frameCount;
        }

        maxStack = Math.max(maxStack, nStack);
        maxLocals = Math.max(maxLocals, currentLocals);
    }

    @Override
    public void visitInsn(final int opcode) {
        lastCodeOffset = code.length;
        // 将指令添加到方法的字节码
        code.putByte(opcode);
        // update currentBlock
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, 0, null, null);
            } else {
                // 更新当前和最大堆栈大小
                int size = stackSize + Frame.SIZE[opcode];
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
            // if opcode == ATHROW or xRETURN, ends current block (no successor)
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                    || opcode == Opcodes.ATHROW) {
                noSuccessor();
            }
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        lastCodeOffset = code.length;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, operand, null, null);
            } else if (opcode != Opcodes.NEWARRAY) {
                // 仅更新NEWARRAY的当前和最大堆栈大小 (BIPUSH或SIPUSH的堆栈大小变化 = 0)
                int size = stackSize + 1;
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // 将指令添加到方法的字节码
        if (opcode == Opcodes.SIPUSH) {
            code.put12(opcode, operand);
        } else { // BIPUSH or NEWARRAY
            code.put11(opcode, operand);
        }
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        lastCodeOffset = code.length;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, var, null, null);
            } else {
                // 更新当前和最大堆栈大小
                if (opcode == Opcodes.RET) {
                    // 没有堆栈更改, 但是当前块的结尾 (没有后继者)
                    currentBlock.status |= Label.RET;
                    // 这里保存'stackSize' 以备将来使用 (see {@link #findSubroutineSuccessors})
                    currentBlock.inputStackTop = stackSize;
                    noSuccessor();
                } else { // xLOAD or xSTORE
                    int size = stackSize + Frame.SIZE[opcode];
                    if (size > maxStackSize) {
                        maxStackSize = size;
                    }
                    stackSize = size;
                }
            }
        }
        if (compute != NOTHING) {
            // updates max locals
            int n;
            if (opcode == Opcodes.LLOAD || opcode == Opcodes.DLOAD
                    || opcode == Opcodes.LSTORE || opcode == Opcodes.DSTORE) {
                n = var + 2;
            } else {
                n = var + 1;
            }
            if (n > maxLocals) {
                maxLocals = n;
            }
        }
        // 将指令添加到方法的字节码
        if (var < 4 && opcode != Opcodes.RET) {
            int opt;
            if (opcode < Opcodes.ISTORE) {
                /* ILOAD_0 */
                opt = 26 + ((opcode - Opcodes.ILOAD) << 2) + var;
            } else {
                /* ISTORE_0 */
                opt = 59 + ((opcode - Opcodes.ISTORE) << 2) + var;
            }
            code.putByte(opt);
        } else if (var >= 256) {
            code.putByte(196 /* WIDE */).put12(opcode, var);
        } else {
            code.put11(opcode, var);
        }
        if (opcode >= Opcodes.ISTORE && compute == FRAMES && handlerCount > 0) {
            visitLabel(new Label());
        }
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        lastCodeOffset = code.length;
        Item i = cw.newStringishItem(ClassWriter.CLASS, type);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, code.length, cw, i);
            } else if (opcode == Opcodes.NEW) {
                // 仅当opcode == NEW时才更新当前和最大堆栈大小
                // (ANEWARRAY, CHECKCAST, INSTANCEOF 没有堆栈更改)
                int size = stackSize + 1;
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // 将指令添加到方法的字节码
        code.put12(opcode, i.index);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        lastCodeOffset = code.length;
        Item i = cw.newFieldItem(owner, name, desc);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, 0, cw, i);
            } else {
                int size;
                // 计算堆栈大小变化
                char c = desc.charAt(0);
                switch (opcode) {
                case Opcodes.GETSTATIC:
                    size = stackSize + (c == 'D' || c == 'J' ? 2 : 1);
                    break;
                case Opcodes.PUTSTATIC:
                    size = stackSize + (c == 'D' || c == 'J' ? -2 : -1);
                    break;
                case Opcodes.GETFIELD:
                    size = stackSize + (c == 'D' || c == 'J' ? 1 : 0);
                    break;
                // case Constants.PUTFIELD:
                default:
                    size = stackSize + (c == 'D' || c == 'J' ? -3 : -2);
                    break;
                }
                // 更新当前和最大堆栈大小
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // 将指令添加到方法的字节码
        code.put12(opcode, i.index);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        lastCodeOffset = code.length;
        Item i = cw.newMethodItem(owner, name, desc, itf);
        int argSize = i.intVal;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, 0, cw, i);
            } else {
                /*
                 * 计算堆栈大小变化.
                 * 为了不对同一个Item的这种变化重复计算多次, 使用此Item的intVal字段来存储此变化, 一旦计算出来.
                 * 更确切地说, 此intVal字段存储参数的大小以及与desc对应的返回值.
                 */
                if (argSize == 0) {
                    // 尚未计算上述大小, 因此计算它们...
                    argSize = Type.getArgumentsAndReturnSizes(desc);
                    // ... 按顺序保存它们
                    // 不要在将来重新计算它们
                    i.intVal = argSize;
                }
                int size;
                if (opcode == Opcodes.INVOKESTATIC) {
                    size = stackSize - (argSize >> 2) + (argSize & 0x03) + 1;
                } else {
                    size = stackSize - (argSize >> 2) + (argSize & 0x03);
                }
                // 更新当前和最大堆栈大小
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // 将指令添加到方法的字节码
        if (opcode == Opcodes.INVOKEINTERFACE) {
            if (argSize == 0) {
                argSize = Type.getArgumentsAndReturnSizes(desc);
                i.intVal = argSize;
            }
            code.put12(Opcodes.INVOKEINTERFACE, i.index).put11(argSize >> 2, 0);
        } else {
            code.put12(opcode, i.index);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(final String name, final String desc,
            final Handle bsm, final Object... bsmArgs) {
        lastCodeOffset = code.length;
        Item i = cw.newInvokeDynamicItem(name, desc, bsm, bsmArgs);
        int argSize = i.intVal;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(Opcodes.INVOKEDYNAMIC, 0, cw, i);
            } else {
                /*
                 * 计算堆栈大小变化.
                 * 为了不对同一个Item的这种变化重复计算多次, 使用此Item的intVal字段来存储此变化, 一旦计算出来.
                 * 更确切地说, 此intVal字段存储参数的大小以及与desc对应的返回值.
                 */
                if (argSize == 0) {
                    // 尚未计算上述大小, 因此计算它们...
                    argSize = Type.getArgumentsAndReturnSizes(desc);
                    // ... 按顺序保存它们
                    // 不要在将来重新计算它们
                    i.intVal = argSize;
                }
                int size = stackSize - (argSize >> 2) + (argSize & 0x03) + 1;

                // 更新当前和最大堆栈大小
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // 将指令添加到方法的字节码
        code.put12(Opcodes.INVOKEDYNAMIC, i.index);
        code.putShort(0);
    }

    @Override
    public void visitJumpInsn(int opcode, final Label label) {
        boolean isWide = opcode >= 200; // GOTO_W
        opcode = isWide ? opcode - 33 : opcode;
        lastCodeOffset = code.length;
        Label nextInsn = null;
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock.frame.execute(opcode, 0, null, null);
                // 'label' 是跳转指令的目标
                label.getFirst().status |= Label.TARGET;
                // 添加'label'作为此基础块的后继者
                addSuccessor(Edge.NORMAL, label);
                if (opcode != Opcodes.GOTO) {
                    // 为下一个基础块创建一个Label
                    nextInsn = new Label();
                }
            } else if (compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(opcode, 0, null, null);
            } else {
                if (opcode == Opcodes.JSR) {
                    if ((label.status & Label.SUBROUTINE) == 0) {
                        label.status |= Label.SUBROUTINE;
                        ++subroutines;
                    }
                    currentBlock.status |= Label.JSR;
                    addSuccessor(stackSize + 1, label);
                    // 为下一个基础块创建一个Label
                    nextInsn = new Label();
                    /*
                     * 请注意, 通过此方法中的构造, JSR块在控制流图中至少具有两个后继者:
                     * 第一个引导JSR之后的下一个指令, 而第二个引导JSR目标.
                     */
                } else {
                    // 更新当前堆栈大小 (最大堆栈大小不变, 因为在这种情况下堆栈大小变化始终为负)
                    stackSize += Frame.SIZE[opcode];
                    addSuccessor(stackSize, label);
                }
            }
        }
        // 将指令添加到方法的字节码
        if ((label.status & Label.RESOLVED) != 0
                && label.position - code.length < Short.MIN_VALUE) {
            /*
             * 向后跳跃的情况, 偏移量 < -32768.
             * 在这种情况下, 自动将GOTO替换为GOTO_W, JSR 替换为JSR_W, IFxxx <l> 替换为 IFNOTxxx <L> GOTO_W <l> L:...,
             * 其中IFNOTxxx 是IFxxx的"相反"操作码(i.e., IFNE 对于 IFEQ), 其中 <L> 指定GOTO_W之后的指令.
             */
            if (opcode == Opcodes.GOTO) {
                code.putByte(200); // GOTO_W
            } else if (opcode == Opcodes.JSR) {
                code.putByte(201); // JSR_W
            } else {
                // 如果IF指令转换为 IFNOT GOTO_W, 则下一条指令成为IFNOT指令的目标
                if (nextInsn != null) {
                    nextInsn.status |= Label.TARGET;
                }
                code.putByte(opcode <= 166 ? ((opcode + 1) ^ 1) - 1
                        : opcode ^ 1);
                code.putShort(8); // jump offset
                // ASM伪 GOTO_W insn, see ClassReader.
                // 不使用真正的GOTO_W, 因为可能需要在之后插入一个帧 (作为 IFNOTxxx 跳转指令的目标).
                code.putByte(220);
                cw.hasAsmInsns = true;
            }
            label.put(this, code, code.length - 1, true);
        } else if (isWide) {
            /*
             * 用户指定的GOTO_W或JSR_W的情况 (通常用于调整指令大小时的ClassReader).
             * 在这种情况下, 保留原始指令.
             */
            code.putByte(opcode + 33);
            label.put(this, code, code.length - 1, true);
        } else {
            /*
             * 向后跳跃的偏移 >= -32768 的情况, 或具有当前未知偏移量的向前跳跃的情况.
             * 在这些情况下, 将偏移量存储为2个字节 (如果需要, 将在resizeInstructions中增加).
             */
            code.putByte(opcode);
            label.put(this, code, code.length - 1, false);
        }
        if (currentBlock != null) {
            if (nextInsn != null) {
                // 如果跳转指令不是GOTO, 则下一条指令也是该指令的后继指令.
                // 调用visitLabel会将此下一条指令的标签添加为当前块的后继者, 并开始一个新的基础块
                visitLabel(nextInsn);
            }
            if (opcode == Opcodes.GOTO) {
                noSuccessor();
            }
        }
    }

    @Override
    public void visitLabel(final Label label) {
        // 解析之前对标签的前向引用
        cw.hasAsmInsns |= label.resolve(this, code.length, code.data);
        // updates currentBlock
        if ((label.status & Label.DEBUG) != 0) {
            return;
        }
        if (compute == FRAMES) {
            if (currentBlock != null) {
                if (label.position == currentBlock.position) {
                    // 后继标签, 不要开始新的基本块
                    currentBlock.status |= (label.status & Label.TARGET);
                    label.frame = currentBlock.frame;
                    return;
                }
                // 结束当前块 (使用一个新的后继者)
                addSuccessor(Edge.NORMAL, label);
            }
            // 开始一个新的当前块
            currentBlock = label;
            if (label.frame == null) {
                label.frame = new Frame();
                label.frame.owner = label;
            }
            // 更新基础块列表
            if (previousBlock != null) {
                if (label.position == previousBlock.position) {
                    previousBlock.status |= (label.status & Label.TARGET);
                    label.frame = previousBlock.frame;
                    currentBlock = previousBlock;
                    return;
                }
                previousBlock.successor = label;
            }
            previousBlock = label;
        } else if (compute == INSERTED_FRAMES) {
            if (currentBlock == null) {
                // 对于构造函数中的visitLabel调用, 此情况应该只发生一次.
                // 实际上, 如果compute等于INSERTED_FRAMES, 则currentBlock不能设置回null (see #noSuccessor).
                currentBlock = label;
            } else {
                // 更新帧所有者, 以便在 visitFrame(Frame)中计算正确的帧偏移量.
                currentBlock.frame.owner = label;
            }
        } else if (compute == MAXS) {
            if (currentBlock != null) {
                // 结束当前块 (使用一个新的后继者)
                currentBlock.outputStackMax = maxStackSize;
                addSuccessor(stackSize, label);
            }
            // 开始一个新的当前块
            currentBlock = label;
            // 重置相关当前和最大堆栈大小
            stackSize = 0;
            maxStackSize = 0;
            // 更新基础块列表
            if (previousBlock != null) {
                previousBlock.successor = label;
            }
            previousBlock = label;
        }
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        lastCodeOffset = code.length;
        Item i = cw.newConstItem(cst);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(Opcodes.LDC, 0, cw, i);
            } else {
                int size;
                // 计算堆栈大小变化
                if (i.type == ClassWriter.LONG || i.type == ClassWriter.DOUBLE) {
                    size = stackSize + 2;
                } else {
                    size = stackSize + 1;
                }
                // 更新当前和最大堆栈大小
                if (size > maxStackSize) {
                    maxStackSize = size;
                }
                stackSize = size;
            }
        }
        // 将指令添加到方法的字节码
        int index = i.index;
        if (i.type == ClassWriter.LONG || i.type == ClassWriter.DOUBLE) {
            code.put12(20 /* LDC2_W */, index);
        } else if (index >= 256) {
            code.put12(19 /* LDC_W */, index);
        } else {
            code.put11(Opcodes.LDC, index);
        }
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        lastCodeOffset = code.length;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(Opcodes.IINC, var, null, null);
            }
        }
        if (compute != NOTHING) {
            // updates max locals
            int n = var + 1;
            if (n > maxLocals) {
                maxLocals = n;
            }
        }
        // 将指令添加到方法的字节码
        if ((var > 255) || (increment > 127) || (increment < -128)) {
            code.putByte(196 /* WIDE */).put12(Opcodes.IINC, var)
                    .putShort(increment);
        } else {
            code.putByte(Opcodes.IINC).put11(var, increment);
        }
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
            final Label dflt, final Label... labels) {
        lastCodeOffset = code.length;
        // 将指令添加到方法的字节码
        int source = code.length;
        code.putByte(Opcodes.TABLESWITCH);
        code.putByteArray(null, 0, (4 - code.length % 4) % 4);
        dflt.put(this, code, source, true);
        code.putInt(min).putInt(max);
        for (int i = 0; i < labels.length; ++i) {
            labels[i].put(this, code, source, true);
        }
        // updates currentBlock
        visitSwitchInsn(dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
            final Label[] labels) {
        lastCodeOffset = code.length;
        // 将指令添加到方法的字节码
        int source = code.length;
        code.putByte(Opcodes.LOOKUPSWITCH);
        code.putByteArray(null, 0, (4 - code.length % 4) % 4);
        dflt.put(this, code, source, true);
        code.putInt(labels.length);
        for (int i = 0; i < labels.length; ++i) {
            code.putInt(keys[i]);
            labels[i].put(this, code, source, true);
        }
        // updates currentBlock
        visitSwitchInsn(dflt, labels);
    }

    private void visitSwitchInsn(final Label dflt, final Label[] labels) {
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock.frame.execute(Opcodes.LOOKUPSWITCH, 0, null, null);
                // 添加当前块后继者
                addSuccessor(Edge.NORMAL, dflt);
                dflt.getFirst().status |= Label.TARGET;
                for (int i = 0; i < labels.length; ++i) {
                    addSuccessor(Edge.NORMAL, labels[i]);
                    labels[i].getFirst().status |= Label.TARGET;
                }
            } else {
                // 更新当前堆栈大小 (最大堆栈大小不变)
                --stackSize;
                // 添加当前块后继者
                addSuccessor(stackSize, dflt);
                for (int i = 0; i < labels.length; ++i) {
                    addSuccessor(stackSize, labels[i]);
                }
            }
            // 结束当前块
            noSuccessor();
        }
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        lastCodeOffset = code.length;
        Item i = cw.newStringishItem(ClassWriter.CLASS, desc);
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES || compute == INSERTED_FRAMES) {
                currentBlock.frame.execute(Opcodes.MULTIANEWARRAY, dims, cw, i);
            } else {
                // 更新当前堆栈大小 (最大堆栈大小不变, 因为堆栈大小变化始终为负或null)
                stackSize += 1 - dims;
            }
        }
        // 将指令添加到方法的字节码
        code.put12(Opcodes.MULTIANEWARRAY, i.index).putByte(dims);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef,
            TypePath typePath, String desc, boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write target_type and target_info
        typeRef = (typeRef & 0xFF0000FF) | (lastCodeOffset << 8);
        AnnotationWriter.putTarget(typeRef, typePath, bv);
        // 写入类型, 并为值计数保留空间
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv,
                bv.length - 2);
        if (visible) {
            aw.next = ctanns;
            ctanns = aw;
        } else {
            aw.next = ictanns;
            ictanns = aw;
        }
        return aw;
    }

    @Override
    public void visitTryCatchBlock(final Label start, final Label end,
            final Label handler, final String type) {
        ++handlerCount;
        Handler h = new Handler();
        h.start = start;
        h.end = end;
        h.handler = handler;
        h.desc = type;
        h.type = type != null ? cw.newClass(type) : 0;
        if (lastHandler == null) {
            firstHandler = h;
        } else {
            lastHandler.next = h;
        }
        lastHandler = h;
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef,
            TypePath typePath, String desc, boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write target_type and target_info
        AnnotationWriter.putTarget(typeRef, typePath, bv);
        // 写入类型, 并为值计数保留空间
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv,
                bv.length - 2);
        if (visible) {
            aw.next = ctanns;
            ctanns = aw;
        } else {
            aw.next = ictanns;
            ictanns = aw;
        }
        return aw;
    }

    @Override
    public void visitLocalVariable(final String name, final String desc,
            final String signature, final Label start, final Label end,
            final int index) {
        if (signature != null) {
            if (localVarType == null) {
                localVarType = new ByteVector();
            }
            ++localVarTypeCount;
            localVarType.putShort(start.position)
                    .putShort(end.position - start.position)
                    .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(signature))
                    .putShort(index);
        }
        if (localVar == null) {
            localVar = new ByteVector();
        }
        ++localVarCount;
        localVar.putShort(start.position)
                .putShort(end.position - start.position)
                .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(desc))
                .putShort(index);
        if (compute != NOTHING) {
            // updates max locals
            char c = desc.charAt(0);
            int n = index + (c == 'J' || c == 'D' ? 2 : 1);
            if (n > maxLocals) {
                maxLocals = n;
            }
        }
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
            TypePath typePath, Label[] start, Label[] end, int[] index,
            String desc, boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write target_type and target_info
        bv.putByte(typeRef >>> 24).putShort(start.length);
        for (int i = 0; i < start.length; ++i) {
            bv.putShort(start[i].position)
                    .putShort(end[i].position - start[i].position)
                    .putShort(index[i]);
        }
        if (typePath == null) {
            bv.putByte(0);
        } else {
            int length = typePath.b[typePath.offset] * 2 + 1;
            bv.putByteArray(typePath.b, typePath.offset, length);
        }
        // 写入类型, 并为值计数保留空间
        bv.putShort(cw.newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv,
                bv.length - 2);
        if (visible) {
            aw.next = ctanns;
            ctanns = aw;
        } else {
            aw.next = ictanns;
            ictanns = aw;
        }
        return aw;
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
        if (lineNumber == null) {
            lineNumber = new ByteVector();
        }
        ++lineNumberCount;
        lineNumber.putShort(start.position);
        lineNumber.putShort(line);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        if (ClassReader.FRAMES && compute == FRAMES) {
            // 使用异常处理程序块完成控制流图
            Handler handler = firstHandler;
            while (handler != null) {
                Label l = handler.start.getFirst();
                Label h = handler.handler.getFirst();
                Label e = handler.end.getFirst();
                // 计算'h'的边缘类型
                String t = handler.desc == null ? "java/lang/Throwable"
                        : handler.desc;
                int kind = Frame.OBJECT | cw.addType(t);
                // h 是一个异常处理程序
                h.status |= Label.TARGET;
                // 将'h'添加为'start'和'end'之间的标签后继者
                while (l != e) {
                    // 为'h'创建一个边缘
                    Edge b = new Edge();
                    b.info = kind;
                    b.successor = h;
                    // 将它添加到'l'的后继者
                    b.next = l.successors;
                    l.successors = b;
                    // 下一个标签
                    l = l.successor;
                }
                handler = handler.next;
            }

            // 创建并访问第一个(隐式)帧
            Frame f = labels.frame;
            f.initInputFrame(cw, access, Type.getArgumentTypes(descriptor),
                    this.maxLocals);
            visitFrame(f);

            /*
             * 修复点算法: 将第一个基础块标记为 '已更改'
             * (i.e. 将其放入'changed'列表), 当有基础块改变时, 选择一个, 将其标记为未更改, 并更新其后续块 (可在此过程中更改).
             */
            int max = 0;
            Label changed = labels;
            while (changed != null) {
                // 从已更改的基础块列表中删除基础块
                Label l = changed;
                changed = changed.next;
                l.next = null;
                f = l.frame;
                // 必须将可到达的跳转目标存储在堆栈映射中
                if ((l.status & Label.TARGET) != 0) {
                    l.status |= Label.STORE;
                }
                // 根据定义, 所有访问过的标签都是可到达的
                l.status |= Label.REACHABLE;
                // 更新(绝对)最大堆栈大小
                int blockMax = f.inputStack.length + l.outputStackMax;
                if (blockMax > max) {
                    max = blockMax;
                }
                // 更新当前基础块的后继者
                Edge e = l.successors;
                while (e != null) {
                    Label n = e.successor.getFirst();
                    boolean change = f.merge(cw, n.frame, e.info);
                    if (change && n.next == null) {
                        // 如果n已更改, 且尚未在'changed'列表中, 将其添加到此列表中
                        n.next = changed;
                        changed = n;
                    }
                    e = e.next;
                }
            }

            // 访问必须存储在堆栈映射中的所有帧
            Label l = labels;
            while (l != null) {
                f = l.frame;
                if ((l.status & Label.STORE) != 0) {
                    visitFrame(f);
                }
                if ((l.status & Label.REACHABLE) == 0) {
                    // 查找死的基础块的起始和结尾
                    Label k = l.successor;
                    int start = l.position;
                    int end = (k == null ? code.length : k.position) - 1;
                    // 如果是非空基础块
                    if (end >= start) {
                        max = Math.max(max, 1);
                        // 用NOP替换指令 ... NOP ATHROW
                        for (int i = start; i < end; ++i) {
                            code.data[i] = Opcodes.NOP;
                        }
                        code.data[end] = (byte) Opcodes.ATHROW;
                        // 为此无法访问的块发出帧
                        int frameIndex = startFrame(start, 0, 1);
                        frame[frameIndex] = Frame.OBJECT
                                | cw.addType("java/lang/Throwable");
                        endFrame();
                        // 从异常处理程序中删除start-end范围
                        firstHandler = Handler.remove(firstHandler, l, k);
                    }
                }
                l = l.successor;
            }

            handler = firstHandler;
            handlerCount = 0;
            while (handler != null) {
                handlerCount += 1;
                handler = handler.next;
            }

            this.maxStack = max;
        } else if (compute == MAXS) {
            // 使用异常处理程序块完成控制流图
            Handler handler = firstHandler;
            while (handler != null) {
                Label l = handler.start;
                Label h = handler.handler;
                Label e = handler.end;
                // 将'h'添加为'start'和'end'之间的标签后继者
                while (l != e) {
                    // 为'h'创建一个边缘
                    Edge b = new Edge();
                    b.info = Edge.EXCEPTION;
                    b.successor = h;
                    // 将它添加到'l'的后继者
                    if ((l.status & Label.JSR) == 0) {
                        b.next = l.successors;
                        l.successors = b;
                    } else {
                        // 如果l是JSR块, 则在前两个边缘之后添加b, 以保留关于JSR块后继者顺序的假设 (see {@link #visitJumpInsn})
                        b.next = l.successors.next.next;
                        l.successors.next.next = b;
                    }
                    // 下一个标签
                    l = l.successor;
                }
                handler = handler.next;
            }

            if (subroutines > 0) {
                // 使用RET后继者完成控制流程图
                /*
                 * 第一步: 查找子程序. 对于每个基础块, 该步骤确定它属于哪个子程序.
                 */
                // 找到属于"main"子程序的基础块
                int id = 0;
                labels.visitSubroutine(null, 1, subroutines);
                // 查找属于真实子程序的基础块
                Label l = labels;
                while (l != null) {
                    if ((l.status & Label.JSR) != 0) {
                        // 子程序由 l 的TARGET定义, 而不是由l定义
                        Label subroutine = l.successors.next.successor;
                        // 如果尚未访问此子程序...
                        if ((subroutine.status & Label.VISITED) == 0) {
                            // ...为它分配一个新的id, 并查找它的基础块
                            id += 1;
                            subroutine.visitSubroutine(null, (id / 32L) << 32
                                    | (1L << (id % 32)), subroutines);
                        }
                    }
                    l = l.successor;
                }
                // 第二步: 查找RET块的后继者
                l = labels;
                while (l != null) {
                    if ((l.status & Label.JSR) != 0) {
                        Label L = labels;
                        while (L != null) {
                            L.status &= ~Label.VISITED2;
                            L = L.successor;
                        }
                        // 子程序由 l 的TARGET定义, 而不是由l定义
                        Label subroutine = l.successors.next.successor;
                        subroutine.visitSubroutine(l, 0, subroutines);
                    }
                    l = l.successor;
                }
            }

            /*
             * 控制流分析算法:
             * 当块堆栈不为空时, 从该堆栈弹出一个块, 更新最大堆栈大小,
             * 计算此块后继者的真实(非相对) 开始堆栈大小,
             * 并将这些后继者推入堆栈 (除非它们已被推入堆栈).
             * Note: 根据假设，块堆栈中块的{@link Label#inputStackTop}是这些块的真实(非相对)起始堆栈大小.
             */
            int max = 0;
            Label stack = labels;
            while (stack != null) {
                // 从堆栈中弹出一个块
                Label l = stack;
                stack = stack.next;
                // 计算此块的真实 (非相对)最大堆栈大小
                int start = l.inputStackTop;
                int blockMax = start + l.outputStackMax;
                // 更新全局最大堆栈大小
                if (blockMax > max) {
                    max = blockMax;
                }
                // 分析块的后继者
                Edge b = l.successors;
                if ((l.status & Label.JSR) != 0) {
                    // 忽略JSR块的第一个边缘 (虚拟后继者)
                    b = b.next;
                }
                while (b != null) {
                    l = b.successor;
                    // 如果这个后继者还没有被推入...
                    if ((l.status & Label.PUSHED) == 0) {
                        // 计算其真正的起始堆栈大小...
                        l.inputStackTop = b.info == Edge.EXCEPTION ? 1 : start
                                + b.info;
                        // ...并将其推入堆栈
                        l.status |= Label.PUSHED;
                        l.next = stack;
                        stack = l;
                    }
                    b = b.next;
                }
            }
            this.maxStack = Math.max(maxStack, max);
        } else {
            this.maxStack = maxStack;
            this.maxLocals = maxLocals;
        }
    }

    @Override
    public void visitEnd() {
    }

    // ------------------------------------------------------------------------
    // Utility methods: control flow analysis algorithm
    // ------------------------------------------------------------------------

    /**
     * 添加{@link #currentBlock currentBlock}块的后继者.
     * 
     * @param info
     *            有关要添加的控制流边缘的信息.
     * @param successor
     *            要添加到当前块的后继块.
     */
    private void addSuccessor(final int info, final Label successor) {
        // 创建并初始化Edge 对象...
        Edge b = new Edge();
        b.info = info;
        b.successor = successor;
        // ...并将其添加到currentBlock块的后继列表中
        b.next = currentBlock.successors;
        currentBlock.successors = b;
    }

    /**
     * 结束当前的基础块.
     * 必须在当前基础块没有任何后继块的情况下, 使用此方法.
     */
    private void noSuccessor() {
        if (compute == FRAMES) {
            Label l = new Label();
            l.frame = new Frame();
            l.frame.owner = l;
            l.resolve(this, code.length, code.data);
            previousBlock.successor = l;
            previousBlock = l;
        } else {
            currentBlock.outputStackMax = maxStackSize;
        }
        if (compute != INSERTED_FRAMES) {
            currentBlock = null;
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods: stack map frames
    // ------------------------------------------------------------------------

    /**
     * 访问从头开始计算的帧.
     * 
     * @param f
     *            必须访问的帧.
     */
    private void visitFrame(final Frame f) {
        int i, t;
        int nTop = 0;
        int nLocal = 0;
        int nStack = 0;
        int[] locals = f.inputLocals;
        int[] stacks = f.inputStack;
        // 计算局部变量的数量 (忽略LONG或DOUBLE之后的TOP类型, 以及所有尾随的TOP类型)
        for (i = 0; i < locals.length; ++i) {
            t = locals[i];
            if (t == Frame.TOP) {
                ++nTop;
            } else {
                nLocal += nTop + 1;
                nTop = 0;
            }
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i;
            }
        }
        // 计算堆栈大小 (忽略LONG或DOUBLE之后的TOP类型)
        for (i = 0; i < stacks.length; ++i) {
            t = stacks[i];
            ++nStack;
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i;
            }
        }
        // 访问帧及其内容
        int frameIndex = startFrame(f.owner.position, nLocal, nStack);
        for (i = 0; nLocal > 0; ++i, --nLocal) {
            t = locals[i];
            frame[frameIndex++] = t;
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i;
            }
        }
        for (i = 0; i < stacks.length; ++i) {
            t = stacks[i];
            frame[frameIndex++] = t;
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i;
            }
        }
        endFrame();
    }

    /**
     * 访问此方法的隐式的第一个帧.
     */
    private void visitImplicitFirstFrame() {
        // 最多可以有 descriptor.length() + 1 局部变量
        int frameIndex = startFrame(0, descriptor.length() + 1, 0);
        if ((access & Opcodes.ACC_STATIC) == 0) {
            if ((access & ACC_CONSTRUCTOR) == 0) {
                frame[frameIndex++] = Frame.OBJECT | cw.addType(cw.thisName);
            } else {
                frame[frameIndex++] = 6; // Opcodes.UNINITIALIZED_THIS;
            }
        }
        int i = 1;
        loop: while (true) {
            int j = i;
            switch (descriptor.charAt(i++)) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
                frame[frameIndex++] = 1; // Opcodes.INTEGER;
                break;
            case 'F':
                frame[frameIndex++] = 2; // Opcodes.FLOAT;
                break;
            case 'J':
                frame[frameIndex++] = 4; // Opcodes.LONG;
                break;
            case 'D':
                frame[frameIndex++] = 3; // Opcodes.DOUBLE;
                break;
            case '[':
                while (descriptor.charAt(i) == '[') {
                    ++i;
                }
                if (descriptor.charAt(i) == 'L') {
                    ++i;
                    while (descriptor.charAt(i) != ';') {
                        ++i;
                    }
                }
                frame[frameIndex++] = Frame.OBJECT
                        | cw.addType(descriptor.substring(j, ++i));
                break;
            case 'L':
                while (descriptor.charAt(i) != ';') {
                    ++i;
                }
                frame[frameIndex++] = Frame.OBJECT
                        | cw.addType(descriptor.substring(j + 1, i++));
                break;
            default:
                break loop;
            }
        }
        frame[1] = frameIndex - 3;
        endFrame();
    }

    /**
     * 开始访问堆栈映射帧.
     * 
     * @param offset
     *            帧对应的指令的偏移量.
     * @param nLocal
     *            帧中局部变量的数量.
     * @param nStack
     *            帧中的堆栈元素的数量.
     * 
     * @return 要在此帧中写入的下一个元素的索引.
     */
    private int startFrame(final int offset, final int nLocal, final int nStack) {
        int n = 3 + nLocal + nStack;
        if (frame == null || frame.length < n) {
            frame = new int[n];
        }
        frame[0] = offset;
        frame[1] = nLocal;
        frame[2] = nStack;
        return 3;
    }

    /**
     * 检查当前帧{@link #frame}的访问是否完成, 如果是, 则将其写入StackMapTable属性.
     */
    private void endFrame() {
        if (previousFrame != null) { // 不要写入第一个帧
            if (stackMap == null) {
                stackMap = new ByteVector();
            }
            writeFrame();
            ++frameCount;
        }
        previousFrame = frame;
        frame = null;
    }

    /**
     * 压缩并在StackMapTable属性中写入当前帧{@link #frame}.
     */
    private void writeFrame() {
        int clocalsSize = frame[1];
        int cstackSize = frame[2];
        if ((cw.version & 0xFFFF) < Opcodes.V1_6) {
            stackMap.putShort(frame[0]).putShort(clocalsSize);
            writeFrameTypes(3, 3 + clocalsSize);
            stackMap.putShort(cstackSize);
            writeFrameTypes(3 + clocalsSize, 3 + clocalsSize + cstackSize);
            return;
        }
        int localsSize = previousFrame[1];
        int type = FULL_FRAME;
        int k = 0;
        int delta;
        if (frameCount == 0) {
            delta = frame[0];
        } else {
            delta = frame[0] - previousFrame[0] - 1;
        }
        if (cstackSize == 0) {
            k = clocalsSize - localsSize;
            switch (k) {
            case -3:
            case -2:
            case -1:
                type = CHOP_FRAME;
                localsSize = clocalsSize;
                break;
            case 0:
                type = delta < 64 ? SAME_FRAME : SAME_FRAME_EXTENDED;
                break;
            case 1:
            case 2:
            case 3:
                type = APPEND_FRAME;
                break;
            }
        } else if (clocalsSize == localsSize && cstackSize == 1) {
            type = delta < 63 ? SAME_LOCALS_1_STACK_ITEM_FRAME
                    : SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
        }
        if (type != FULL_FRAME) {
            // 验证局部变量是否相同
            int l = 3;
            for (int j = 0; j < localsSize; j++) {
                if (frame[l] != previousFrame[l]) {
                    type = FULL_FRAME;
                    break;
                }
                l++;
            }
        }
        switch (type) {
        case SAME_FRAME:
            stackMap.putByte(delta);
            break;
        case SAME_LOCALS_1_STACK_ITEM_FRAME:
            stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME + delta);
            writeFrameTypes(3 + clocalsSize, 4 + clocalsSize);
            break;
        case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
            stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(
                    delta);
            writeFrameTypes(3 + clocalsSize, 4 + clocalsSize);
            break;
        case SAME_FRAME_EXTENDED:
            stackMap.putByte(SAME_FRAME_EXTENDED).putShort(delta);
            break;
        case CHOP_FRAME:
            stackMap.putByte(SAME_FRAME_EXTENDED + k).putShort(delta);
            break;
        case APPEND_FRAME:
            stackMap.putByte(SAME_FRAME_EXTENDED + k).putShort(delta);
            writeFrameTypes(3 + localsSize, 3 + clocalsSize);
            break;
        // case FULL_FRAME:
        default:
            stackMap.putByte(FULL_FRAME).putShort(delta).putShort(clocalsSize);
            writeFrameTypes(3, 3 + clocalsSize);
            stackMap.putShort(cstackSize);
            writeFrameTypes(3 + clocalsSize, 3 + clocalsSize + cstackSize);
        }
    }

    /**
     * 将当前帧{@link #frame}的某些类型写入StackMapTableAttribute.
     * 此方法将类型从{@link Label}中使用的格式转换为StackMapTable属性中使用的格式.
     * 特别是, 它将类型表索引转换为常量池索引.
     * 
     * @param start
     *            要写入的{@link #frame}中的第一个类型的索引.
     * @param end
     *            要写入的{@link #frame}中的最后一个类型的索引 (不包括).
     */
    private void writeFrameTypes(final int start, final int end) {
        for (int i = start; i < end; ++i) {
            int t = frame[i];
            int d = t & Frame.DIM;
            if (d == 0) {
                int v = t & Frame.BASE_VALUE;
                switch (t & Frame.BASE_KIND) {
                case Frame.OBJECT:
                    stackMap.putByte(7).putShort(
                            cw.newClass(cw.typeTable[v].strVal1));
                    break;
                case Frame.UNINITIALIZED:
                    stackMap.putByte(8).putShort(cw.typeTable[v].intVal);
                    break;
                default:
                    stackMap.putByte(v);
                }
            } else {
                StringBuilder sb = new StringBuilder();
                d >>= 28;
                while (d-- > 0) {
                    sb.append('[');
                }
                if ((t & Frame.BASE_KIND) == Frame.OBJECT) {
                    sb.append('L');
                    sb.append(cw.typeTable[t & Frame.BASE_VALUE].strVal1);
                    sb.append(';');
                } else {
                    switch (t & 0xF) {
                    case 1:
                        sb.append('I');
                        break;
                    case 2:
                        sb.append('F');
                        break;
                    case 3:
                        sb.append('D');
                        break;
                    case 9:
                        sb.append('Z');
                        break;
                    case 10:
                        sb.append('B');
                        break;
                    case 11:
                        sb.append('C');
                        break;
                    case 12:
                        sb.append('S');
                        break;
                    default:
                        sb.append('J');
                    }
                }
                stackMap.putByte(7).putShort(cw.newClass(sb.toString()));
            }
        }
    }

    private void writeFrameType(final Object type) {
        if (type instanceof String) {
            stackMap.putByte(7).putShort(cw.newClass((String) type));
        } else if (type instanceof Integer) {
            stackMap.putByte(((Integer) type).intValue());
        } else {
            stackMap.putByte(8).putShort(((Label) type).position);
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods: dump bytecode array
    // ------------------------------------------------------------------------

    /**
     * 返回此方法的字节码的大小.
     * 
     * @return 此方法的字节码的大小.
     */
    final int getSize() {
        if (classReaderOffset != 0) {
            return 6 + classReaderLength;
        }
        int size = 8;
        if (code.length > 0) {
            if (code.length > 65535) {
                throw new RuntimeException("Method code too large!");
            }
            cw.newUTF8("Code");
            size += 18 + code.length + 8 * handlerCount;
            if (localVar != null) {
                cw.newUTF8("LocalVariableTable");
                size += 8 + localVar.length;
            }
            if (localVarType != null) {
                cw.newUTF8("LocalVariableTypeTable");
                size += 8 + localVarType.length;
            }
            if (lineNumber != null) {
                cw.newUTF8("LineNumberTable");
                size += 8 + lineNumber.length;
            }
            if (stackMap != null) {
                boolean zip = (cw.version & 0xFFFF) >= Opcodes.V1_6;
                cw.newUTF8(zip ? "StackMapTable" : "StackMap");
                size += 8 + stackMap.length;
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                cw.newUTF8("RuntimeVisibleTypeAnnotations");
                size += 8 + ctanns.getSize();
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                cw.newUTF8("RuntimeInvisibleTypeAnnotations");
                size += 8 + ictanns.getSize();
            }
            if (cattrs != null) {
                size += cattrs.getSize(cw, code.data, code.length, maxStack,
                        maxLocals);
            }
        }
        if (exceptionCount > 0) {
            cw.newUTF8("Exceptions");
            size += 8 + 2 * exceptionCount;
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((cw.version & 0xFFFF) < Opcodes.V1_5
                    || (access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                cw.newUTF8("Synthetic");
                size += 6;
            }
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            cw.newUTF8("Deprecated");
            size += 6;
        }
        if (ClassReader.SIGNATURES && signature != null) {
            cw.newUTF8("Signature");
            cw.newUTF8(signature);
            size += 8;
        }
        if (methodParameters != null) {
            cw.newUTF8("MethodParameters");
            size += 7 + methodParameters.length;
        }
        if (ClassReader.ANNOTATIONS && annd != null) {
            cw.newUTF8("AnnotationDefault");
            size += 6 + annd.length;
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            cw.newUTF8("RuntimeVisibleAnnotations");
            size += 8 + anns.getSize();
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            cw.newUTF8("RuntimeInvisibleAnnotations");
            size += 8 + ianns.getSize();
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            cw.newUTF8("RuntimeVisibleTypeAnnotations");
            size += 8 + tanns.getSize();
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            cw.newUTF8("RuntimeInvisibleTypeAnnotations");
            size += 8 + itanns.getSize();
        }
        if (ClassReader.ANNOTATIONS && panns != null) {
            cw.newUTF8("RuntimeVisibleParameterAnnotations");
            size += 7 + 2 * (panns.length - synthetics);
            for (int i = panns.length - 1; i >= synthetics; --i) {
                size += panns[i] == null ? 0 : panns[i].getSize();
            }
        }
        if (ClassReader.ANNOTATIONS && ipanns != null) {
            cw.newUTF8("RuntimeInvisibleParameterAnnotations");
            size += 7 + 2 * (ipanns.length - synthetics);
            for (int i = ipanns.length - 1; i >= synthetics; --i) {
                size += ipanns[i] == null ? 0 : ipanns[i].getSize();
            }
        }
        if (attrs != null) {
            size += attrs.getSize(cw, null, 0, -1, -1);
        }
        return size;
    }

    /**
     * 将此方法的字节代码放在给定的字节向量中.
     * 
     * @param out
     *            必须复制此方法的字节码的字节向量.
     */
    final void put(final ByteVector out) {
        final int FACTOR = ClassWriter.TO_ACC_SYNTHETIC;
        int mask = ACC_CONSTRUCTOR | Opcodes.ACC_DEPRECATED
                | ClassWriter.ACC_SYNTHETIC_ATTRIBUTE
                | ((access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) / FACTOR);
        out.putShort(access & ~mask).putShort(name).putShort(desc);
        if (classReaderOffset != 0) {
            out.putByteArray(cw.cr.b, classReaderOffset, classReaderLength);
            return;
        }
        int attributeCount = 0;
        if (code.length > 0) {
            ++attributeCount;
        }
        if (exceptionCount > 0) {
            ++attributeCount;
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((cw.version & 0xFFFF) < Opcodes.V1_5
                    || (access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                ++attributeCount;
            }
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            ++attributeCount;
        }
        if (ClassReader.SIGNATURES && signature != null) {
            ++attributeCount;
        }
        if (methodParameters != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && annd != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && panns != null) {
            ++attributeCount;
        }
        if (ClassReader.ANNOTATIONS && ipanns != null) {
            ++attributeCount;
        }
        if (attrs != null) {
            attributeCount += attrs.getCount();
        }
        out.putShort(attributeCount);
        if (code.length > 0) {
            int size = 12 + code.length + 8 * handlerCount;
            if (localVar != null) {
                size += 8 + localVar.length;
            }
            if (localVarType != null) {
                size += 8 + localVarType.length;
            }
            if (lineNumber != null) {
                size += 8 + lineNumber.length;
            }
            if (stackMap != null) {
                size += 8 + stackMap.length;
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                size += 8 + ctanns.getSize();
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                size += 8 + ictanns.getSize();
            }
            if (cattrs != null) {
                size += cattrs.getSize(cw, code.data, code.length, maxStack,
                        maxLocals);
            }
            out.putShort(cw.newUTF8("Code")).putInt(size);
            out.putShort(maxStack).putShort(maxLocals);
            out.putInt(code.length).putByteArray(code.data, 0, code.length);
            out.putShort(handlerCount);
            if (handlerCount > 0) {
                Handler h = firstHandler;
                while (h != null) {
                    out.putShort(h.start.position).putShort(h.end.position)
                            .putShort(h.handler.position).putShort(h.type);
                    h = h.next;
                }
            }
            attributeCount = 0;
            if (localVar != null) {
                ++attributeCount;
            }
            if (localVarType != null) {
                ++attributeCount;
            }
            if (lineNumber != null) {
                ++attributeCount;
            }
            if (stackMap != null) {
                ++attributeCount;
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                ++attributeCount;
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                ++attributeCount;
            }
            if (cattrs != null) {
                attributeCount += cattrs.getCount();
            }
            out.putShort(attributeCount);
            if (localVar != null) {
                out.putShort(cw.newUTF8("LocalVariableTable"));
                out.putInt(localVar.length + 2).putShort(localVarCount);
                out.putByteArray(localVar.data, 0, localVar.length);
            }
            if (localVarType != null) {
                out.putShort(cw.newUTF8("LocalVariableTypeTable"));
                out.putInt(localVarType.length + 2).putShort(localVarTypeCount);
                out.putByteArray(localVarType.data, 0, localVarType.length);
            }
            if (lineNumber != null) {
                out.putShort(cw.newUTF8("LineNumberTable"));
                out.putInt(lineNumber.length + 2).putShort(lineNumberCount);
                out.putByteArray(lineNumber.data, 0, lineNumber.length);
            }
            if (stackMap != null) {
                boolean zip = (cw.version & 0xFFFF) >= Opcodes.V1_6;
                out.putShort(cw.newUTF8(zip ? "StackMapTable" : "StackMap"));
                out.putInt(stackMap.length + 2).putShort(frameCount);
                out.putByteArray(stackMap.data, 0, stackMap.length);
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                out.putShort(cw.newUTF8("RuntimeVisibleTypeAnnotations"));
                ctanns.put(out);
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                out.putShort(cw.newUTF8("RuntimeInvisibleTypeAnnotations"));
                ictanns.put(out);
            }
            if (cattrs != null) {
                cattrs.put(cw, code.data, code.length, maxLocals, maxStack, out);
            }
        }
        if (exceptionCount > 0) {
            out.putShort(cw.newUTF8("Exceptions")).putInt(
                    2 * exceptionCount + 2);
            out.putShort(exceptionCount);
            for (int i = 0; i < exceptionCount; ++i) {
                out.putShort(exceptions[i]);
            }
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((cw.version & 0xFFFF) < Opcodes.V1_5
                    || (access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                out.putShort(cw.newUTF8("Synthetic")).putInt(0);
            }
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            out.putShort(cw.newUTF8("Deprecated")).putInt(0);
        }
        if (ClassReader.SIGNATURES && signature != null) {
            out.putShort(cw.newUTF8("Signature")).putInt(2)
                    .putShort(cw.newUTF8(signature));
        }
        if (methodParameters != null) {
            out.putShort(cw.newUTF8("MethodParameters"));
            out.putInt(methodParameters.length + 1).putByte(
                    methodParametersCount);
            out.putByteArray(methodParameters.data, 0, methodParameters.length);
        }
        if (ClassReader.ANNOTATIONS && annd != null) {
            out.putShort(cw.newUTF8("AnnotationDefault"));
            out.putInt(annd.length);
            out.putByteArray(annd.data, 0, annd.length);
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            out.putShort(cw.newUTF8("RuntimeVisibleAnnotations"));
            anns.put(out);
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            out.putShort(cw.newUTF8("RuntimeInvisibleAnnotations"));
            ianns.put(out);
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            out.putShort(cw.newUTF8("RuntimeVisibleTypeAnnotations"));
            tanns.put(out);
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            out.putShort(cw.newUTF8("RuntimeInvisibleTypeAnnotations"));
            itanns.put(out);
        }
        if (ClassReader.ANNOTATIONS && panns != null) {
            out.putShort(cw.newUTF8("RuntimeVisibleParameterAnnotations"));
            AnnotationWriter.put(panns, synthetics, out);
        }
        if (ClassReader.ANNOTATIONS && ipanns != null) {
            out.putShort(cw.newUTF8("RuntimeInvisibleParameterAnnotations"));
            AnnotationWriter.put(ipanns, synthetics, out);
        }
        if (attrs != null) {
            attrs.put(cw, null, 0, -1, -1, out);
        }
    }
}
