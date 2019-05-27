package org.springframework.util;

import java.io.ByteArrayOutputStream;

/**
 * {@link java.io.ByteArrayOutputStream}的扩展:
 * <ul>
 * <li>公共{@link org.springframework.util.ResizableByteArrayOutputStream#grow(int)}
 * 和{@link org.springframework.util.ResizableByteArrayOutputStream#resize(int)}方法
 * 来更好地控制内部缓冲区的大小</li>
 * <li>默认情况下具有更高的初始容量 (256)</li>
 * </ul>
 *
 * <p>从4.2开始, 这个类已被{@link FastByteArrayOutputStream}取代, 用于Spring的内部使用,
 * 不需要对{@link ByteArrayOutputStream}进行赋值
 * (因为{@link FastByteArrayOutputStream}在缓冲区大小调整管理方面效率更高, 但不扩展标准{@link ByteArrayOutputStream}).
 */
public class ResizableByteArrayOutputStream extends ByteArrayOutputStream {

	private static final int DEFAULT_INITIAL_CAPACITY = 256;


	/**
	 * 默认初始容量为256字节.
	 */
	public ResizableByteArrayOutputStream() {
		super(DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * @param initialCapacity 初始缓冲区大小, 以字节为单位
	 */
	public ResizableByteArrayOutputStream(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * 将内部缓冲区大小调整为指定容量.
	 * 
	 * @param targetCapacity 所需的缓冲区大小
	 * 
	 * @throws IllegalArgumentException 如果给定容量小于缓冲区中已存储的内容的实际大小
	 */
	public synchronized void resize(int targetCapacity) {
		Assert.isTrue(targetCapacity >= this.count, "New capacity must not be smaller than current size");
		byte[] resizedBuffer = new byte[targetCapacity];
		System.arraycopy(this.buf, 0, resizedBuffer, 0, this.count);
		this.buf = resizedBuffer;
	}

	/**
	 * 增加内部缓冲区大小.
	 * 
	 * @param additionalCapacity 要添加到当前缓冲区大小的字节数
	 */
	public synchronized void grow(int additionalCapacity) {
		Assert.isTrue(additionalCapacity >= 0, "Additional capacity must be 0 or higher");
		if (this.count + additionalCapacity > this.buf.length) {
			int newCapacity = Math.max(this.buf.length * 2, this.count + additionalCapacity);
			resize(newCapacity);
		}
	}

	/**
	 * 返回此流的内部缓冲区的当前大小.
	 */
	public synchronized int capacity() {
		return this.buf.length;
	}
}
