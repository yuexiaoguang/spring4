package org.springframework.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * 用于文件和流复制的简单实用方法.
 * 所有复制方法都使用4096字节的块大小, 并在完成后关闭所有受影响的流.
 * 可以在{@link StreamUtils}中找到此类中用于打开流的复制方法的变体.
 *
 * <p>主要用于框架内, 也适用于应用程序代码.
 */
public abstract class FileCopyUtils {

	public static final int BUFFER_SIZE = StreamUtils.BUFFER_SIZE;


	//---------------------------------------------------------------------
	// Copy methods for java.io.File
	//---------------------------------------------------------------------

	/**
	 * 将给定输入文件的内容复制到给定的输出文件.
	 * 
	 * @param in 要从中复制的文件
	 * @param out 要复制到的文件
	 * 
	 * @return 要复制的字节数
	 * @throws IOException 发生I/O错误
	 */
	public static int copy(File in, File out) throws IOException {
		Assert.notNull(in, "No input File specified");
		Assert.notNull(out, "No output File specified");

		return copy(new BufferedInputStream(new FileInputStream(in)),
				new BufferedOutputStream(new FileOutputStream(out)));
	}

	/**
	 * 将给定字节数组的内容复制到给定的输出文件.
	 * 
	 * @param in 要从中复制的字节数组
	 * @param out 要复制到的文件
	 * 
	 * @throws IOException 发生I/O错误
	 */
	public static void copy(byte[] in, File out) throws IOException {
		Assert.notNull(in, "No input byte array specified");
		Assert.notNull(out, "No output File specified");

		ByteArrayInputStream inStream = new ByteArrayInputStream(in);
		OutputStream outStream = new BufferedOutputStream(new FileOutputStream(out));
		copy(inStream, outStream);
	}

	/**
	 * 将给定输入文件的内容复制到新的字节数组中.
	 * 
	 * @param in 要从中复制的文件
	 * 
	 * @return 已复制到的新字节数组
	 * @throws IOException 发生I/O错误
	 */
	public static byte[] copyToByteArray(File in) throws IOException {
		Assert.notNull(in, "No input File specified");

		return copyToByteArray(new BufferedInputStream(new FileInputStream(in)));
	}


	//---------------------------------------------------------------------
	// Copy methods for java.io.InputStream / java.io.OutputStream
	//---------------------------------------------------------------------

	/**
	 * 将给定InputStream的内容复制到给定的OutputStream.
	 * 完成后关闭两个流.
	 * 
	 * @param in 要从中复制的流
	 * @param out 要复制到的流
	 * 
	 * @return 要复制的字节数
	 * @throws IOException 发生I/O错误
	 */
	public static int copy(InputStream in, OutputStream out) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		Assert.notNull(out, "No OutputStream specified");

		try {
			return StreamUtils.copy(in, out);
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ex) {
			}
			try {
				out.close();
			}
			catch (IOException ex) {
			}
		}
	}

	/**
	 * 将给定字节数组的内容复制到给定的OutputStream.
	 * 完成后关闭流.
	 * 
	 * @param in 要从中复制的字节数组
	 * @param out 要复制到的OutputStream
	 * 
	 * @throws IOException 发生I/O错误
	 */
	public static void copy(byte[] in, OutputStream out) throws IOException {
		Assert.notNull(in, "No input byte array specified");
		Assert.notNull(out, "No OutputStream specified");

		try {
			out.write(in);
		}
		finally {
			try {
				out.close();
			}
			catch (IOException ex) {
			}
		}
	}

	/**
	 * 将给定InputStream的内容复制到新的字节数组中.
	 * 完成后关闭流.
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


	//---------------------------------------------------------------------
	// Copy methods for java.io.Reader / java.io.Writer
	//---------------------------------------------------------------------

	/**
	 * 将给定Reader的内容复制到给定的Writer.
	 * 完成后关闭两者.
	 * 
	 * @param in 要从中复制的Reader
	 * @param out 要复制到的Writer
	 * 
	 * @return 复制的字符数
	 * @throws IOException 发生I/O错误
	 */
	public static int copy(Reader in, Writer out) throws IOException {
		Assert.notNull(in, "No Reader specified");
		Assert.notNull(out, "No Writer specified");

		try {
			int byteCount = 0;
			char[] buffer = new char[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				byteCount += bytesRead;
			}
			out.flush();
			return byteCount;
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ex) {
			}
			try {
				out.close();
			}
			catch (IOException ex) {
			}
		}
	}

	/**
	 * 将给定String的内容复制到给定的输出Writer.
	 * 完成后关闭Writer.
	 * 
	 * @param in 要复制的字符串
	 * @param out 要复制到的Writer
	 * 
	 * @throws IOException 发生I/O错误
	 */
	public static void copy(String in, Writer out) throws IOException {
		Assert.notNull(in, "No input String specified");
		Assert.notNull(out, "No Writer specified");

		try {
			out.write(in);
		}
		finally {
			try {
				out.close();
			}
			catch (IOException ex) {
			}
		}
	}

	/**
	 * 将给定Reader的内容复制到String中.
	 * 完成后关闭Reader.
	 * 
	 * @param in 要从中复制的Reader(可能是{@code null}或为空)
	 * 
	 * @return 已复制到的字符串 (可能为空)
	 * @throws IOException 发生I/O错误
	 */
	public static String copyToString(Reader in) throws IOException {
		if (in == null) {
			return "";
		}

		StringWriter out = new StringWriter();
		copy(in, out);
		return out.toString();
	}
}
