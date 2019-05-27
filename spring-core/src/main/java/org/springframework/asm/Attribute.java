package org.springframework.asm;

/**
 * 非标准类, 字段, 方法或代码属性.
 */
public class Attribute {

    /**
     * 此属性的类型.
     */
    public final String type;

    /**
     * 此属性的原始值, 仅用于未知属性.
     */
    byte[] value;

    /**
     * 此属性列表中的下一个属性. 可能是<tt>null</tt>.
     */
    Attribute next;

    /**
     * 构造一个新的空属性.
     * 
     * @param type
     *            属性的类型.
     */
    protected Attribute(final String type) {
        this.type = type;
    }

    /**
     * 如果此类属性未知, 则返回<tt>true</tt>.
     * 此方法的默认实现始终返回<tt>true</tt>.
     * 
     * @return <tt>true</tt>如果这种类型的属性未知.
     */
    public boolean isUnknown() {
        return true;
    }

    /**
     * 如果此类属性是代码属性, 则返回<tt>true</tt>.
     * 
     * @return <tt>true</tt>如果这种类型的属性是代码属性.
     */
    public boolean isCodeAttribute() {
        return false;
    }

    /**
     * 返回与此属性对应的标签.
     * 
     * @return 与此属性对应的标签, 或<tt>null</tt>如果此属性不是包含标签的代码属性.
     */
    protected Label[] getLabels() {
        return null;
    }

    /**
     * 读取{@link #type type}属性.
     * 此方法必须返回一个类型为{@link #type type}的<i>new</i> {@link Attribute}对象,
     * 对应于从给定偏移量开始的<tt>len</tt>字节, 在给定的类读取器中.
     * 
     * @param cr
     *            包含要读取的属性的类.
     * @param off
     *            {@link ClassReader#b cr.b}中属性内容的第一个字节的索引.
     *            这里不考虑包含属性类型和长度的6个属性头字节.
     * @param len
     *            属性内容的长度.
     * @param buf
     *            缓冲区, 用于调用{@link ClassReader#readUTF8 readUTF8}, {@link ClassReader#readClass(int,char[]) readClass}
     *            或{@link ClassReader#readConst readConst}.
     * @param codeOff
     *            {@link ClassReader#b cr.b}中代码属性内容的第一个字节的索引;
     *            如果要读取的属性不是代码属性, 则返回-1.
     *            这里不考虑包含属性类型和长度的6个属性头字节.
     * @param labels
     *            方法代码的标签, 或<tt>null</tt>如果要读取的属性不是代码属性.
     * 
     * @return 与给定字节对应的<i>new</i> {@link Attribute}对象.
     */
    protected Attribute read(final ClassReader cr, final int off,
            final int len, final char[] buf, final int codeOff,
            final Label[] labels) {
        Attribute attr = new Attribute(type);
        attr.value = new byte[len];
        System.arraycopy(cr.b, off, attr.value, 0, len);
        return attr;
    }

    /**
     * 返回此属性的字节数组形式.
     * 
     * @param cw
     *            必须添加此属性的类.
     *            此参数可用于向此类的常量池添加与此属性对应的项.
     * @param code
     *            与此代码属性对应的方法的字节码, 或<tt>null</tt>如果此属性不是代码属性.
     * @param len
     *            与此代码属性对应的方法的字节码长度, 或<tt>null</tt>如果此属性不是代码属性.
     * @param maxStack
     *            与此代码属性对应的方法的最大堆栈大小, 或-1, 如果此属性不是代码属性.
     * @param maxLocals
     *            与此代码属性对应的方法的最大局部变量的数量, 或 -1, 如果此属性不是代码属性.
     * 
     * @return 此属性的字节数组.
     */
    protected ByteVector write(final ClassWriter cw, final byte[] code,
            final int len, final int maxStack, final int maxLocals) {
        ByteVector v = new ByteVector();
        v.data = value;
        v.length = value.length;
        return v;
    }

    /**
     * 返回以此属性开头的属性列表的长度.
     * 
     * @return 属性列表的长度.
     */
    final int getCount() {
        int count = 0;
        Attribute attr = this;
        while (attr != null) {
            count += 1;
            attr = attr.next;
        }
        return count;
    }

    /**
     * 返回此属性列表中所有属性的大小.
     * 
     * @param cw
     *            用于将属性转换为字节数组的类编写器, 使用{@link #write write}方法.
     * @param code
     *            与这些代码属性对应的方法的字节码, 或<tt>null</tt>如果这些属性不是代码属性.
     * @param len
     *            与这些代码属性对应的方法的字节码长度, 或<tt>null</tt>如果这些属性不是代码属性.
     * @param maxStack
     *            与这些代码属性对应的方法的最大堆栈大小, 或 -1, 如果这些属性不是代码属性.
     * @param maxLocals
     *            与这些代码属性对应的方法的最大局部变量的数量, 或 -1, 如果这些属性不是代码属性.
     * 
     * @return 此属性列表中所有属性的大小. 此大小包括属性header的大小.
     */
    final int getSize(final ClassWriter cw, final byte[] code, final int len,
            final int maxStack, final int maxLocals) {
        Attribute attr = this;
        int size = 0;
        while (attr != null) {
            cw.newUTF8(attr.type);
            size += attr.write(cw, code, len, maxStack, maxLocals).length + 6;
            attr = attr.next;
        }
        return size;
    }

    /**
     * 在给定的字节向量中写入此属性列表的所有属性.
     * 
     * @param cw
     *            用于将属性转换为字节数组的类编写器, 使用{@link #write write} 方法.
     * @param code
     *            与这些代码属性对应的方法的字节码, 或<tt>null</tt>如果这些属性不是代码属性.
     * @param len
     *            与这些代码属性对应的方法的字节码长度, 或<tt>null</tt>如果这些属性不是代码属性.
     * @param maxStack
     *            与这些代码属性对应的方法的最大堆栈大小, 或 -1, 如果这些属性不是代码属性.
     * @param maxLocals
     *            与这些代码属性对应的方法的最大局部变量的数量, 或 -1, 如果这些属性不是代码属性.
     * @param out
     *            必须写入的属性的位置.
     */
    final void put(final ClassWriter cw, final byte[] code, final int len,
            final int maxStack, final int maxLocals, final ByteVector out) {
        Attribute attr = this;
        while (attr != null) {
            ByteVector b = attr.write(cw, code, len, maxStack, maxLocals);
            out.putShort(cw.newUTF8(attr.type)).putInt(b.length);
            out.putByteArray(b.data, 0, b.length);
            attr = attr.next;
        }
    }
}
