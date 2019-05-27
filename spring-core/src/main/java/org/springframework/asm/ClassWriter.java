package org.springframework.asm;

/**
 * 一个{@link ClassVisitor}, 以字节码形式生成类.
 * 更确切地说, 此访问器生成符合Java类文件格式的字节数组.
 * 它可以单独使用, "从头开始"生成Java类, 或者与一个或多个{@link ClassReader ClassReader}和适配器类访问器一起使用,
 * 以从一个或多个现有Java类生成修改后的类.
 */
public class ClassWriter extends ClassVisitor {

    /**
     * 自动计算最大堆栈大小和方法的最大局部变量数.
     * 如果设置了此标志, 那么{@link #visitMethod visitMethod}方法返回的
     * {@link MethodVisitor}的{@link MethodVisitor#visitMaxs visitMaxs}方法的参数将被忽略,
     * 并从每个方法的签名和字节码自动计算.
     */
    public static final int COMPUTE_MAXS = 1;

    /**
     * 从头开始自动计算方法的堆栈映射帧.
     * 如果设置了此标志, 则忽略对{@link MethodVisitor#visitFrame}方法的调用, 并从方法字节码重新计算堆栈映射帧.
     * {@link MethodVisitor#visitMaxs visitMaxs}方法的参数也被忽略, 并从字节码重新计算.
     * 换句话说, COMPUTE_FRAMES 意味着 COMPUTE_MAXS.
     */
    public static final int COMPUTE_FRAMES = 2;

    /**
     * 伪访问标志, 用于区分合成属性和合成访问标志.
     */
    static final int ACC_SYNTHETIC_ATTRIBUTE = 0x40000;

    /**
     * 要从 ACC_SYNTHETIC_ATTRIBUTE 转换为 Opcode.ACC_SYNTHETIC的因子.
     */
    static final int TO_ACC_SYNTHETIC = ACC_SYNTHETIC_ATTRIBUTE
            / Opcodes.ACC_SYNTHETIC;

    /**
     * 没有任何参数的指令类型.
     */
    static final int NOARG_INSN = 0;

    /**
     * 带有signed byte参数的指令类型.
     */
    static final int SBYTE_INSN = 1;

    /**
     * 带有signed short参数的指令类型.
     */
    static final int SHORT_INSN = 2;

    /**
     * 带有局部变量索引参数的指令类型.
     */
    static final int VAR_INSN = 3;

    /**
     * 具有隐式局部变量索引参数的指令类型.
     */
    static final int IMPLVAR_INSN = 4;

    /**
     * 带有类型描述符参数的指令类型.
     */
    static final int TYPE_INSN = 5;

    /**
     * 字段和方法调用指令的类型.
     */
    static final int FIELDORMETH_INSN = 6;

    /**
     * INVOKEINTERFACE/INVOKEDYNAMIC 指令的类型.
     */
    static final int ITFMETH_INSN = 7;

    /**
     * INVOKEDYNAMIC指令的类型.
     */
    static final int INDYMETH_INSN = 8;

    /**
     * 具有2字节字节码偏移标签的指令类型.
     */
    static final int LABEL_INSN = 9;

    /**
     * 具有4字节字节码偏移标签的指令类型.
     */
    static final int LABELW_INSN = 10;

    /**
     * LDC指令的类型.
     */
    static final int LDC_INSN = 11;

    /**
     * LDC_W和LDC2_W指令的类型.
     */
    static final int LDCW_INSN = 12;

    /**
     * IINC指令的类型.
     */
    static final int IINC_INSN = 13;

    /**
     * TABLESWITCH指令的类型.
     */
    static final int TABL_INSN = 14;

    /**
     * LOOKUPSWITCH指令的类型.
     */
    static final int LOOK_INSN = 15;

    /**
     * MULTIANEWARRAY指令的类型.
     */
    static final int MANA_INSN = 16;

    /**
     * WIDE指令的类型.
     */
    static final int WIDE_INSN = 17;

    /**
     * 具有无符号2字节偏移标签的ASM伪指令的类型 (see Label#resolve).
     */
    static final int ASM_LABEL_INSN = 18;

    /**
     * 具有4字节偏移标签的ASM伪指令的类型.
     */
    static final int ASM_LABELW_INSN = 19;

    /**
     * 表示在现有帧之间插入的帧.
     * 只有在可以从先前已有的帧, 和这个现有的帧与插入的帧之间的指令, 计算帧内容时, 才能使用这种帧, 而不需要知道类型层次结构.
     * 仅在扩展ASM伪指令时, 在方法中插入无条件跳转时, 才使用这种帧 (see ClassReader).
     */
    static final int F_INSERT = 256;

    /**
     * 所有JVM操作码的指令类型.
     */
    static final byte[] TYPE;

    /**
     * CONSTANT_Class常量池项的类型.
     */
    static final int CLASS = 7;

    /**
     * CONSTANT_Fieldref常量池项的类型.
     */
    static final int FIELD = 9;

    /**
     * CONSTANT_Methodref常量池项的类型.
     */
    static final int METH = 10;

    /**
     * CONSTANT_InterfaceMethodref常量池项的类型.
     */
    static final int IMETH = 11;

    /**
     * CONSTANT_String常量池项的类型.
     */
    static final int STR = 8;

    /**
     * CONSTANT_Integer常量池项的类型.
     */
    static final int INT = 3;

    /**
     * CONSTANT_Float常量池项的类型.
     */
    static final int FLOAT = 4;

    /**
     * CONSTANT_Long常量池项的类型.
     */
    static final int LONG = 5;

    /**
     * CONSTANT_Double常量池项的类型.
     */
    static final int DOUBLE = 6;

    /**
     * CONSTANT_NameAndType常量池项的类型.
     */
    static final int NAME_TYPE = 12;

    /**
     * CONSTANT_Utf8常量池项的类型.
     */
    static final int UTF8 = 1;

    /**
     * CONSTANT_MethodType常量池项的类型.
     */
    static final int MTYPE = 16;

    /**
     * CONSTANT_MethodHandle常量池项的类型.
     */
    static final int HANDLE = 15;

    /**
     * CONSTANT_InvokeDynamic常量池项的类型.
     */
    static final int INDY = 18;

    /**
     * CONSTANT_Module常量池项的类型.
     */
    static final int MODULE = 19;

