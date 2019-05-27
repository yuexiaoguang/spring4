package org.springframework.core.io.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 将{@link Resource}描述符与特定编码或{@code Charset}组合在一起的持有者, 用于从资源中读取.
 *
 * <p>用作支持使用特定编码读取内容的操作的参数, 通常通过{@code java.io.Reader}.
 */
public class EncodedResource implements InputStreamSource {

	private final Resource resource;

	private final String encoding;

	private final Charset charset;


	/**
	 * 为给定的{@code Resource}创建一个新的{@code EncodedResource}, 而不指定显式编码或{@code Charset}.
	 * 
	 * @param resource 要保留的{@code Resource} (never {@code null})
	 */
	public EncodedResource(Resource resource) {
		this(resource, null, null);
	}

	/**
	 * 使用指定的{@code encoding}为给定的{@code Resource}创建一个新的{@code EncodedResource}.
	 * 
	 * @param resource 要保留的{@code Resource} (never {@code null})
	 * @param encoding 用于从资源中读取的编码
	 */
	public EncodedResource(Resource resource, String encoding) {
		this(resource, encoding, null);
	}

	/**
	 * 使用指定的{@code Charset}为给定的{@code Resource}创建一个新的{@code EncodedResource}.
	 * 
	 * @param resource 要保留的{@code Resource} (never {@code null})
	 * @param charset 用于从资源中读取的{@code Charset}
	 */
	public EncodedResource(Resource resource, Charset charset) {
		this(resource, null, charset);
	}

	private EncodedResource(Resource resource, String encoding, Charset charset) {
		super();
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
		this.encoding = encoding;
		this.charset = charset;
	}


	/**
	 * 返回此{@code EncodedResource}所持有的{@code Resource}.
	 */
	public final Resource getResource() {
		return this.resource;
	}

	/**
	 * 返回用于从{@linkplain #getResource() 资源}读取的编码, 或{@code null}.
	 */
	public final String getEncoding() {
		return this.encoding;
	}

	/**
	 * 返回用于从{@linkplain #getResource() 资源}读取的{@code Charset}, 或{@code null}.
	 */
	public final Charset getCharset() {
		return this.charset;
	}

	/**
	 * 确定是否需要{@link Reader}, 而不是{@link InputStream},
	 * i.e. 是否已指定{@linkplain #getEncoding() encoding}或{@link #getCharset() Charset} has been specified.
	 */
	public boolean requiresReader() {
		return (this.encoding != null || this.charset != null);
	}

	/**
	 * 打开指定资源的{@code java.io.Reader}, 使用指定的{@link #getCharset() Charset}或{@linkplain #getEncoding() encoding}.
	 * 
	 * @throws IOException 如果打开读取器失败
	 */
	public Reader getReader() throws IOException {
		if (this.charset != null) {
			return new InputStreamReader(this.resource.getInputStream(), this.charset);
		}
		else if (this.encoding != null) {
			return new InputStreamReader(this.resource.getInputStream(), this.encoding);
		}
		else {
			return new InputStreamReader(this.resource.getInputStream());
		}
	}

	/**
	 * 打开指定资源的{@code InputStream}, 忽略任何指定的{@link #getCharset() Charset}或{@linkplain #getEncoding() encoding}.
	 * 
	 * @throws IOException 如果打开InputStream失败
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return this.resource.getInputStream();
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof EncodedResource)) {
			return false;
		}
		EncodedResource otherResource = (EncodedResource) other;
		return (this.resource.equals(otherResource.resource) &&
				ObjectUtils.nullSafeEquals(this.charset, otherResource.charset) &&
				ObjectUtils.nullSafeEquals(this.encoding, otherResource.encoding));
	}

	@Override
	public int hashCode() {
		return this.resource.hashCode();
	}

	@Override
	public String toString() {
		return this.resource.toString();
	}

}
