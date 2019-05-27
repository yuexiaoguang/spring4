package org.springframework.context.event;

import org.springframework.context.ApplicationContext;

/**
 * {@code ApplicationContext}关闭时引发的事件.
 */
@SuppressWarnings("serial")
public class ContextClosedEvent extends ApplicationContextEvent {

	/**
	 * @param source 已关闭的{@code ApplicationContext} (must not be {@code null})
	 */
	public ContextClosedEvent(ApplicationContext source) {
		super(source);
	}

}
