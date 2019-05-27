package org.springframework.core.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.springframework.util.Assert;

/**
 * 给定字节数组的{@link Resource}实现.
 * <p>为给定的字节数组创建{@link ByteArrayInputStream}.
 *
 * <p>用于从任何给定的字节数组加载内容, 而不必求助于一次性使用{@link InputStreamResource}.
 * 特别适用于从本地内容创建邮件附件, 其中JavaMail需要能够多次读取流.
 */
public class ByteArrayResource extends AbstractResource {

	private final byte[] byteArray;

	private final String description;


	/**
	 * @param byteArray 要包装的字节数组
	 */
	public ByteArrayResource(byte[] byteArray) {
		this(byteArray, "resource loaded from byte array");
	}

	/**
	 * @param byteArray 要包装的字节数组
	 * @param description 字节数组的来源
	 */
	public ByteArrayResource(byte[] byteArray, String description) {
		Assert.notNull(byteArray, "Byte array must not be null");
		this.byteArray = byteArray;
		this.description = (description != null ? description : "");
	}


	/**
	 * 返回底层字节数组.
	 */
	public final byte[] getByteArray() {
		return this.byteArray;
	}

	/**
	 * 此实现始终返回{@code true}.
	 */
	@Override
	public boolean exists() {
		return true;
	}

	/**
	 * 此实现返回底层字节数组的长度.
	 */
	@Override
	public long contentLength() {
		return this.byteArray.length;
	}

	/**
	 * 此实现为底层字节数组返回ByteArrayInputStream.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(this.byteArray);
	}

	/**
	 * 此实现返回包含传入的{@code description}的描述.
	 */
	@Override
	public String getDescription() {
		return "Byte array resource [" + this.description + "]";
	}


	/**
	 * 此实现比较底层字节数组.
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this ||
			(obj instanceof ByteArrayResource && Arrays.equals(((ByteArrayResource) obj).byteArray, this.byteArray)));
	}

	/**
	 * 此实现基于底层字节数组返回哈希码.
	 */
	@Override
	public int hashCode() {
		return (byte[].class.hashCode() * 29 * this.byteArray.length);
	}

}