    /**
     * CONSTANT_Package常量池项的类型.
     */
    static final int PACKAGE = 20;

    /**
     * 所有CONSTANT_MethodHandle常量池项的基值.
     * 在内部, ASM将CONSTANT_MethodHandle的9种变体存储到9个不同的项目中 (从21到29).
     */
    static final int HANDLE_BASE = 20;

    /**
     * 普通类型项存储在ClassWriter {@link ClassWriter#typeTable}中, 而不是常量池,
     * 以避免与ClassWriter常量池的哈希表中的常规常量池项冲突.
     */
    static final int TYPE_NORMAL = 30;

    /**
     * 未初始化的类型Item存储在ClassWriter {@link ClassWriter#typeTable}中, 而不是常量池,
     * 以避免与ClassWriter常量池的哈希表中的常规常量池项冲突.
     */
    static final int TYPE_UNINIT = 31;

    /**
     * 合并类型Item存储在ClassWriter {@link ClassWriter#typeTable}中, 而不是常量池,
     * 以避免与ClassWriter常量池的哈希表中的常规常量池项冲突.
     */
    static final int TYPE_MERGED = 32;

    /**
     * BootstrapMethods项的类型.
     * 这些项存储在名为BootstrapMethods的特殊类属性中, 而不是存储在常量池中.
     */
    static final int BSM = 33;

    /**
     * 构建这个类写入器的类读取器.
     */
    ClassReader cr;

    /**
     * 要生成的类的次要版本号和主要版本号.
     */
    int version;

    /**
     * 要在常量池中添加的下一个条目的索引.
     */
    int index;

    /**
     * 这个类的常量池.
     */
    final ByteVector pool;

    /**
     * 常量池的哈希表数据.
     */
    Item[] items;

    /**
     * 常量池哈希表的阈值.
     */
    int threshold;

    /**
     * 用于在{@link #items}哈希表中查找条目的可重用Key.
     */
    final Item key;

    /**
     * 用于在{@link #items}哈希表中查找条目的可重用Key.
     */
    final Item key2;

    /**
     * 用于在{@link #items}哈希表中查找条目的可重用Key.
     */
    final Item key3;

    /**
     * 用于在{@link #items}哈希表中查找条目的可重用Key.
     */
    final Item key4;

    /**
     * 用于临时存储内部名称的类型表, 不一定存储在常量池中.
     * 此类型表由用于从头开始计算堆栈映射帧的控制流和数据流分析算法使用.
     * 此数组关联到每个索引<tt>i</tt>, Item的索引为<tt>i</tt>.
     * 存储在此数组中的所有Item对象也存储在{@link #items}哈希表中.
     * 这两个数组允许从索引中检索Item, 或者反过来, 从其值中获取Item的索引.
     * 每个Item在其{@link Item#strVal1}字段中存储内部名称.
     */
    Item[] typeTable;

    /**
     * {@link #typeTable}数组中的元素数.
     */
    private short typeCount;

    /**
     * 此类的访问标志.
     */
    private int access;

    /**
     * 包含此类的内部名称的常量池项.
     */
    private int name;

    /**
     * 此类的内部名称.
     */
    String thisName;

    /**
     * 包含此类签名的常量池项.
     */
    private int signature;

    /**
     * 包含此类的超类的内部名称的常量池项.
     */
    private int superName;

    /**
     * 此类或接口实现或继承的接口数量.
     */
    private int interfaceCount;

    /**
     * 由此类或接口实现或继承的接口.
     * 更确切地说, 此数组包含常量池项的索引, 这些项包含这些接口的内部名称.
     */
    private int[] interfaces;

    /**
     * 常量池项的索引, 该项包含从中编译此类的源文件的名称.
     */
    private int sourceFile;

    /**
     * 此类的SourceDebug属性.
     */
    private ByteVector sourceDebug;

    /**
     * 此类的module属性.
     */
    private ModuleWriter moduleWriter;

    /**
     * 常量池项, 包含此类的封闭类的名称.
     */
    private int enclosingMethodOwner;

    /**
     * 常量池项, 包含此类的封闭方法的名称和描述符.
     */
    private int enclosingMethod;

    /**
     * 此类的运行时可见注解.
     */
    private AnnotationWriter anns;

    /**
     * 此类的运行时不可见注解.
     */
    private AnnotationWriter ianns;

    /**
     * 此类的运行时可见类型注解.
     */
    private AnnotationWriter tanns;

    /**
     * 此类的运行时不可见类型注解.
     */
    private AnnotationWriter itanns;

    /**
     * 此类的非标准属性.
     */
    private Attribute attrs;

    /**
     * InnerClasses属性中的条目数.
     */
    private int innerClassesCount;

    /**
     * InnerClasses属性.
     */
    private ByteVector innerClasses;

    /**
     * BootstrapMethods属性中的条目数.
     */
    int bootstrapMethodsCount;

    /**
     * BootstrapMethods属性.
     */
    ByteVector bootstrapMethods;

    /**
     * 此类的字段.
     * 这些字段存储在{@link FieldWriter}对象的链表中, 通过{@link FieldWriter#fv}字段相互链接.
     * 此字段存储此列表的第一个元素.
     */
    FieldWriter firstField;

    /**
     * 此类的字段.
     * 这些字段存储在{@link FieldWriter}对象的链表中, 通过{@link FieldWriter#fv}字段相互链接.
     * 此字段存储此列表的最后一个元素.
     */
    FieldWriter lastField;

    /**
     * 此类的方法.
     * 这些方法存储在{@link MethodWriter}对象的链表中, 通过{@link MethodWriter#mv}字段相互链接.
     * 此字段存储此列表的第一个元素.
     */
    MethodWriter firstMethod;

    /**
     * 此类的方法.
     * 这些方法存储在{@link MethodWriter}对象的链表中, 通过{@link MethodWriter#mv}字段相互链接.
     * 此字段存储此列表的最后一个元素.
     */
    MethodWriter lastMethod;

    /**
     * 表示必须自动计算的内容.
     */
    private int compute;

    /**
     * <tt>true</tt> 如果某些方法使用ASM伪指令进行宽前向跳跃, 则需要将其扩展为标准字节码指令序列.
     * 在这种情况下, 将使用ClassReader重新读取和重写类 -> ClassWriter链执行此转换.
     */
    boolean hasAsmInsns;

