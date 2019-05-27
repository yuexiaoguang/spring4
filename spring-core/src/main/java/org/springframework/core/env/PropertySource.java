package org.springframework.core.env;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 表示名称/值属性对的源的抽象基类.
 * 底层{@linkplain #getSource() 源对象}可以是封装属性的任何类型{@code T}.
 * 示例包括{@link java.util.Properties}对象, {@link java.util.Map}对象,
 * {@code ServletContext}和{@code ServletConfig}对象 (用于访问init参数).
 * 浏览{@code PropertySource}类型层次结构以查看提供的实现.
 *
 * <p>{@code PropertySource}对象通常不是孤立使用的, 而是通过{@link PropertySources}对象使用,
 * 该对象聚合属性源并与{@link PropertyResolver}实现结合使用, 该实现可以在整个{@code PropertySources}集合中执行基于优先级的搜索.
 *
 * <p>{@code PropertySource}标识不是基于封装属性的内容确定的, 而是基于{@code PropertySource}单独的{@link #getName() name}.
 * 这对于在集合上下文中操作{@code PropertySource}对象很有用.
 * 有关详细信息, 请参阅{@link MutablePropertySources}中的操作以及{@link #named(String)}和{@link #toString()}方法.
 *
 * <p>请注意, 在使用@{@link org.springframework.context.annotation.Configuration Configuration}类时,
 * @{@link org.springframework.context.annotation.PropertySource PropertySource}注解
 * 提供了一种方便的声明方式, 可以将属性源添加到封闭的{@code Environment}中.
 */
public abstract class PropertySource<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final String name;

	protected final T source;


	/**
	 * 使用给定的名称和源对象创建一个新的{@code PropertySource}.
	 */
	public PropertySource(String name, T source) {
		Assert.hasText(name, "Property source name must contain at least one character");
		Assert.notNull(source, "Property source must not be null");
		this.name = name;
		this.source = source;
	}

	/**
	 * 创建一个具有给定名称的新{@code PropertySource}, 并使用新的{@code Object}实例作为底层源.
	 * <p>在创建永远不会查询实际源, 但返回硬编码值的匿名实现时, 通常可用于测试场景.
	 */
	@SuppressWarnings("unchecked")
	public PropertySource(String name) {
		this(name, (T) new Object());
	}


	/**
	 * 返回此{@code PropertySource}的名称
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回此{@code PropertySource}的底层源对象.
	 */
	public T getSource() {
		return this.source;
	}

	/**
	 * 返回此{@code PropertySource}是否包含给定名称.
	 * <p>此实现只是从{@link #getProperty(String)}检查{@code null}返回值.
	 * 如果可能, 子类可能希望实现更有效的算法.
	 * 
	 * @param name 要查找的属性名称
	 */
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

	/**
	 * 返回与给定名称关联的值, 或{@code null}.
	 * 
	 * @param name 要查找的属性
	 */
	public abstract Object getProperty(String name);


	/**
	 * 这个{@code PropertySource}对象等于给定的对象, 如果:
	 * <ul>
	 * <li>同一个实例
	 * <li>两个对象的{@code name}属性相同
	 * </ul>
	 * <p>除了{@code name}之外, 不评估任何属性.
	 */
	@Override
	public boolean equals(Object obj) {
		return (this == obj || (obj instanceof PropertySource &&
				ObjectUtils.nullSafeEquals(this.name, ((PropertySource<?>) obj).name)));
	}

	/**
	 * 返回从此{@code PropertySource}对象的{@code name}属性派生的哈希码.
	 */
	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.name);
	}

	/**
	 * 如果当前日志级别不包括调试, 则生成简明输出 (类型和名称).
	 * 如果启用了debug, 则生成详细输出, 包括PropertySource实例的哈希码和每个名称/值属性对.
	 * <p>此变量详细程度可用作属性源, 例如系统属性或环境变量, 可能包含任意数量的属性对, 可能导致难以读取异常和日志消息.
	 */
	@Override
	public String toString() {
		if (logger.isDebugEnabled()) {
			return getClass().getSimpleName() + "@" + System.identityHashCode(this) +
					" {name='" + this.name + "', properties=" + this.source + "}";
		}
		else {
			return getClass().getSimpleName() + " {name='" + this.name + "'}";
		}
	}


	/**
	 * 返回{@code PropertySource}实现, 仅用于集合比较.
	 * <p>主要供内部使用, 但给定{@code PropertySource}对象的集合, 可以如下使用:
	 * <pre class="code">
	 * {@code List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
	 * sources.add(new MapPropertySource("sourceA", mapA));
	 * sources.add(new MapPropertySource("sourceB", mapB));
	 * assert sources.contains(PropertySource.named("sourceA"));
	 * assert sources.contains(PropertySource.named("sourceB"));
	 * assert !sources.contains(PropertySource.named("sourceC"));
	 * }</pre>
	 * 如果调用{@code equals(Object)}, {@code hashCode()}, 和{@code toString()}以外的任何方法,
	 * 则返回的{@code PropertySource}将抛出{@code UnsupportedOperationException}.
	 * 
	 * @param name 要创建和返回的比较{@code PropertySource}的名称.
	 */
	public static PropertySource<?> named(String name) {
		return new ComparisonPropertySource(name);
	}


	/**
	 * 在应用程序上下文创建时, 无法实时初始化实际属性源的情况下, {@code PropertySource}将用作占位符.
	 * 例如, 基于{@code ServletContext}的属性源必须等待, 直到{@code ServletContext}对象可用于其封闭的{@code ApplicationContext}.
	 * 在这种情况下, 应使用存根来保存属性源的预期默认位置/顺序, 然后在上下文刷新期间替换.
	 */
	public static class StubPropertySource extends PropertySource<Object> {

		public StubPropertySource(String name) {
			super(name, new Object());
		}

		/**
		 * 总是返回{@code null}.
		 */
		@Override
		public String getProperty(String name) {
			return null;
		}
	}


	static class ComparisonPropertySource extends StubPropertySource {

		private static final String USAGE_ERROR =
				"ComparisonPropertySource instances are for use with collection comparison only";

		public ComparisonPropertySource(String name) {
			super(name);
		}

		@Override
		public Object getSource() {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}

		@Override
		public boolean containsProperty(String name) {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}

		@Override
		public String getProperty(String name) {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}
	}
}
