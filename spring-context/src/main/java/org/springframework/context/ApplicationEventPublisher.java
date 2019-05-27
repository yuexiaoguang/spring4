package org.springframework.context;

/**
 * 封装事件发布功能的接口.
 * 用作{@link ApplicationContext}的超级接口.
 */
public interface ApplicationEventPublisher {

	/**
	 * 通知在应用程序事件的此应用程序中注册的所有匹配监听器.
	 * 事件可以是框架事件 (例如 RequestHandledEvent) 或特定于应用程序的事件.
	 * 
	 * @param event 要发布的事件
	 */
	void publishEvent(ApplicationEvent event);

	/**
	 * 通知在此应用程序中注册的所有匹配的监听器.
	 * <p>如果指定的{@code event}不是{@link ApplicationEvent}, 则它包含在{@link PayloadApplicationEvent}中.
	 * 
	 * @param event 要发布的事件
	 */
	void publishEvent(Object event);

}
