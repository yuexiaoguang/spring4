package org.springframework.asm;

/**
 * 一个{@link AnnotationVisitor}, 以字节码形式生成注解.
 */
final class AnnotationWriter extends AnnotationVisitor {

    /**
     * 必须添加此注解的类编写器.
     */
    private final ClassWriter cw;

    /**
     * 此注解中的值的数量.
     */
    private int size;

    /**
     * <tt>true<tt>如果值已命名, 否则<tt>false</tt>.
     * 用于注解默认值和注解数组的注解编写器使用未命名的值.
     */
    private final boolean named;

    /**
     * 字节码形式的注解值.
     * 该字节向量仅包含值本身, i.e. 值的数量必须在这些字节之前存储为无符号short.
     */
    private final ByteVector bv;

    /**
     * 用于存储此注解的值的数量的字节向量. See {@link #bv}.
     */
    private final ByteVector parent;

    /**
     * 此注解的值的数量必须存储在{@link #parent}中.
     */
    private final int offset;

    /**
     * 下一个注解编写器. 该字段用于存储注解列表.
     */
    AnnotationWriter next;

    /**
     * 上一个注解编写器. 该字段用于存储注解列表.
     */
    AnnotationWriter prev;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * @param cw
     *            必须添加此注解的类编写器.
     * @param named
     *            <tt>true<tt>如果值已命名, 否则<tt>false</tt>.
     * @param bv
     *            必须存储注解值的位置.
     * @param parent
     *            必须存储注解值的数量的位置.
     * @param offset
     *            必须存储注解值的数量的<tt>parent</tt>中的位置.
     */
    AnnotationWriter(final ClassWriter cw, final boolean named,
            final ByteVector bv, final ByteVector parent, final int offset) {
        super(Opcodes.ASM6);
        this.cw = cw;
        this.named = named;
        this.bv = bv;
        this.parent = parent;
        this.offset = offset;
    }

    // ------------------------------------------------------------------------
    // Implementation of the AnnotationVisitor abstract class
    // ------------------------------------------------------------------------

    @Override
    public void visit(final String name, final Object value) {
        ++size;
        if (named) {
            bv.putShort(cw.newUTF8(name));
        }
        if (value instanceof String) {
            bv.put12('s', cw.newUTF8((String) value));
        } else if (value instanceof Byte) {
            bv.put12('B', cw.newInteger(((Byte) value).byteValue()).index);
        } else if (value instanceof Boolean) {
            int v = ((Boolean) value).booleanValue() ? 1 : 0;
            bv.put12('Z', cw.newInteger(v).index);
        } else if (value instanceof Character) {
            bv.put12('C', cw.newInteger(((Character) value).charValue()).index);
        } else if (value instanceof Short) {
            bv.put12('S', cw.newInteger(((Short) value).shortValue()).index);
        } else if (value instanceof Type) {
            bv.put12('c', cw.newUTF8(((Type) value).getDescriptor()));
        } else if (value instanceof byte[]) {
            byte[] v = (byte[]) value;
            bv.put12('[', v.length);
            for (int i = 0; i < v.length; i++) {
                bv.put12('B', cw.newInteger(v[i]).index);
            }
        } else if (value instanceof boolean[]) {
            boolean[] v = (boolean[]) value;
            bv.put12('[', v.length);
            for (int i = 0; i < v.length; i++) {
                bv.put12('Z', cw.newInteger(v[i] ? 1 : 0).index);
            }
        } else if (value instanceof short[]) {
            short[] v = (short[]) value;
            bv.put12('[', v.length);
            for (int i = 0; i < v.length; i++) {
                bv.put12('S', cw.newInteger(v[i]).index);
            }
        } else if (value instanceof char[]) {
            char[] v = (char[]) value;
            bv.put12('[', v.length);
            for (int i = 0; i < v.length; i++) {
                bv.put12('C', cw.newInteger(v[i]).index);
            }
        } else if (value instanceof int[]) {
            int[] v = (int[]) value;
            bv.put12('[', v.length);
            for (int i = 0; i < v.length; i++) {
                bv.put12('I', cw.newInteger(v[i]).index);
            }
        } else if (value instanceof long[]) {
            long[] v = (long[]) value;
            bv.put12('[', v.length);
            for (int i = 0; i < v.length; i++) {
                bv.put12('J', cw.newLong(v[i]).index);
            }
        } else if (value instanceof float[]) {
            float[] v = (float[]) value;
            bv.put12('[', v.length);
            for (int i = 0; i < v.length; i++) {
                bv.put12('F', cw.newFloat(v[i]).index);
            }
        } else if (value instanceof double[]) {
            double[] v = (double[]) value;
            bv.put12('[', v.length);
            for (int i = 0; i < v.length; i++) {
                bv.put12('D', cw.newDouble(v[i]).index);
            }
        } else {
            Item i = cw.newConstItem(value);
            bv.put12(".s.IFJDCS".charAt(i.type), i.index);
        }
    }

