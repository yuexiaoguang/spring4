package org.springframework.asm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Java字段或方法类型.
 * 此类可用于更轻松地操作类型和方法描述符.
 */
public class Type {

    /**
     * <tt>void</tt>类型的种类. See {@link #getSort getSort}.
     */
    public static final int VOID = 0;

    /**
     * <tt>boolean</tt>类型的种类. See {@link #getSort getSort}.
     */
    public static final int BOOLEAN = 1;

    /**
     * <tt>char</tt>类型的种类. See {@link #getSort getSort}.
     */
    public static final int CHAR = 2;

    /**
     * <tt>byte</tt>类型的种类. See {@link #getSort getSort}.
     */
    public static final int BYTE = 3;

    /**
     * <tt>short</tt>类型的种类. See {@link #getSort getSort}.
     */
    public static final int SHORT = 4;

    /**
     * <tt>int</tt>类型的种类. See {@link #getSort getSort}.
     */
    public static final int INT = 5;

    /**
     * <tt>float</tt>类型的种类. See {@link #getSort getSort}.
     */
    public static final int FLOAT = 6;

    /**
     * <tt>long</tt>类型的种类. See {@link #getSort getSort}.
     */
    public static final int LONG = 7;

    /**
     * <tt>double</tt>类型的种类. See {@link #getSort getSort}.
     */
    public static final int DOUBLE = 8;

    /**
     * 数组引用类型的种类. See {@link #getSort getSort}.
     */
    public static final int ARRAY = 9;

    /**
     * 对象引用类型的种类. See {@link #getSort getSort}.
     */
    public static final int OBJECT = 10;

    /**
     * 方法类型的种类. See {@link #getSort getSort}.
     */
    public static final int METHOD = 11;

    /**
     * <tt>void</tt>类型.
     */
    public static final Type VOID_TYPE = new Type(VOID, null, ('V' << 24)
            | (5 << 16) | (0 << 8) | 0, 1);

    /**
     * The <tt>boolean</tt> type.
     */
    public static final Type BOOLEAN_TYPE = new Type(BOOLEAN, null, ('Z' << 24)
            | (0 << 16) | (5 << 8) | 1, 1);

    /**
     * The <tt>char</tt> type.
     */
    public static final Type CHAR_TYPE = new Type(CHAR, null, ('C' << 24)
            | (0 << 16) | (6 << 8) | 1, 1);

    /**
     * The <tt>byte</tt> type.
     */
    public static final Type BYTE_TYPE = new Type(BYTE, null, ('B' << 24)
            | (0 << 16) | (5 << 8) | 1, 1);

    /**
     * The <tt>short</tt> type.
     */
    public static final Type SHORT_TYPE = new Type(SHORT, null, ('S' << 24)
            | (0 << 16) | (7 << 8) | 1, 1);

    /**
     * The <tt>int</tt> type.
     */
    public static final Type INT_TYPE = new Type(INT, null, ('I' << 24)
            | (0 << 16) | (0 << 8) | 1, 1);

    /**
     * The <tt>float</tt> type.
     */
    public static final Type FLOAT_TYPE = new Type(FLOAT, null, ('F' << 24)
            | (2 << 16) | (2 << 8) | 1, 1);

    /**
     * The <tt>long</tt> type.
     */
    public static final Type LONG_TYPE = new Type(LONG, null, ('J' << 24)
            | (1 << 16) | (1 << 8) | 2, 1);

    /**
     * The <tt>double</tt> type.
     */
    public static final Type DOUBLE_TYPE = new Type(DOUBLE, null, ('D' << 24)
            | (3 << 16) | (3 << 8) | 2, 1);

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    /**
     * 此Java类型的种类.
     */
    private final int sort;

    /**
     * 包含此Java类型的内部名称的缓冲区.
     * 该字段仅用于引用类型.
     */
    private final char[] buf;

