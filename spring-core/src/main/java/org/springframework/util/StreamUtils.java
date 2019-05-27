package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * 处理流的简单实用方法.
 * 此类的复制方法类似于{@link FileCopyUtils}中定义的复制方法, 不同的是所有受影响的流在完成后保持打开状态.
 * 所有复制方法都使用4096字节的块大小.
 *
 * <p>主要用于框架内, 也适用于应用程序代码.
 */
public abstract class StreamUtils {

	public static final int BUFFER_SIZE = 4096;

	private static final byte[] EMPTY_CONTENT = new byte[0];


	/**
	 * 将给定InputStream的内容复制到新的字节数组中.
	 * 完成后保持流打开状态.
	 * 
	 * @param in 要从中复制的流 (可能是{@code null}或为空)
	 * 
	 * @return 已复制到的新字节数组 (可能为空)
	 * @throws IOException 发生I/O错误
	 */
	public static byte[] copyToByteArray(InputStream in) throws IOException {
		if (in == null) {
			return new byte[0];
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
		copy(in, out);
		return out.toByteArray();
	}

	/**
	 * 将给定InputStream的内容复制到String中.
	 * 完成后保持流打开状态.
	 * 
	 * @param in 要从中复制的流 (可能是{@code null}或为空)
	 * 
	 * @return 已复制到的字符串 (可能为空)
	 * @throws IOException 发生I/O错误
	 */
	public static String copyToString(InputStream in, Charset charset) throws IOException {
		if (in == null) {
			return "";
		}

		StringBuilder out = new StringBuilder();
		InputStreamReader reader = new InputStreamReader(in, charset);
		char[] buffer = new char[BUFFER_SIZE];
		int bytesRead = -1;
		while ((bytesRead = reader.read(buffer)) != -1) {
			out.append(buffer, 0, bytesRead);
		}
		return out.toString();
	}

	/**
	 * 将给定字节数组的内容复制到给定的OutputStream.
	 * 完成后保持流打开状态.
	 * 
	 * @param in 要从中复制的字节数组
	 * @param out 要复制到的OutputStream
	 * 
	 * @throws IOException 发生I/O错误
	 */
	public static void copy(byte[] in, OutputStream out) throws IOException {
		Assert.notNull(in, "No input byte array specified");
		Assert.notNull(out, "No OutputStream specified");

		out.write(in);
	}

	/**
	 * 将给定String的内容复制到给定的输出OutputStream.
	 * 完成后保持流打开状态.
	 * 
	 * @param in 要复制的字符串
	 * @param charset 字符集
	 * @param out 要复制到的OutputStream
	 * 
	 * @throws IOException 发生I/O错误
	 */
	public static void copy(String in, Charset charset, OutputStream out) throws IOException {
		Assert.notNull(in, "No input String specified");
		Assert.notNull(charset, "No charset specified");
		Assert.notNull(out, "No OutputStream specified");

		Writer writer = new OutputStreamWriter(out, charset);
		writer.write(in);
		writer.flush();
	}

	/**
	 * 将给定InputStream的内容复制到给定的OutputStream.
	 * 完成后保持流打开状态.
	 * 
	 * @param in 要从中复制的InputStream
	 * @param out 要复制到的OutputStream
	 * 
	 * @return 复制的字节数
	 * @throws IOException 发生I/O错误
	 */
	public static int copy(InputStream in, OutputStream out) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		Assert.notNull(out, "No OutputStream specified");

		int byteCount = 0;
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = -1;
		while ((bytesRead = in.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
			byteCount += bytesRead;
		}
		out.flush();
		return byteCount;
	}

	/**
	 * 将给定InputStream的部分内容复制到给定的OutputStream.
	 * <p>如果指定的范围超过InputStream的长度, 则复制到流的末尾, 并返回实际的复制字节数.
	 * <p>完成后保持流打开状态.
	 * 
	 * @param in 要从中复制的InputStream
	 * @param out 要复制到的OutputStream
	 * @param start 从中开始复制的位置
	 * @param end 结束复制的位置
	 * 
	 * @return 复制的字节数
	 * @throws IOException 发生I/O错误
	 */
	public static long copyRange(InputStream in, OutputStream out, long start, long end) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		Assert.notNull(out, "No OutputStream specified");

		long skipped = in.skip(start);
		if (skipped < start) {
			throw new IOException("Skipped only " + skipped + " bytes out of " + start + " required");
		}

		long bytesToCopy = end - start + 1;
		byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];
		while (bytesToCopy > 0) {
			int bytesRead = in.read(buffer);
			if (bytesRead == -1) {
				break;
			}
			else if (bytesRead <= bytesToCopy) {
				out.write(buffer, 0, bytesRead);
				bytesToCopy -= bytesRead;
			}
			else {
				out.write(buffer, 0, (int) bytesToCopy);
				bytesToCopy = 0;
			}
		}
		return (end - start + 1 - bytesToCopy);
	}

	/**
	 * 排干给定InputStream的剩余内容.
	 * 完成后保持InputStream打开状态.
	 * 
	 * @param in 要排干的InputStream
	 * 
	 * @return 要读取的字节数
	 * @throws IOException 发生I/O错误
	 */
	public static int drain(InputStream in) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = -1;
		int byteCount = 0;
		while ((bytesRead = in.read(buffer)) != -1) {
			byteCount += bytesRead;
		}
		return byteCount;
	}

	/**
	 * 返回一个高效的空{@link InputStream}.
	 * 
	 * @return 基于空字节数组的{@link ByteArrayInputStream}
	 */
	public static InputStream emptyInput() {
		return new ByteArrayInputStream(EMPTY_CONTENT);
	}

	/**
	 * 返回给定{@link InputStream}的变体, 其中调用{@link InputStream#close() close()}无效.
	 * 
	 * @param in 要装饰的InputStream
	 * 
	 * @return 一个忽略了关闭调用的InputStream的版本
	 */
	public static InputStream nonClosing(InputStream in) {
		Assert.notNull(in, "No InputStream specified");
		return new NonClosingInputStream(in);
	}

	/**
	 * 返回给定{@link OutputStream}的变体, 其中调用{@link OutputStream#close() close()}无效.
	 * 
	 * @param out 要装饰的OutputStream
	 * 
	 * @return 一个忽略了关闭调用的OutputStream的版本
	 */
	public static OutputStream nonClosing(OutputStream out) {
		Assert.notNull(out, "No OutputStream specified");
		return new NonClosingOutputStream(out);
	}


	private static class NonClosingInputStream extends FilterInputStream {

		public NonClosingInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() throws IOException {
		}
	}


	private static class NonClosingOutputStream extends FilterOutputStream {

		public NonClosingOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(byte[] b, int off, int let) throws IOException {
			// 必须覆盖此方法以提高性能
			out.write(b, off, let);
		}

		@Override
		public void close() throws IOException {
		}
	}

}
