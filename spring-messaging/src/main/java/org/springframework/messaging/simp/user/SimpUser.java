package org.springframework.messaging.simp.user;

import java.util.Set;

/**
 * 表示已连接的用户.
 */
public interface SimpUser {

	/**
	 * 唯一的用户名.
	 */
	String getName();

	/**
	 * 用户是否有任何会话.
	 */
	boolean hasSessions();

	/**
	 * 查找给定id的会话.
	 * 
	 * @param sessionId 会话id
	 * 
	 * @return {@code null}的匹配会话.
	 */
	SimpSession getSession(String sessionId);

	/**
	 * 返回用户的会话.
	 * 返回的Set是一个副本, 永远不会被修改.
	 * 
	 * @return 会话id, 或空集合.
	 */
	Set<SimpSession> getSessions();

}
