package org.springframework.beans.factory;

/**
 * 要在销毁时释放资源的bean实现的接口.
 * {@link BeanFactory}将在scoped bean的单个销毁上调用destroy方法.
 * {@link org.springframework.context.ApplicationContext}应该在关闭时处理它的所有单例, 由应用程序生命周期驱动.
 *
 * <p>Spring管理的bean也可以为了相同的目的实现Java的{@link AutoCloseable}接口.
 * 实现接口的替代方法是指定自定义destroy方法, 例如在XML bean定义中.
 * 有关所有Bean生命周期方法的列表, see the {@link BeanFactory BeanFactory javadocs}.
 */
public interface DisposableBean {

	/**
	 * {@code BeanFactory}在销毁bean时调用.
	 * 
	 * @throws Exception 在关闭错误的情况下. 异常将被记录, 但不会被重新抛出以允许其他bean也释放其资源.
	 */
	void destroy() throws Exception;

}