    /**
     * 此Java类型的内部名称在{@link #buf buf}中的偏移量, 或者对于基本类型, 此类型的大小, 描述符和getOpcode的偏移量
     * (字节0包含大小, 字节1包含描述符, 字节2包含IALOAD或IASTORE的偏移量, 字节3包含所有其他指令的偏移量).
     */
    private final int off;

    /**
     * 此Java类型的内部名称的长度.
     */
    private final int len;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * @param sort
     *            要构造的引用类型的种类.
     * @param buf
     *            包含前一类型描述符的缓冲区.
     * @param off
     *            此描述符在前一个缓冲区中的偏移量.
     * @param len
     *            此描述符的长度.
     */
    private Type(final int sort, final char[] buf, final int off, final int len) {
        this.sort = sort;
        this.buf = buf;
        this.off = off;
        this.len = len;
    }

    /**
     * 返回与给定类型描述符对应的Java类型.
     * 
     * @param typeDescriptor
     *            字段或方法类型描述符.
     * 
     * @return 与给定类型描述符对应的Java类型.
     */
    public static Type getType(final String typeDescriptor) {
        return getType(typeDescriptor.toCharArray(), 0);
    }

    /**
     * 返回与给定内部名称对应的Java类型.
     * 
     * @param internalName
     *            内部名称.
     * 
     * @return 与给定内部名称对应的Java类型.
     */
    public static Type getObjectType(final String internalName) {
        char[] buf = internalName.toCharArray();
        return new Type(buf[0] == '[' ? ARRAY : OBJECT, buf, 0, buf.length);
    }

    /**
     * 返回与给定方法描述符对应的Java类型.
     * 等效于<code>Type.getType(methodDescriptor)</code>.
     * 
     * @param methodDescriptor
     *            方法描述符.
     * 
     * @return 与给定方法描述符对应的Java类型.
     */
    public static Type getMethodType(final String methodDescriptor) {
        return getType(methodDescriptor.toCharArray(), 0);
    }

    /**
     * 返回与给定参数和返回类型对应的Java方法类型.
     * 
     * @param returnType
     *            方法的返回类型.
     * @param argumentTypes
     *            方法的参数类型.
     * 
     * @return 与给定参数和返回类型对应的Java类型.
     */
    public static Type getMethodType(final Type returnType,
            final Type... argumentTypes) {
        return getType(getMethodDescriptor(returnType, argumentTypes));
    }

    /**
     * 返回与给定类对应的Java类型.
     * 
     * @param c
     *            类.
     * 
     * @return 与给定类对应的Java类型.
     */
    public static Type getType(final Class<?> c) {
        if (c.isPrimitive()) {
            if (c == Integer.TYPE) {
                return INT_TYPE;
            } else if (c == Void.TYPE) {
                return VOID_TYPE;
            } else if (c == Boolean.TYPE) {
                return BOOLEAN_TYPE;
            } else if (c == Byte.TYPE) {
                return BYTE_TYPE;
            } else if (c == Character.TYPE) {
                return CHAR_TYPE;
            } else if (c == Short.TYPE) {
                return SHORT_TYPE;
            } else if (c == Double.TYPE) {
                return DOUBLE_TYPE;
            } else if (c == Float.TYPE) {
                return FLOAT_TYPE;
            } else /* if (c == Long.TYPE) */{
                return LONG_TYPE;
            }
        } else {
            return getType(getDescriptor(c));
        }
    }

    /**
     * 返回与给定构造函数对应的Java方法类型.
     * 
     * @param c
     *            {@link Constructor Constructor}对象.
     * 
     * @return 与给定构造函数对应的Java方法类型.
     */
    public static Type getType(final Constructor<?> c) {
        return getType(getConstructorDescriptor(c));
    }

    /**
     * 返回与给定方法对应的Java方法类型.
     * 
     * @param m
     *            {@link Method Method}对象.
     * 
     * @return 与给定方法对应的Java方法类型.
     */
    public static Type getType(final Method m) {
        return getType(getMethodDescriptor(m));
    }

