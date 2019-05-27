package org.springframework.core.env;

import org.springframework.util.ObjectUtils;

/**
 * {@link PropertySource}实现, 能够查询其底层源对象以枚举所有可能的属性名称/值对.
 * 公开{@link #getPropertyNames()}方法, 允许调用者内省可用属性, 而无需访问底层源对象.
 * 这也有助于更有效地实现{@link #containsProperty(String)}, 因为它可以调用{@link #getPropertyNames()}并遍历返回的数组,
 * 而不是尝试调用{@link #getProperty(String)}, 因为它更昂贵.
 * 实现可以考虑缓存{@link #getPropertyNames()}的结果以充分利用此性能机会.
 *
 * <p>大多数框架提供的{@code PropertySource}实现都是可枚举的;
 * 反例将是{@code JndiPropertySource}, 由于JNDI的性质, 在任何给定时间都无法确定所有可能的属性名称;
 * 相反, 它只能尝试访问一个属性 (通过{@link #getProperty(String)}), 以评估它是否存在.
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {

	public EnumerablePropertySource(String name, T source) {
		super(name, source);
	}

	protected EnumerablePropertySource(String name) {
		super(name);
	}


	/**
	 * 返回此{@code PropertySource}是否包含具有给定名称的属性.
	 * <p>此实现检查{@link #getPropertyNames()}数组中是否存在给定名称.
	 * 
	 * @param name 要查找的属性的名称
	 */
	@Override
	public boolean containsProperty(String name) {
		return ObjectUtils.containsElement(getPropertyNames(), name);
	}

	/**
	 * 返回{@linkplain #getSource() source}对象包含的所有属性的名称 (never {@code null}).
	 */
	public abstract String[] getPropertyNames();

}
