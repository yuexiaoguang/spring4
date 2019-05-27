package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;

/**
 * 在事件监听器表达式求值期间, 使用的根对象.
 */
class EventExpressionRootObject {

	private final ApplicationEvent event;

	private final Object[] args;

	public EventExpressionRootObject(ApplicationEvent event, Object[] args) {
		this.event = event;
		this.args = args;
	}

	public ApplicationEvent getEvent() {
		return event;
	}

	public Object[] getArgs() {
		return args;
	}

}