    /**
     * 返回与给定方法描述符的参数类型对应的Java类型.
     * 
     * @param methodDescriptor
     *            方法描述符.
     * 
     * @return 与给定方法描述符的参数类型对应的Java类型.
     */
    public static Type[] getArgumentTypes(final String methodDescriptor) {
        char[] buf = methodDescriptor.toCharArray();
        int off = 1;
        int size = 0;
        while (true) {
            char car = buf[off++];
            if (car == ')') {
                break;
            } else if (car == 'L') {
                while (buf[off++] != ';') {
                }
                ++size;
            } else if (car != '[') {
                ++size;
            }
        }
        Type[] args = new Type[size];
        off = 1;
        size = 0;
        while (buf[off] != ')') {
            args[size] = getType(buf, off);
            off += args[size].len + (args[size].sort == OBJECT ? 2 : 0);
            size += 1;
        }
        return args;
    }

    /**
     * 返回与给定方法的参数类型对应的Java类型.
     * 
     * @param method
     *            方法.
     * 
     * @return 与给定方法的参数类型对应的Java类型.
     */
    public static Type[] getArgumentTypes(final Method method) {
        Class<?>[] classes = method.getParameterTypes();
        Type[] types = new Type[classes.length];
        for (int i = classes.length - 1; i >= 0; --i) {
            types[i] = getType(classes[i]);
        }
        return types;
    }

    /**
     * 返回与给定方法描述符的返回类型对应的Java类型.
     * 
     * @param methodDescriptor
     *            方法描述符.
     * 
     * @return 与给定方法描述符的返回类型对应的Java类型.
     */
    public static Type getReturnType(final String methodDescriptor) {
        char[] buf = methodDescriptor.toCharArray();
        int off = 1;
        while (true) {
            char car = buf[off++];
            if (car == ')') {
                return getType(buf, off);
            } else if (car == 'L') {
                while (buf[off++] != ';') {
                }
            }
        }
    }

    /**
     * 返回与给定方法的返回类型对应的Java类型.
     * 
     * @param method
     *            方法.
     * 
     * @return 与给定方法的返回类型对应的Java类型.
     */
    public static Type getReturnType(final Method method) {
        return getType(method.getReturnType());
    }

    /**
     * 计算参数的大小和方法的返回值.
     * 
     * @param desc
     *            方法的描述符.
     * 
     * @return 方法的参数的大小 (加一, 隐含的 this 参数), argSize, 以及其返回值的大小,
     * retSize, 打包成一个 int i = <tt>(argSize &lt;&lt; 2) | retSize</tt>
     * (因此, argSize等于<tt>i &gt;&gt; 2</tt>, 而retSize等于<tt>i &amp; 0x03</tt>).
     */
    public static int getArgumentsAndReturnSizes(final String desc) {
        int n = 1;
        int c = 1;
        while (true) {
            char car = desc.charAt(c++);
            if (car == ')') {
                car = desc.charAt(c);
                return n << 2
                        | (car == 'V' ? 0 : (car == 'D' || car == 'J' ? 2 : 1));
            } else if (car == 'L') {
                while (desc.charAt(c++) != ';') {
                }
                n += 1;
            } else if (car == '[') {
                while ((car = desc.charAt(c)) == '[') {
                    ++c;
                }
                if (car == 'D' || car == 'J') {
                    n -= 1;
                }
            } else if (car == 'D' || car == 'J') {
                n += 2;
            } else {
                n += 1;
            }
        }
    }

