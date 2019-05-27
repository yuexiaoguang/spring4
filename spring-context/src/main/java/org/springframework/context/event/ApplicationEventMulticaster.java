package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;

/**
 * 由可以管理许多{@link ApplicationListener}对象并向其发布事件的对象实现的接口.
 *
 * <p>{@link org.springframework.context.ApplicationEventPublisher},
 * 通常是Spring {@link org.springframework.context.ApplicationContext},
 * 可以使用ApplicationEventMulticaster作为实际发布事件的委托.
 */
public interface ApplicationEventMulticaster {

	/**
	 * 添加一个监听器以通知所有事件.
	 * 
	 * @param listener 要添加的监听器
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * 添加一个监听器bean以通知所有事件.
	 * 
	 * @param listenerBeanName 要添加的监听器的名称
	 */
	void addApplicationListenerBean(String listenerBeanName);

	/**
	 * 从通知列表中删除监听器.
	 * 
	 * @param listener 要删除的监听器
	 */
	void removeApplicationListener(ApplicationListener<?> listener);

	/**
	 * 从通知列表中删除监听器.
	 * 
	 * @param listenerBeanName 要删除的监听器的名称
	 */
	void removeApplicationListenerBean(String listenerBeanName);

	/**
	 * 删除在此多播器中注册的所有监听器.
	 * <p>在删除调用之后, 多播器将不会对事件通知执行任何操作, 直到注册新的监听器.
	 */
	void removeAllListeners();

	/**
	 * 将给定的应用程序事件多播到适当的监听器.
	 * <p>如果可能, 请考虑使用{@link #multicastEvent(ApplicationEvent, ResolvableType)},
	 * 因为它为基于泛型的事件提供了更好的支持.
	 * 
	 * @param event 要多播的事件
	 */
	void multicastEvent(ApplicationEvent event);

	/**
	 * 将给定的应用程序事件多播到适当的监听器.
	 * <p>如果{@code eventType} 是 {@code null}, 默认类型是基于{@code event}实例构建的.
	 * 
	 * @param event 要多播的事件
	 * @param eventType 事件类型 (可以是 null)
	 */
	void multicastEvent(ApplicationEvent event, ResolvableType eventType);

}
