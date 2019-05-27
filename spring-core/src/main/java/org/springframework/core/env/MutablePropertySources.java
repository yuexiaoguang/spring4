package org.springframework.core.env;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link PropertySources}接口的默认实现.
 * 允许操作包含的属性源, 并提供用于复制现有{@code PropertySources}实例的构造函数.
 *
 * <p>在诸如{@link #addFirst}和{@link #addLast}之类的方法中提到了<em>优先级</em>,
 * 这是关于在使用{@link PropertyResolver}解析给定属性时搜索属性源的顺序.
 */
public class MutablePropertySources implements PropertySources {

	private final Log logger;

	private final List<PropertySource<?>> propertySourceList = new CopyOnWriteArrayList<PropertySource<?>>();


	public MutablePropertySources() {
		this.logger = LogFactory.getLog(getClass());
	}

	/**
	 * 从给定的propertySources对象创建一个新的{@code MutablePropertySources},
	 * 保留包含的{@code PropertySource}对象的原始顺序.
	 */
	public MutablePropertySources(PropertySources propertySources) {
		this();
		for (PropertySource<?> propertySource : propertySources) {
			addLast(propertySource);
		}
	}

	/**
	 * 创建一个新的{@link MutablePropertySources}对象并继承给定的记录器,
	 * 通常来自一个封闭的{@link Environment}.
	 */
	MutablePropertySources(Log logger) {
		this.logger = logger;
	}


	@Override
	public boolean contains(String name) {
		return this.propertySourceList.contains(PropertySource.named(name));
	}

	@Override
	public PropertySource<?> get(String name) {
		int index = this.propertySourceList.indexOf(PropertySource.named(name));
		return (index != -1 ? this.propertySourceList.get(index) : null);
	}

	@Override
	public Iterator<PropertySource<?>> iterator() {
		return this.propertySourceList.iterator();
	}

	/**
	 * 添加具有最高优先级的给定属性源对象.
	 */
	public void addFirst(PropertySource<?> propertySource) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding PropertySource '" + propertySource.getName() + "' with highest search precedence");
		}
		removeIfPresent(propertySource);
		this.propertySourceList.add(0, propertySource);
	}

	/**
	 * 添加具有最低优先级的给定属性源对象.
	 */
	public void addLast(PropertySource<?> propertySource) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding PropertySource '" + propertySource.getName() + "' with lowest search precedence");
		}
		removeIfPresent(propertySource);
		this.propertySourceList.add(propertySource);
	}

	/**
	 * 添加给定属性源对象, 其优先级直接高于指定的相对属性源.
	 */
	public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding PropertySource '" + propertySource.getName() +
					"' with search precedence immediately higher than '" + relativePropertySourceName + "'");
		}
		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
		removeIfPresent(propertySource);
		int index = assertPresentAndGetIndex(relativePropertySourceName);
		addAtIndex(index, propertySource);
	}

	/**
	 * 添加给定属性源对象, 其优先级直接低于指定的相对属性源.
	 */
	public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding PropertySource '" + propertySource.getName() +
					"' with search precedence immediately lower than '" + relativePropertySourceName + "'");
		}
		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
		removeIfPresent(propertySource);
		int index = assertPresentAndGetIndex(relativePropertySourceName);
		addAtIndex(index + 1, propertySource);
	}

	/**
	 * 返回给定属性源的优先级, 或{@code -1}.
	 */
	public int precedenceOf(PropertySource<?> propertySource) {
		return this.propertySourceList.indexOf(propertySource);
	}

	/**
	 * 删除并返回具有给定名称的属性源, 或{@code null}.
	 * 
	 * @param name 要查找和删除的属性源的名称
	 */
	public PropertySource<?> remove(String name) {
		if (logger.isDebugEnabled()) {
			logger.debug("Removing PropertySource '" + name + "'");
		}
		int index = this.propertySourceList.indexOf(PropertySource.named(name));
		return (index != -1 ? this.propertySourceList.remove(index) : null);
	}

	/**
	 * 使用给定的属性源对象替换具有给定名称的属性源.
	 * 
	 * @param name 要查找和替换的属性源的名称
	 * @param propertySource 替代属性源
	 * 
	 * @throws IllegalArgumentException 如果没有具有给定名称的属性源
	 */
	public void replace(String name, PropertySource<?> propertySource) {
		if (logger.isDebugEnabled()) {
			logger.debug("Replacing PropertySource '" + name + "' with '" + propertySource.getName() + "'");
		}
		int index = assertPresentAndGetIndex(name);
		this.propertySourceList.set(index, propertySource);
	}

	/**
	 * 返回包含的{@link PropertySource}对象的数量.
	 */
	public int size() {
		return this.propertySourceList.size();
	}

	@Override
	public String toString() {
		return this.propertySourceList.toString();
	}

	/**
	 * 确保未相对于自身添加给定的属性源.
	 */
	protected void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
		String newPropertySourceName = propertySource.getName();
		if (relativePropertySourceName.equals(newPropertySourceName)) {
			throw new IllegalArgumentException(
					"PropertySource named '" + newPropertySourceName + "' cannot be added relative to itself");
		}
	}

	/**
	 * 删除给定的属性源.
	 */
	protected void removeIfPresent(PropertySource<?> propertySource) {
		this.propertySourceList.remove(propertySource);
	}

	/**
	 * 在列表中的特定索引处添加给定的属性源.
	 */
	private void addAtIndex(int index, PropertySource<?> propertySource) {
		removeIfPresent(propertySource);
		this.propertySourceList.add(index, propertySource);
	}

	/**
	 * 断言命名属性源存在并返回其索引.
	 * 
	 * @param name 要查找的{@linkplain PropertySource#getName() 属性源的名称}
	 * 
	 * @throws IllegalArgumentException 如果指定的属性源不存在
	 */
	private int assertPresentAndGetIndex(String name) {
		int index = this.propertySourceList.indexOf(PropertySource.named(name));
		if (index == -1) {
			throw new IllegalArgumentException("PropertySource named '" + name + "' does not exist");
		}
		return index;
	}
}
