package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * 模拟{@link Resource resource}中任意位置的类.
 *
 * <p>通常用于跟踪XML配置文件中有问题或错误的元数据的位置.
 * 例如, {@link #getSource() source}位置可能是 '在beans.properties的第76行定义的bean具有无效的Class';
 * 另一个源可能是来自解析的XML {@link org.w3c.dom.Document}的实际DOM元素;
 * 或源对象可能只是{@code null}.
 */
public class Location {

	private final Resource resource;

	private final Object source;


	/**
	 * @param resource 与此位置关联的资源
	 */
	public Location(Resource resource) {
		this(resource, null);
	}

	/**
	 * @param resource 与此位置关联的资源
	 * @param source 关联的资源中的实际位置 (may be {@code null})
	 */
	public Location(Resource resource, Object source) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
		this.source = source;
	}


	/**
	 * 获取与此位置关联的资源.
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * 获取关联的{@link #getResource() resource}中的实际位置 (may be {@code null}).
	 * <p>有关返回对象的实际类型的示例，请参阅此类的 {@link Location 此类的类级别javadoc}.
	 */
	public Object getSource() {
		return this.source;
	}

}
