package org.springframework.messaging.simp.user;

import java.util.Set;

/**
 * 表示已连接用户的会话.
 */
public interface SimpSession {

	/**
	 * 返回会话ID.
	 */
	String getId();

	/**
	 * 返回与会话关联的用户.
	 */
	SimpUser getUser();

	/**
	 * 返回此会话的订阅.
	 */
	Set<SimpSubscription> getSubscriptions();

}
