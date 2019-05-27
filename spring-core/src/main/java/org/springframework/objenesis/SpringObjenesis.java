package org.springframework.objenesis;

import org.springframework.core.SpringProperties;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.strategy.InstantiatorStrategy;
import org.springframework.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * {@link ObjenesisStd} / {@link ObjenesisBase}的Spring特定变体,
 * 提供基于{@code Class}键而不是类名的缓存, 并允许选择性地使用缓存.
 */
public class SpringObjenesis implements Objenesis {

	/**
	 * 指示Spring忽略Objenesis的系统属性, 甚至不试图使用它.
	 * 将此标志设置为"true"等同于让Spring发现Objenesis在运行时不工作, 立即触发回退代码路径:
	 * 最重要的是, 这意味着将通过默认构造函数通过常规实例化创建所有CGLIB AOP代理.
	 */
	public static final String IGNORE_OBJENESIS_PROPERTY_NAME = "spring.objenesis.ignore";


	private final InstantiatorStrategy strategy;

	private final ConcurrentReferenceHashMap<Class<?>, ObjectInstantiator<?>> cache =
			new ConcurrentReferenceHashMap<Class<?>, ObjectInstantiator<?>>();

	private volatile Boolean worthTrying;


	/**
	 * 使用标准实例化策略创建一个新的{@code SpringObjenesis}实例.
	 */
	public SpringObjenesis() {
		this(null);
	}

	/**
	 * 使用给定的标准实例化策略创建一个新的{@code SpringObjenesis}实例.
	 * 
	 * @param strategy 要使用的实例化策略
	 */
	public SpringObjenesis(InstantiatorStrategy strategy) {
		this.strategy = (strategy != null ? strategy : new StdInstantiatorStrategy());

		// 提前评估"spring.objenesis.ignore"属性...
		if (SpringProperties.getFlag(SpringObjenesis.IGNORE_OBJENESIS_PROPERTY_NAME)) {
			this.worthTrying = Boolean.FALSE;
		}
	}


	/**
	 * 返回此Objenesis实例是否值得尝试创建实例, i.e. 它是否尚未使用或已知可行.
	 * <p>如果已经确定配置的Objenesis实例化策略根本不在当前JVM上工作,
	 * 或者如果"spring.objenesis.ignore"属性已设置为"true", 则此方法返回{@code false}.
	 */
	public boolean isWorthTrying() {
		return (this.worthTrying != Boolean.FALSE);
	}

	/**
	 * 通过Objenesis创建给定类的新实例.
	 * 
	 * @param clazz 用于创建实例的类
	 * @param useCache 是否使用实例化缓存 (通常为{@code true}但可以设置为{@code false}, e.g. 可重新加载的类)
	 * 
	 * @return 新实例 (never {@code null})
	 * @throws ObjenesisException 如果实例创建失败
	 */
	public <T> T newInstance(Class<T> clazz, boolean useCache) {
		if (!useCache) {
			return newInstantiatorOf(clazz).newInstance();
		}
		return getInstantiatorOf(clazz).newInstance();
	}

	public <T> T newInstance(Class<T> clazz) {
		return getInstantiatorOf(clazz).newInstance();
	}

	@SuppressWarnings("unchecked")
	public <T> ObjectInstantiator<T> getInstantiatorOf(Class<T> clazz) {
		ObjectInstantiator<?> instantiator = this.cache.get(clazz);
		if (instantiator == null) {
			ObjectInstantiator<T> newInstantiator = newInstantiatorOf(clazz);
			instantiator = this.cache.putIfAbsent(clazz, newInstantiator);
			if (instantiator == null) {
				instantiator = newInstantiator;
			}
		}
		return (ObjectInstantiator<T>) instantiator;
	}

	protected <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> clazz) {
		Boolean currentWorthTrying = this.worthTrying;
		try {
			ObjectInstantiator<T> instantiator = this.strategy.newInstantiatorOf(clazz);
			if (currentWorthTrying == null) {
				this.worthTrying = Boolean.TRUE;
			}
			return instantiator;
		}
		catch (ObjenesisException ex) {
			if (currentWorthTrying == null) {
				Throwable cause = ex.getCause();
				if (cause instanceof ClassNotFoundException || cause instanceof IllegalAccessException) {
					// 表示所选的实例化策略不适用于给定的JVM.
					// 通常无法初始化默认的 SunReflectionFactoryInstantiator.
					// 假设任何后来使用Objenesis的尝试都会失败...
					this.worthTrying = Boolean.FALSE;
				}
			}
			throw ex;
		}
		catch (NoClassDefFoundError err) {
			// 发生在Google App Engine的生产版本上, 来自受限制的"sun.reflect.ReflectionFactory"类...
			if (currentWorthTrying == null) {
				this.worthTrying = Boolean.FALSE;
			}
			throw new ObjenesisException(err);
		}
	}
}
