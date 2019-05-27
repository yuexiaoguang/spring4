package org.springframework.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;

/**
 * 希望被通知其运行的{@link ApplicationContext}的对象实现的接口.
 *
 * <p>例如, 当对象需要访问一组协作bean时, 实现此接口是有意义的.
 * 请注意, 通过bean引用进行配置比仅用于bean查找目的而实现此接口更好.
 *
 * <p>如果对象需要访问文件资源, 也可以实现此接口,
 * i.e. 想要调用 {@code getResource}, 想要发布应用程序事件, 或者需要访问MessageSource.
 * 但是, 最好实现更具体的{@link ResourceLoaderAware},
 * {@link ApplicationEventPublisherAware}或{@link MessageSourceAware}接口, 在这种特定情况下.
 *
 * <p>请注意, 文件资源依赖项也可以作为{@link org.springframework.core.io.Resource}类型的bean属性公开,
 * 通过字符串填充由bean工厂自动进行类型转换.
 * 这样就不需要为了访问特定的文件资源而实现任何回调接口.
 *
 * <p>{@link org.springframework.context.support.ApplicationObjectSupport} 是应用程序对象的便捷基类, 实现了这个接口.
 *
 * <p>有关所有bean生命周期方法的列表, 请参阅{@link org.springframework.beans.factory.BeanFactory BeanFactory javadocs}.
 */
public interface ApplicationContextAware extends Aware {

	/**
	 * 设置此对象运行的ApplicationContext.
	 * 通常, 此调用将用于初始化对象.
	 * <p>在普通bean属性填充之后, 但在回调之前调用,
	 * 例如{@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet()} 或自定义init方法.
	 * 在{@link ResourceLoaderAware#setResourceLoader}, {@link ApplicationEventPublisherAware#setApplicationEventPublisher},
	 * {@link MessageSourceAware}之后调用.
	 * 
	 * @param applicationContext 此对象使用的ApplicationContext对象
	 * 
	 * @throws ApplicationContextException 在上下文初始化错误的情况下
	 * @throws BeansException 如果由应用程序上下文方法抛出
	 */
	void setApplicationContext(ApplicationContext applicationContext) throws BeansException;

}
