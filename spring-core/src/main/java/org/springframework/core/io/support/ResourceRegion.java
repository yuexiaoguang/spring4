package org.springframework.core.io.support;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link Resource}实现的区域, 由{@link Resource}中的{@code position}和该区域长度的字节{@code count}实现.
 */
public class ResourceRegion {

	private final Resource resource;

	private final long position;

	private final long count;


	/**
	 * 从给定的{@link Resource}创建一个新的{@code ResourceRegion}.
	 * 资源的这个区域由给定{@code Resource}中的start {@code position}和byte {@code count}表示.
	 * 
	 * @param resource Resource
	 * @param position 该资源中该区域的起始位置
	 * @param count 该资源中区域的字节数
	 */
	public ResourceRegion(Resource resource, long position, long count) {
		Assert.notNull(resource, "Resource must not be null");
		Assert.isTrue(position >= 0, "'position' must be larger than or equal to 0");
		Assert.isTrue(count >= 0, "'count' must be larger than or equal to 0");
		this.resource = resource;
		this.position = position;
		this.count = count;
	}


	/**
	 * 返回此{@code ResourceRegion}的底层{@link资源}
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * 返回底层{@link Resource}中该区域的起始位置
	 */
	public long getPosition() {
		return this.position;
	}

	/**
	 * 返回底层{@link Resource}中此区域的字节数
	 */
	public long getCount() {
		return this.count;
	}
}
