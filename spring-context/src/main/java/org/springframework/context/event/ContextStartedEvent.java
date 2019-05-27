package org.springframework.context.event;

import org.springframework.context.ApplicationContext;

/**
 * {@code ApplicationContext}启动时引发的事件.
 */
@SuppressWarnings("serial")
public class ContextStartedEvent extends ApplicationContextEvent {

	/**
	 * @param source 已启动的{@code ApplicationContext} (must not be {@code null})
	 */
	public ContextStartedEvent(ApplicationContext source) {
		super(source);
	}

}
