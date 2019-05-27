package org.springframework.util;

import java.nio.charset.Charset;
import java.util.Base64;
import javax.xml.bind.DatatypeConverter;

import org.springframework.lang.UsesJava8;

/**
 * 用于Base64编码和解码的简单实用程序类.
 *
 * <p>适配Java 8的 {@link java.util.Base64}类或Apache Commons Codec的{@link org.apache.commons.codec.binary.Base64}类.
 * 如果没有Java 8或Commons Codec, {@link #encode}/{@link #decode}调用将抛出IllegalStateException.
 * 但是, 从Spring 4.2开始, {@link #encodeToString}和{@link #decodeFromString}仍然可以工作,
 * 因为它们可以作为后备委托给JAXB DatatypeConverter.
 * 但是, 当使用RFC 4648 "URL 和文件名安全字母"的"UrlSafe"方法时, 这不适用; 需要委托.
 *
 * <p><em>Note:</em> 使用URL和文件名安全字母编码时, Apache Commons Codec不会添加填充 ({@code =}).
 */
public abstract class Base64Utils {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");


	private static final Base64Delegate delegate;

	static {
		Base64Delegate delegateToUse = null;
		// JDK 8的java.util.Base64类存在?
		if (ClassUtils.isPresent("java.util.Base64", Base64Utils.class.getClassLoader())) {
			delegateToUse = new JdkBase64Delegate();
		}
		// Apache Commons Codec存在于类路径中?
		else if (ClassUtils.isPresent("org.apache.commons.codec.binary.Base64", Base64Utils.class.getClassLoader())) {
			delegateToUse = new CommonsCodecBase64Delegate();
		}
		delegate = delegateToUse;
	}

	/**
	 * 断言实际上支持字节数组之间的Byte64编码.
	 * 
	 * @throws IllegalStateException 如果Java 8和Apache Commons Codec都不存在
	 */
	private static void assertDelegateAvailable() {
		Assert.state(delegate != null,
				"Neither Java 8 nor Apache Commons Codec found - Base64 encoding between byte arrays not supported");
	}


	/**
	 * 编码给定的字节数组.
	 * 
	 * @param src 原始字节数组 (may be {@code null})
	 * 
	 * @return 编码后的字节数组 (如果输入为{@code null}, 则为{@code null})
	 * @throws IllegalStateException 如果不支持字节数组之间的Base64编码, i.e. 运行时不存在Java 8或Apache Commons Codec
	 */
	public static byte[] encode(byte[] src) {
		assertDelegateAvailable();
		return delegate.encode(src);
	}

	/**
	 * 解码给定的字节数组.
	 * 
	 * @param src 编码后的字节数组 (may be {@code null})
	 * 
	 * @return 原始字节数组 (如果输入为{@code null}, 则为{@code null})
	 * @throws IllegalStateException 如果不支持字节数组之间的Base64编码, i.e. 运行时不存在Java 8或Apache Commons Codec
	 */
	public static byte[] decode(byte[] src) {
		assertDelegateAvailable();
		return delegate.decode(src);
	}

	/**
	 * 使用RFC 4648 "URL and Filename Safe Alphabet"对给定的字节数组进行编码.
	 * 
	 * @param src 原始字节数组 (may be {@code null})
	 * 
	 * @return 编码后的字节数组 (如果输入为{@code null}, 则为{@code null})
	 * @throws IllegalStateException 如果不支持字节数组之间的Base64编码, i.e. 运行时不存在Java 8或Apache Commons Codec
	 */
	public static byte[] encodeUrlSafe(byte[] src) {
		assertDelegateAvailable();
		return delegate.encodeUrlSafe(src);
	}

	/**
	 * 使用RFC 4648 "URL and Filename Safe Alphabet"对给定的字节数组进行解码.
	 * 
	 * @param src 编码后的字节数组 (may be {@code null})
	 * 
	 * @return 原始字节数组 (如果输入为{@code null}, 则为{@code null})
	 * @throws IllegalStateException 如果不支持字节数组之间的Base64编码, i.e. 运行时不存在Java 8或Apache Commons Codec
	 */
	public static byte[] decodeUrlSafe(byte[] src) {
		assertDelegateAvailable();
		return delegate.decodeUrlSafe(src);
	}

