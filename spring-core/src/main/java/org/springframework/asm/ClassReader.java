package org.springframework.asm;

import java.io.IOException;
import java.io.InputStream;

/**
 * 一个Java类解析器, 用于使{@link ClassVisitor}访问现有类.
 * 此类解析符合Java类文件格式的字节数组, 并为每个字段, 方法和字节码指令调用给定类访问器的相应访问方法.
 */
public class ClassReader {

    /**
     * 为True以启用签名支持.
     */
    static final boolean SIGNATURES = true;

    /**
     * 为True以启用注解支持.
     */
    static final boolean ANNOTATIONS = true;

    /**
     * 为True以启用堆栈映射帧支持.
     */
    static final boolean FRAMES = true;

    /**
     * 为True以启用字节码写入支持.
     */
    static final boolean WRITER = true;

    /**
     * 为True以启用JSR_W和GOTO_W支持.
     */
    static final boolean RESIZE = true;

    /**
     * 跳过方法代码.
     * 如果设置了此标志, 则不会访问<code>CODE</code>属性.
     * 例如, 这可用于检索方法和方法参数的注解.
     */
    public static final int SKIP_CODE = 1;

    /**
     * 跳过类中的调试信息.
     * 如果设置了此标志, 则不访问该类的调试信息,
     * i.e. 不会调用{@link MethodVisitor#visitLocalVariable visitLocalVariable}和
     * {@link MethodVisitor#visitLineNumber visitLineNumber}方法.
     */
    public static final int SKIP_DEBUG = 2;

    /**
     * 跳过类中的堆栈映射帧.
     * 如果设置了此标志, 则不访问该类的堆栈映射帧,
     * i.e. 不会调用{@link MethodVisitor#visitFrame visitFrame}方法.
     * 使用{@link ClassWriter#COMPUTE_FRAMES}选项时, 此标志很有用:
     * 它避免访问将被忽略的帧, 并在类编写器中从头开始重新计算.
     */
    public static final int SKIP_FRAMES = 4;

    /**
     * 用于扩展堆栈映射帧.
     * 默认情况下, 以原始格式访问堆栈映射帧 (i.e. "expanded"用于版本低于V1_6的类, "compressed"用于其他类).
     * 如果设置了此标志, 则始终以扩展格式访问堆栈映射帧
     * (此选项在ClassReader和ClassWriter中添加了一个解压缩/重新压缩的步骤, 这会降低性能).
     */
    public static final int EXPAND_FRAMES = 8;

    /**
     * 将ASM伪指令扩展为等效的标准字节码指令序列.
     * 在解析正向跳转时, 可能会发生, 为其保留的有符号2字节偏移量不足以存储字节码偏移量.
     * 在这种情况下, 使用无符号2字节偏移量, 将跳转指令替换为临时的ASM伪指令 (see Label#resolve).
     * 此内部标志用于重新读取包含此类指令的类, 以便用标准指令替换它们.
     * 此外, 当使用此标志时, GOTO_W 和 JSR_W <i>不会</i>转换为GOTO和JSR,
     * 以确保在ClassReader中用GOTO替换GOTO_W, 并在ClassWriter中转换回GOTO_W的无限循环不会发生.
     */
    static final int EXPAND_ASM_INSNS = 256;

    /**
     * 要解析的类.
     * <i>不得修改此数组的内容.
     * 此字段适用于{@link Attribute}子类, 类生成器或适配器通常不需要此字段.</i>
     */
    public final byte[] b;

    /**
     * {@link #b b}中每个常量池项的起始索引, 加一.
     * 一个字节的偏移量, 会跳过指示其类型的常量池项标记.
     */
    private final int[] items;

    /**
     * 与CONSTANT_Utf8项对应的String对象.
     * 此缓存避免了对给定CONSTANT_Utf8常量池条目的多次解析, 这极大地提高了性能 (通过因子2到3).
     * 这种缓存策略可以扩展到所有常量池条目, 但它的好处对于这些条目来说不是那么好
     * (因为它们比CONSTANT_Utf8条目要便宜得多).
     */
    private final String[] strings;

    /**
     * 类的常量池中包含的字符串的最大长度.
     */
    private final int maxStringLength;

    /**
     * {@link #b b}中类header信息 (access, name...)的起始索引.
     */
    public final int header;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * @param b
     *            要读取的类的字节码.
     */
    public ClassReader(final byte[] b) {
        this(b, 0, b.length);
    }

    /**
     * @param b
     *            要读取的类的字节码.
     * @param off
     *            类数据的起始偏移量.
     * @param len
     *            类数据的长度.
     */
    public ClassReader(final byte[] b, final int off, final int len) {
        this.b = b;
        // checks the class version
		/* SPRING PATCH: REMOVED FOR FORWARD COMPATIBILITY WITH JDK 9
        if (readShort(off + 6) > Opcodes.V1_8) {
            throw new IllegalArgumentException();
        }
		*/
        // 解析常量池
        items = new int[readUnsignedShort(off + 8)];
        int n = items.length;
        strings = new String[n];
        int max = 0;
        int index = off + 10;
        for (int i = 1; i < n; ++i) {
            items[i] = index + 1;
            int size;
            switch (b[index]) {
            case ClassWriter.FIELD:
            case ClassWriter.METH:
            case ClassWriter.IMETH:
            case ClassWriter.INT:
            case ClassWriter.FLOAT:
            case ClassWriter.NAME_TYPE:
            case ClassWriter.INDY:
                size = 5;
                break;
            case ClassWriter.LONG:
            case ClassWriter.DOUBLE:
                size = 9;
                ++i;
                break;
            case ClassWriter.UTF8:
                size = 3 + readUnsignedShort(index + 1);
                if (size > max) {
                    max = size;
                }
                break;
            case ClassWriter.HANDLE:
                size = 4;
                break;
            // case ClassWriter.CLASS:
            // case ClassWriter.STR:
            // case ClassWriter.MTYPE
            // case ClassWriter.PACKAGE:
            // case ClassWriter.MODULE:
            default:
                size = 3;
                break;
            }
            index += size;
        }
        maxStringLength = max;
        // 类header信息在常量池之后开始
        header = index;
    }

    /**
     * 返回类的访问标志 (see {@link Opcodes}).
     * 当字节码在1.5之前并且这些标志由属性表示时, 此值可能不反映Deprecated 的和Synthetic 的标志.
     * 
     * @return 类的访问标志
     */
    public int getAccess() {
        return readUnsignedShort(header);
    }

    /**
     * 返回类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     * 
     * @return 内部类名
     */
    public String getClassName() {
        return readClass(header + 2, new char[maxStringLength]);
    }

    /**
     * 返回超类的内部名称 (see {@link Type#getInternalName() getInternalName}).
     * 对于接口, 超类是{@link Object}.
     * 
     * @return 超类的内部名称, 或<tt>null</tt>对于{@link Object}类.
     */
    public String getSuperName() {
        return readClass(header + 4, new char[maxStringLength]);
    }

    /**
     * 返回类的接口的内部名称 (see {@link Type#getInternalName() getInternalName}).
     * 
     * @return 所有已实现的接口的内部名称数组, 或<tt>null</tt>.
     */
    public String[] getInterfaces() {
        int index = header + 6;
        int n = readUnsignedShort(index);
        String[] interfaces = new String[n];
        if (n > 0) {
            char[] buf = new char[maxStringLength];
            for (int i = 0; i < n; ++i) {
                index += 2;
                interfaces[i] = readClass(index, buf);
            }
        }
        return interfaces;
    }

    /**
     * 将常量池数据复制到给定的{@link ClassWriter}.
     * 应该在{@link #accept(ClassVisitor,int)}方法之前调用.
     * 
     * @param classWriter
     *            将常量池复制到的{@link ClassWriter}.
     */
    void copyPool(final ClassWriter classWriter) {
        char[] buf = new char[maxStringLength];
        int ll = items.length;
        Item[] items2 = new Item[ll];
        for (int i = 1; i < ll; i++) {
            int index = items[i];
            int tag = b[index - 1];
            Item item = new Item(i);
            int nameType;
            switch (tag) {
            case ClassWriter.FIELD:
            case ClassWriter.METH:
            case ClassWriter.IMETH:
                nameType = items[readUnsignedShort(index + 2)];
                item.set(tag, readClass(index, buf), readUTF8(nameType, buf),
                        readUTF8(nameType + 2, buf));
                break;
            case ClassWriter.INT:
                item.set(readInt(index));
                break;
            case ClassWriter.FLOAT:
                item.set(Float.intBitsToFloat(readInt(index)));
                break;
            case ClassWriter.NAME_TYPE:
                item.set(tag, readUTF8(index, buf), readUTF8(index + 2, buf),
                        null);
                break;
            case ClassWriter.LONG:
                item.set(readLong(index));
                ++i;
                break;
            case ClassWriter.DOUBLE:
                item.set(Double.longBitsToDouble(readLong(index)));
                ++i;
                break;
            case ClassWriter.UTF8: {
                String s = strings[i];
                if (s == null) {
                    index = items[i];
                    s = strings[i] = readUTF(index + 2,
                            readUnsignedShort(index), buf);
                }
                item.set(tag, s, null, null);
                break;
            }
            case ClassWriter.HANDLE: {
                int fieldOrMethodRef = items[readUnsignedShort(index + 1)];
                nameType = items[readUnsignedShort(fieldOrMethodRef + 2)];
                item.set(ClassWriter.HANDLE_BASE + readByte(index),
                        readClass(fieldOrMethodRef, buf),
                        readUTF8(nameType, buf), readUTF8(nameType + 2, buf));
                break;
            }
            case ClassWriter.INDY:
                if (classWriter.bootstrapMethods == null) {
                    copyBootstrapMethods(classWriter, items2, buf);
                }
                nameType = items[readUnsignedShort(index + 2)];
                item.set(readUTF8(nameType, buf), readUTF8(nameType + 2, buf),
                        readUnsignedShort(index));
                break;
            // case ClassWriter.STR:
            // case ClassWriter.CLASS:
            // case ClassWriter.MTYPE:
            // case ClassWriter.MODULE:
            // case ClassWriter.PACKAGE:
            default:
                item.set(tag, readUTF8(index, buf), null, null);
                break;
            }

            int index2 = item.hashCode % items2.length;
            item.next = items2[index2];
            items2[index2] = item;
        }

        int off = items[1] - 1;
        classWriter.pool.putByteArray(b, off, header - off);
        classWriter.items = items2;
        classWriter.threshold = (int) (0.75d * ll);
        classWriter.index = ll;
    }

