package org.springframework.asm;

/**
 * 动态可扩展的字节向量.
 * 此类大致相当于ByteArrayOutputStream之上的DataOutputStream, 但效率更高.
 */
public class ByteVector {

    /**
     * 此向量的内容.
     */
    byte[] data;

    /**
     * 此向量中的实际字节数.
     */
    int length;

    /**
     * 使用默认的初始大小.
     */
    public ByteVector() {
        data = new byte[64];
    }

    /**
     * 使用给定的初始大小.
     * 
     * @param initialSize
     *            要构造的字节向量的初始大小.
     */
    public ByteVector(final int initialSize) {
        data = new byte[initialSize];
    }

    /**
     * 将一个字节放入此字节向量中. 如有必要, 字节向量会自动扩大.
     * 
     * @param b
     *            字节.
     * 
     * @return 此字节向量.
     */
    public ByteVector putByte(final int b) {
        int length = this.length;
        if (length + 1 > data.length) {
            enlarge(1);
        }
        data[length++] = (byte) b;
        this.length = length;
        return this;
    }

    /**
     * 将两个字节放入此字节向量中. 如有必要, 字节向量会自动扩大.
     * 
     * @param b1
     *            字节.
     * @param b2
     *            字节.
     * 
     * @return 此字节向量.
     */
    ByteVector put11(final int b1, final int b2) {
        int length = this.length;
        if (length + 2 > data.length) {
            enlarge(2);
        }
        byte[] data = this.data;
        data[length++] = (byte) b1;
        data[length++] = (byte) b2;
        this.length = length;
        return this;
    }

    /**
     * 在这个字节向量中放入一个short. 如有必要, 字节向量会自动扩大.
     * 
     * @param s
     *            a short.
     * 
     * @return 此字节向量.
     */
    public ByteVector putShort(final int s) {
        int length = this.length;
        if (length + 2 > data.length) {
            enlarge(2);
        }
        byte[] data = this.data;
        data[length++] = (byte) (s >>> 8);
        data[length++] = (byte) s;
        this.length = length;
        return this;
    }

    /**
     * 将一个byte 和一个 short放入此字节向量中. 如有必要, 字节向量会自动扩大.
     * 
     * @param b
     *            a byte.
     * @param s
     *            a short.
     * 
     * @return 此字节向量.
     */
    ByteVector put12(final int b, final int s) {
        int length = this.length;
        if (length + 3 > data.length) {
            enlarge(3);
        }
        byte[] data = this.data;
        data[length++] = (byte) b;
        data[length++] = (byte) (s >>> 8);
        data[length++] = (byte) s;
        this.length = length;
        return this;
    }

    /**
     * 将一个int 放入此字节向量中. 如有必要, 字节向量会自动扩大.
     * 
     * @param i
     *            an int.
     * 
     * @return 此字节向量.
     */
    public ByteVector putInt(final int i) {
        int length = this.length;
        if (length + 4 > data.length) {
            enlarge(4);
        }
        byte[] data = this.data;
        data[length++] = (byte) (i >>> 24);
        data[length++] = (byte) (i >>> 16);
        data[length++] = (byte) (i >>> 8);
        data[length++] = (byte) i;
        this.length = length;
        return this;
    }

    /**
     * 将一个long放入此字节向量中. 如有必要, 字节向量会自动扩大.
     * 
     * @param l
     *            a long.
     * 
     * @return 此字节向量.
     */
    public ByteVector putLong(final long l) {
        int length = this.length;
        if (length + 8 > data.length) {
            enlarge(8);
        }
        byte[] data = this.data;
        int i = (int) (l >>> 32);
        data[length++] = (byte) (i >>> 24);
        data[length++] = (byte) (i >>> 16);
        data[length++] = (byte) (i >>> 8);
        data[length++] = (byte) i;
        i = (int) l;
        data[length++] = (byte) (i >>> 24);
        data[length++] = (byte) (i >>> 16);
        data[length++] = (byte) (i >>> 8);
        data[length++] = (byte) i;
        this.length = length;
        return this;
    }