    @Override
    public void visitEnum(final String name, final String desc,
            final String value) {
        ++size;
        if (named) {
            bv.putShort(cw.newUTF8(name));
        }
        bv.put12('e', cw.newUTF8(desc)).putShort(cw.newUTF8(value));
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String name,
            final String desc) {
        ++size;
        if (named) {
            bv.putShort(cw.newUTF8(name));
        }
        // 写入标签和类型, 并为值计数保留空间
        bv.put12('@', cw.newUTF8(desc)).putShort(0);
        return new AnnotationWriter(cw, true, bv, bv, bv.length - 2);
    }

    @Override
    public AnnotationVisitor visitArray(final String name) {
        ++size;
        if (named) {
            bv.putShort(cw.newUTF8(name));
        }
        // 写入标签, 并为数组大小保留空间
        bv.put12('[', 0);
        return new AnnotationWriter(cw, false, bv, bv, bv.length - 2);
    }

    @Override
    public void visitEnd() {
        if (parent != null) {
            byte[] data = parent.data;
            data[offset] = (byte) (size >>> 8);
            data[offset + 1] = (byte) size;
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    /**
     * 返回此注解编写器列表的大小.
     * 
     * @return 列表的大小.
     */
    int getSize() {
        int size = 0;
        AnnotationWriter aw = this;
        while (aw != null) {
            size += aw.bv.length;
            aw = aw.next;
        }
        return size;
    }

    /**
     * 将此注解编写器列表的注解放入给定的字节向量中.
     * 
     * @param out
     *            必须放置注解的地方.
     */
    void put(final ByteVector out) {
        int n = 0;
        int size = 2;
        AnnotationWriter aw = this;
        AnnotationWriter last = null;
        while (aw != null) {
            ++n;
            size += aw.bv.length;
            aw.visitEnd(); // in case user forgot to call visitEnd
            aw.prev = last;
            last = aw;
            aw = aw.next;
        }
        out.putInt(size);
        out.putShort(n);
        aw = last;
        while (aw != null) {
            out.putByteArray(aw.bv.data, 0, aw.bv.length);
            aw = aw.prev;
        }
    }

    /**
     * 将给定的注解列表放入给定的字节向量中.
     * 
     * @param panns
     *            注解编写器.
     * @param off
     *            第一个注解要写入的索引.
     * @param out
     *            必须放置注解的地方.
     */
    static void put(final AnnotationWriter[] panns, final int off,
            final ByteVector out) {
        int size = 1 + 2 * (panns.length - off);
        for (int i = off; i < panns.length; ++i) {
            size += panns[i] == null ? 0 : panns[i].getSize();
        }
        out.putInt(size).putByte(panns.length - off);
        for (int i = off; i < panns.length; ++i) {
            AnnotationWriter aw = panns[i];
            AnnotationWriter last = null;
            int n = 0;
            while (aw != null) {
                ++n;
                aw.visitEnd(); // in case user forgot to call visitEnd
                aw.prev = last;
                last = aw;
                aw = aw.next;
            }
            out.putShort(n);
            aw = last;
            while (aw != null) {
                out.putByteArray(aw.bv.data, 0, aw.bv.length);
                aw = aw.prev;
            }
        }
    }

    /**
     * 将给定的类型引用和类型路径放入给定的bytevector中.
     * 不支持 LOCAL_VARIABLE 和 RESOURCE_VARIABLE 目标类型.
     * 
     * @param typeRef
     *            对注解类型的引用. See {@link TypeReference}.
     * @param typePath
     *            'typeRef'中带注解的类型参数, 通配符绑定, 数组元素类型, 静态内部类型的路径.
     *            如果注释将'typeRef'作为一个整体, 则可以是<tt>null</tt>.
     * @param out
     *            必须放置类型引用和类型路径的位置.
     */
    static void putTarget(int typeRef, TypePath typePath, ByteVector out) {
        switch (typeRef >>> 24) {
        case 0x00: // CLASS_TYPE_PARAMETER
        case 0x01: // METHOD_TYPE_PARAMETER
        case 0x16: // METHOD_FORMAL_PARAMETER
            out.putShort(typeRef >>> 16);
            break;
        case 0x13: // FIELD
        case 0x14: // METHOD_RETURN
        case 0x15: // METHOD_RECEIVER
            out.putByte(typeRef >>> 24);
            break;
        case 0x47: // CAST
        case 0x48: // CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
        case 0x49: // METHOD_INVOCATION_TYPE_ARGUMENT
        case 0x4A: // CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
        case 0x4B: // METHOD_REFERENCE_TYPE_ARGUMENT
            out.putInt(typeRef);
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
            out.put12(typeRef >>> 24, (typeRef & 0xFFFF00) >> 8);
            break;
        }
        if (typePath == null) {
            out.putByte(0);
        } else {
            int length = typePath.b[typePath.offset] * 2 + 1;
            out.putByteArray(typePath.b, typePath.offset, length);
        }
    }
}