    /**
     * 返回与给定类型描述符对应的Java类型.
     * 对于方法描述符, buf应该只包含描述符本身.
     * 
     * @param buf
     *            包含类型描述符的缓冲区.
     * @param off
     *            此描述符在前一个缓冲区中的偏移量.
     * 
     * @return 与给定类型描述符对应的Java类型.
     */
    private static Type getType(final char[] buf, final int off) {
        int len;
        switch (buf[off]) {
        case 'V':
            return VOID_TYPE;
        case 'Z':
            return BOOLEAN_TYPE;
        case 'C':
            return CHAR_TYPE;
        case 'B':
            return BYTE_TYPE;
        case 'S':
            return SHORT_TYPE;
        case 'I':
            return INT_TYPE;
        case 'F':
            return FLOAT_TYPE;
        case 'J':
            return LONG_TYPE;
        case 'D':
            return DOUBLE_TYPE;
        case '[':
            len = 1;
            while (buf[off + len] == '[') {
                ++len;
            }
            if (buf[off + len] == 'L') {
                ++len;
                while (buf[off + len] != ';') {
                    ++len;
                }
            }
            return new Type(ARRAY, buf, off, len + 1);
        case 'L':
            len = 1;
            while (buf[off + len] != ';') {
                ++len;
            }
            return new Type(OBJECT, buf, off + 1, len - 1);
            // case '(':
        default:
            return new Type(METHOD, buf, off, buf.length - off);
        }
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * 返回此Java类型的种类.
     * 
     * @return {@link #VOID VOID}, {@link #BOOLEAN BOOLEAN}, {@link #CHAR CHAR},
     *         {@link #BYTE BYTE}, {@link #SHORT SHORT}, {@link #INT INT},
     *         {@link #FLOAT FLOAT}, {@link #LONG LONG}, {@link #DOUBLE DOUBLE},
     *         {@link #ARRAY ARRAY}, {@link #OBJECT OBJECT} or {@link #METHOD
     *         METHOD}.
     */
    public int getSort() {
        return sort;
    }

    /**
     * 返回此数组类型的维数.
     * 此方法仅应用于数组类型.
     * 
     * @return 此数组类型的维数.
     */
    public int getDimensions() {
        int i = 1;
        while (buf[off + i] == '[') {
            ++i;
        }
        return i;
    }

    /**
     * 返回此数组类型的元素的类型.
     * 此方法仅应用于数组类型.
     * 
     * @return 此数组类型的元素的类型.
     */
    public Type getElementType() {
        return getType(buf, off + getDimensions());
    }

    /**
     * 返回与此类型对应的类的二进制名称.
     * 不得在方法类型上使用此方法.
     * 
     * @return 与此类型对应的类的二进制名称.
     */
    public String getClassName() {
        switch (sort) {
        case VOID:
            return "void";
        case BOOLEAN:
            return "boolean";
        case CHAR:
            return "char";
        case BYTE:
            return "byte";
        case SHORT:
            return "short";
        case INT:
            return "int";
        case FLOAT:
            return "float";
        case LONG:
            return "long";
        case DOUBLE:
            return "double";
        case ARRAY:
            StringBuilder sb = new StringBuilder(getElementType().getClassName());
            for (int i = getDimensions(); i > 0; --i) {
                sb.append("[]");
            }
            return sb.toString();
        case OBJECT:
            return new String(buf, off, len).replace('/', '.').intern();
        default:
            return null;
        }
    }

    /**
     * 返回与此对象或数组类型对应的类的内部名称.
     * 类的内部名称是其完全限定名称 (由Class.getName()返回的, 其中'.'被替换为 '/'.
     * 此方法仅应用于对象或数组类型.
     * 
     * @return 与此对象类型对应的类的内部名称.
     */
    public String getInternalName() {
        return new String(buf, off, len).intern();
    }

    /**
     * 返回此类型的方法的参数类型.
     * 此方法仅用于方法类型.
     * 
     * @return 此类型的方法的参数类型.
     */
    public Type[] getArgumentTypes() {
        return getArgumentTypes(getDescriptor());
    }

    /**
     * 返回此类型的方法的返回类型.
     * 此方法仅用于方法类型.
     * 
     * @return 此类型的方法的返回类型.
     */
    public Type getReturnType() {
        return getReturnType(getDescriptor());
    }

    /**
     * 返回此类型的方法的参数以及返回值的大小.
     * 此方法仅用于方法类型.
     * 
     * @return 参数的大小(加一, 隐含的 this 参数), argSize, 以及其返回值的大小, retSize,
     *         打包成一个int i = <tt>(argSize &lt;&lt; 2) | retSize</tt>
     *         (因此, argSize等于<tt>i &gt;&gt; 2</tt>, 而retSize等于<tt>i &amp; 0x03</tt>).
     */
    public int getArgumentsAndReturnSizes() {
        return getArgumentsAndReturnSizes(getDescriptor());
    }

    // ------------------------------------------------------------------------
    // Conversion to type descriptors
    // ------------------------------------------------------------------------

    /**
     * 返回与此Java类型对应的描述符.
     * 
     * @return 与此Java类型对应的描述符.
     */
    public String getDescriptor() {
        StringBuilder buf = new StringBuilder();
        getDescriptor(buf);
        return buf.toString();
    }

    /**
     * 返回与给定参数和返回类型对应的描述符.
     * 
     * @param returnType
     *            方法的返回类型.
     * @param argumentTypes
     *            方法的参数类型.
     * 
     * @return 对应于给定参数和返回类型的描述符.
     */
    public static String getMethodDescriptor(final Type returnType,
            final Type... argumentTypes) {
        StringBuilder buf = new StringBuilder();
        buf.append('(');
        for (int i = 0; i < argumentTypes.length; ++i) {
            argumentTypes[i].getDescriptor(buf);
        }
        buf.append(')');
        returnType.getDescriptor(buf);
        return buf.toString();
    }

    /**
     * 将与此Java类型对应的描述符附加到给定的字符串缓冲区.
     * 
     * @param buf
     *            必须附加描述符的字符串缓冲区.
     */
    private void getDescriptor(final StringBuilder buf) {
        if (this.buf == null) {
            // 对于原始类型, 描述符在'off'的字节3中 (buf == null)
            buf.append((char) ((off & 0xFF000000) >>> 24));
        } else if (sort == OBJECT) {
            buf.append('L');
            buf.append(this.buf, off, len);
            buf.append(';');
        } else { // sort == ARRAY || sort == METHOD
            buf.append(this.buf, off, len);
        }
    }

    // ------------------------------------------------------------------------
    // Direct conversion from classes to type descriptors, without intermediate Type objects
    // ------------------------------------------------------------------------

    /**
     * 返回给定类的内部名称.
     * 类的内部名称是其完全限定名称, 由Class.getName()返回, 其中'.'被替换为'/'.
     * 
     * @param c
     *            对象或数组类.
     * 
     * @return 给定类的内部名称.
     */
    public static String getInternalName(final Class<?> c) {
        return c.getName().replace('.', '/');
    }

    /**
     * 返回与给定Java类型对应的描述符.
     * 
     * @param c
     *            对象类, 原始类或数组类.
     * 
     * @return 与给定Java类型对应的描述符.
     */
    public static String getDescriptor(final Class<?> c) {
        StringBuilder buf = new StringBuilder();
        getDescriptor(buf, c);
        return buf.toString();
    }

    /**
     * 返回与给定构造函数对应的描述符.
     * 
     * @param c
     *            {@link Constructor Constructor}对象.
     * 
     * @return 给定构造函数的描述符.
     */
    public static String getConstructorDescriptor(final Constructor<?> c) {
        Class<?>[] parameters = c.getParameterTypes();
        StringBuilder buf = new StringBuilder();
        buf.append('(');
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(buf, parameters[i]);
        }
        return buf.append(")V").toString();
    }

    /**
     * 返回与给定方法对应的描述符.
     * 
     * @param m
     *            {@link Method Method}对象.
     * 
     * @return 给定方法的描述符.
     */
    public static String getMethodDescriptor(final Method m) {
        Class<?>[] parameters = m.getParameterTypes();
        StringBuilder buf = new StringBuilder();
        buf.append('(');
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(buf, parameters[i]);
        }
        buf.append(')');
        getDescriptor(buf, m.getReturnType());
        return buf.toString();
    }

    /**
     * 将给定类的描述符附加到给定的字符串缓冲区.
     * 
     * @param buf
     *            必须附加描述符的字符串缓冲区.
     * @param c
     *            必须计算其描述符的类.
     */
    private static void getDescriptor(final StringBuilder buf, final Class<?> c) {
        Class<?> d = c;
        while (true) {
            if (d.isPrimitive()) {
                char car;
                if (d == Integer.TYPE) {
                    car = 'I';
                } else if (d == Void.TYPE) {
                    car = 'V';
                } else if (d == Boolean.TYPE) {
                    car = 'Z';
                } else if (d == Byte.TYPE) {
                    car = 'B';
                } else if (d == Character.TYPE) {
                    car = 'C';
                } else if (d == Short.TYPE) {
                    car = 'S';
                } else if (d == Double.TYPE) {
                    car = 'D';
                } else if (d == Float.TYPE) {
                    car = 'F';
                } else /* if (d == Long.TYPE) */{
                    car = 'J';
                }
                buf.append(car);
                return;
            } else if (d.isArray()) {
                buf.append('[');
                d = d.getComponentType();
            } else {
                buf.append('L');
                String name = d.getName();
                int len = name.length();
                for (int i = 0; i < len; ++i) {
                    char car = name.charAt(i);
                    buf.append(car == '.' ? '/' : car);
                }
                buf.append(';');
                return;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Corresponding size and opcodes
    // ------------------------------------------------------------------------

    /**
     * 返回此类型的值的大小. 此方法不得用于方法类型.
     * 
     * @return 此类型的值的大小, i.e., 2对应<tt>long</tt> 和 <tt>double</tt>, 0 对应<tt>void</tt>, 其它为 1.
     */
    public int getSize() {
        // 对于基本类型, 大小在'off'的字节0中 (buf == null)
        return buf == null ? (off & 0xFF) : 1;
    }

    /**
     * 返回适用于此Java类型的JVM指令操作码.
     * 此方法不得用于方法类型.
     * 
     * @param opcode
     *            JVM指令操作码.
     *            此操作码必须是 ILOAD,
     *            ISTORE, IALOAD, IASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG,
     *            ISHL, ISHR, IUSHR, IAND, IOR, IXOR 和 IRETURN之一.
     * 
     * @return 与给定操作码类似的操作码, 但适用于此Java类型.
     * 例如, 如果此类型为<tt>float</tt>, 且<tt>opcode</tt>是 IRETURN, 则此方法返回FRETURN.
     */
    public int getOpcode(final int opcode) {
        if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) {
            // 对于基本类型, IALOAD或IASTORE的偏移量在'off'的字节1中 (buf == null)
            return opcode + (buf == null ? (off & 0xFF00) >> 8 : 4);
        } else {
            // 对于基本类型, 其他指令的偏移量在'off'的字节2中 (buf == null)
            return opcode + (buf == null ? (off & 0xFF0000) >> 16 : 4);
        }
    }

    // ------------------------------------------------------------------------
    // Equals, hashCode and toString
    // ------------------------------------------------------------------------

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Type)) {
            return false;
        }
        Type t = (Type) o;
        if (sort != t.sort) {
            return false;
        }
        if (sort >= ARRAY) {
            if (len != t.len) {
                return false;
            }
            for (int i = off, j = t.off, end = i + len; i < end; i++, j++) {
                if (buf[i] != t.buf[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hc = 13 * sort;
        if (sort >= ARRAY) {
            for (int i = off, end = i + len; i < end; i++) {
                hc = 17 * (hc + buf[i]);
            }
        }
        return hc;
    }

    @Override
    public String toString() {
        return getDescriptor();
    }
}
