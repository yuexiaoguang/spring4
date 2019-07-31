package org.springframework.web.bind.support;

/**
 * 简单的接口, 可以注入到处理器方法中, 允许它们发出会话处理完成的信号.
 * 然后, 处理器调用者可以跟进适当的清理, e.g. 在此处理器处理期间隐式创建的会话属性
 * (根据 {@link org.springframework.web.bind.annotation.SessionAttributes @SessionAttributes}注解).
 */
public interface SessionStatus {

	/**
	 * 将当前处理器的会话处理标记为已完成, 以便清除会话属性.
	 */
	void setComplete();

	/**
	 * 返回当前处理器的会话处理是否标记为已完成.
	 */
	boolean isComplete();

}
