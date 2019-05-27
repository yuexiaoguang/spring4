package org.springframework.messaging.simp.user;

/**
 * 表示用户会话中的订阅.
 */
public interface SimpSubscription {

	/**
	 * 返回订阅关联的ID, never {@code null}.
	 */
	String getId();

	/**
	 * 返回订阅的会话, never {@code null}.
	 */
	SimpSession getSession();

	/**
	 * 返回订阅的目标, never {@code null}.
	 */
	String getDestination();

}
