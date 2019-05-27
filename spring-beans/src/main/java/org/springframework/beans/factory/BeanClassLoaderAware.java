package org.springframework.beans.factory;

/**
 * 允许bean知道bean的{@link ClassLoader类加载器}的回调;
 * 即, 当前bean工厂用于加载bean类的类加载器.
 *
 * <p>这主要是由框架类实现的, 框架类必须按名称获取应用程序类, 尽管它们可能是从共享类加载器加载的.
 *
 * <p>有关所有Bean生命周期方法的列表, see the {@link BeanFactory BeanFactory javadocs}.
 */
public interface BeanClassLoaderAware extends Aware {

	/**
	 * 将bean {@link ClassLoader类加载器}提供给bean实例的回调.
	 * <p>在普通bean属性的填充之后, 但在初始化回调之前调用,
	 * 例如{@link InitializingBean InitializingBean}的 {@link InitializingBean#afterPropertiesSet()}方法或自定义初始化方法.
	 * 
	 * @param classLoader 拥有的类加载器; 在{@code null}情况下必须使用默认的{@code ClassLoader},
	 * 例如通过{@link org.springframework.util.ClassUtils#getDefaultClassLoader()}获取 {@code ClassLoader}
	 */
	void setBeanClassLoader(ClassLoader classLoader);

}