    /**
     * 将一个UTF8 string 放入此字节向量中. 如有必要, 字节向量会自动扩大.
     * 
     * @param s
     *            一个字符串, 其UTF8编码长度必须小于65536.
     * 
     * @return 此字节向量.
     */
    public ByteVector putUTF8(final String s) {
        int charLength = s.length();
        if (charLength > 65535) {
            throw new IllegalArgumentException();
        }
        int len = length;
        if (len + 2 + charLength > data.length) {
            enlarge(2 + charLength);
        }
        byte[] data = this.data;
        // 乐观算法:
        // 不再计算字节长度, 然后序列化字符串 (这需要两个循环),
        // 假设字节长度等于char长度 (这是最常见的情况), 然后立即开始序列化字符串.
        // 在序列化过程中, 如果发现这个假设是错误的, 继续使用通用方法.
        data[len++] = (byte) (charLength >>> 8);
        data[len++] = (byte) charLength;
        for (int i = 0; i < charLength; ++i) {
            char c = s.charAt(i);
            if (c >= '\001' && c <= '\177') {
                data[len++] = (byte) c;
            } else {
                length = len;
                return encodeUTF8(s, i, 65535);
            }
        }
        length = len;
        return this;
    }

    /**
     * 将UTF8字符串放入此字节向量中. 如有必要, 字节向量会自动扩大.
     * 字符串长度在编码字符之前以两个字节编码, 如果有空间的话 (i.e. 如果this.length - i - 2 >= 0).
     * 
     * @param s
     *            要编码的字符串.
     * @param i
     *            要编码的第一个字符的索引.
     *            假设先前的字符已被编码, 每个字符仅使用一个字节.
     * @param maxByteLength
     *            编码字符串的最大字节长度, 包括已编码的字符.
     * 
     * @return 此字节向量.
     */
    ByteVector encodeUTF8(final String s, int i, int maxByteLength) {
        int charLength = s.length();
        int byteLength = i;
        char c;
        for (int j = i; j < charLength; ++j) {
            c = s.charAt(j);
            if (c >= '\001' && c <= '\177') {
                byteLength++;
            } else if (c > '\u07FF') {
                byteLength += 3;
            } else {
                byteLength += 2;
            }
        }
        if (byteLength > maxByteLength) {
            throw new IllegalArgumentException();
        }
        int start = length - i - 2;
        if (start >= 0) {
          data[start] = (byte) (byteLength >>> 8);
          data[start + 1] = (byte) byteLength;
        }
        if (length + byteLength - i > data.length) {
            enlarge(byteLength - i);
        }
        int len = length;
        for (int j = i; j < charLength; ++j) {
            c = s.charAt(j);
            if (c >= '\001' && c <= '\177') {
                data[len++] = (byte) c;
            } else if (c > '\u07FF') {
                data[len++] = (byte) (0xE0 | c >> 12 & 0xF);
                data[len++] = (byte) (0x80 | c >> 6 & 0x3F);
                data[len++] = (byte) (0x80 | c & 0x3F);
            } else {
                data[len++] = (byte) (0xC0 | c >> 6 & 0x1F);
                data[len++] = (byte) (0x80 | c & 0x3F);
            }
        }
        length = len;
        return this;
    }

    /**
     * 将一个字节数组放入此字节向量中. 如有必要, 字节向量会自动扩大.
     * 
     * @param b
     *            字节数组. 可以是<tt>null</tt>, 将<tt>len</tt> null字节放入此字节向量.
     * @param off
     *            必须复制的b的第一个字节的索引.
     * @param len
     *            必须复制的b的字节数.
     * 
     * @return 此字节向量.
     */
    public ByteVector putByteArray(final byte[] b, final int off, final int len) {
        if (length + len > data.length) {
            enlarge(len);
        }
        if (b != null) {
            System.arraycopy(b, off, data, length, len);
        }
        length += len;
        return this;
    }

    /**
     * 扩大此字节向量, 以便它可以接收n个更多字节.
     * 
     * @param size
     *            此字节向量应能够接收的额外字节数.
     */
    private void enlarge(final int size) {
        int length1 = 2 * data.length;
        int length2 = length + size;
        byte[] newData = new byte[length1 > length2 ? length1 : length2];
        System.arraycopy(data, 0, newData, 0, length);
        data = newData;
    }
}