	/**
	 * 将给定的字节数组编码为String.
	 * 
	 * @param src 原始字节数组 (may be {@code null})
	 * 
	 * @return 编码后的UTF-8字符串 (如果输入为{@code null}, 则为{@code null})
	 */
	public static String encodeToString(byte[] src) {
		if (src == null) {
			return null;
		}
		if (src.length == 0) {
			return "";
		}

		if (delegate != null) {
			// Full encoder available
			return new String(delegate.encode(src), DEFAULT_CHARSET);
		}
		else {
			// JAXB fallback for String case
			return DatatypeConverter.printBase64Binary(src);
		}
	}

	/**
	 * 从UTF-8字符串解码给定的字节数组.
	 * 
	 * @param src 编码后的UTF-8字符串 (may be {@code null})
	 * 
	 * @return 原始字节数组 (如果输入为{@code null}, 则为{@code null})
	 */
	public static byte[] decodeFromString(String src) {
		if (src == null) {
			return null;
		}
		if (src.isEmpty()) {
			return new byte[0];
		}

		if (delegate != null) {
			// Full encoder available
			return delegate.decode(src.getBytes(DEFAULT_CHARSET));
		}
		else {
			// JAXB fallback for String case
			return DatatypeConverter.parseBase64Binary(src);
		}
	}

	/**
	 * 使用RFC 4648 "URL and Filename Safe Alphabet"将给定的字节数组编码为String.
	 * 
	 * @param src 原始字节数组 (may be {@code null})
	 * 
	 * @return 编码后的UTF-8字符串 (如果输入为{@code null}, 则为{@code null})
	 * @throws IllegalStateException 如果不支持字节数组之间的Base64编码, i.e. 运行时不存在Java 8或Apache Commons Codec
	 */
	public static String encodeToUrlSafeString(byte[] src) {
		assertDelegateAvailable();
		return new String(delegate.encodeUrlSafe(src), DEFAULT_CHARSET);
	}

	/**
	 * 使用RFC 4648 "URL and Filename Safe Alphabet"从UTF-8字符串解码给定的字节数组.
	 * 
	 * @param src 编码后的UTF-8字符串 (may be {@code null})
	 * 
	 * @return 原始字节数组 (如果输入为{@code null}, 则为{@code null})
	 * @throws IllegalStateException 如果不支持字节数组之间的Base64编码, i.e. 运行时不存在Java 8或Apache Commons Codec
	 */
	public static byte[] decodeFromUrlSafeString(String src) {
		assertDelegateAvailable();
		return delegate.decodeUrlSafe(src.getBytes(DEFAULT_CHARSET));
	}


	interface Base64Delegate {

		byte[] encode(byte[] src);

		byte[] decode(byte[] src);

		byte[] encodeUrlSafe(byte[] src);

		byte[] decodeUrlSafe(byte[] src);
	}


	@UsesJava8
	static class JdkBase64Delegate implements Base64Delegate {

		@Override
		public byte[] encode(byte[] src) {
			if (src == null || src.length == 0) {
				return src;
			}
			return Base64.getEncoder().encode(src);
		}

		@Override
		public byte[] decode(byte[] src) {
			if (src == null || src.length == 0) {
				return src;
			}
			return Base64.getDecoder().decode(src);
		}

		@Override
		public byte[] encodeUrlSafe(byte[] src) {
			if (src == null || src.length == 0) {
				return src;
			}
			return Base64.getUrlEncoder().encode(src);
		}

		@Override
		public byte[] decodeUrlSafe(byte[] src) {
			if (src == null || src.length == 0) {
				return src;
			}
			return Base64.getUrlDecoder().decode(src);
		}

	}


	static class CommonsCodecBase64Delegate implements Base64Delegate {

		private final org.apache.commons.codec.binary.Base64 base64 =
				new org.apache.commons.codec.binary.Base64();

		private final org.apache.commons.codec.binary.Base64 base64UrlSafe =
				new org.apache.commons.codec.binary.Base64(0, null, true);

		@Override
		public byte[] encode(byte[] src) {
			return this.base64.encode(src);
		}

		@Override
		public byte[] decode(byte[] src) {
			return this.base64.decode(src);
		}

		@Override
		public byte[] encodeUrlSafe(byte[] src) {
			return this.base64UrlSafe.encode(src);
		}

		@Override
		public byte[] decodeUrlSafe(byte[] src) {
			return this.base64UrlSafe.decode(src);
		}
	}
}
