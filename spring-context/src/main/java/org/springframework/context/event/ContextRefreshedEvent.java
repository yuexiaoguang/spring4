package org.springframework.context.event;

import org.springframework.context.ApplicationContext;

/**
 * {@code ApplicationContext}初始化或刷新时引发的事件.
 */
@SuppressWarnings("serial")
public class ContextRefreshedEvent extends ApplicationContextEvent {

	/**
	 * @param source 已初始化或刷新的{@code ApplicationContext} (must not be {@code null})
	 */
	public ContextRefreshedEvent(ApplicationContext source) {
		super(source);
	}

}
