package org.springframework.beans.factory;

/**
 * 由{@link BeanFactory}中使用的对象实现的接口, 它们本身就是单个对象的工厂.
 * 如果bean实现了这个接口, 它被用作要暴露的对象的工厂, 不直接作为将暴露自己的bean实例.
 *
 * <p><b>NB: 实现此接口的bean不能用作普通bean.</b>
 * FactoryBean以bean样式定义, 但为bean引用而公开的对象({@link #getObject()})始终是它创建的对象.
 *
 * <p>FactoryBeans可以支持单例和原型, 并且可以根据需要延迟创建对象, 也可以在启动时实时创建对象.
 * {@link SmartFactoryBean}接口允许公开更细粒度的行为元数据.
 *
 * <p>该接口在框架内部大量使用, 例如对于AOP {@link org.springframework.aop.framework.ProxyFactoryBean}
 * 或 {@link org.springframework.jndi.JndiObjectFactoryBean}.
 * 它也可以用于自定义组件; 但是, 这仅适用于基础架构代码.
 *
 * <p><b>{@code FactoryBean}是一个程序化合同. 实现不应该依赖于注解驱动的注入或其他反射机制.</b>
 * {@link #getObjectType()} {@link #getObject()} 调用可能会在引导过程的早期到达, 甚至领先于任何后处理器设置.
 * 如果需要访问其他bean, 实现{@link BeanFactoryAware}并以编程方式获取它们.
 *
 * <p>最终, FactoryBean对象参与包含BeanFactory的bean创建的同步.
 * 除了FactoryBean本身内的延迟初始化之外, 通常不需要内部同步 (or the like).
 */
public interface FactoryBean<T> {

	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）.
	 * <p>与{@link BeanFactory}一样, 这允许支持Singleton和Prototype设计模式.
	 * <p>如果此FactoryBean在调用时尚未完全初始化 (例如, 因为它涉及循环引用),
	 * 抛出相应的 {@link FactoryBeanNotInitializedException}.
	 * <p>截止Spring 2.0, 允许FactoryBeans返回{@code null}对象.
	 * 工厂会将此视为正常值; 在这种情况下, 它不再抛出FactoryBeanNotInitializedException.
	 * 鼓励FactoryBean实现视情况自己抛出FactoryBeanNotInitializedException.
	 * 
	 * @return bean的一个实例 (can be {@code null})
	 * @throws Exception 如果出现创建错误
	 */
	T getObject() throws Exception;

	/**
	 * 返回此FactoryBean创建的对象类型, 或{@code null}如果事先不知道的话.
	 * <p>这允许人们在不实例化对象的情况下检查特定类型的bean, 例如在自动装配上.
	 * <p>在创建单例对象的实现的情况下, 这种方法应该尽量避免单例创建; 它应该提前估计类型.
	 * 对于原型, 在这里返回一个有意义的类型也是可取的.
	 * <p>可以在完全初始化此FactoryBean之前调用此方法.
	 * 它不能依赖于初始化期间创建的状态; 当然, 它仍然可以使用这种状态.
	 * <p><b>NOTE:</b> 自动装配将忽略在此处返回{@code null}的FactoryBeans.
	 * 因此, 强烈建议正确实现此方法, 使用FactoryBean的当前状态.
	 * 
	 * @return 此FactoryBean创建的对象类型, 或{@code null}如果在调用时不知道
	 */
	Class<?> getObjectType();

	/**
	 * 该工厂管理的对象是否为单例? 即, {@link #getObject()}是否总是返回相同的对象 (可以缓存的引用)?
	 * <p><b>NOTE:</b> 如果FactoryBean指示持有单例对象, 从{@code getObject()}返回的对象可能会被BeanFactory缓存.
	 * 因此, 除非FactoryBean始终公开相同的引用, 否则不返回{@code true}.
	 * <p>FactoryBean本身的单例状态通常由其BeanFactory提供; 通常, 它必须被定义为单例.
	 * <p><b>NOTE:</b> 此方法返回{@code false}不一定表示返回的对象是独立的实例.
	 * 扩展的{@link SmartFactoryBean}接口的实现可以通过其{@link SmartFactoryBean#isPrototype()}方法显式的指示独立的实例.
	 * 如果{@code isSingleton()}实现返回{@code false}, 那么简单地假定不实现此扩展接口的普通{@link FactoryBean}实现总是返回独立的实例.
	 * 
	 * @return 暴露的对象是否是单例
	 */
	boolean isSingleton();

}
