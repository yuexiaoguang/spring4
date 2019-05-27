package org.springframework.asm;

/**
 * 一个{@link FieldVisitor}, 以字节码形式生成Java字段.
 */
final class FieldWriter extends FieldVisitor {

    /**
     * 必须添加此字段的类编写器.
     */
    private final ClassWriter cw;

    /**
     * 此字段的访问标志.
     */
    private final int access;

    /**
     * 包含此方法名称的常量池项的索引.
     */
    private final int name;

    /**
     * 包含此字段描述符的常量池项的索引.
     */
    private final int desc;

    /**
     * 包含此字段签名的常量池项的索引.
     */
    private int signature;

    /**
     * 包含此字段的常量值的常量池项的索引.
     */
    private int value;

    /**
     * 此字段的运行时可见注解. May be <tt>null</tt>.
     */
    private AnnotationWriter anns;

    /**
     * 此字段的运行时不可见注解. May be <tt>null</tt>.
     */
    private AnnotationWriter ianns;

    /**
     * 此字段的运行时可见类型注解. May be <tt>null</tt>.
     */
    private AnnotationWriter tanns;

    /**
     * 此字段的运行时不可见类型注解. May be <tt>null</tt>.
     */
    private AnnotationWriter itanns;

    /**
     * 此字段的非标准属性. May be <tt>null</tt>.
     */
    private Attribute attrs;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * @param cw
     *            必须添加此字段的类编写器.
     * @param access
     *            该字段的访问标志 (see {@link Opcodes}).
     * @param name
     *            该字段的名称.
     * @param desc
     *            字段的描述符 (see {@link Type}).
     * @param signature
     *            字段的签名. May be <tt>null</tt>.
     * @param value
     *            字段的常量值. May be <tt>null</tt>.
     */
    FieldWriter(final ClassWriter cw, final int access, final String name,
            final String desc, final String signature, final Object value) {
        super(Opcodes.ASM6);
        if (cw.firstField == null) {
            cw.firstField = this;
        } else {
            cw.lastField.fv = this;
        }
        cw.lastField = this;
        this.cw = cw;
        this.access = access;
        this.name = cw.newUTF8(name);
        this.desc = cw.newUTF8(desc);
        if (ClassReader.SIGNATURES && signature != null) {
            this.signature = cw.newUTF8(signature);
        }
        if (value != null) {
            this.value = cw.newConstItem(value).index;
        }
    }

    // ------------------------------------------------------------------------
    // Implementation of the FieldVisitor abstract class
    // ------------------------------------------------------------------------

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
    public void visitAttribute(final Attribute attr) {
        attr.next = attrs;
        attrs = attr;
    }

    @Override
    public void visitEnd() {
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    /**
     * 返回此字段的大小.
     * 
     * @return 此字段的大小.
     */
    int getSize() {
        int size = 8;
        if (value != 0) {
            cw.newUTF8("ConstantValue");
            size += 8;
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
        if (ClassReader.SIGNATURES && signature != 0) {
            cw.newUTF8("Signature");
            size += 8;
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
        if (attrs != null) {
            size += attrs.getSize(cw, null, 0, -1, -1);
        }
        return size;
    }

    /**
     * 将此字段的内容放入给定的字节向量中.
     * 
     * @param out
     *            必须放置此字段的内容.
     */
    void put(final ByteVector out) {
        final int FACTOR = ClassWriter.TO_ACC_SYNTHETIC;
        int mask = Opcodes.ACC_DEPRECATED | ClassWriter.ACC_SYNTHETIC_ATTRIBUTE
                | ((access & ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) / FACTOR);
        out.putShort(access & ~mask).putShort(name).putShort(desc);
        int attributeCount = 0;
        if (value != 0) {
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
        if (ClassReader.SIGNATURES && signature != 0) {
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
        if (attrs != null) {
            attributeCount += attrs.getCount();
        }
        out.putShort(attributeCount);
        if (value != 0) {
            out.putShort(cw.newUTF8("ConstantValue"));
            out.putInt(2).putShort(value);
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
        if (ClassReader.SIGNATURES && signature != 0) {
            out.putShort(cw.newUTF8("Signature"));
            out.putInt(2).putShort(signature);
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
        if (attrs != null) {
            attrs.put(cw, null, 0, -1, -1, out);
        }
    }
}