    /**
     * 将引导方法数据复制到给定的{@link ClassWriter}.
     * 应该在{@link #accept(ClassVisitor,int)}方法之前调用.
     * 
     * @param classWriter
     *            将bootstrap方法复制到的{@link ClassWriter}.
     */
    private void copyBootstrapMethods(final ClassWriter classWriter,
            final Item[] items, final char[] c) {
        // finds the "BootstrapMethods" attribute
        int u = getAttributes();
        boolean found = false;
        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);
            if ("BootstrapMethods".equals(attrName)) {
                found = true;
                break;
            }
            u += 6 + readInt(u + 4);
        }
        if (!found) {
            return;
        }
        // 复制类编写器中的bootstrap方法
        int boostrapMethodCount = readUnsignedShort(u + 8);
        for (int j = 0, v = u + 10; j < boostrapMethodCount; j++) {
            int position = v - u - 10;
            int hashCode = readConst(readUnsignedShort(v), c).hashCode();
            for (int k = readUnsignedShort(v + 2); k > 0; --k) {
                hashCode ^= readConst(readUnsignedShort(v + 4), c).hashCode();
                v += 2;
            }
            v += 4;
            Item item = new Item(j);
            item.set(position, hashCode & 0x7FFFFFFF);
            int index = item.hashCode % items.length;
            item.next = items[index];
            items[index] = item;
        }
        int attrSize = readInt(u + 4);
        ByteVector bootstrapMethods = new ByteVector(attrSize + 62);
        bootstrapMethods.putByteArray(b, u + 10, attrSize - 2);
        classWriter.bootstrapMethodsCount = boostrapMethodCount;
        classWriter.bootstrapMethods = bootstrapMethods;
    }

    /**
     * @param is
     *            从中读取类的输入流.
     * 
     * @throws IOException
     *             如果在读取过程中出现问题.
     */
    public ClassReader(final InputStream is) throws IOException {
        this(readClass(is, false));
    }

    /**
     * @param name
     *            要读取的类的二进制限定名称.
     * 
     * @throws IOException
     *             如果在读取过程中出现问题.
     */
    public ClassReader(final String name) throws IOException {
        this(readClass(
                ClassLoader.getSystemResourceAsStream(name.replace('.', '/')
                        + ".class"), true));
    }

    /**
     * 读取类的字节码.
     * 
     * @param is
     *            从中读取类的输入流.
     * @param close
     *            如果读取后关闭输入流, 则为true.
     * 
     * @return 从给定输入流读取的字节码.
     * @throws IOException
     *             如果在读取过程中出现问题.
     */
    private static byte[] readClass(final InputStream is, boolean close)
            throws IOException {
        if (is == null) {
            throw new IOException("Class not found");
        }
        try {
            byte[] b = new byte[is.available()];
            int len = 0;
            while (true) {
                int n = is.read(b, len, b.length - len);
                if (n == -1) {
                    if (len < b.length) {
                        byte[] c = new byte[len];
                        System.arraycopy(b, 0, c, 0, len);
                        b = c;
                    }
                    return b;
                }
                len += n;
                if (len == b.length) {
                    int last = is.read();
                    if (last < 0) {
                        return b;
                    }
                    byte[] c = new byte[b.length + 1000];
                    System.arraycopy(b, 0, c, 0, len);
                    c[len++] = (byte) last;
                    b = c;
                }
            }
        } finally {
            if (close) {
                is.close();
            }
        }
    }

    // ------------------------------------------------------------------------
    // Public methods
    // ------------------------------------------------------------------------

    /**
     * 使给定的访问器访问此{@link ClassReader}的Java类.
     * 此类是构造函数中指定的类 (see {@link #ClassReader(byte[]) ClassReader}).
     * 
     * @param classVisitor
     *            必须访问此类的访问器.
     * @param flags
     *            用于修改此类的默认行为.
     *            See {@link #SKIP_DEBUG}, {@link #EXPAND_FRAMES} , {@link #SKIP_FRAMES}, {@link #SKIP_CODE}.
     */
    public void accept(final ClassVisitor classVisitor, final int flags) {
        accept(classVisitor, new Attribute[0], flags);
    }

    /**
     * 使给定的访问器访问此{@link ClassReader}的Java类.
     * 此类是构造函数中指定的类 (see {@link #ClassReader(byte[]) ClassReader}).
     * 
     * @param classVisitor
     *            必须访问此类的访问器.
     * @param attrs
     *            在访问类时必须解析的属性原型.
     *            任何类型不等于原型类型的属性都不会被解析:
     *            它的字节数组值将不变地传递给ClassWriter.
     *            <i>如果此值包含对常量池的引用, 或者具有与类元素的语法或语义链接,
     *            而类元素已由读取器和写入器之间的类适配器转换, 则可能会损坏它</i>.
     * @param flags
     *            用于修改此类的默认行为.
     *            See {@link #SKIP_DEBUG}, {@link #EXPAND_FRAMES}, {@link #SKIP_FRAMES}, {@link #SKIP_CODE}.
     */
    public void accept(final ClassVisitor classVisitor,
            final Attribute[] attrs, final int flags) {
        int u = header; // 类文件中的当前偏移量
        char[] c = new char[maxStringLength]; // 用于读取字符串的缓冲区

        Context context = new Context();
        context.attrs = attrs;
        context.flags = flags;
        context.buffer = c;

        // 读取类声明
        int access = readUnsignedShort(u);
        String name = readClass(u + 2, c);
        String superClass = readClass(u + 4, c);
        String[] interfaces = new String[readUnsignedShort(u + 6)];
        u += 8;
        for (int i = 0; i < interfaces.length; ++i) {
            interfaces[i] = readClass(u, c);
            u += 2;
        }

        // 读取类属性
        String signature = null;
        String sourceFile = null;
        String sourceDebug = null;
        String enclosingOwner = null;
        String enclosingName = null;
        String enclosingDesc = null;
        String moduleMainClass = null;
        int anns = 0;
        int ianns = 0;
        int tanns = 0;
        int itanns = 0;
        int innerClasses = 0;
        int module = 0;
        int packages = 0;
        Attribute attributes = null;

        u = getAttributes();
        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);
            // 测试按递减频率顺序排序 (基于典型类中观察到的频率)
            if ("SourceFile".equals(attrName)) {
                sourceFile = readUTF8(u + 8, c);
            } else if ("InnerClasses".equals(attrName)) {
                innerClasses = u + 8;
            } else if ("EnclosingMethod".equals(attrName)) {
                enclosingOwner = readClass(u + 8, c);
                int item = readUnsignedShort(u + 10);
                if (item != 0) {
                    enclosingName = readUTF8(items[item], c);
                    enclosingDesc = readUTF8(items[item] + 2, c);
                }
            } else if (SIGNATURES && "Signature".equals(attrName)) {
                signature = readUTF8(u + 8, c);
            } else if (ANNOTATIONS
                    && "RuntimeVisibleAnnotations".equals(attrName)) {
                anns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleTypeAnnotations".equals(attrName)) {
                tanns = u + 8;
            } else if ("Deprecated".equals(attrName)) {
                access |= Opcodes.ACC_DEPRECATED;
            } else if ("Synthetic".equals(attrName)) {
                access |= Opcodes.ACC_SYNTHETIC
                        | ClassWriter.ACC_SYNTHETIC_ATTRIBUTE;
            } else if ("SourceDebugExtension".equals(attrName)) {
                int len = readInt(u + 4);
                sourceDebug = readUTF(u + 8, len, new char[len]);
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleAnnotations".equals(attrName)) {
                ianns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleTypeAnnotations".equals(attrName)) {
                itanns = u + 8;
            } else if ("Module".equals(attrName)) {
                module = u + 8;
            } else if ("ModuleMainClass".equals(attrName)) {
                moduleMainClass = readClass(u + 8, c);
            } else if ("ModulePackages".equals(attrName)) {
                packages = u + 10;
            } else if ("BootstrapMethods".equals(attrName)) {
                int[] bootstrapMethods = new int[readUnsignedShort(u + 8)];
                for (int j = 0, v = u + 10; j < bootstrapMethods.length; j++) {
                    bootstrapMethods[j] = v;
                    v += 2 + readUnsignedShort(v + 2) << 1;
                }
                context.bootstrapMethods = bootstrapMethods;
            } else {
                Attribute attr = readAttribute(attrs, attrName, u + 8,
                        readInt(u + 4), c, -1, null);
                if (attr != null) {
                    attr.next = attributes;
                    attributes = attr;
                }
            }
            u += 6 + readInt(u + 4);
        }

        // 访问类声明
        classVisitor.visit(readInt(items[1] - 7), access, name, signature,
                superClass, interfaces);

        // 访问源和调试信息
        if ((flags & SKIP_DEBUG) == 0
                && (sourceFile != null || sourceDebug != null)) {
            classVisitor.visitSource(sourceFile, sourceDebug);
        }

        // 访问模块信息和相关属性
        if (module != 0) {
            readModule(classVisitor, context, module,
                    moduleMainClass, packages);
        }

        // 访问外层类
        if (enclosingOwner != null) {
            classVisitor.visitOuterClass(enclosingOwner, enclosingName,
                    enclosingDesc);
        }

        // 访问类注解和类型注解
        if (ANNOTATIONS && anns != 0) {
            for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        classVisitor.visitAnnotation(readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && ianns != 0) {
            for (int i = readUnsignedShort(ianns), v = ianns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        classVisitor.visitAnnotation(readUTF8(v, c), false));
            }
        }
        if (ANNOTATIONS && tanns != 0) {
            for (int i = readUnsignedShort(tanns), v = tanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        classVisitor.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && itanns != 0) {
            for (int i = readUnsignedShort(itanns), v = itanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        classVisitor.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), false));
            }
        }

        // 访问属性
        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            classVisitor.visitAttribute(attributes);
            attributes = attr;
        }

        // 访问内部类
        if (innerClasses != 0) {
            int v = innerClasses + 2;
            for (int i = readUnsignedShort(innerClasses); i > 0; --i) {
                classVisitor.visitInnerClass(readClass(v, c),
                        readClass(v + 2, c), readUTF8(v + 4, c),
                        readUnsignedShort(v + 6));
                v += 8;
            }
        }

        // 访问字段和方法
        u = header + 10 + 2 * interfaces.length;
        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            u = readField(classVisitor, context, u);
        }
        u += 2;
        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            u = readMethod(classVisitor, context, u);
        }

        // 访问类的结尾
        classVisitor.visitEnd();
    }

    /**
     * 读取模块属性并访问它.
     *
     * @param classVisitor
     *           当前类访问器
     * @param context
     *           有关正在解析的类的信息.
     * @param u
     *           在类文件中module属性的开始偏移量.
     * @param mainClass
     *           模块主类的名称或null.
     * @param packages
     *           隐藏的package属性的起始偏移量.
     */
    private void readModule(final ClassVisitor classVisitor,
            final Context context, int u,
            final String mainClass, int packages) {

        char[] buffer = context.buffer;

        // 读取模块名称, 标志和版本
        String name = readModule(u, buffer);
        int flags = readUnsignedShort(u + 2);
        String version = readUTF8(u + 4, buffer);
        u += 6;

        ModuleVisitor mv = classVisitor.visitModule(name, flags, version);
        if (mv == null) {
            return;
        }

        // module attributes (main class, packages)
        if (mainClass != null) {
            mv.visitMainClass(mainClass);
        }

        if (packages != 0) {
            for (int i = readUnsignedShort(packages - 2); i > 0; --i) {
                String packaze = readPackage(packages, buffer);
                mv.visitPackage(packaze);
                packages += 2;
            }
        }

        // reads requires
        u += 2;
        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            String module = readModule(u, buffer);
            int access = readUnsignedShort(u + 2);
            String requireVersion = readUTF8(u + 4, buffer);
            mv.visitRequire(module, access, requireVersion);
            u += 6;
        }

        // reads exports
        u += 2;
        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            String export = readPackage(u, buffer);
            int access = readUnsignedShort(u + 2);
            int exportToCount = readUnsignedShort(u + 4);
            u += 6;
            String[] tos = null;
            if (exportToCount != 0) {
                tos = new String[exportToCount];
                for (int j = 0; j < tos.length; ++j) {
                    tos[j] = readModule(u, buffer);
                    u += 2;
                }
            }
            mv.visitExport(export, access, tos);
        }

        // reads opens
        u += 2;
        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            String open = readPackage(u, buffer);
            int access = readUnsignedShort(u + 2);
            int openToCount = readUnsignedShort(u + 4);
            u += 6;
            String[] tos = null;
            if (openToCount != 0) {
                tos = new String[openToCount];
                for (int j = 0; j < tos.length; ++j) {
                    tos[j] = readModule(u, buffer);
                    u += 2;
                }
            }
            mv.visitOpen(open, access, tos);
        }

        // read uses
        u += 2;
        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            mv.visitUse(readClass(u, buffer));
            u += 2;
        }

        // read provides
        u += 2;
        for (int i = readUnsignedShort(u - 2); i > 0; --i) {
            String service = readClass(u, buffer);
            int provideWithCount = readUnsignedShort(u + 2);
            u += 4;
            String[] withs = new String[provideWithCount];
            for (int j = 0; j < withs.length; ++j) {
                withs[j] = readClass(u, buffer);
                u += 2;
            }
            mv.visitProvide(service, withs);
        }

        mv.visitEnd();
    }

    /**
     * 读取一个字段并让给定的访问器访问它.
     * 
     * @param classVisitor
     *            必须访问该字段的访问器.
     * @param context
     *            有关正在解析的类的信息.
     * @param u
     *            类文件中字段的起始偏移量.
     * 
     * @return 类中字段后面的第一个字节的偏移量.
     */
    private int readField(final ClassVisitor classVisitor,
            final Context context, int u) {
        // 读取字段声明
        char[] c = context.buffer;
        int access = readUnsignedShort(u);
        String name = readUTF8(u + 2, c);
        String desc = readUTF8(u + 4, c);
        u += 6;

        // 读取字段属性
        String signature = null;
        int anns = 0;
        int ianns = 0;
        int tanns = 0;
        int itanns = 0;
        Object value = null;
        Attribute attributes = null;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);
            // 测试按递减频率顺序排序 (基于典型类中观察到的频率)
            if ("ConstantValue".equals(attrName)) {
                int item = readUnsignedShort(u + 8);
                value = item == 0 ? null : readConst(item, c);
            } else if (SIGNATURES && "Signature".equals(attrName)) {
                signature = readUTF8(u + 8, c);
            } else if ("Deprecated".equals(attrName)) {
                access |= Opcodes.ACC_DEPRECATED;
            } else if ("Synthetic".equals(attrName)) {
                access |= Opcodes.ACC_SYNTHETIC
                        | ClassWriter.ACC_SYNTHETIC_ATTRIBUTE;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleAnnotations".equals(attrName)) {
                anns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleTypeAnnotations".equals(attrName)) {
                tanns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleAnnotations".equals(attrName)) {
                ianns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleTypeAnnotations".equals(attrName)) {
                itanns = u + 8;
            } else {
                Attribute attr = readAttribute(context.attrs, attrName, u + 8,
                        readInt(u + 4), c, -1, null);
                if (attr != null) {
                    attr.next = attributes;
                    attributes = attr;
                }
            }
            u += 6 + readInt(u + 4);
        }
        u += 2;

        // 访问字段声明
        FieldVisitor fv = classVisitor.visitField(access, name, desc,
                signature, value);
        if (fv == null) {
            return u;
        }

        // 访问字段注解和类型注解
        if (ANNOTATIONS && anns != 0) {
            for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        fv.visitAnnotation(readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && ianns != 0) {
            for (int i = readUnsignedShort(ianns), v = ianns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        fv.visitAnnotation(readUTF8(v, c), false));
            }
        }
        if (ANNOTATIONS && tanns != 0) {
            for (int i = readUnsignedShort(tanns), v = tanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        fv.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && itanns != 0) {
            for (int i = readUnsignedShort(itanns), v = itanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        fv.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), false));
            }
        }

        // 访问字段属性
        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            fv.visitAttribute(attributes);
            attributes = attr;
        }

        // 访问字段的结尾
        fv.visitEnd();

        return u;
    }

    /**
     * 读取方法并让给定的访问器访问它.
     * 
     * @param classVisitor
     *            必须访问该方法的访问器.
     * @param context
     *            有关正在解析的类的信息.
     * @param u
     *            类文件中方法的起始偏移量.
     * 
     * @return 类中方法后面的第一个字节的偏移量.
     */
    private int readMethod(final ClassVisitor classVisitor,
            final Context context, int u) {
        // 读取方法声明
        char[] c = context.buffer;
        context.access = readUnsignedShort(u);
        context.name = readUTF8(u + 2, c);
        context.desc = readUTF8(u + 4, c);
        u += 6;

        // 读取方法属性
        int code = 0;
        int exception = 0;
        String[] exceptions = null;
        String signature = null;
        int methodParameters = 0;
        int anns = 0;
        int ianns = 0;
        int tanns = 0;
        int itanns = 0;
        int dann = 0;
        int mpanns = 0;
        int impanns = 0;
        int firstAttribute = u;
        Attribute attributes = null;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);
            // 测试按递减频率顺序排序 (基于典型类中观察到的频率)
            if ("Code".equals(attrName)) {
                if ((context.flags & SKIP_CODE) == 0) {
                    code = u + 8;
                }
            } else if ("Exceptions".equals(attrName)) {
                exceptions = new String[readUnsignedShort(u + 8)];
                exception = u + 10;
                for (int j = 0; j < exceptions.length; ++j) {
                    exceptions[j] = readClass(exception, c);
                    exception += 2;
                }
            } else if (SIGNATURES && "Signature".equals(attrName)) {
                signature = readUTF8(u + 8, c);
            } else if ("Deprecated".equals(attrName)) {
                context.access |= Opcodes.ACC_DEPRECATED;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleAnnotations".equals(attrName)) {
                anns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleTypeAnnotations".equals(attrName)) {
                tanns = u + 8;
            } else if (ANNOTATIONS && "AnnotationDefault".equals(attrName)) {
                dann = u + 8;
            } else if ("Synthetic".equals(attrName)) {
                context.access |= Opcodes.ACC_SYNTHETIC
                        | ClassWriter.ACC_SYNTHETIC_ATTRIBUTE;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleAnnotations".equals(attrName)) {
                ianns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleTypeAnnotations".equals(attrName)) {
                itanns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeVisibleParameterAnnotations".equals(attrName)) {
                mpanns = u + 8;
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleParameterAnnotations".equals(attrName)) {
                impanns = u + 8;
            } else if ("MethodParameters".equals(attrName)) {
                methodParameters = u + 8;
            } else {
                Attribute attr = readAttribute(context.attrs, attrName, u + 8,
                        readInt(u + 4), c, -1, null);
                if (attr != null) {
                    attr.next = attributes;
                    attributes = attr;
                }
            }
            u += 6 + readInt(u + 4);
        }
        u += 2;

        // 访问方法声明
        MethodVisitor mv = classVisitor.visitMethod(context.access,
                context.name, context.desc, signature, exceptions);
        if (mv == null) {
            return u;
        }

        /*
         * 如果返回的MethodVisitor实际上是一个MethodWriter, 则意味着读取器和写入器之间没有方法适配器.
         * 另外, 如果从这个读取器复制了写入器的常量池 (mw.cw.cr == this), 并且方法的签名和异常没有被更改,
         * 然后可以跳过所有访问事件, 只需将方法的原始代码复制到写入器
         * (访问, 名称和描述符可以更改, 这并不重要, 因为它们不会像读取器那样被复制).
         */
        if (WRITER && mv instanceof MethodWriter) {
            MethodWriter mw = (MethodWriter) mv;
            if (mw.cw.cr == this &&
					(signature != null ? signature.equals(mw.signature) : mw.signature == null)) {
                boolean sameExceptions = false;
                if (exceptions == null) {
                    sameExceptions = mw.exceptionCount == 0;
                } else if (exceptions.length == mw.exceptionCount) {
                    sameExceptions = true;
                    for (int j = exceptions.length - 1; j >= 0; --j) {
                        exception -= 2;
                        if (mw.exceptions[j] != readUnsignedShort(exception)) {
                            sameExceptions = false;
                            break;
                        }
                    }
                }
                if (sameExceptions) {
                    /*
                     * 不直接将代码复制到MethodWriter中以保存字节数组复制操作.
                     * 真正的复制将在ClassWriter.toByteArray()中完成.
                     */
                    mw.classReaderOffset = firstAttribute;
                    mw.classReaderLength = u - firstAttribute;
                    return u;
                }
            }
        }

        // visit the method parameters
        if (methodParameters != 0) {
            for (int i = b[methodParameters] & 0xFF, v = methodParameters + 1; i > 0; --i, v = v + 4) {
                mv.visitParameter(readUTF8(v, c), readUnsignedShort(v + 2));
            }
        }

        // visits the method annotations
        if (ANNOTATIONS && dann != 0) {
            AnnotationVisitor dv = mv.visitAnnotationDefault();
            readAnnotationValue(dann, c, null, dv);
            if (dv != null) {
                dv.visitEnd();
            }
        }
        if (ANNOTATIONS && anns != 0) {
            for (int i = readUnsignedShort(anns), v = anns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        mv.visitAnnotation(readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && ianns != 0) {
            for (int i = readUnsignedShort(ianns), v = ianns + 2; i > 0; --i) {
                v = readAnnotationValues(v + 2, c, true,
                        mv.visitAnnotation(readUTF8(v, c), false));
            }
        }
        if (ANNOTATIONS && tanns != 0) {
            for (int i = readUnsignedShort(tanns), v = tanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        mv.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), true));
            }
        }
        if (ANNOTATIONS && itanns != 0) {
            for (int i = readUnsignedShort(itanns), v = itanns + 2; i > 0; --i) {
                v = readAnnotationTarget(context, v);
                v = readAnnotationValues(v + 2, c, true,
                        mv.visitTypeAnnotation(context.typeRef,
                                context.typePath, readUTF8(v, c), false));
            }
        }
        if (ANNOTATIONS && mpanns != 0) {
            readParameterAnnotations(mv, context, mpanns, true);
        }
        if (ANNOTATIONS && impanns != 0) {
            readParameterAnnotations(mv, context, impanns, false);
        }

        // visits the method attributes
        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            mv.visitAttribute(attributes);
            attributes = attr;
        }

        // visits the method code
        if (code != 0) {
            mv.visitCode();
            readCode(mv, context, code);
        }

        // visits the end of the method
        mv.visitEnd();

        return u;
    }

    /**
     * 读取方法的字节码并使给定的访问器访问它.
     * 
     * @param mv
     *            必须访问方法代码的访问器.
     * @param context
     *            有关正在解析的类的信息.
     * @param u
     *            类文件中code属性的起始偏移量.
     */
    private void readCode(final MethodVisitor mv, final Context context, int u) {
        // reads the header
        byte[] b = this.b;
        char[] c = context.buffer;
        int maxStack = readUnsignedShort(u);
        int maxLocals = readUnsignedShort(u + 2);
        int codeLength = readInt(u + 4);
        u += 8;

        // reads the bytecode to find the labels
        int codeStart = u;
        int codeEnd = u + codeLength;
        Label[] labels = context.labels = new Label[codeLength + 2];
        readLabel(codeLength + 1, labels);
        while (u < codeEnd) {
            int offset = u - codeStart;
            int opcode = b[u] & 0xFF;
            switch (ClassWriter.TYPE[opcode]) {
            case ClassWriter.NOARG_INSN:
            case ClassWriter.IMPLVAR_INSN:
                u += 1;
                break;
            case ClassWriter.LABEL_INSN:
                readLabel(offset + readShort(u + 1), labels);
                u += 3;
                break;
            case ClassWriter.ASM_LABEL_INSN:
                readLabel(offset + readUnsignedShort(u + 1), labels);
                u += 3;
                break;
            case ClassWriter.LABELW_INSN:
            case ClassWriter.ASM_LABELW_INSN:
                readLabel(offset + readInt(u + 1), labels);
                u += 5;
                break;
            case ClassWriter.WIDE_INSN:
                opcode = b[u + 1] & 0xFF;
                if (opcode == Opcodes.IINC) {
                    u += 6;
                } else {
                    u += 4;
                }
                break;
            case ClassWriter.TABL_INSN:
                // skips 0 to 3 padding bytes
                u = u + 4 - (offset & 3);
                // reads instruction
                readLabel(offset + readInt(u), labels);
                for (int i = readInt(u + 8) - readInt(u + 4) + 1; i > 0; --i) {
                    readLabel(offset + readInt(u + 12), labels);
                    u += 4;
                }
                u += 12;
                break;
            case ClassWriter.LOOK_INSN:
                // skips 0 to 3 padding bytes
                u = u + 4 - (offset & 3);
                // reads instruction
                readLabel(offset + readInt(u), labels);
                for (int i = readInt(u + 4); i > 0; --i) {
                    readLabel(offset + readInt(u + 12), labels);
                    u += 8;
                }
                u += 8;
                break;
            case ClassWriter.VAR_INSN:
            case ClassWriter.SBYTE_INSN:
            case ClassWriter.LDC_INSN:
                u += 2;
                break;
            case ClassWriter.SHORT_INSN:
            case ClassWriter.LDCW_INSN:
            case ClassWriter.FIELDORMETH_INSN:
            case ClassWriter.TYPE_INSN:
            case ClassWriter.IINC_INSN:
                u += 3;
                break;
            case ClassWriter.ITFMETH_INSN:
            case ClassWriter.INDYMETH_INSN:
                u += 5;
                break;
            // case MANA_INSN:
            default:
                u += 4;
                break;
            }
        }

        // 读取try catch条目以查找标签, 并访问它们
        for (int i = readUnsignedShort(u); i > 0; --i) {
            Label start = readLabel(readUnsignedShort(u + 2), labels);
            Label end = readLabel(readUnsignedShort(u + 4), labels);
            Label handler = readLabel(readUnsignedShort(u + 6), labels);
            String type = readUTF8(items[readUnsignedShort(u + 8)], c);
            mv.visitTryCatchBlock(start, end, handler, type);
            u += 8;
        }
        u += 2;

        // reads the code attributes
        int[] tanns = null; // start index of each visible type annotation
        int[] itanns = null; // start index of each invisible type annotation
        int tann = 0; // current index in tanns array
        int itann = 0; // current index in itanns array
        int ntoff = -1; // next visible type annotation code offset
        int nitoff = -1; // next invisible type annotation code offset
        int varTable = 0;
        int varTypeTable = 0;
        boolean zip = true;
        boolean unzip = (context.flags & EXPAND_FRAMES) != 0;
        int stackMap = 0;
        int stackMapSize = 0;
        int frameCount = 0;
        Context frame = null;
        Attribute attributes = null;

        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readUTF8(u + 2, c);
            if ("LocalVariableTable".equals(attrName)) {
                if ((context.flags & SKIP_DEBUG) == 0) {
                    varTable = u + 8;
                    for (int j = readUnsignedShort(u + 8), v = u; j > 0; --j) {
                        int label = readUnsignedShort(v + 10);
                        if (labels[label] == null) {
                            readLabel(label, labels).status |= Label.DEBUG;
                        }
                        label += readUnsignedShort(v + 12);
                        if (labels[label] == null) {
                            readLabel(label, labels).status |= Label.DEBUG;
                        }
                        v += 10;
                    }
                }
            } else if ("LocalVariableTypeTable".equals(attrName)) {
                varTypeTable = u + 8;
            } else if ("LineNumberTable".equals(attrName)) {
                if ((context.flags & SKIP_DEBUG) == 0) {
                    for (int j = readUnsignedShort(u + 8), v = u; j > 0; --j) {
                        int label = readUnsignedShort(v + 10);
                        if (labels[label] == null) {
                            readLabel(label, labels).status |= Label.DEBUG;
                        }
                        Label l = labels[label];
                        while (l.line > 0) {
                            if (l.next == null) {
                                l.next = new Label();
                            }
                            l = l.next;
                        }
                        l.line = readUnsignedShort(v + 12);
                        v += 4;
                    }
                }
            } else if (ANNOTATIONS
                    && "RuntimeVisibleTypeAnnotations".equals(attrName)) {
                tanns = readTypeAnnotations(mv, context, u + 8, true);
                ntoff = tanns.length == 0 || readByte(tanns[0]) < 0x43 ? -1
                        : readUnsignedShort(tanns[0] + 1);
            } else if (ANNOTATIONS
                    && "RuntimeInvisibleTypeAnnotations".equals(attrName)) {
                itanns = readTypeAnnotations(mv, context, u + 8, false);
                nitoff = itanns.length == 0 || readByte(itanns[0]) < 0x43 ? -1
                        : readUnsignedShort(itanns[0] + 1);
            } else if (FRAMES && "StackMapTable".equals(attrName)) {
                if ((context.flags & SKIP_FRAMES) == 0) {
                    stackMap = u + 10;
                    stackMapSize = readInt(u + 4);
                    frameCount = readUnsignedShort(u + 8);
                }
                /*
                 * 这里不提取与属性内容对应的标签.
                 * 这将需要完整解析属性, 这需要在第二阶段重复 (见下文).
                 * 而是一次一帧地读取属性的内容 (i.e. 在访问帧之后, 读取下一帧),
                 * 它包含的标签也一次一帧地提取.
                 * 由于帧的排序, 只有"one frame lookahead"不是问题,
                 * i.e. 不可能看到小于当前insn偏移, 并且不存在Label的偏移量.
                 */
                /*
                 * 对于UNINITIALIZED类型的偏移, 情况并非如此.
                 * 通过解析堆栈映射表, 而不进行完全解码来解决这个问题 (见下文).
                 */
            } else if (FRAMES && "StackMap".equals(attrName)) {
                if ((context.flags & SKIP_FRAMES) == 0) {
                    zip = false;
                    stackMap = u + 10;
                    stackMapSize = readInt(u + 4);
                    frameCount = readUnsignedShort(u + 8);
                }
                /*
                 * IMPORTANT! 这里假设帧是有序的, 就像在StackMapTable属性中一样, 尽管属性格式不能保证这一点.
                 */
            } else {
                for (int j = 0; j < context.attrs.length; ++j) {
                    if (context.attrs[j].type.equals(attrName)) {
                        Attribute attr = context.attrs[j].read(this, u + 8,
                                readInt(u + 4), c, codeStart - 8, labels);
                        if (attr != null) {
                            attr.next = attributes;
                            attributes = attr;
                        }
                    }
                }
            }
            u += 6 + readInt(u + 4);
        }
        u += 2;

        // 生成第一个 (隐式) 堆栈映射帧
        if (FRAMES && stackMap != 0) {
            /*
             * 对于第一个显式帧, 偏移量不是 offset_delta + 1, 而只是 offset_delta;
             * 将隐式帧偏移设置为 -1, 允许在所有情况下使用"offset_delta + 1"规则
             */
            frame = context;
            frame.offset = -1;
            frame.mode = 0;
            frame.localCount = 0;
            frame.localDiff = 0;
            frame.stackCount = 0;
            frame.local = new Object[maxLocals];
            frame.stack = new Object[maxStack];
            if (unzip) {
                getImplicitFrame(context);
            }
            /*
             * 查找UNINITIALIZED帧类型的标签.
             * 不是解码堆栈映射表的每个元素, 而是查找"看起来像"UNINITIALIZED类型的3个连续字节
             * (标签8, 代码边界内的偏移量, 此偏移量处的NEW指令).
             * 可能会发现误报 (i.e. 不是真正的 UNINITIALIZED 类型), 但这应该是罕见的, 唯一的后果就是创建一个不需要的标签.
             * 这比为每条NEW指令创建标签更好, 并且比完全解码整个堆栈映射表更快.
             */
            for (int i = stackMap; i < stackMap + stackMapSize - 2; ++i) {
                if (b[i] == 8) { // UNINITIALIZED FRAME TYPE
                    int v = readUnsignedShort(i + 1);
                    if (v >= 0 && v < codeLength) {
                        if ((b[codeStart + v] & 0xFF) == Opcodes.NEW) {
                            readLabel(v, labels);
                        }
                    }
                }
            }
        }
        if ((context.flags & EXPAND_ASM_INSNS) != 0
            && (context.flags & EXPAND_FRAMES) != 0) {
            // 扩展ASM伪指令可以引入F_INSERT帧, 即使该方法当前没有任何帧.
            // 这些插入的帧也必须通过逐个模拟字节码指令的效果来计算, 从第一个和最后一个现有帧 (或隐含的第一个)开始.
            // 最后, 由于MethodWriter计算它的方式(使用 compute = INSERTED_FRAMES 选项), MethodWriter需要在访问第一条指令之前知道maxLocals.
            // 由于所有这些原因, 在这种情况下我们总是访问隐式的第一帧 (仅传递maxLocals - 其余的可以在MethodWriter中计算).
            mv.visitFrame(Opcodes.F_NEW, maxLocals, null, 0, null);
        }

        // 访问指令
        int opcodeDelta = (context.flags & EXPAND_ASM_INSNS) == 0 ? -33 : 0;
        boolean insertFrame = false;
        u = codeStart;
        while (u < codeEnd) {
            int offset = u - codeStart;

            // 访问此偏移的标签和行号
            Label l = labels[offset];
            if (l != null) {
                Label next = l.next;
                l.next = null;
                mv.visitLabel(l);
                if ((context.flags & SKIP_DEBUG) == 0 && l.line > 0) {
                    mv.visitLineNumber(l.line, l);
                    while (next != null) {
                        mv.visitLineNumber(next.line, l);
                        next = next.next;
                    }
                }
            }

            // 访问此偏移的帧
            while (FRAMES && frame != null
                    && (frame.offset == offset || frame.offset == -1)) {
                // 如果有一个这个偏移的帧, 让访问器访问它, 并读取下一个帧.
                if (frame.offset != -1) {
                    if (!zip || unzip) {
                        mv.visitFrame(Opcodes.F_NEW, frame.localCount,
                                frame.local, frame.stackCount, frame.stack);
                    } else {
                        mv.visitFrame(frame.mode, frame.localDiff, frame.local,
                                frame.stackCount, frame.stack);
                    }
                    // 如果已存在此偏移的帧, 则无需插入新的帧.
                    insertFrame = false;
                }
                if (frameCount > 0) {
                    stackMap = readFrame(stackMap, zip, unzip, frame);
                    --frameCount;
                } else {
                    frame = null;
                }
            }
            // 如果在上一次迭代期间通过将insertFrame设置为true来请求, 则为此偏移量插入一个帧.
            // 实际的帧内容将在MethodWriter中计算.
            if (FRAMES && insertFrame) {
                mv.visitFrame(ClassWriter.F_INSERT, 0, null, 0, null);
                insertFrame = false;
            }

            // 访问此偏移处的指令
            int opcode = b[u] & 0xFF;
            switch (ClassWriter.TYPE[opcode]) {
            case ClassWriter.NOARG_INSN:
                mv.visitInsn(opcode);
                u += 1;
                break;
            case ClassWriter.IMPLVAR_INSN:
                if (opcode > Opcodes.ISTORE) {
                    opcode -= 59; // ISTORE_0
                    mv.visitVarInsn(Opcodes.ISTORE + (opcode >> 2),
                            opcode & 0x3);
                } else {
                    opcode -= 26; // ILOAD_0
                    mv.visitVarInsn(Opcodes.ILOAD + (opcode >> 2), opcode & 0x3);
                }
                u += 1;
                break;
            case ClassWriter.LABEL_INSN:
                mv.visitJumpInsn(opcode, labels[offset + readShort(u + 1)]);
                u += 3;
                break;
            case ClassWriter.LABELW_INSN:
                mv.visitJumpInsn(opcode + opcodeDelta, labels[offset
                        + readInt(u + 1)]);
                u += 5;
                break;
            case ClassWriter.ASM_LABEL_INSN: {
                // 更改临时操作码202到217 (包括), 218 和 219 到 IFEQ ... JSR (包括), IFNULL 和 IFNONNULL
                opcode = opcode < 218 ? opcode - 49 : opcode - 20;
                Label target = labels[offset + readUnsignedShort(u + 1)];
                // 将GOTO替换为GOTO_W, JSR替换为JSR_W, IFxxx <l> 替换为 IFNOTxxx <L> GOTO_W <l> L:...,
                // 其中IFNOTxxx 是 IFxxx 的"相反"操作码(i.e., IFNE 对于 IFEQ), 其中<L> 在GOTO_W之后指定指令.
                if (opcode == Opcodes.GOTO || opcode == Opcodes.JSR) {
                    mv.visitJumpInsn(opcode + 33, target);
                } else {
                    opcode = opcode <= 166 ? ((opcode + 1) ^ 1) - 1
                            : opcode ^ 1;
                    Label endif = readLabel(offset + 3, labels);
                    mv.visitJumpInsn(opcode, endif);
                    mv.visitJumpInsn(200, target); // GOTO_W
                    // endif在GOTO_W之后指定指令, 并作为下一条指令的一部分被访问.
                    // 由于它是跳跃目标, 需要在这里插入一个帧.
                    insertFrame = true;
                }
                u += 3;
                break;
            }
            case ClassWriter.ASM_LABELW_INSN: {
                // 用真实的GOTO_W指令替换伪GOTO_W指令.
                mv.visitJumpInsn(200, labels[offset + readInt(u + 1)]);
                // 后面的指令是跳转目标(因为伪GOTO_W 用于模式IFNOTxxx <L> GOTO_W <l> L:..., 参阅MethodWriter), 因此需要在此处插入一个帧.
                insertFrame = true;
                u += 5;
                break;
            }
            case ClassWriter.WIDE_INSN:
                opcode = b[u + 1] & 0xFF;
                if (opcode == Opcodes.IINC) {
                    mv.visitIincInsn(readUnsignedShort(u + 2), readShort(u + 4));
                    u += 6;
                } else {
                    mv.visitVarInsn(opcode, readUnsignedShort(u + 2));
                    u += 4;
                }
                break;
            case ClassWriter.TABL_INSN: {
                // 跳过0到3个填充字节
                u = u + 4 - (offset & 3);
                // 读取指令
                int label = offset + readInt(u);
                int min = readInt(u + 4);
                int max = readInt(u + 8);
                Label[] table = new Label[max - min + 1];
                u += 12;
                for (int i = 0; i < table.length; ++i) {
                    table[i] = labels[offset + readInt(u)];
                    u += 4;
                }
                mv.visitTableSwitchInsn(min, max, labels[label], table);
                break;
            }
            case ClassWriter.LOOK_INSN: {
                // 跳过0到3个填充字节
                u = u + 4 - (offset & 3);
                // 读取指令
                int label = offset + readInt(u);
                int len = readInt(u + 4);
                int[] keys = new int[len];
                Label[] values = new Label[len];
                u += 8;
                for (int i = 0; i < len; ++i) {
                    keys[i] = readInt(u);
                    values[i] = labels[offset + readInt(u + 4)];
                    u += 8;
                }
                mv.visitLookupSwitchInsn(labels[label], keys, values);
                break;
            }
            case ClassWriter.VAR_INSN:
                mv.visitVarInsn(opcode, b[u + 1] & 0xFF);
                u += 2;
                break;
            case ClassWriter.SBYTE_INSN:
                mv.visitIntInsn(opcode, b[u + 1]);
                u += 2;
                break;
            case ClassWriter.SHORT_INSN:
                mv.visitIntInsn(opcode, readShort(u + 1));
                u += 3;
                break;
            case ClassWriter.LDC_INSN:
                mv.visitLdcInsn(readConst(b[u + 1] & 0xFF, c));
                u += 2;
                break;
            case ClassWriter.LDCW_INSN:
                mv.visitLdcInsn(readConst(readUnsignedShort(u + 1), c));
                u += 3;
                break;
            case ClassWriter.FIELDORMETH_INSN:
            case ClassWriter.ITFMETH_INSN: {
                int cpIndex = items[readUnsignedShort(u + 1)];
                boolean itf = b[cpIndex - 1] == ClassWriter.IMETH;
                String iowner = readClass(cpIndex, c);
                cpIndex = items[readUnsignedShort(cpIndex + 2)];
                String iname = readUTF8(cpIndex, c);
                String idesc = readUTF8(cpIndex + 2, c);
                if (opcode < Opcodes.INVOKEVIRTUAL) {
                    mv.visitFieldInsn(opcode, iowner, iname, idesc);
                } else {
                    mv.visitMethodInsn(opcode, iowner, iname, idesc, itf);
                }
                if (opcode == Opcodes.INVOKEINTERFACE) {
                    u += 5;
                } else {
                    u += 3;
                }
                break;
            }
            case ClassWriter.INDYMETH_INSN: {
                int cpIndex = items[readUnsignedShort(u + 1)];
                int bsmIndex = context.bootstrapMethods[readUnsignedShort(cpIndex)];
                Handle bsm = (Handle) readConst(readUnsignedShort(bsmIndex), c);
                int bsmArgCount = readUnsignedShort(bsmIndex + 2);
                Object[] bsmArgs = new Object[bsmArgCount];
                bsmIndex += 4;
                for (int i = 0; i < bsmArgCount; i++) {
                    bsmArgs[i] = readConst(readUnsignedShort(bsmIndex), c);
                    bsmIndex += 2;
                }
                cpIndex = items[readUnsignedShort(cpIndex + 2)];
                String iname = readUTF8(cpIndex, c);
                String idesc = readUTF8(cpIndex + 2, c);
                mv.visitInvokeDynamicInsn(iname, idesc, bsm, bsmArgs);
                u += 5;
                break;
            }
            case ClassWriter.TYPE_INSN:
                mv.visitTypeInsn(opcode, readClass(u + 1, c));
                u += 3;
                break;
            case ClassWriter.IINC_INSN:
                mv.visitIincInsn(b[u + 1] & 0xFF, b[u + 2]);
                u += 3;
                break;
            // case MANA_INSN:
            default:
                mv.visitMultiANewArrayInsn(readClass(u + 1, c), b[u + 3] & 0xFF);
                u += 4;
                break;
            }

            // 访问指令注解
            while (tanns != null && tann < tanns.length && ntoff <= offset) {
                if (ntoff == offset) {
                    int v = readAnnotationTarget(context, tanns[tann]);
                    readAnnotationValues(v + 2, c, true,
                            mv.visitInsnAnnotation(context.typeRef,
                                    context.typePath, readUTF8(v, c), true));
                }
                ntoff = ++tann >= tanns.length || readByte(tanns[tann]) < 0x43 ? -1
                        : readUnsignedShort(tanns[tann] + 1);
            }
            while (itanns != null && itann < itanns.length && nitoff <= offset) {
                if (nitoff == offset) {
                    int v = readAnnotationTarget(context, itanns[itann]);
                    readAnnotationValues(v + 2, c, true,
                            mv.visitInsnAnnotation(context.typeRef,
                                    context.typePath, readUTF8(v, c), false));
                }
                nitoff = ++itann >= itanns.length
                        || readByte(itanns[itann]) < 0x43 ? -1
                        : readUnsignedShort(itanns[itann] + 1);
            }
        }
        if (labels[codeLength] != null) {
            mv.visitLabel(labels[codeLength]);
        }

        // 访问局部变量表
        if ((context.flags & SKIP_DEBUG) == 0 && varTable != 0) {
            int[] typeTable = null;
            if (varTypeTable != 0) {
                u = varTypeTable + 2;
                typeTable = new int[readUnsignedShort(varTypeTable) * 3];
                for (int i = typeTable.length; i > 0;) {
                    typeTable[--i] = u + 6; // signature
                    typeTable[--i] = readUnsignedShort(u + 8); // index
                    typeTable[--i] = readUnsignedShort(u); // start
                    u += 10;
                }
            }
            u = varTable + 2;
            for (int i = readUnsignedShort(varTable); i > 0; --i) {
                int start = readUnsignedShort(u);
                int length = readUnsignedShort(u + 2);
                int index = readUnsignedShort(u + 8);
                String vsignature = null;
                if (typeTable != null) {
                    for (int j = 0; j < typeTable.length; j += 3) {
                        if (typeTable[j] == start && typeTable[j + 1] == index) {
                            vsignature = readUTF8(typeTable[j + 2], c);
                            break;
                        }
                    }
                }
                mv.visitLocalVariable(readUTF8(u + 4, c), readUTF8(u + 6, c),
                        vsignature, labels[start], labels[start + length],
                        index);
                u += 10;
            }
        }

        // 访问局部变量类型注解
        if (tanns != null) {
            for (int i = 0; i < tanns.length; ++i) {
                if ((readByte(tanns[i]) >> 1) == (0x40 >> 1)) {
                    int v = readAnnotationTarget(context, tanns[i]);
                    v = readAnnotationValues(v + 2, c, true,
                            mv.visitLocalVariableAnnotation(context.typeRef,
                                    context.typePath, context.start,
                                    context.end, context.index, readUTF8(v, c),
                                    true));
                }
            }
        }
        if (itanns != null) {
            for (int i = 0; i < itanns.length; ++i) {
                if ((readByte(itanns[i]) >> 1) == (0x40 >> 1)) {
                    int v = readAnnotationTarget(context, itanns[i]);
                    v = readAnnotationValues(v + 2, c, true,
                            mv.visitLocalVariableAnnotation(context.typeRef,
                                    context.typePath, context.start,
                                    context.end, context.index, readUTF8(v, c),
                                    false));
                }
            }
        }

        // 访问代码属性
        while (attributes != null) {
            Attribute attr = attributes.next;
            attributes.next = null;
            mv.visitAttribute(attributes);
            attributes = attr;
        }

        // 访问最大堆栈和最大本地值
        mv.visitMaxs(maxStack, maxLocals);
    }

    /**
     * 解析类型注解表以查找标签, 并访问try catch块注解.
     * 
     * @param u
     *            类型注解表的起始偏移量.
     * @param mv
     *            用于访问try catch块注解的方法访问器.
     * @param context
     *            有关正在解析的类的信息.
     * @param visible
     *            如果要解析的类型注解表包含运行时可见注解.
     * 
     * @return 解析的表中每种类型注解的起始偏移量.
     */
    private int[] readTypeAnnotations(final MethodVisitor mv,
            final Context context, int u, boolean visible) {
        char[] c = context.buffer;
        int[] offsets = new int[readUnsignedShort(u)];
        u += 2;
        for (int i = 0; i < offsets.length; ++i) {
            offsets[i] = u;
            int target = readInt(u);
            switch (target >>> 24) {
            case 0x00: // CLASS_TYPE_PARAMETER
            case 0x01: // METHOD_TYPE_PARAMETER
            case 0x16: // METHOD_FORMAL_PARAMETER
                u += 2;
                break;
            case 0x13: // FIELD
            case 0x14: // METHOD_RETURN
            case 0x15: // METHOD_RECEIVER
                u += 1;
                break;
            case 0x40: // LOCAL_VARIABLE
            case 0x41: // RESOURCE_VARIABLE
                for (int j = readUnsignedShort(u + 1); j > 0; --j) {
                    int start = readUnsignedShort(u + 3);
                    int length = readUnsignedShort(u + 5);
                    readLabel(start, context.labels);
                    readLabel(start + length, context.labels);
                    u += 6;
                }
                u += 3;
                break;
            case 0x47: // CAST
            case 0x48: // CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
            case 0x49: // METHOD_INVOCATION_TYPE_ARGUMENT
            case 0x4A: // CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
            case 0x4B: // METHOD_REFERENCE_TYPE_ARGUMENT
                u += 4;
                break;
            // case 0x10: // CLASS_EXTENDS
            // case 0x11: // CLASS_TYPE_PARAMETER_BOUND
            // case 0x12: // METHOD_TYPE_PARAMETER_BOUND
            // case 0x17: // THROWS
            // case 0x42: // EXCEPTION_PARAMETER
            // case 0x43: // INSTANCEOF
            // case 0x44: // NEW
            // case 0x45: // CONSTRUCTOR_REFERENCE
            // case 0x46: // METHOD_REFERENCE
            default:
                u += 3;
                break;
            }
            int pathLength = readByte(u);
            if ((target >>> 24) == 0x42) {
                TypePath path = pathLength == 0 ? null : new TypePath(b, u);
                u += 1 + 2 * pathLength;
                u = readAnnotationValues(u + 2, c, true,
                        mv.visitTryCatchAnnotation(target, path,
                                readUTF8(u, c), visible));
            } else {
                u = readAnnotationValues(u + 3 + 2 * pathLength, c, true, null);
            }
        }
        return offsets;
    }

    /**
     * 解析类型注解的header 以提取其target_type 和 target_path (结果存储在给定的上下文中),
     * 并返回type_annotation结构其余部分的起始偏移量
     * (i.e. type_index字段的偏移量, 后跟num_element_value_pairs, 然后是 name,value 对).
     * 
     * @param context
     *            有关正在解析的类的信息. 这是必须存储提取的target_type和target_path的地方.
     * @param u
     *            type_annotation结构的起始偏移量.
     * 
     * @return type_annotation结构的其余部分的起始偏移量.
     */
    private int readAnnotationTarget(final Context context, int u) {
        int target = readInt(u);
        switch (target >>> 24) {
        case 0x00: // CLASS_TYPE_PARAMETER
        case 0x01: // METHOD_TYPE_PARAMETER
        case 0x16: // METHOD_FORMAL_PARAMETER
            target &= 0xFFFF0000;
            u += 2;
            break;
        case 0x13: // FIELD
        case 0x14: // METHOD_RETURN
        case 0x15: // METHOD_RECEIVER
            target &= 0xFF000000;
            u += 1;
            break;
        case 0x40: // LOCAL_VARIABLE
        case 0x41: { // RESOURCE_VARIABLE
            target &= 0xFF000000;
            int n = readUnsignedShort(u + 1);
            context.start = new Label[n];
            context.end = new Label[n];
            context.index = new int[n];
            u += 3;
            for (int i = 0; i < n; ++i) {
                int start = readUnsignedShort(u);
                int length = readUnsignedShort(u + 2);
                context.start[i] = readLabel(start, context.labels);
                context.end[i] = readLabel(start + length, context.labels);
                context.index[i] = readUnsignedShort(u + 4);
                u += 6;
            }
            break;
        }
        case 0x47: // CAST
        case 0x48: // CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
        case 0x49: // METHOD_INVOCATION_TYPE_ARGUMENT
        case 0x4A: // CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
        case 0x4B: // METHOD_REFERENCE_TYPE_ARGUMENT
            target &= 0xFF0000FF;
            u += 4;
            break;
        // case 0x10: // CLASS_EXTENDS
        // case 0x11: // CLASS_TYPE_PARAMETER_BOUND
        // case 0x12: // METHOD_TYPE_PARAMETER_BOUND
        // case 0x17: // THROWS
        // case 0x42: // EXCEPTION_PARAMETER
        // case 0x43: // INSTANCEOF
        // case 0x44: // NEW
        // case 0x45: // CONSTRUCTOR_REFERENCE
        // case 0x46: // METHOD_REFERENCE
        default:
            target &= (target >>> 24) < 0x43 ? 0xFFFFFF00 : 0xFF000000;
            u += 3;
            break;
        }
        int pathLength = readByte(u);
        context.typeRef = target;
        context.typePath = pathLength == 0 ? null : new TypePath(b, u);
        return u + 1 + 2 * pathLength;
    }

    /**
     * 读取参数注解并使给定的访问器访问它们.
     * 
     * @param mv
     *            必须访问注解的访问器.
     * @param context
     *            有关正在解析的类的信息.
     * @param v
     *            要读取的注解的{@link #b b}中的起始偏移量.
     * @param visible
     *            <tt>true</tt>如果要读取的注解在运行时可见.
     */
    private void readParameterAnnotations(final MethodVisitor mv,
            final Context context, int v, final boolean visible) {
        int i;
        int n = b[v++] & 0xFF;
        // 解决javac中的错误
        // (javac编译器生成一个参数注释数组, 其大小等于Java源文件中的参数数,
        // 它应该生成一个数组, 其大小等于方法描述符中的参数数量 - 其中包括编译器添加的合成参数).
        // 这种解决方法假设合成参数是第一个.
        int synthetics = Type.getArgumentTypes(context.desc).length - n;
        AnnotationVisitor av;
        for (i = 0; i < synthetics; ++i) {
            // 虚拟注解, 用于检测MethodWriter中的合成参数
            av = mv.visitParameterAnnotation(i, "Ljava/lang/Synthetic;", false);
            if (av != null) {
                av.visitEnd();
            }
        }
        char[] c = context.buffer;
        for (; i < n + synthetics; ++i) {
            int j = readUnsignedShort(v);
            v += 2;
            for (; j > 0; --j) {
                av = mv.visitParameterAnnotation(i, readUTF8(v, c), visible);
                v = readAnnotationValues(v + 2, c, true, av);
            }
        }
    }

    /**
     * 读取注解的值并使给定的访问器访问它们.
     * 
     * @param v
     *            要读取的值的{@link #b b}中的起始偏移量
     *            (包括给出值数的unsigned short).
     * @param buf
     *            缓冲区, 用于调用{@link #readUTF8 readUTF8}, {@link #readClass(int,char[]) readClass}或{@link #readConst readConst}.
     * @param named
     *            注解值是否已命名.
     * @param av
     *            必须访问值的访问器.
     * 
     * @return 注解值的结尾偏移量.
     */
    private int readAnnotationValues(int v, final char[] buf,
            final boolean named, final AnnotationVisitor av) {
        int i = readUnsignedShort(v);
        v += 2;
        if (named) {
            for (; i > 0; --i) {
                v = readAnnotationValue(v + 2, buf, readUTF8(v, buf), av);
            }
        } else {
            for (; i > 0; --i) {
                v = readAnnotationValue(v, buf, null, av);
            }
        }
        if (av != null) {
            av.visitEnd();
        }
        return v;
    }

    /**
     * 读取注解的值并使给定的访问器访问它们.
     * 
     * @param v
     *            要读取的值的{@link #b b}中的起始偏移量
     *            (<i>不包括值名称常量池索引</i>).
     * @param buf
     *            缓冲区, 用于调用{@link #readUTF8 readUTF8}, {@link #readClass(int,char[]) readClass}或{@link #readConst readConst}.
     * @param name
     *            要读取的值的名称.
     * @param av
     *            必须访问值的访问器.
     * 
     * @return 注解值的结尾偏移量.
     */
    private int readAnnotationValue(int v, final char[] buf, final String name,
            final AnnotationVisitor av) {
        int i;
        if (av == null) {
            switch (b[v] & 0xFF) {
            case 'e': // enum_const_value
                return v + 5;
            case '@': // annotation_value
                return readAnnotationValues(v + 3, buf, true, null);
            case '[': // array_value
                return readAnnotationValues(v + 1, buf, false, null);
            default:
                return v + 3;
            }
        }
        switch (b[v++] & 0xFF) {
        case 'I': // pointer to CONSTANT_Integer
        case 'J': // pointer to CONSTANT_Long
        case 'F': // pointer to CONSTANT_Float
        case 'D': // pointer to CONSTANT_Double
            av.visit(name, readConst(readUnsignedShort(v), buf));
            v += 2;
            break;
        case 'B': // pointer to CONSTANT_Byte
            av.visit(name, (byte) readInt(items[readUnsignedShort(v)]));
            v += 2;
            break;
        case 'Z': // pointer to CONSTANT_Boolean
            av.visit(name,
                    readInt(items[readUnsignedShort(v)]) == 0 ? Boolean.FALSE
                            : Boolean.TRUE);
            v += 2;
            break;
        case 'S': // pointer to CONSTANT_Short
            av.visit(name, (short) readInt(items[readUnsignedShort(v)]));
            v += 2;
            break;
        case 'C': // pointer to CONSTANT_Char
            av.visit(name, (char) readInt(items[readUnsignedShort(v)]));
            v += 2;
            break;
        case 's': // pointer to CONSTANT_Utf8
            av.visit(name, readUTF8(v, buf));
            v += 2;
            break;
        case 'e': // enum_const_value
            av.visitEnum(name, readUTF8(v, buf), readUTF8(v + 2, buf));
            v += 4;
            break;
        case 'c': // class_info
            av.visit(name, Type.getType(readUTF8(v, buf)));
            v += 2;
            break;
        case '@': // annotation_value
            v = readAnnotationValues(v + 2, buf, true,
                    av.visitAnnotation(name, readUTF8(v, buf)));
            break;
        case '[': // array_value
            int size = readUnsignedShort(v);
            v += 2;
            if (size == 0) {
                return readAnnotationValues(v - 2, buf, false,
                        av.visitArray(name));
            }
            switch (this.b[v++] & 0xFF) {
            case 'B':
                byte[] bv = new byte[size];
                for (i = 0; i < size; i++) {
                    bv[i] = (byte) readInt(items[readUnsignedShort(v)]);
                    v += 3;
                }
                av.visit(name, bv);
                --v;
                break;
            case 'Z':
                boolean[] zv = new boolean[size];
                for (i = 0; i < size; i++) {
                    zv[i] = readInt(items[readUnsignedShort(v)]) != 0;
                    v += 3;
                }
                av.visit(name, zv);
                --v;
                break;
            case 'S':
                short[] sv = new short[size];
                for (i = 0; i < size; i++) {
                    sv[i] = (short) readInt(items[readUnsignedShort(v)]);
                    v += 3;
                }
                av.visit(name, sv);
                --v;
                break;
            case 'C':
                char[] cv = new char[size];
                for (i = 0; i < size; i++) {
                    cv[i] = (char) readInt(items[readUnsignedShort(v)]);
                    v += 3;
                }
                av.visit(name, cv);
                --v;
                break;
            case 'I':
                int[] iv = new int[size];
                for (i = 0; i < size; i++) {
                    iv[i] = readInt(items[readUnsignedShort(v)]);
                    v += 3;
                }
                av.visit(name, iv);
                --v;
                break;
            case 'J':
                long[] lv = new long[size];
                for (i = 0; i < size; i++) {
                    lv[i] = readLong(items[readUnsignedShort(v)]);
                    v += 3;
                }
                av.visit(name, lv);
                --v;
                break;
            case 'F':
                float[] fv = new float[size];
                for (i = 0; i < size; i++) {
                    fv[i] = Float
                            .intBitsToFloat(readInt(items[readUnsignedShort(v)]));
                    v += 3;
                }
                av.visit(name, fv);
                --v;
                break;
            case 'D':
                double[] dv = new double[size];
                for (i = 0; i < size; i++) {
                    dv[i] = Double
                            .longBitsToDouble(readLong(items[readUnsignedShort(v)]));
                    v += 3;
                }
                av.visit(name, dv);
                --v;
                break;
            default:
                v = readAnnotationValues(v - 3, buf, false, av.visitArray(name));
            }
        }
        return v;
    }

    /**
     * 计算当前正在解析的方法的隐式帧(在给定的{@link Context}中定义), 并将其存储在给定的上下文中.
     * 
     * @param frame
     *            有关正在解析的类的信息.
     */
    private void getImplicitFrame(final Context frame) {
        String desc = frame.desc;
        Object[] locals = frame.local;
        int local = 0;
        if ((frame.access & Opcodes.ACC_STATIC) == 0) {
            if ("<init>".equals(frame.name)) {
                locals[local++] = Opcodes.UNINITIALIZED_THIS;
            } else {
                locals[local++] = readClass(header + 2, frame.buffer);
            }
        }
        int i = 1;
        loop: while (true) {
            int j = i;
            switch (desc.charAt(i++)) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
                locals[local++] = Opcodes.INTEGER;
                break;
            case 'F':
                locals[local++] = Opcodes.FLOAT;
                break;
            case 'J':
                locals[local++] = Opcodes.LONG;
                break;
            case 'D':
                locals[local++] = Opcodes.DOUBLE;
                break;
            case '[':
                while (desc.charAt(i) == '[') {
                    ++i;
                }
                if (desc.charAt(i) == 'L') {
                    ++i;
                    while (desc.charAt(i) != ';') {
                        ++i;
                    }
                }
                locals[local++] = desc.substring(j, ++i);
                break;
            case 'L':
                while (desc.charAt(i) != ';') {
                    ++i;
                }
                locals[local++] = desc.substring(j + 1, i++);
                break;
            default:
                break loop;
            }
        }
        frame.localCount = local;
    }

    /**
     * 读取堆栈映射帧, 并将结果存储在给定的{@link Context}对象中.
     * 
     * @param stackMap
     *            类文件中堆栈映射帧的起始偏移量.
     * @param zip
     *            stackMap的堆栈映射帧是否被压缩.
     * @param unzip
     *            如果堆栈映射帧必须是未压缩的.
     * @param frame
     *            必须存储已解析的堆栈映射帧的位置.
     * 
     * @return 解析帧后的第一个字节的偏移量.
     */
    private int readFrame(int stackMap, boolean zip, boolean unzip,
            Context frame) {
        char[] c = frame.buffer;
        Label[] labels = frame.labels;
        int tag;
        int delta;
        if (zip) {
            tag = b[stackMap++] & 0xFF;
        } else {
            tag = MethodWriter.FULL_FRAME;
            frame.offset = -1;
        }
        frame.localDiff = 0;
        if (tag < MethodWriter.SAME_LOCALS_1_STACK_ITEM_FRAME) {
            delta = tag;
            frame.mode = Opcodes.F_SAME;
            frame.stackCount = 0;
        } else if (tag < MethodWriter.RESERVED) {
            delta = tag - MethodWriter.SAME_LOCALS_1_STACK_ITEM_FRAME;
            stackMap = readFrameType(frame.stack, 0, stackMap, c, labels);
            frame.mode = Opcodes.F_SAME1;
            frame.stackCount = 1;
        } else {
            delta = readUnsignedShort(stackMap);
            stackMap += 2;
            if (tag == MethodWriter.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
                stackMap = readFrameType(frame.stack, 0, stackMap, c, labels);
                frame.mode = Opcodes.F_SAME1;
                frame.stackCount = 1;
            } else if (tag >= MethodWriter.CHOP_FRAME
                    && tag < MethodWriter.SAME_FRAME_EXTENDED) {
                frame.mode = Opcodes.F_CHOP;
                frame.localDiff = MethodWriter.SAME_FRAME_EXTENDED - tag;
                frame.localCount -= frame.localDiff;
                frame.stackCount = 0;
            } else if (tag == MethodWriter.SAME_FRAME_EXTENDED) {
                frame.mode = Opcodes.F_SAME;
                frame.stackCount = 0;
            } else if (tag < MethodWriter.FULL_FRAME) {
                int local = unzip ? frame.localCount : 0;
                for (int i = tag - MethodWriter.SAME_FRAME_EXTENDED; i > 0; i--) {
                    stackMap = readFrameType(frame.local, local++, stackMap, c,
                            labels);
                }
                frame.mode = Opcodes.F_APPEND;
                frame.localDiff = tag - MethodWriter.SAME_FRAME_EXTENDED;
                frame.localCount += frame.localDiff;
                frame.stackCount = 0;
            } else { // if (tag == FULL_FRAME) {
                frame.mode = Opcodes.F_FULL;
                int n = readUnsignedShort(stackMap);
                stackMap += 2;
                frame.localDiff = n;
                frame.localCount = n;
                for (int local = 0; n > 0; n--) {
                    stackMap = readFrameType(frame.local, local++, stackMap, c,
                            labels);
                }
                n = readUnsignedShort(stackMap);
                stackMap += 2;
                frame.stackCount = n;
                for (int stack = 0; n > 0; n--) {
                    stackMap = readFrameType(frame.stack, stack++, stackMap, c,
                            labels);
                }
            }
        }
        frame.offset += delta + 1;
        readLabel(frame.offset, labels);
        return stackMap;
    }

    /**
     * 读取堆栈映射帧类型, 并将其存储在给定数组中的给定索引处.
     * 
     * @param frame
     *            必须存储解析的类型的数组.
     * @param index
     *            'frame'中必须存储的已解析类型的索引.
     * @param v
     *            要读取的堆栈映射帧类型的起始偏移量.
     * @param buf
     *            读取字符串的缓冲区.
     * @param labels
     *            当前正在解析的方法的标签, 由其偏移量索引.
     *            如果解析的类型是未初始化类型, 则相应的NEW指令的新标签将存储在此数组中, 如果它尚不存在.
     * 
     * @return 解析类型后第一个字节的偏移量.
     */
    private int readFrameType(final Object[] frame, final int index, int v,
            final char[] buf, final Label[] labels) {
        int type = b[v++] & 0xFF;
        switch (type) {
        case 0:
            frame[index] = Opcodes.TOP;
            break;
        case 1:
            frame[index] = Opcodes.INTEGER;
            break;
        case 2:
            frame[index] = Opcodes.FLOAT;
            break;
        case 3:
            frame[index] = Opcodes.DOUBLE;
            break;
        case 4:
            frame[index] = Opcodes.LONG;
            break;
        case 5:
            frame[index] = Opcodes.NULL;
            break;
        case 6:
            frame[index] = Opcodes.UNINITIALIZED_THIS;
            break;
        case 7: // Object
            frame[index] = readClass(v, buf);
            v += 2;
            break;
        default: // Uninitialized
            frame[index] = readLabel(readUnsignedShort(v), labels);
            v += 2;
        }
        return v;
    }

    /**
     * 返回与给定偏移量对应的标签.
     * 如果尚未创建给定偏移量, 则此方法的默认实现会为其创建标签.
     * 
     * @param offset
     *            方法中的字节码偏移量.
     * @param labels
     *            已创建的标签, 按其偏移量索引.
     *            如果偏移量的标签已存在, 则此方法不能创建新标签.
     *            否则, 它必须将新标签存储在此数组中.
     * 
     * @return 非null Label, 必须等于labels[offset].
     */
    protected Label readLabel(int offset, Label[] labels) {
        // SPRING PATCH: 宽松地处理偏移量不匹配
        if (offset >= labels.length) {
            return new Label();
        }
        // END OF PATCH
        if (labels[offset] == null) {
            labels[offset] = new Label();
        }
        return labels[offset];
    }

    /**
     * 返回此类的attribute_info结构的起始索引.
     * 
     * @return 此类的attribute_info结构的起始索引.
     */
    private int getAttributes() {
        // skips the header
        int u = header + 8 + readUnsignedShort(header + 6) * 2;
        // skips fields and methods
        for (int i = readUnsignedShort(u); i > 0; --i) {
            for (int j = readUnsignedShort(u + 8); j > 0; --j) {
                u += 6 + readInt(u + 12);
            }
            u += 8;
        }
        u += 2;
        for (int i = readUnsignedShort(u); i > 0; --i) {
            for (int j = readUnsignedShort(u + 8); j > 0; --j) {
                u += 6 + readInt(u + 12);
            }
            u += 8;
        }
        // attribute_info结构在方法之后开始
        return u + 2;
    }

    /**
     * 读取{@link #b b}中的属性.
     * 
     * @param attrs
     *            在访问类时必须解析的属性原型.
     *            忽略任何类型不等于原型类型的属性 (i.e. 返回空的{@link Attribute}实例).
     * @param type
     *            属性的类型.
     * @param off
     *            {@link #b b}中属性内容的第一个字节的索引.
     *            这里不考虑6个属性header字节, 包含属性的类型和长度 (它们已被读取).
     * @param len
     *            属性内容的长度.
     * @param buf
     *            缓冲区, 用于调用{@link #readUTF8 readUTF8}, {@link #readClass(int,char[]) readClass}或{@link #readConst readConst}.
     * @param codeOff
     *            {@link #b b}中代码属性内容的第一个字节的索引, 或 -1 如果要读取的属性不是代码属性.
     *            这里不考虑6个属性header字节, 包含属性的类型和长度.
     * @param labels
     *            方法代码的标签, 或<tt>null</tt>如果要读取的属性不是代码属性.
     * 
     * @return 已读取的属性, 或<tt>null</tt>以跳过此属性.
     */
    private Attribute readAttribute(final Attribute[] attrs, final String type,
            final int off, final int len, final char[] buf, final int codeOff,
            final Label[] labels) {
        for (int i = 0; i < attrs.length; ++i) {
            if (attrs[i].type.equals(type)) {
                return attrs[i].read(this, off, len, buf, codeOff, labels);
            }
        }
        return new Attribute(type).read(this, off, len, null, -1, null);
    }

    // ------------------------------------------------------------------------
    // Utility methods: low level parsing
    // ------------------------------------------------------------------------

    /**
     * 返回{@link #b b}中常量池项的数量.
     * 
     * @return 常量池项的数量.
     */
    public int getItemCount() {
        return items.length;
    }

    /**
     * 返回{@link #b b}中常量池项的起始索引, 加一.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param item
     *            常量池项的索引.
     * 
     * @return {@link #b b}中常量池项的起始索引, 加一.
     */
    public int getItem(final int item) {
        return items[item];
    }

    /**
     * 返回类的常量池中包含的字符串的最大长度.
     * 
     * @return 字符串的最大长度.
     */
    public int getMaxStringLength() {
        return maxStringLength;
    }

    /**
     * 读取{@link #b b}中的字节值.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param index
     *            要在{@link #b b}中读取的值的起始索引.
     * 
     * @return 读取的值.
     */
    public int readByte(final int index) {
        return b[index] & 0xFF;
    }

    /**
     * 读取{@link #b b}中unsigned short值.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param index
     *            要在{@link #b b}中读取的值的起始索引.
     * 
     * @return 读取的值.
     */
    public int readUnsignedShort(final int index) {
        byte[] b = this.b;
        return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
    }

    /**
     * 读取{@link #b b}中signed short值.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param index
     *            要在{@link #b b}中读取的值的起始索引.
     * 
     * @return 读取的值.
     */
    public short readShort(final int index) {
        byte[] b = this.b;
        return (short) (((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF));
    }

    /**
     * 读取{@link #b b}中的signed int值.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param index
     *            要在{@link #b b}中读取的值的起始索引.
     * 
     * @return 读取的值.
     */
    public int readInt(final int index) {
        byte[] b = this.b;
        return ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16)
                | ((b[index + 2] & 0xFF) << 8) | (b[index + 3] & 0xFF);
    }

    /**
     * 读取{@link #b b}中signed long值.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param index
     *            要在{@link #b b}中读取的值的起始索引.
     * 
     * @return 读取的值.
     */
    public long readLong(final int index) {
        long l1 = readInt(index);
        long l0 = readInt(index + 4) & 0xFFFFFFFFL;
        return (l1 << 32) | l0;
    }

    /**
     * 读取{@link #b b}中的UTF8字符串常量池项.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param index
     *            {@link #b b}中unsigned short值的起始索引, 其值是UTF8常量池项的索引.
     * @param buf
     *            用于读取条目的缓冲区. 该缓冲区必须足够大. 它不会自动调整大小.
     * 
     * @return 与指定的UTF8项对应的String.
     */
    public String readUTF8(int index, final char[] buf) {
        int item = readUnsignedShort(index);
        if (index == 0 || item == 0) {
            return null;
        }
        String s = strings[item];
        if (s != null) {
            return s;
        }
        index = items[item];
        return strings[item] = readUTF(index + 2, readUnsignedShort(index), buf);
    }

    /**
     * 读取{@link #b b}中的UTF8字符串.
     * 
     * @param index
     *            要读取的UTF8字符串的起始偏移量.
     * @param utfLen
     *            要读取的UTF8字符串的长度.
     * @param buf
     *            用于读取条目的缓冲区. 该缓冲区必须足够大. 它不会自动调整大小.
     * 
     * @return 与指定的UTF8字符串对应的String.
     */
    private String readUTF(int index, final int utfLen, final char[] buf) {
        int endIndex = index + utfLen;
        byte[] b = this.b;
        int strLen = 0;
        int c;
        int st = 0;
        char cc = 0;
        while (index < endIndex) {
            c = b[index++];
            switch (st) {
            case 0:
                c = c & 0xFF;
                if (c < 0x80) { // 0xxxxxxx
                    buf[strLen++] = (char) c;
                } else if (c < 0xE0 && c > 0xBF) { // 110x xxxx 10xx xxxx
                    cc = (char) (c & 0x1F);
                    st = 1;
                } else { // 1110 xxxx 10xx xxxx 10xx xxxx
                    cc = (char) (c & 0x0F);
                    st = 2;
                }
                break;

            case 1: // byte 2 of 2-byte char or byte 3 of 3-byte char
                buf[strLen++] = (char) ((cc << 6) | (c & 0x3F));
                st = 0;
                break;

            case 2: // byte 2 of 3-byte char
                cc = (char) ((cc << 6) | (c & 0x3F));
                st = 1;
                break;
            }
        }
        return new String(buf, 0, strLen);
    }

    /**
     * 读取一个字符串常量项 (CONSTANT_Class, CONSTANT_String, CONSTANT_MethodType, CONSTANT_Module or CONSTANT_Package
     * 
     * @param index
     * @param buf
     * 
     * @return
     */
    private String readStringish(final int index, final char[] buf) {
        // 计算b中条目的起始索引, 并读取由该条目的前两个字节指定的CONSTANT_Utf8条目
        return readUTF8(items[readUnsignedShort(index)], buf);
    }

    /**
     * 读取{@link #b b}中类常量池项.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param index
     *            {@link #b b}中unsigned short值的起始索引, 其值是类常量池项的索引.
     * @param buf
     *            用于读取条目的缓冲区. 该缓冲区必须足够大. 它不会自动调整大小.
     * 
     * @return 与指定类项对应的String.
     */
    public String readClass(final int index, final char[] buf) {
        return readStringish(index, buf);
    }

    /**
     * 读取{@link #b b}中模块常量池项.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     *
     * @param index
     *            {@link #b b}中unsigned short值的起始索引, 其值是模块常量池项的索引.
     * @param buf
     *            用于读取条目的缓冲区. 该缓冲区必须足够大. 它不会自动调整大小.
     * 
     * @return 与指定模块项对应的String.
     */
    public String readModule(final int index, final char[] buf) {
        return readStringish(index, buf);
    }

    /**
     * 读取{@link #b b}中模块常量池项.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     *
     * @param index
     *            {@link #b b}中unsigned short值的起始索引, 其值是模块常量池项的索引.
     * @param buf
     *            用于读取条目的缓冲区. 该缓冲区必须足够大. 它不会自动调整大小.
     * 
     * @return 与指定模块项对应的String.
     */
    public String readPackage(final int index, final char[] buf) {
        return readStringish(index, buf);
    }

    /**
     * 读取{@link #b b}中的数字或字符串常量池项.
     * <i>此方法适用于{@link Attribute}子类, 类生成器或适配器通常不需要此方法.</i>
     * 
     * @param item
     *            常量池项的索引.
     * @param buf
     *            用于读取条目的缓冲区. 该缓冲区必须足够大. 它不会自动调整大小.
     * 
     * @return 对应于给定常量池项的{@link Integer}, {@link Float}, {@link Long}, {@link Double},
     * {@link String}, {@link Type}或 {@link Handle}.
     */
    public Object readConst(final int item, final char[] buf) {
        int index = items[item];
        switch (b[index - 1]) {
        case ClassWriter.INT:
            return readInt(index);
        case ClassWriter.FLOAT:
            return Float.intBitsToFloat(readInt(index));
        case ClassWriter.LONG:
            return readLong(index);
        case ClassWriter.DOUBLE:
            return Double.longBitsToDouble(readLong(index));
        case ClassWriter.CLASS:
            return Type.getObjectType(readUTF8(index, buf));
        case ClassWriter.STR:
            return readUTF8(index, buf);
        case ClassWriter.MTYPE:
            return Type.getMethodType(readUTF8(index, buf));
        default: // case ClassWriter.HANDLE_BASE + [1..9]:
            int tag = readByte(index);
            int[] items = this.items;
            int cpIndex = items[readUnsignedShort(index + 1)];
            boolean itf = b[cpIndex - 1] == ClassWriter.IMETH;
            String owner = readClass(cpIndex, buf);
            cpIndex = items[readUnsignedShort(cpIndex + 2)];
            String name = readUTF8(cpIndex, buf);
            String desc = readUTF8(cpIndex + 2, buf);
            return new Handle(tag, owner, name, desc, itf);
        }
    }
}
