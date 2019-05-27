package org.springframework.beans.factory;

/**
 * 在{@link BeanFactory}引导期间, 在单例预实例化阶段结束时, 触发的回调接口.
 * 该接口可以由单例bean实现, 以便在常规单例实例化算法之后执行一些初始化,
 * 避免意外实时初始化的副作用 (e.g. 从 {@link ListableBeanFactory#getBeansOfType}调用).
 * 从这个意义上说, 它是{@link InitializingBean}的替代品, 它在bean的本地构建阶段结束时被触发.
 *
 * <p>这个回调变体有点类似于{@link org.springframework.context.event.ContextRefreshedEvent},
 * 但不需要{@link org.springframework.context.ApplicationListener}的实现,
 * 无需跨上下文层次结构等过滤上下文引用.
 * 它还意味着对{@code beans}包的依赖性更小, 并且受到独立的{@link ListableBeanFactory}实现的尊重,
 * 不只是在{@link org.springframework.context.ApplicationContext}环境中.
 *
 * <p><b>NOTE:</b> 如果您打算启动/管理异步任务,
 * 最好实现{@link org.springframework.context.Lifecycle}, 它为运行时管理提供了更丰富的模型, 并允许分阶段启动/关闭.
 */
public interface SmartInitializingSingleton {

	/**
	 * 在单例预实例化阶段结束时调用, 保证已经创建了所有常规单例bean.
	 * 此方法中的{@link ListableBeanFactory＃getBeansOfType}调用不会在引导期间触发意外的副作用.
	 * <p><b>NOTE:</b> {@link BeanFactory} 引导之后, 按需要延迟初始化的单例bean不会触发此回调,
	 * 而不是任何其他bean范围.
	 * 小心地将它用于具有预期引导语义的bean.
	 */
	void afterSingletonsInstantiated();

}
