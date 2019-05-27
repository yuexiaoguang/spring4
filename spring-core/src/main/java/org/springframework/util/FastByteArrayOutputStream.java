package org.springframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * {@link java.io.ByteArrayOutputStream}的快速替代.
 * 请注意, 此变体<i>不</i>扩展 {@code ByteArrayOutputStream}, 与其兄弟{@link ResizableByteArrayOutputStream}不同.
 *
 * <p>与{@link java.io.ByteArrayOutputStream}不同, 此实现由{@code byte[]}的{@link java.util.LinkedList}支持,
 * 而不是1不断调整{@code byte[]}的大小.
 * 它在扩展时不会复制缓冲区.
 *
 * <p>初始缓冲区仅在首次写入流时创建.
 * 如果使用{@link #writeTo(OutputStream)}方法提取内容, 也不会复制内部缓冲区.
 */
public class FastByteArrayOutputStream extends OutputStream {

	private static final int DEFAULT_BLOCK_SIZE = 256;


	// 用于存储内容字节的缓冲区
	private final LinkedList<byte[]> buffers = new LinkedList<byte[]>();

	// 分配第一个byte[]时使用的大小, 以字节为单位
	private final int initialBlockSize;

	// 分配下一个byte[]时使用的大小, 以字节为单位
	private int nextBlockSize = 0;

	// 先前缓冲区中的字节数. (当前缓冲区中的字节数是 'index'.)
	private int alreadyBufferedSize = 0;

	// 下一个要写入的buffers.getLast()中的 byte[]中的索引
	private int index = 0;

	// 流是否关闭?
	private boolean closed = false;


	/**
	 * 默认初始容量为256字节.
	 */
	public FastByteArrayOutputStream() {
		this(DEFAULT_BLOCK_SIZE);
	}

	/**
	 * @param initialBlockSize 初始缓冲区大小, 以字节为单位
	 */
	public FastByteArrayOutputStream(int initialBlockSize) {
		Assert.isTrue(initialBlockSize > 0, "Initial block size must be greater than 0");
		this.initialBlockSize = initialBlockSize;
		this.nextBlockSize = initialBlockSize;
	}


	// Overridden methods

	@Override
	public void write(int datum) throws IOException {
		if (this.closed) {
			throw new IOException("Stream closed");
		}
		else {
			if (this.buffers.peekLast() == null || this.buffers.getLast().length == this.index) {
				addBuffer(1);
			}
			// store the byte
			this.buffers.getLast()[this.index++] = (byte) datum;
		}
	}

	@Override
	public void write(byte[] data, int offset, int length) throws IOException {
		if (data == null) {
			throw new NullPointerException();
		}
		else if (offset < 0 || offset + length > data.length || length < 0) {
			throw new IndexOutOfBoundsException();
		}
		else if (this.closed) {
			throw new IOException("Stream closed");
		}
		else {
			if (this.buffers.peekLast() == null || this.buffers.getLast().length == this.index) {
				addBuffer(length);
			}
			if (this.index + length > this.buffers.getLast().length) {
				int pos = offset;
				do {
					if (this.index == this.buffers.getLast().length) {
						addBuffer(length);
					}
					int copyLength = this.buffers.getLast().length - this.index;
					if (length < copyLength) {
						copyLength = length;
					}
					System.arraycopy(data, pos, this.buffers.getLast(), this.index, copyLength);
					pos += copyLength;
					this.index += copyLength;
					length -= copyLength;
				}
				while (length > 0);
			}
			else {
				// copy in the sub-array
				System.arraycopy(data, offset, this.buffers.getLast(), this.index, length);
				this.index += length;
			}
		}
	}

	@Override
	public void close() {
		this.closed = true;
	}

	/**
	 * 使用平台的默认字符集, 将缓冲区的内容转换为字符串解码字节.
	 * 新<tt>String</tt>的长度是字符集的函数, 因此可能不等于缓冲区的大小.
	 * <p>此方法始终使用平台默认字符集的默认替换字符串, 替换格式错误的输入和不可映射字符序列.
	 * 当需要更多地控制解码过程时, 应该使用{@linkplain java.nio.charset.CharsetDecoder}类.
	 * 
	 * @return 从缓冲区内容解码的字符串
	 */
	@Override
	public String toString() {
		return new String(toByteArrayUnsafe());
	}


	// Custom methods

	/**
	 * 返回此<code>FastByteArrayOutputStream</code>中存储的字节数.
	 */
	public int size() {
		return (this.alreadyBufferedSize + this.index);
	}

	/**
	 * 将流的数据转换为字节数组, 并返回字节数组.
	 * <p>还用字节数组替换内部结构以节省内存:
	 * 如果正在制作字节数组, 请记住并使用它.
	 * 这种方法也意味着如果此方法被调用两次而中间没有任何写入, 则第二次调用不执行任何操作.
	 * <p>此方法"不安全", 因为它返回内部缓冲区. 调用者不应修改返回的缓冲区.
	 * 
	 * @return 此输出流的当前内容, 作为字节数组.
	 */
	public byte[] toByteArrayUnsafe() {
		int totalSize = size();
		if (totalSize == 0) {
			return new byte[0];
		}
		resize(totalSize);
		return this.buffers.getFirst();
	}

	/**
	 * 创建一个新分配的字节数组.
	 * <p>它的大小是此输出流的当前大小, 且缓冲区的有效内容已复制到其中.</p>
	 * 
	 * @return 此输出流的当前内容.
	 */
	public byte[] toByteArray() {
		byte[] bytesUnsafe = toByteArrayUnsafe();
		byte[] ret = new byte[bytesUnsafe.length];
		System.arraycopy(bytesUnsafe, 0, ret, 0, bytesUnsafe.length);
		return ret;
	}

	/**
	 * 重置此<code>FastByteArrayOutputStream</code>的内容.
	 * <p>输出流中当前累积的所有输出都将被丢弃.
	 * 输出流可以再次使用.
	 */
	public void reset() {
		this.buffers.clear();
		this.nextBlockSize = this.initialBlockSize;
		this.closed = false;
		this.index = 0;
		this.alreadyBufferedSize = 0;
	}

	/**
	 * 获取{@link InputStream}以检索此OutputStream中的数据.
	 * <p>请注意, 如果在OutputStream上调用任何方法
	 * (包括但不限于任何写入方法, {@link #reset()}, {@link #toByteArray()}, 和{@link #toByteArrayUnsafe()}),
	 * 然后{@link java.io.InputStream}的行为是未定义的.
	 * 
	 * @return 此OutputStream内容的{@link InputStream}
	 */
	public InputStream getInputStream() {
		return new FastByteArrayInputStream(this);
	}

	/**
	 * 将缓冲区内容写入给定的OutputStream.
	 * 
	 * @param out 要写入的OutputStream
	 */
	public void writeTo(OutputStream out) throws IOException {
		Iterator<byte[]> it = this.buffers.iterator();
		while (it.hasNext()) {
			byte[] bytes = it.next();
			if (it.hasNext()) {
				out.write(bytes, 0, bytes.length);
			}
			else {
				out.write(bytes, 0, this.index);
			}
		}
	}

	/**
	 * 将内部缓冲区大小调整为指定容量.
	 * 
	 * @param targetCapacity 所需的缓冲区大小
	 * 
	 * @throws IllegalArgumentException 如果给定容量小于缓冲区中已存储的内容的实际大小
	 */
	public void resize(int targetCapacity) {
		Assert.isTrue(targetCapacity >= size(), "New capacity must not be smaller than current size");
		if (this.buffers.peekFirst() == null) {
			this.nextBlockSize = targetCapacity - size();
		}
		else if (size() == targetCapacity && this.buffers.getFirst().length == targetCapacity) {
			// do nothing - already at the targetCapacity
		}
		else {
			int totalSize = size();
			byte[] data = new byte[targetCapacity];
			int pos = 0;
			Iterator<byte[]> it = this.buffers.iterator();
			while (it.hasNext()) {
				byte[] bytes = it.next();
				if (it.hasNext()) {
					System.arraycopy(bytes, 0, data, pos, bytes.length);
					pos += bytes.length;
				}
				else {
					System.arraycopy(bytes, 0, data, pos, this.index);
				}
			}
			this.buffers.clear();
			this.buffers.add(data);
			this.index = totalSize;
			this.alreadyBufferedSize = 0;
		}
	}

	/**
	 * 创建一个新缓冲区并将其存储在LinkedList中
	 * <p>添加一个可以存储至少{@code minCapacity}字节的新缓冲区.
	 */
	private void addBuffer(int minCapacity) {
		if (this.buffers.peekLast() != null) {
			this.alreadyBufferedSize += this.index;
			this.index = 0;
		}
		if (this.nextBlockSize < minCapacity) {
			this.nextBlockSize = nextPowerOf2(minCapacity);
		}
		this.buffers.add(new byte[this.nextBlockSize]);
		this.nextBlockSize *= 2;  // block size doubles each time
	}

	/**
	 * 获得2的下一个幂 (例如, 119的2的下一个幂是128).
	 */
	private static int nextPowerOf2(int val) {
		val--;
		val = (val >> 1) | val;
		val = (val >> 2) | val;
		val = (val >> 4) | val;
		val = (val >> 8) | val;
		val = (val >> 16) | val;
		val++;
		return val;
	}


	/**
	 * {@link java.io.InputStream}的一个实现, 它从给定的<code>FastByteArrayOutputStream</code>读取.
	 */
	private static final class FastByteArrayInputStream extends UpdateMessageDigestInputStream {

		private final FastByteArrayOutputStream fastByteArrayOutputStream;

		private final Iterator<byte[]> buffersIterator;

		private byte[] currentBuffer;

		private int currentBufferLength = 0;

		private int nextIndexInCurrentBuffer = 0;

		private int totalBytesRead = 0;

		/**
		 * 创建由给定<code>FastByteArrayOutputStream</code>支持的新<code>FastByteArrayOutputStreamInputStream</code>.
		 */
		public FastByteArrayInputStream(FastByteArrayOutputStream fastByteArrayOutputStream) {
			this.fastByteArrayOutputStream = fastByteArrayOutputStream;
			this.buffersIterator = fastByteArrayOutputStream.buffers.iterator();
			if (this.buffersIterator.hasNext()) {
				this.currentBuffer = this.buffersIterator.next();
				if (this.currentBuffer == fastByteArrayOutputStream.buffers.getLast()) {
					this.currentBufferLength = fastByteArrayOutputStream.index;
				}
				else {
					this.currentBufferLength = this.currentBuffer.length;
				}
			}
		}

		@Override
		public int read() {
			if (this.currentBuffer == null) {
				// This stream doesn't have any data in it...
				return -1;
			}
			else {
				if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
					this.totalBytesRead++;
					return this.currentBuffer[this.nextIndexInCurrentBuffer++];
				}
				else {
					if (this.buffersIterator.hasNext()) {
						this.currentBuffer = this.buffersIterator.next();
						if (this.currentBuffer == this.fastByteArrayOutputStream.buffers.getLast()) {
							this.currentBufferLength = this.fastByteArrayOutputStream.index;
						}
						else {
							this.currentBufferLength = this.currentBuffer.length;
						}
						this.nextIndexInCurrentBuffer = 0;
					}
					else {
						this.currentBuffer = null;
					}
					return read();
				}
			}
		}

		@Override
		public int read(byte[] b) {
			return read(b, 0, b.length);
		}

		@Override
		public int read(byte[] b, int off, int len) {
			if (b == null) {
				throw new NullPointerException();
			}
			else if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException();
			}
			else if (len == 0) {
				return 0;
			}
			else {
				if (this.currentBuffer == null) {
					// This stream doesn't have any data in it...
					return -1;
				}
				else {
					if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
						int bytesToCopy = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
						System.arraycopy(this.currentBuffer, this.nextIndexInCurrentBuffer, b, off, bytesToCopy);
						this.totalBytesRead += bytesToCopy;
						this.nextIndexInCurrentBuffer += bytesToCopy;
						int remaining = read(b, off + bytesToCopy, len - bytesToCopy);
						return bytesToCopy + Math.max(remaining, 0);
					}
					else {
						if (this.buffersIterator.hasNext()) {
							this.currentBuffer = this.buffersIterator.next();
							if (this.currentBuffer == this.fastByteArrayOutputStream.buffers.getLast()) {
								this.currentBufferLength = this.fastByteArrayOutputStream.index;
							}
							else {
								this.currentBufferLength = this.currentBuffer.length;
							}
							this.nextIndexInCurrentBuffer = 0;
						}
						else {
							this.currentBuffer = null;
						}
						return read(b, off, len);
					}
				}
			}
		}

		@Override
		public long skip(long n) throws IOException {
			if (n > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("n exceeds maximum (" + Integer.MAX_VALUE + "): " + n);
			}
			else if (n == 0) {
				return 0;
			}
			else if (n < 0) {
				throw new IllegalArgumentException("n must be 0 or greater: " + n);
			}
			int len = (int) n;
			if (this.currentBuffer == null) {
				// This stream doesn't have any data in it...
				return 0;
			}
			else {
				if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
					int bytesToSkip = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
					this.totalBytesRead += bytesToSkip;
					this.nextIndexInCurrentBuffer += bytesToSkip;
					return (bytesToSkip + skip(len - bytesToSkip));
				}
				else {
					if (this.buffersIterator.hasNext()) {
						this.currentBuffer = this.buffersIterator.next();
						if (this.currentBuffer == this.fastByteArrayOutputStream.buffers.getLast()) {
							this.currentBufferLength = this.fastByteArrayOutputStream.index;
						}
						else {
							this.currentBufferLength = this.currentBuffer.length;
						}
						this.nextIndexInCurrentBuffer = 0;
					}
					else {
						this.currentBuffer = null;
					}
					return skip(len);
				}
			}
		}

		@Override
		public int available() {
			return (this.fastByteArrayOutputStream.size() - this.totalBytesRead);
		}

		/**
		 * 使用此流中的剩余字节更新消息摘要.
		 * 
		 * @param messageDigest 要更新的消息摘要
		 */
		@Override
		public void updateMessageDigest(MessageDigest messageDigest) {
			updateMessageDigest(messageDigest, available());
		}

		/**
		 * 使用此流中的下一个len字节更新消息摘要.
		 * 避免创建新的字节数组, 并使用内部缓冲区来提高性能.
		 * 
		 * @param messageDigest 要更新的消息摘要
		 * @param len 从此流中读取多少字节, 并用于更新消息摘要
		 */
		@Override
		public void updateMessageDigest(MessageDigest messageDigest, int len) {
			if (this.currentBuffer == null) {
				// This stream doesn't have any data in it...
				return;
			}
			else if (len == 0) {
				return;
			}
			else if (len < 0) {
				throw new IllegalArgumentException("len must be 0 or greater: " + len);
			}
			else {
				if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
					int bytesToCopy = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
					messageDigest.update(this.currentBuffer, this.nextIndexInCurrentBuffer, bytesToCopy);
					this.nextIndexInCurrentBuffer += bytesToCopy;
					updateMessageDigest(messageDigest, len - bytesToCopy);
				}
				else {
					if (this.buffersIterator.hasNext()) {
						this.currentBuffer = this.buffersIterator.next();
						if (this.currentBuffer == this.fastByteArrayOutputStream.buffers.getLast()) {
							this.currentBufferLength = this.fastByteArrayOutputStream.index;
						}
						else {
							this.currentBufferLength = this.currentBuffer.length;
						}
						this.nextIndexInCurrentBuffer = 0;
					}
					else {
						this.currentBuffer = null;
					}
					updateMessageDigest(messageDigest, len);
				}
			}
		}
	}
}
