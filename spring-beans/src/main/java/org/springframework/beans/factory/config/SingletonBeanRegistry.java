package org.springframework.beans.factory.config;

/**
 * 为共享bean实例定义注册表的接口.
 * 可以通过{@link org.springframework.beans.factory.BeanFactory}实现来实现, 以便以统一的方式公开他们的单例管理工具.
 *
 * <p>{@link ConfigurableBeanFactory}接口扩展了此接口.
 */
public interface SingletonBeanRegistry {

	/**
	 * 在给定的bean名称下, 在bean注册表中将给定的现有对象注册为singleton.
	 * <p>应该完全初始化给定的实例;
	 * 注册表不会执行任何初始化回调 (特别是, 它不会调用InitializingBean的{@code afterPropertiesSet}方法).
	 * 给定的实例不会收到任何销毁回调 (例如DisposableBean的 {@code destroy}方法).
	 * <p>在完整的BeanFactory中运行时:
	 * <b>如果bean应该接收初始化和/或销毁回调, 则注册bean定义, 而不是现有实例.</b>
	 * <p>通常在注册表配置期间调用, 但也可用于单例的运行时注册.
	 * 因此, 注册表实现应该synchronize单例访问;
	 * 如果它支持BeanFactory对单例的懒惰初始化, 它将无论如何都必须这样做.
	 * 
	 * @param beanName bean的名称
	 * @param singletonObject 现有的单例对象
	 */
	void registerSingleton(String beanName, Object singletonObject);

	/**
	 * 返回在给定名称下注册的（原始）单例对象.
	 * <p>只检查已经实例化的单例; 不返回尚未实例化的单例bean定义的Object.
	 * <p>此方法的主要目的是访问手动注册的单例 (see {@link #registerSingleton}).
	 * 也可以用于以原始方式访问已经创建的bean定义定义的单例.
	 * <p><b>NOTE:</b> 此查找方法不知道FactoryBean前缀或别名.
	 * 在获取单例实例之前, 需要首先解析规范bean名称.
	 * 
	 * @param beanName 要查找的bean的名称
	 * 
	 * @return 注册的单例对象, 或{@code null}
	 */
	Object getSingleton(String beanName);

	/**
	 * 检查此注册表是否包含具有给定名称的单例实例.
	 * <p>只检查已经实例化的单例; 对于尚未实例化的单例bean定义, 不会返回{@code true}.
	 * <p>此方法的主要目的是访问手动注册的单例 (see {@link #registerSingleton}).
	 * 也可用于检查是否已创建由bean定义定义的单例.
	 * <p>检查bean工厂是否包含具有给定名称的bean定义, 使用ListableBeanFactory的 {@code containsBeanDefinition}.
	 * 同时调用 {@code containsBeanDefinition}和{@code containsSingleton}可以回答特定bean工厂是否包含具有给定名称的本地bean实例.
	 * <p>使用BeanFactory的{@code containsBean}进行常规检查, 以确定工厂是否知道具有给定名称的bean
	 * (是手动注册的单例实例, 还是由bean定义创建的), 还检查祖先工厂.
	 * <p><b>NOTE:</b> 此查找方法不知道FactoryBean前缀或别名.
	 * 在获取单例实例之前, 需要首先解析规范bean名称.
	 * 
	 * @param beanName 要查找的bean的名称
	 * 
	 * @return 如果此bean工厂包含具有给定名称的单例实例
	 */
	boolean containsSingleton(String beanName);

	/**
	 * 返回在此注册表中注册的单例bean的名称.
	 * <p>只检查已经实例化的单例; 不返回尚未实例化的单例bean定义的名称.
	 * <p>此方法的主要目的是访问手动注册的单例 (see {@link #registerSingleton}).
	 * 也可以用于检查已经创建了的bean定义定义的单例.
	 * 
	 * @return 名称列表 (never {@code null})
	 */
	String[] getSingletonNames();

	/**
	 * 返回在此注册表中注册的单例bean的数量.
	 * <p>只检查已经实例化的单例; 不计算尚未实例化的单例bean定义.
	 * <p>此方法的主要目的是访问手动注册的单例 (see {@link #registerSingleton}).
	 * 也可以用于计算已经创建的bean定义定义的单例数量.
	 * 
	 * @return 单身bean的数量
	 */
	int getSingletonCount();

	/**
	 * 返回此注册表使用的单例互斥锁 (对于外部合作).
	 * 
	 * @return 互斥锁对象 (never {@code null})
	 */
	Object getSingletonMutex();

}
