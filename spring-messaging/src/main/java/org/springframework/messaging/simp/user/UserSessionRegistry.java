package org.springframework.messaging.simp.user;

import java.util.Set;

/**
 * 添加和删​​除用户会话的约定.
 *
 * <p>从4.2开始, 此接口被{@link SimpUserRegistry}取代, 公开了返回所有已注册用户的方法, 并为每个用户提供更广泛的信息.
 *
 * @deprecated in favor of {@link SimpUserRegistry} in combination with
 * {@link org.springframework.context.ApplicationListener} listening for
 * {@link org.springframework.web.socket.messaging.AbstractSubProtocolEvent} events.
 */
@Deprecated
public interface UserSessionRegistry {

	/**
	 * 返回用户的活动会话ID.
	 * 返回的Set是永远不会被修改的快照.
	 * 
	 * @param userName 要查找的用户
	 * 
	 * @return 具有0个或更多会话ID的集合, never {@code null}.
	 */
	Set<String> getSessionIds(String userName);

	/**
	 * 为用户注册活动会话ID.
	 * 
	 * @param userName 用户名
	 * @param sessionId 会话id
	 */
	void registerSessionId(String userName, String sessionId);

	/**
	 * 取消注册用户的活动会话ID.
	 * 
	 * @param userName 用户名
	 * @param sessionId 会话id
	 */
	void unregisterSessionId(String userName, String sessionId);

}
