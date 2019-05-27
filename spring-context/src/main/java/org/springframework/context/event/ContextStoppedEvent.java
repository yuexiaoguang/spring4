package org.springframework.context.event;

import org.springframework.context.ApplicationContext;

/**
 * {@code ApplicationContext}停止时引发的事件.
 */
@SuppressWarnings("serial")
public class ContextStoppedEvent extends ApplicationContextEvent {

	/**
	 * @param source 已停止的{@code ApplicationContext} (must not be {@code null})
	 */
	public ContextStoppedEvent(ApplicationContext source) {
		super(source);
	}

}
