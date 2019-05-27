package org.springframework.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * 简单的{@link Resource}实现, 它包含资源描述但不指向实际可读的资源.
 *
 * <p>如果API需要{@code Resource}参数, 但不一定用于实际读取, 则用作占位符.
 */
public class DescriptiveResource extends AbstractResource {

	private final String description;


	/**
	 * @param description 资源描述
	 */
	public DescriptiveResource(String description) {
		this.description = (description != null ? description : "");
	}


	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public boolean isReadable() {
		return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new FileNotFoundException(
				getDescription() + " cannot be opened because it does not point to a readable resource");
	}

	@Override
	public String getDescription() {
		return this.description;
	}


	/**
	 * 此实现比较底层描述String.
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this ||
			(obj instanceof DescriptiveResource && ((DescriptiveResource) obj).description.equals(this.description)));
	}

	/**
	 * 此实现返回底层描述String的哈希码.
	 */
	@Override
	public int hashCode() {
		return this.description.hashCode();
	}

}
