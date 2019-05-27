package org.springframework.asm;

/**
 * 一个常量池项. 可以使用{@link ClassWriter}类中的'newXXX'方法创建常量池项.
 */
final class Item {

    /**
     * 常量池中此项的索引.
     */
    int index;

    /**
     * 此常量池项的类型.
     * 单个类用于表示所有常量池项类型, 以便最小化此包的字节码大小.
     * 该字段的值是{@link ClassWriter#INT},
     * {@link ClassWriter#LONG}, {@link ClassWriter#FLOAT},
     * {@link ClassWriter#DOUBLE}, {@link ClassWriter#UTF8},
     * {@link ClassWriter#STR}, {@link ClassWriter#CLASS},
     * {@link ClassWriter#NAME_TYPE}, {@link ClassWriter#FIELD},
     * {@link ClassWriter#METH}, {@link ClassWriter#IMETH},
     * {@link ClassWriter#MODULE}, {@link ClassWriter#PACKAGE},
     * {@link ClassWriter#MTYPE}, {@link ClassWriter#INDY}之一.
     * 
     * MethodHandle常量的9个变量使用9个值的范围存储,
     * 从{@link ClassWriter#HANDLE_BASE} + 1 到{@link ClassWriter#HANDLE_BASE} + 9.
     * 
     * 特殊Item类型用于存储在ClassWriter {@link ClassWriter#typeTable}中的Item, 而不是常量池,
     * 为了避免与ClassWriter常量池的哈希表中的常规常量池项冲突.
     * 这些特殊项类型是{@link ClassWriter#TYPE_NORMAL}, {@link ClassWriter#TYPE_UNINIT} 和 {@link ClassWriter#TYPE_MERGED}.
     */
    int type;

    /**
     * 此项的值, 对于int项.
     */
    int intVal;

    /**
     * 此项的值, 对于long 项.
     */
    long longVal;

    /**
     * 对于不包含原始值的项, 此项的值的第一部分.
     */
    String strVal1;

    /**
     * 对于不包含原始值的项, 此项的值的第二部分.
     */
    String strVal2;

    /**
     * 对于不包含原始值的项, 此项的值的第三部分.
     */
    String strVal3;

    /**
     * 此常量池项的哈希码值.
     */
    int hashCode;

    /**
     * 链接到另一个常量池项, 用于常量池的哈希表中的冲突列表.
     */
    Item next;

    /**
     * 构造一个未初始化的{@link Item}.
     */
    Item() {
    }

    /**
     * 在给定位置为常量池元素构造一个未初始化的{@link Item}.
     * 
     * @param index
     *            要构建的项的索引.
     */
    Item(final int index) {
        this.index = index;
    }

    /**
     * 构造给定项的副本.
     * 
     * @param index
     *            要构建的项的索引.
     * @param i
     *            必须复制到要构造的项中的项.
     */
    Item(final int index, final Item i) {
        this.index = index;
        type = i.type;
        intVal = i.intVal;
        longVal = i.longVal;
        strVal1 = i.strVal1;
        strVal2 = i.strVal2;
        strVal3 = i.strVal3;
        hashCode = i.hashCode;
    }

    /**
     * 将此项设置为整数项.
     * 
     * @param intVal
     *            此项的值.
     */
    void set(final int intVal) {
        this.type = ClassWriter.INT;
        this.intVal = intVal;
        this.hashCode = 0x7FFFFFFF & (type + intVal);
    }

    /**
     * 将此项设置为long项.
     * 
     * @param longVal
     *            此项的值.
     */
    void set(final long longVal) {
        this.type = ClassWriter.LONG;
        this.longVal = longVal;
        this.hashCode = 0x7FFFFFFF & (type + (int) longVal);
    }

    /**
     * 将此项设置为float项.
     * 
     * @param floatVal
     *            此项的值.
     */
    void set(final float floatVal) {
        this.type = ClassWriter.FLOAT;
        this.intVal = Float.floatToRawIntBits(floatVal);
        this.hashCode = 0x7FFFFFFF & (type + (int) floatVal);
    }

    /**
     * 将此项设置为 double 项.
     * 
     * @param doubleVal
     *            此项的值.
     */
    void set(final double doubleVal) {
        this.type = ClassWriter.DOUBLE;
        this.longVal = Double.doubleToRawLongBits(doubleVal);
        this.hashCode = 0x7FFFFFFF & (type + (int) doubleVal);
    }

