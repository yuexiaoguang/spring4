package org.springframework.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 给定{@link InputStream}的{@link Resource}实现.
 * <p>只应在没有其他特定{@code Resource}实现适用的情况下使用.
 * 特别是, 在可能的情况下, 更喜欢{@link ByteArrayResource}或任何基于文件的{@code Resource}实现.
 *
 * <p>与其他{@code Resource}实现相比, 这是<i>已经打开的</i>资源的描述符 - 因此从{@link #isOpen()}返回{@code true}.
 * 如果需要将资源描述符保留在某处, 或者需要多次从流中读取, 请不要使用{@code InputStreamResource}.
 */
public class InputStreamResource extends AbstractResource {

	private final InputStream inputStream;

	private final String description;

	private boolean read = false;


	/**
	 * @param inputStream 要使用的InputStream
	 */
	public InputStreamResource(InputStream inputStream) {
		this(inputStream, "resource loaded through InputStream");
	}

	/**
	 * @param inputStream 要使用的InputStream
	 * @param description InputStream的来源
	 */
	public InputStreamResource(InputStream inputStream, String description) {
		if (inputStream == null) {
			throw new IllegalArgumentException("InputStream must not be null");
		}
		this.inputStream = inputStream;
		this.description = (description != null ? description : "");
	}


	/**
	 * 此实现始终返回{@code true}.
	 */
	@Override
	public boolean exists() {
		return true;
	}

	/**
	 * 此实现始终返回{@code true}.
	 */
	@Override
	public boolean isOpen() {
		return true;
	}

	/**
	 * 如果尝试多次读取基础流, 则此实现会抛出IllegalStateException.
	 */
	@Override
	public InputStream getInputStream() throws IOException, IllegalStateException {
		if (this.read) {
			throw new IllegalStateException("InputStream has already been read - " +
					"do not use InputStreamResource if a stream needs to be read multiple times");
		}
		this.read = true;
		return this.inputStream;
	}

	/**
	 * 此实现返回包含传入的描述的描述.
	 */
	@Override
	public String getDescription() {
		return "InputStream resource [" + this.description + "]";
	}


	/**
	 * 此实现比较底层InputStream.
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this ||
			(obj instanceof InputStreamResource && ((InputStreamResource) obj).inputStream.equals(this.inputStream)));
	}

	/**
	 * 此实现返回底层InputStream的哈希码.
	 */
	@Override
	public int hashCode() {
		return this.inputStream.hashCode();
	}
}