    // ------------------------------------------------------------------------
    // Static initializer
    // ------------------------------------------------------------------------

    /**
     * 计算JVM操作码的指令类型.
     */
    static {
        int i;
        byte[] b = new byte[221];
        String s = "AAAAAAAAAAAAAAAABCLMMDDDDDEEEEEEEEEEEEEEEEEEEEAAAAAAAADD"
                + "DDDEEEEEEEEEEEEEEEEEEEEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAANAAAAAAAAAAAAAAAAAAAAJJJJJJJJJJJJJJJJDOPAA"
                + "AAAAGGGGGGGHIFBFAAFFAARQJJKKSSSSSSSSSSSSSSSSSST";
        for (i = 0; i < b.length; ++i) {
            b[i] = (byte) (s.charAt(i) - 'A');
        }
        TYPE = b;

        // 用于生成上述字符串的代码
        //
        // // SBYTE_INSN instructions
        // b[Constants.NEWARRAY] = SBYTE_INSN;
        // b[Constants.BIPUSH] = SBYTE_INSN;
        //
        // // SHORT_INSN instructions
        // b[Constants.SIPUSH] = SHORT_INSN;
        //
        // // (IMPL)VAR_INSN instructions
        // b[Constants.RET] = VAR_INSN;
        // for (i = Constants.ILOAD; i <= Constants.ALOAD; ++i) {
        // b[i] = VAR_INSN;
        // }
        // for (i = Constants.ISTORE; i <= Constants.ASTORE; ++i) {
        // b[i] = VAR_INSN;
        // }
        // for (i = 26; i <= 45; ++i) { // ILOAD_0 to ALOAD_3
        // b[i] = IMPLVAR_INSN;
        // }
        // for (i = 59; i <= 78; ++i) { // ISTORE_0 to ASTORE_3
        // b[i] = IMPLVAR_INSN;
        // }
        //
        // // TYPE_INSN instructions
        // b[Constants.NEW] = TYPE_INSN;
        // b[Constants.ANEWARRAY] = TYPE_INSN;
        // b[Constants.CHECKCAST] = TYPE_INSN;
        // b[Constants.INSTANCEOF] = TYPE_INSN;
        //
        // // (Set)FIELDORMETH_INSN instructions
        // for (i = Constants.GETSTATIC; i <= Constants.INVOKESTATIC; ++i) {
        // b[i] = FIELDORMETH_INSN;
        // }
        // b[Constants.INVOKEINTERFACE] = ITFMETH_INSN;
        // b[Constants.INVOKEDYNAMIC] = INDYMETH_INSN;
        //
        // // LABEL(W)_INSN instructions
        // for (i = Constants.IFEQ; i <= Constants.JSR; ++i) {
        // b[i] = LABEL_INSN;
        // }
        // b[Constants.IFNULL] = LABEL_INSN;
        // b[Constants.IFNONNULL] = LABEL_INSN;
        // b[200] = LABELW_INSN; // GOTO_W
        // b[201] = LABELW_INSN; // JSR_W
        // // temporary opcodes used internally by ASM - see Label and
        // MethodWriter
        // for (i = 202; i < 220; ++i) {
        // b[i] = ASM_LABEL_INSN;
        // }
        // b[220] = ASM_LABELW_INSN;
        //
        // // LDC(_W) instructions
        // b[Constants.LDC] = LDC_INSN;
        // b[19] = LDCW_INSN; // LDC_W
        // b[20] = LDCW_INSN; // LDC2_W
        //
        // // special instructions
        // b[Constants.IINC] = IINC_INSN;
        // b[Constants.TABLESWITCH] = TABL_INSN;
        // b[Constants.LOOKUPSWITCH] = LOOK_INSN;
        // b[Constants.MULTIANEWARRAY] = MANA_INSN;
        // b[196] = WIDE_INSN; // WIDE
        //
        // for (i = 0; i < b.length; ++i) {
        // System.err.print((char)('A' + b[i]));
        // }
        // System.err.println();
    }

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * @param flags
     *            用于修改此类的默认行为.
     *            See {@link #COMPUTE_MAXS}, {@link #COMPUTE_FRAMES}.
     */
    public ClassWriter(final int flags) {
        super(Opcodes.ASM6);
        index = 1;
        pool = new ByteVector();
        items = new Item[256];
        threshold = (int) (0.75d * items.length);
        key = new Item();
        key2 = new Item();
        key3 = new Item();
        key4 = new Item();
        this.compute = (flags & COMPUTE_FRAMES) != 0 ? MethodWriter.FRAMES
                : ((flags & COMPUTE_MAXS) != 0 ? MethodWriter.MAXS
                        : MethodWriter.NOTHING);
    }

    /**
     * 构造一个新的{@link ClassWriter}对象, 并启用"主要添加"字节码转换的优化.
     * 这些优化如下:
     * 
     * <ul>
     * <li>原始类中的常量池将按原样复制到新类中, 从而节省时间.
     * 如有必要，将在末尾添加新的常量池条目，但<i>不会删除</i>未使用的常量池条目.</li>
     * <li>未转换的方法将直接从原始类字节码复制到新类中 (i.e. 不会发出所有方法指令的访问事件), 这样可以节省很多时间.
     * {@link ClassReader}接收来自{@link ClassWriter}的{@link MethodVisitor}对象 (而不是来自任何其他{@link ClassVisitor}实例),
     * 这样可以检测到未转换的方法.</li>
     * </ul>
     * 
     * @param classReader
     *            用于读取原始类的{@link ClassReader}.
     *            它将用于从原始类复制整个常量池, 并在适用的情况下复制原始字节码的其他片段.
     * @param flags
     *            用于修改此类的默认行为.
     *            <i>这些选项标志不会影响在新类中复制的方法.
     *            这意味着不会为这些方法计算最大堆栈大小和堆栈帧</i>.
     *            See {@link #COMPUTE_MAXS}, {@link #COMPUTE_FRAMES}.
     */
    public ClassWriter(final ClassReader classReader, final int flags) {
        this(flags);
        classReader.copyPool(this);
        this.cr = classReader;
    }

    // ------------------------------------------------------------------------
    // Implementation of the ClassVisitor abstract class
    // ------------------------------------------------------------------------

    @Override
    public final void visit(final int version, final int access,
            final String name, final String signature, final String superName,
            final String[] interfaces) {
        this.version = version;
        this.access = access;
        this.name = newClass(name);
        thisName = name;
        if (ClassReader.SIGNATURES && signature != null) {
            this.signature = newUTF8(signature);
        }
        this.superName = superName == null ? 0 : newClass(superName);
        if (interfaces != null && interfaces.length > 0) {
            interfaceCount = interfaces.length;
            this.interfaces = new int[interfaceCount];
            for (int i = 0; i < interfaceCount; ++i) {
                this.interfaces[i] = newClass(interfaces[i]);
            }
        }
    }

    @Override
    public final void visitSource(final String file, final String debug) {
        if (file != null) {
            sourceFile = newUTF8(file);
        }
        if (debug != null) {
            sourceDebug = new ByteVector().encodeUTF8(debug, 0,
                    Integer.MAX_VALUE);
        }
    }

    @Override
    public final ModuleVisitor visitModule(final String name,
            final int access, final String version) {
        return moduleWriter = new ModuleWriter(this,
                newModule(name), access,
                version == null ? 0 : newUTF8(version));
    }

    @Override
    public final void visitOuterClass(final String owner, final String name,
            final String desc) {
        enclosingMethodOwner = newClass(owner);
        if (name != null && desc != null) {
            enclosingMethod = newNameType(name, desc);
        }
    }

    @Override
    public final AnnotationVisitor visitAnnotation(final String desc,
            final boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // 写入类型, 并为值计数保留空间
        bv.putShort(newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(this, true, bv, bv, 2);
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
    public final AnnotationVisitor visitTypeAnnotation(int typeRef,
            TypePath typePath, final String desc, final boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write target_type and target_info
        AnnotationWriter.putTarget(typeRef, typePath, bv);
        // 写入类型, 并为值计数保留空间
        bv.putShort(newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(this, true, bv, bv,
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
    public final void visitAttribute(final Attribute attr) {
        attr.next = attrs;
        attrs = attr;
    }

    @Override
    public final void visitInnerClass(final String name,
            final String outerName, final String innerName, final int access) {
        if (innerClasses == null) {
            innerClasses = new ByteVector();
        }
        // Sec. 4.7.6 的JVMS声明"表示一个不是包成员的类或接口C的constant_pool表中的每个CONSTANT_Class_info条目,
        // 必须在classes数组中只有一个相应的条目".
        // 为了避免重复, 在每个CONSTANT_Class_info条目C的Item的intVal字段中跟踪是否已经为C添加了内部类条目 
        // (此字段未用于类条目, 并且更改其值不会更改哈希码和相等性测试).
        // 如果是这样, 将此内部类条目的索引 (加一)存储在intVal中. 该hack允许在 O(1) 时间内重复检测.
        Item nameItem = newStringishItem(CLASS, name);
        if (nameItem.intVal == 0) {
            ++innerClassesCount;
            innerClasses.putShort(nameItem.index);
            innerClasses.putShort(outerName == null ? 0 : newClass(outerName));
            innerClasses.putShort(innerName == null ? 0 : newUTF8(innerName));
            innerClasses.putShort(access);
            nameItem.intVal = innerClassesCount;
        } else {
            // 将内部类条目 nameItem.intVal - 1 与此方法的参数进行比较, 如果存在差异则抛出异常?
        }
    }

    @Override
    public final FieldVisitor visitField(final int access, final String name,
            final String desc, final String signature, final Object value) {
        return new FieldWriter(this, access, name, desc, signature, value);
    }

    @Override
    public final MethodVisitor visitMethod(final int access, final String name,
            final String desc, final String signature, final String[] exceptions) {
        return new MethodWriter(this, access, name, desc, signature,
                exceptions, compute);
    }

    @Override
    public final void visitEnd() {
    }

    // ------------------------------------------------------------------------
    // Other public methods
    // ------------------------------------------------------------------------

    /**
     * 返回使用此类写入器生成的类的字节码.
     * 
     * @return 生成的类的字节码.
     */
    public byte[] toByteArray() {
        if (index > 0xFFFF) {
            throw new RuntimeException("Class file too large!");
        }
        // 计算此类的字节码的实际大小
        int size = 24 + 2 * interfaceCount;
        int nbFields = 0;
        FieldWriter fb = firstField;
        while (fb != null) {
            ++nbFields;
            size += fb.getSize();
            fb = (FieldWriter) fb.fv;
        }
        int nbMethods = 0;
        MethodWriter mb = firstMethod;
        while (mb != null) {
            ++nbMethods;
            size += mb.getSize();
            mb = (MethodWriter) mb.mv;
        }
        int attributeCount = 0;
        if (bootstrapMethods != null) {
            // 把它作为第一个属性, 以改善一点 ClassReader.copyBootstrapMethods
            ++attributeCount;
            size += 8 + bootstrapMethods.length;
            newUTF8("BootstrapMethods");
        }
        if (ClassReader.SIGNATURES && signature != 0) {
            ++attributeCount;
            size += 8;
            newUTF8("Signature");
        }
        if (sourceFile != 0) {
            ++attributeCount;
            size += 8;
            newUTF8("SourceFile");
        }
        if (sourceDebug != null) {
            ++attributeCount;
            size += sourceDebug.length + 6;
            newUTF8("SourceDebugExtension");
        }
        if (enclosingMethodOwner != 0) {
            ++attributeCount;
            size += 10;
            newUTF8("EnclosingMethod");
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            ++attributeCount;
            size += 6;
            newUTF8("Deprecated");
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((version & 0xFFFF) < Opcodes.V1_5
                    || (access & ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                ++attributeCount;
                size += 6;
                newUTF8("Synthetic");
            }
        }
        if (innerClasses != null) {
            ++attributeCount;
            size += 8 + innerClasses.length;
            newUTF8("InnerClasses");
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            ++attributeCount;
            size += 8 + anns.getSize();
            newUTF8("RuntimeVisibleAnnotations");
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            ++attributeCount;
            size += 8 + ianns.getSize();
            newUTF8("RuntimeInvisibleAnnotations");
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            ++attributeCount;
            size += 8 + tanns.getSize();
            newUTF8("RuntimeVisibleTypeAnnotations");
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            ++attributeCount;
            size += 8 + itanns.getSize();
            newUTF8("RuntimeInvisibleTypeAnnotations");
        }
        if (moduleWriter != null) {
            attributeCount += 1 + moduleWriter.attributeCount;
            size += 6 + moduleWriter.size + moduleWriter.attributesSize;
            newUTF8("Module");
        }
        if (attrs != null) {
            attributeCount += attrs.getCount();
            size += attrs.getSize(this, null, 0, -1, -1);
        }
        size += pool.length;
        // 分配这个大小的字节向量, 以避免 ByteVector.enlarge() 方法中不必要的arraycopy操作
        ByteVector out = new ByteVector(size);
        out.putInt(0xCAFEBABE).putInt(version);
        out.putShort(index).putByteArray(pool.data, 0, pool.length);
        int mask = Opcodes.ACC_DEPRECATED | ACC_SYNTHETIC_ATTRIBUTE
                | ((access & ACC_SYNTHETIC_ATTRIBUTE) / TO_ACC_SYNTHETIC);
        out.putShort(access & ~mask).putShort(name).putShort(superName);
        out.putShort(interfaceCount);
        for (int i = 0; i < interfaceCount; ++i) {
            out.putShort(interfaces[i]);
        }
        out.putShort(nbFields);
        fb = firstField;
        while (fb != null) {
            fb.put(out);
            fb = (FieldWriter) fb.fv;
        }
        out.putShort(nbMethods);
        mb = firstMethod;
        while (mb != null) {
            mb.put(out);
            mb = (MethodWriter) mb.mv;
        }
        out.putShort(attributeCount);
        if (bootstrapMethods != null) {
            out.putShort(newUTF8("BootstrapMethods"));
            out.putInt(bootstrapMethods.length + 2).putShort(
                    bootstrapMethodsCount);
            out.putByteArray(bootstrapMethods.data, 0, bootstrapMethods.length);
        }
        if (ClassReader.SIGNATURES && signature != 0) {
            out.putShort(newUTF8("Signature")).putInt(2).putShort(signature);
        }
        if (sourceFile != 0) {
            out.putShort(newUTF8("SourceFile")).putInt(2).putShort(sourceFile);
        }
        if (sourceDebug != null) {
            int len = sourceDebug.length;
            out.putShort(newUTF8("SourceDebugExtension")).putInt(len);
            out.putByteArray(sourceDebug.data, 0, len);
        }
        if (moduleWriter != null) {
            out.putShort(newUTF8("Module"));
            moduleWriter.put(out);
            moduleWriter.putAttributes(out);
        }
        if (enclosingMethodOwner != 0) {
            out.putShort(newUTF8("EnclosingMethod")).putInt(4);
            out.putShort(enclosingMethodOwner).putShort(enclosingMethod);
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            out.putShort(newUTF8("Deprecated")).putInt(0);
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((version & 0xFFFF) < Opcodes.V1_5
                    || (access & ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                out.putShort(newUTF8("Synthetic")).putInt(0);
            }
        }
        if (innerClasses != null) {
            out.putShort(newUTF8("InnerClasses"));
            out.putInt(innerClasses.length + 2).putShort(innerClassesCount);
            out.putByteArray(innerClasses.data, 0, innerClasses.length);
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            out.putShort(newUTF8("RuntimeVisibleAnnotations"));
            anns.put(out);
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            out.putShort(newUTF8("RuntimeInvisibleAnnotations"));
            ianns.put(out);
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            out.putShort(newUTF8("RuntimeVisibleTypeAnnotations"));
            tanns.put(out);
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            out.putShort(newUTF8("RuntimeInvisibleTypeAnnotations"));
            itanns.put(out);
        }
        if (attrs != null) {
            attrs.put(this, null, 0, -1, -1, out);
        }
        if (hasAsmInsns) {
            boolean hasFrames = false;
            mb = firstMethod;
            while (mb != null) {
                hasFrames |= mb.frameCount > 0;
                mb = (MethodWriter) mb.mv;
            }
            anns = null;
            ianns = null;
            attrs = null;
            moduleWriter = null;
            innerClassesCount = 0;
            innerClasses = null;
            firstField = null;
            lastField = null;
            firstMethod = null;
            lastMethod = null;
            compute = hasFrames ? MethodWriter.INSERTED_FRAMES : 0;
            hasAsmInsns = false;
            new ClassReader(out.data).accept(this,
                    (hasFrames ? ClassReader.EXPAND_FRAMES : 0)
                    | ClassReader.EXPAND_ASM_INSNS);
            return toByteArray();
        }
        return out.data;
    }

    // ------------------------------------------------------------------------
    // Utility methods: constant pool management
    // ------------------------------------------------------------------------

    /**
     * 将数字或字符串常量添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * 
     * @param cst
     *            要添加到常量池的常量的值.
     *            此参数必须是{@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link Type}.
     * 
     * @return 具有给定值的新的或已存在的常量项.
     */
    Item newConstItem(final Object cst) {
        if (cst instanceof Integer) {
            int val = ((Integer) cst).intValue();
            return newInteger(val);
        } else if (cst instanceof Byte) {
            int val = ((Byte) cst).intValue();
            return newInteger(val);
        } else if (cst instanceof Character) {
            int val = ((Character) cst).charValue();
            return newInteger(val);
        } else if (cst instanceof Short) {
            int val = ((Short) cst).intValue();
            return newInteger(val);
        } else if (cst instanceof Boolean) {
            int val = ((Boolean) cst).booleanValue() ? 1 : 0;
            return newInteger(val);
        } else if (cst instanceof Float) {
            float val = ((Float) cst).floatValue();
            return newFloat(val);
        } else if (cst instanceof Long) {
            long val = ((Long) cst).longValue();
            return newLong(val);
        } else if (cst instanceof Double) {
            double val = ((Double) cst).doubleValue();
            return newDouble(val);
        } else if (cst instanceof String) {
            return newStringishItem(STR, (String) cst);
        } else if (cst instanceof Type) {
            Type t = (Type) cst;
            int s = t.getSort();
            if (s == Type.OBJECT) {
                return newStringishItem(CLASS, t.getInternalName());
            } else if (s == Type.METHOD) {
                return newStringishItem(MTYPE, t.getDescriptor());
            } else { // s == primitive type or array
                return newStringishItem(CLASS, t.getDescriptor());
            }
        } else if (cst instanceof Handle) {
            Handle h = (Handle) cst;
            return newHandleItem(h.tag, h.owner, h.name, h.desc, h.itf);
        } else {
            throw new IllegalArgumentException("value " + cst);
        }
    }

    /**
     * 将数字或字符串常量添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param cst
     *            要添加到常量池的常量的值.
     *            此参数必须是{@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}.
     * 
     * @return 具有给定值的新常量项或已存在常量项的索引.
     */
    public int newConst(final Object cst) {
        return newConstItem(cst).index;
    }

    /**
     * 将UTF8字符串添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param value
     *            the String value.
     * 
     * @return 新的或已存在的UTF8项的索引.
     */
    public int newUTF8(final String value) {
        key.set(UTF8, value, null, null);
        Item result = get(key);
        if (result == null) {
            pool.putByte(UTF8).putUTF8(value);
            result = new Item(index++, key);
            put(result);
        }
        return result.index;
    }

    /**
     * 将字符串引用, 类引用, 方法类型, 模块或包添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * 
     * @param type
     *            STR, CLASS, MTYPE, MODULE, PACKAGE类型
     * @param value
     *            引用的字符串值.
     * 
     * @return 新的或已存在的引用条目.
     */
    Item newStringishItem(final int type, final String value) {
        key2.set(type, value, null, null);
        Item result = get(key2);
        if (result == null) {
            pool.put12(type, newUTF8(value));
            result = new Item(index++, key2);
            put(result);
        }
        return result;
    }

    /**
     * 将类引用添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param value
     *            类的内部名称.
     * 
     * @return 新的或已存在的类引用项的索引.
     */
    public int newClass(final String value) {
        return newStringishItem(CLASS, value).index;
    }

    /**
     * 将方法类型引用添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param methodDesc
     *            方法类型的方法描述符.
     * 
     * @return 新的或已存在的方法类型引用项的索引.
     */
    public int newMethodType(final String methodDesc) {
        return newStringishItem(MTYPE, methodDesc).index;
    }

    /**
     * 将模块引用添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param moduleName
     *            模块的名称.
     * 
     * @return 新的或已存在的模块引用项的索引.
     */
    public int newModule(final String moduleName) {
        return newStringishItem(MODULE, moduleName).index;
    }

    /**
     * 将包引用添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     *
     * @param packageName
     *            内部格式的包的名称.
     * 
     * @return 新的或已存在的模块引用项的索引.
     */
    public int newPackage(final String packageName) {
        return newStringishItem(PACKAGE, packageName).index;
    }

    /**
     * 添加句柄到正在构建的类的常量池.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param tag
     *            句柄的类型.
     *            必须是{@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD},
     *            {@link Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     *            {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL}, {@link Opcodes#H_INVOKEINTERFACE}.
     * @param owner
     *            字段或方法所有者类的内部名称.
     * @param name
     *            字段或方法的名称.
     * @param desc
     *            字段或方法的描述符.
     * @param itf
     *            如果所有者是接口, 则为true.
     * 
     * @return 新的或已存在的方法类型引用项.
     */
    Item newHandleItem(final int tag, final String owner, final String name,
            final String desc, final boolean itf) {
        key4.set(HANDLE_BASE + tag, owner, name, desc);
        Item result = get(key4);
        if (result == null) {
            if (tag <= Opcodes.H_PUTSTATIC) {
                put112(HANDLE, tag, newField(owner, name, desc));
            } else {
                put112(HANDLE,
                        tag,
                        newMethod(owner, name, desc, itf));
            }
            result = new Item(index++, key4);
            put(result);
        }
        return result;
    }

    /**
     * 添加句柄到正在构建的类的常量池.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param tag
     *            句柄的类型.
     *            必须是{@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD},
     *            {@link Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     *            {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL}, {@link Opcodes#H_INVOKEINTERFACE}.
     * @param owner
     *            字段或方法所有者类的内部名称.
     * @param name
     *            字段或方法的名称.
     * @param desc
     *            字段或方法的描述符.
     * 
     * @return 新的或已存在的方法类型引用项的索引.
     *
     * @deprecated this method is superseded by
     *             {@link #newHandle(int, String, String, String, boolean)}.
     */
    @Deprecated
    public int newHandle(final int tag, final String owner, final String name,
            final String desc) {
        return newHandle(tag, owner, name, desc, tag == Opcodes.H_INVOKEINTERFACE);
    }

    /**
     * 添加句柄到正在构建的类的常量池.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     *
     * @param tag
     *            句柄的类型.
     *            必须是{@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD},
     *            {@link Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     *            {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL}, {@link Opcodes#H_INVOKEINTERFACE}.
     * @param owner
     *            字段或方法所有者类的内部名称.
     * @param name
     *            字段或方法的名称.
     * @param desc
     *            字段或方法的描述符.
     * @param itf
     *            如果所有者是接口, 则为true.
     * 
     * @return 新的或已存在的方法类型引用项的索引.
     */
    public int newHandle(final int tag, final String owner, final String name,
            final String desc, final boolean itf) {
        return newHandleItem(tag, owner, name, desc, itf).index;
    }

    /**
     * 添加调用动态引用到正在构建的类的常量池.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param name
     *            被调用方法的名称.
     * @param desc
     *            调用方法的描述符.
     * @param bsm
     *            引导方法.
     * @param bsmArgs
     *            bootstrap方法常量参数.
     * 
     * @return 新的或已存在的调用动态类型引用项.
     */
    Item newInvokeDynamicItem(final String name, final String desc,
            final Handle bsm, final Object... bsmArgs) {
        // cache for performance
        ByteVector bootstrapMethods = this.bootstrapMethods;
        if (bootstrapMethods == null) {
            bootstrapMethods = this.bootstrapMethods = new ByteVector();
        }

        int position = bootstrapMethods.length; // record current position

        int hashCode = bsm.hashCode();
        bootstrapMethods.putShort(newHandle(bsm.tag, bsm.owner, bsm.name,
                bsm.desc, bsm.isInterface()));

        int argsLength = bsmArgs.length;
        bootstrapMethods.putShort(argsLength);

        for (int i = 0; i < argsLength; i++) {
            Object bsmArg = bsmArgs[i];
            hashCode ^= bsmArg.hashCode();
            bootstrapMethods.putShort(newConst(bsmArg));
        }

        byte[] data = bootstrapMethods.data;
        int length = (1 + 1 + argsLength) << 1; // (bsm + argCount + arguments)
        hashCode &= 0x7FFFFFFF;
        Item result = items[hashCode % items.length];
        loop: while (result != null) {
            if (result.type != BSM || result.hashCode != hashCode) {
                result = result.next;
                continue;
            }

            // 因为数据编码参数的大小, 不需要测试这些大小是否等于
            int resultPosition = result.intVal;
            for (int p = 0; p < length; p++) {
                if (data[position + p] != data[resultPosition + p]) {
                    result = result.next;
                    continue loop;
                }
            }
            break;
        }

        int bootstrapMethodIndex;
        if (result != null) {
            bootstrapMethodIndex = result.index;
            bootstrapMethods.length = position; // revert to old position
        } else {
            bootstrapMethodIndex = bootstrapMethodsCount++;
            result = new Item(bootstrapMethodIndex);
            result.set(position, hashCode);
            put(result);
        }

        // now, create the InvokeDynamic constant
        key3.set(name, desc, bootstrapMethodIndex);
        result = get(key3);
        if (result == null) {
            put122(INDY, bootstrapMethodIndex, newNameType(name, desc));
            result = new Item(index++, key3);
            put(result);
        }
        return result;
    }

    /**
     * 添加调用动态引用到正在构建的类的常量池.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param name
     *            被调用方法的名称.
     * @param desc
     *            调用方法的描述符.
     * @param bsm
     *            引导方法.
     * @param bsmArgs
     *            bootstrap方法常量参数.
     * 
     * @return 新的或已存在的调用动态类型引用项的索引.
     */
    public int newInvokeDynamic(final String name, final String desc,
            final Handle bsm, final Object... bsmArgs) {
        return newInvokeDynamicItem(name, desc, bsm, bsmArgs).index;
    }

    /**
     * 将字段引用添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * 
     * @param owner
     *            字段所有者类的内部名称.
     * @param name
     *            该字段的名称.
     * @param desc
     *            字段的描述符.
     * 
     * @return 新的或已存在的字段引用项.
     */
    Item newFieldItem(final String owner, final String name, final String desc) {
        key3.set(FIELD, owner, name, desc);
        Item result = get(key3);
        if (result == null) {
            put122(FIELD, newClass(owner), newNameType(name, desc));
            result = new Item(index++, key3);
            put(result);
        }
        return result;
    }

    /**
     * 将字段引用添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param owner
     *            字段所有者类的内部名称.
     * @param name
     *            该字段的名称.
     * @param desc
     *            字段的描述符.
     * 
     * @return 新的或已存在的字段引用项的索引.
     */
    public int newField(final String owner, final String name, final String desc) {
        return newFieldItem(owner, name, desc).index;
    }

    /**
     * 将方法引用添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * 
     * @param owner
     *            方法所有者类的内部名称.
     * @param name
     *            方法的名称.
     * @param desc
     *            方法的描述符.
     * @param itf
     *            <tt>true</tt>如果<tt>所有者</tt>是一个接口.
     * 
     * @return 新的或已存在的方法引用项.
     */
    Item newMethodItem(final String owner, final String name,
            final String desc, final boolean itf) {
        int type = itf ? IMETH : METH;
        key3.set(type, owner, name, desc);
        Item result = get(key3);
        if (result == null) {
            put122(type, newClass(owner), newNameType(name, desc));
            result = new Item(index++, key3);
            put(result);
        }
        return result;
    }

    /**
     * 将方法引用添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param owner
     *            方法所有者类的内部名称.
     * @param name
     *            方法的名称.
     * @param desc
     *            方法的描述符.
     * @param itf
     *            <tt>true</tt>如果<tt>所有者</tt>是一个接口.
     * 
     * @return 新的或已存在的方法引用项的索引.
     */
    public int newMethod(final String owner, final String name,
            final String desc, final boolean itf) {
        return newMethodItem(owner, name, desc, itf).index;
    }

    /**
     * 将一个整数添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * 
     * @param value
     *            the int value.
     * 
     * @return 一个新的或已经存在的int项.
     */
    Item newInteger(final int value) {
        key.set(value);
        Item result = get(key);
        if (result == null) {
            pool.putByte(INT).putInt(value);
            result = new Item(index++, key);
            put(result);
        }
        return result;
    }

    /**
     * 将float添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * 
     * @param value
     *            the float value.
     * 
     * @return 一个新的或已经存在的float项.
     */
    Item newFloat(final float value) {
        key.set(value);
        Item result = get(key);
        if (result == null) {
            pool.putByte(FLOAT).putInt(key.intVal);
            result = new Item(index++, key);
            put(result);
        }
        return result;
    }

    /**
     * 将long添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * 
     * @param value
     *            the long value.
     * 
     * @return 一个新的或已经存在的long项.
     */
    Item newLong(final long value) {
        key.set(value);
        Item result = get(key);
        if (result == null) {
            pool.putByte(LONG).putLong(value);
            result = new Item(index, key);
            index += 2;
            put(result);
        }
        return result;
    }

    /**
     * 将double添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * 
     * @param value
     *            the double value.
     * 
     * @return 一个新的或已经存在的double项.
     */
    Item newDouble(final double value) {
        key.set(value);
        Item result = get(key);
        if (result == null) {
            pool.putByte(DOUBLE).putLong(key.longVal);
            result = new Item(index, key);
            index += 2;
            put(result);
        }
        return result;
    }

    /**
     * 将名称和类型添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param name
     *            名称.
     * @param desc
     *            类型描述符.
     * 
     * @return 新的或已存在的名称和类型项的索引.
     */
    public int newNameType(final String name, final String desc) {
        return newNameTypeItem(name, desc).index;
    }

    /**
     * 将名称和类型添加到正在构建的类的常量池中.
     * 如果常量池已包含类似项, 则不执行任何操作.
     * 
     * @param name
     *            名称.
     * @param desc
     *            类型描述符.
     * 
     * @return 新的或已存在的名称和类型项.
     */
    Item newNameTypeItem(final String name, final String desc) {
        key2.set(NAME_TYPE, name, desc, null);
        Item result = get(key2);
        if (result == null) {
            put122(NAME_TYPE, newUTF8(name), newUTF8(desc));
            result = new Item(index++, key2);
            put(result);
        }
        return result;
    }

    /**
     * 将给定的内部名称添加到{@link #typeTable}并返回其索引.
     * 如果类型表已包含此内部名称, 则不执行任何操作.
     * 
     * @param type
     *            要添加到类型表的内部名称.
     * 
     * @return 类型表中此内部名称的索引.
     */
    int addType(final String type) {
        key.set(TYPE_NORMAL, type, null, null);
        Item result = get(key);
        if (result == null) {
            result = addType(key);
        }
        return result.index;
    }

    /**
     * 将给定的"未初始化"类型添加到{@link #typeTable}并返回其索引.
     * 此方法用于UNINITIALIZED类型, 由内部名称和字节码偏移量组成.
     * 
     * @param type
     *            要添加到类型表的内部名称.
     * @param offset
     *            创建此UNINITIALIZED类型值的NEW指令的字节码偏移量.
     * 
     * @return 类型表中此内部名称的索引.
     */
    int addUninitializedType(final String type, final int offset) {
        key.type = TYPE_UNINIT;
        key.intVal = offset;
        key.strVal1 = type;
        key.hashCode = 0x7FFFFFFF & (TYPE_UNINIT + type.hashCode() + offset);
        Item result = get(key);
        if (result == null) {
            result = addType(key);
        }
        return result.index;
    }

    /**
     * 将给定的项添加到{@link #typeTable}.
     * 
     * @param item
     *            要添加到类型表的值.
     * 
     * @return 添加的Item, 它是一个与给定Item具有相同值的新Item实例.
     */
    private Item addType(final Item item) {
        ++typeCount;
        Item result = new Item(typeCount, item);
        put(result);
        if (typeTable == null) {
            typeTable = new Item[16];
        }
        if (typeCount == typeTable.length) {
            Item[] newTable = new Item[2 * typeTable.length];
            System.arraycopy(typeTable, 0, newTable, 0, typeTable.length);
            typeTable = newTable;
        }
        typeTable[typeCount] = result;
        return result;
    }

    /**
     * 返回两个给定类型的公共超类型的索引.
     * 此方法调用{@link #getCommonSuperClass}, 并将结果缓存在{@link #items}哈希表中, 以使用相同的参数加速将来的调用.
     * 
     * @param type1
     *            {@link #typeTable}中内部名称的索引.
     * @param type2
     *            {@link #typeTable}中内部名称的索引.
     * 
     * @return 两种给定类型的公共超类型的索引.
     */
    int getMergedType(final int type1, final int type2) {
        key2.type = TYPE_MERGED;
        key2.longVal = type1 | (((long) type2) << 32);
        key2.hashCode = 0x7FFFFFFF & (TYPE_MERGED + type1 + type2);
        Item result = get(key2);
        if (result == null) {
            String t = typeTable[type1].strVal1;
            String u = typeTable[type2].strVal1;
            key2.intVal = addType(getCommonSuperClass(t, u));
            result = new Item((short) 0, key2);
            put(result);
        }
        return result.intVal;
    }

    /**
     * 返回两个给定类型的公共超类型.
     * 此方法的默认实现<i>加载</i>两个给定的类, 并使用java.lang.Class 方法查找公共超类.
     * 可以覆盖它以其他方式计算此普通超类型, 特别是在不实际加载任何类的情况下,
     * 或者考虑这个ClassWriter当前正在生成的类, 当然可以加载它, 因为它正在构建中.
     * 
     * @param type1
     *            类的内部名称.
     * @param type2
     *            另一个类的内部名称.
     * 
     * @return 两个给定类的公共超类的内部名称.
     */
    protected String getCommonSuperClass(final String type1, final String type2) {
        Class<?> c, d;
        // SPRING PATCH: PREFER APPLICATION CLASSLOADER
        ClassLoader classLoader = getClassLoader();
        try {
            c = Class.forName(type1.replace('/', '.'), false, classLoader);
            d = Class.forName(type2.replace('/', '.'), false, classLoader);
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
        if (c.isAssignableFrom(d)) {
            return type1;
        }
        if (d.isAssignableFrom(c)) {
            return type2;
        }
        if (c.isInterface() || d.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                c = c.getSuperclass();
            } while (!c.isAssignableFrom(d));
            return c.getName().replace('.', '/');
        }
    }

    // SPRING PATCH: PREFER THREAD CONTEXT CLASSLOADER FOR APPLICATION CLASSES
    protected ClassLoader getClassLoader() {
        ClassLoader classLoader = null;
        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // 无法访问线程上下文ClassLoader - 回退...
        }
        return (classLoader != null ? classLoader : getClass().getClassLoader());
    }

    /**
     * 返回常量池的哈希表项, 该项等于给定项.
     * 
     * @param key
     *            常量池项.
     * 
     * @return 等于给定项的常量池的哈希表项​​, 或<tt>null</tt>如果没有这样的项.
     */
    private Item get(final Item key) {
        Item i = items[key.hashCode % items.length];
        while (i != null && (i.type != key.type || !key.isEqualTo(i))) {
            i = i.next;
        }
        return i;
    }

    /**
     * 将给定项放在常量池的哈希表中.
     * 哈希表<i>必须</i>不包含此项.
     * 
     * @param i
     *            要添加到常量池的哈希表的项.
     */
    private void put(final Item i) {
        if (index + typeCount > threshold) {
            int ll = items.length;
            int nl = ll * 2 + 1;
            Item[] newItems = new Item[nl];
            for (int l = ll - 1; l >= 0; --l) {
                Item j = items[l];
                while (j != null) {
                    int index = j.hashCode % newItems.length;
                    Item k = j.next;
                    j.next = newItems[index];
                    newItems[index] = j;
                    j = k;
                }
            }
            items = newItems;
            threshold = (int) (nl * 0.75);
        }
        int index = i.hashCode % items.length;
        i.next = items[index];
        items[index] = i;
    }

    /**
     * 将一个byte和两个short放入常量池中.
     * 
     * @param b
     *            a byte.
     * @param s1
     *            a short.
     * @param s2
     *            another short.
     */
    private void put122(final int b, final int s1, final int s2) {
        pool.put12(b, s1).putShort(s2);
    }

    /**
     * 将两个byte和一个short放入常量池中.
     * 
     * @param b1
     *            a byte.
     * @param b2
     *            another byte.
     * @param s
     *            a short.
     */
    private void put112(final int b1, final int b2, final int s) {
        pool.put11(b1, b2).putShort(s);
    }
}