    /**
     * 将此项设置为不包含原始值的项.
     * 
     * @param type
     *            此项的类型.
     * @param strVal1
     *            此项的值的第一部分.
     * @param strVal2
     *            此项的值的第二部分.
     * @param strVal3
     *            此项的值的第三部分.
     */
    @SuppressWarnings("fallthrough")
    void set(final int type, final String strVal1, final String strVal2,
            final String strVal3) {
        this.type = type;
        this.strVal1 = strVal1;
        this.strVal2 = strVal2;
        this.strVal3 = strVal3;
        switch (type) {
        case ClassWriter.CLASS:
            this.intVal = 0;     // 类的intVal必须为零, see visitInnerClass
        case ClassWriter.UTF8:
        case ClassWriter.STR:
        case ClassWriter.MTYPE:
        case ClassWriter.MODULE:
        case ClassWriter.PACKAGE:
        case ClassWriter.TYPE_NORMAL:
            hashCode = 0x7FFFFFFF & (type + strVal1.hashCode());
            return;
        case ClassWriter.NAME_TYPE: {
            hashCode = 0x7FFFFFFF & (type + strVal1.hashCode()
                    * strVal2.hashCode());
            return;
        }
        // ClassWriter.FIELD:
        // ClassWriter.METH:
        // ClassWriter.IMETH:
        // ClassWriter.HANDLE_BASE + 1..9
        default:
            hashCode = 0x7FFFFFFF & (type + strVal1.hashCode()
                    * strVal2.hashCode() * strVal3.hashCode());
        }
    }

    /**
     * 将项设置为InvokeDynamic项.
     * 
     * @param name
     *            invokedynamic的名称.
     * @param desc
     *            invokedynamic的描述.
     * @param bsmIndex
     *            类属性BootrapMethods基于零的索引.
     */
    void set(String name, String desc, int bsmIndex) {
        this.type = ClassWriter.INDY;
        this.longVal = bsmIndex;
        this.strVal1 = name;
        this.strVal2 = desc;
        this.hashCode = 0x7FFFFFFF & (ClassWriter.INDY + bsmIndex
                * strVal1.hashCode() * strVal2.hashCode());
    }

    /**
     * 将项设置为BootstrapMethod项.
     * 
     * @param position
     *            在类属性BootrapMethods中字节的位置.
     * @param hashCode
     *            项的哈希码.
     *            此哈希码是从引导方法的哈希码和所有引导参数的哈希码处理的.
     */
    void set(int position, int hashCode) {
        this.type = ClassWriter.BSM;
        this.intVal = position;
        this.hashCode = hashCode;
    }

    /**
     * 指示给定项是否等于此项.
     * <i>此方法假设这两个项具有相同的{@link #type}</i>.
     * 
     * @param i
     *            要与此项进行比较的项. 这两个项必须具有相同的{@link #type}.
     * 
     * @return <tt>true</tt>如果相等, 否则<tt>false</tt>.
     */
    boolean isEqualTo(final Item i) {
        switch (type) {
        case ClassWriter.UTF8:
        case ClassWriter.STR:
        case ClassWriter.CLASS:
        case ClassWriter.MODULE:
        case ClassWriter.PACKAGE:
        case ClassWriter.MTYPE:
        case ClassWriter.TYPE_NORMAL:
            return i.strVal1.equals(strVal1);
        case ClassWriter.TYPE_MERGED:
        case ClassWriter.LONG:
        case ClassWriter.DOUBLE:
            return i.longVal == longVal;
        case ClassWriter.INT:
        case ClassWriter.FLOAT:
            return i.intVal == intVal;
        case ClassWriter.TYPE_UNINIT:
            return i.intVal == intVal && i.strVal1.equals(strVal1);
        case ClassWriter.NAME_TYPE:
            return i.strVal1.equals(strVal1) && i.strVal2.equals(strVal2);
        case ClassWriter.INDY: {
            return i.longVal == longVal && i.strVal1.equals(strVal1)
                    && i.strVal2.equals(strVal2);
        }
        // case ClassWriter.FIELD:
        // case ClassWriter.METH:
        // case ClassWriter.IMETH:
        // case ClassWriter.HANDLE_BASE + 1..9
        default:
            return i.strVal1.equals(strVal1) && i.strVal2.equals(strVal2)
                    && i.strVal3.equals(strVal3);
        }
    }

}
