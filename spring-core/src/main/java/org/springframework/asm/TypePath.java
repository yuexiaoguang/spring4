package org.springframework.asm;

/**
 * 封闭类型中类型参数, 通配符绑定, 数组元素类型, 或静态内部类型的路径.
 */
public class TypePath {

    /**
     * 进入数组类型的元素类型的类型路径步骤.
     * See {@link #getStep getStep}.
     */
    public final static int ARRAY_ELEMENT = 0;

    /**
     * 进入类的类型的嵌套类型的类型路径步骤.
     * See {@link #getStep getStep}.
     */
    public final static int INNER_TYPE = 1;

    /**
     * 进入通配符类型边界的类型路径步骤.
     * See {@link #getStep getStep}.
     */
    public final static int WILDCARD_BOUND = 2;

    /**
     * 进入泛型类型的类型参数的类型路径步骤.
     * See {@link #getStep getStep}.
     */
    public final static int TYPE_ARGUMENT = 3;

    /**
     * 存储路径的字节数组, 采用Java类文件格式.
     */
    byte[] b;

    /**
     * 'b'中类型路径的第一个字节的偏移量.
     */
    int offset;

    /**
     * @param b
     *            包含Java类文件格式的类型路径的字节数组.
     * @param offset
     *            'b'中类型路径的第一个字节的偏移量.
     */
    TypePath(byte[] b, int offset) {
        this.b = b;
        this.offset = offset;
    }

    /**
     * 返回此路径的长度.
     * 
     * @return 路径的长度.
     */
    public int getLength() {
        return b[offset];
    }

    /**
     * 返回此路径的给定步骤的值.
     * 
     * @param index
     *            0到{@link #getLength()}之间的索引, 不包括.
     * @return {@link #ARRAY_ELEMENT ARRAY_ELEMENT}, {@link #INNER_TYPE
     *         INNER_TYPE}, {@link #WILDCARD_BOUND WILDCARD_BOUND},或
     *         {@link #TYPE_ARGUMENT TYPE_ARGUMENT}.
     */
    public int getStep(int index) {
        return b[offset + 2 * index + 1];
    }

    /**
     * 返回给定步骤插入的类型参数的索引.
     * 此方法仅应用于值为{@link #TYPE_ARGUMENT TYPE_ARGUMENT}的步骤.
     * 
     * @param index
     *            0到{@link #getLength()}之间的索引, 不包括.
     * 
     * @return 给定步骤进入的类型参数的索引.
     */
    public int getStepArgument(int index) {
        return b[offset + 2 * index + 2];
    }

    /**
     * 将字符串形式的类型路径, {@link #toString()}使用的格式, 转换为TypePath对象.
     * 
     * @param typePath
     *            字符串形式的类型路径, {@link #toString()}使用的格式. May be null or empty.
     * 
     * @return 相应的TypePath对象, 或null 如果路径为空.
     */
    public static TypePath fromString(final String typePath) {
        if (typePath == null || typePath.length() == 0) {
            return null;
        }
        int n = typePath.length();
        ByteVector out = new ByteVector(n);
        out.putByte(0);
        for (int i = 0; i < n;) {
            char c = typePath.charAt(i++);
            if (c == '[') {
                out.put11(ARRAY_ELEMENT, 0);
            } else if (c == '.') {
                out.put11(INNER_TYPE, 0);
            } else if (c == '*') {
                out.put11(WILDCARD_BOUND, 0);
            } else if (c >= '0' && c <= '9') {
                int typeArg = c - '0';
                while (i < n && (c = typePath.charAt(i)) >= '0' && c <= '9') {
                    typeArg = typeArg * 10 + c - '0';
                    i += 1;
                }
                if (i < n && typePath.charAt(i) == ';') {
                    i += 1;
                }
                out.put11(TYPE_ARGUMENT, typeArg);
            }
        }
        out.data[0] = (byte) (out.length / 2);
        return new TypePath(out.data, 0);
    }

    /**
     * 返回此类型路径的字符串表示形式.
     * {@link #ARRAY_ELEMENT ARRAY_ELEMENT}步骤以'['表示, {@link #INNER_TYPE INNER_TYPE}步骤以'.'表示,
     * {@link #WILDCARD_BOUND WILDCARD_BOUND}步骤以'*'表示,
     * {@link #TYPE_ARGUMENT TYPE_ARGUMENT}步骤及其十进制形式的类型参数索引, 后跟';'.
     */
    @Override
    public String toString() {
        int length = getLength();
        StringBuilder result = new StringBuilder(length * 2);
        for (int i = 0; i < length; ++i) {
            switch (getStep(i)) {
            case ARRAY_ELEMENT:
                result.append('[');
                break;
            case INNER_TYPE:
                result.append('.');
                break;
            case WILDCARD_BOUND:
                result.append('*');
                break;
            case TYPE_ARGUMENT:
                result.append(getStepArgument(i)).append(';');
                break;
            default:
                result.append('_');
            }
        }
        return result.toString();
    }
}
