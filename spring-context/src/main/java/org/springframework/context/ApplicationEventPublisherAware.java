package org.springframework.context;

import org.springframework.beans.factory.Aware;

/**
 * 希望被通知其运行的ApplicationEventPublisher (通常是 ApplicationContext)的对象实现的接口.
 */
public interface ApplicationEventPublisherAware extends Aware {

	/**
	 * 设置此对象运行的ApplicationEventPublisher.
	 * <p>在普通bean属性的填充之后, 但在初始化回调之前调用, 例如InitializingBean的afterPropertiesSet或自定义init方法.
	 * 在ApplicationContextAware的setApplicationContext之前调用.
	 * 
	 * @param applicationEventPublisher 此对象使用的事件发布者
	 */
	void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher);

}
