package org.springframework.web.socket.sockjs.transport;

import org.springframework.web.socket.WebSocketSession;

/**
 * Spring标准的{@link WebSocketSession}的SockJS扩展.
 */
public interface SockJsSession extends WebSocketSession {

	/**
	 * 返回自会话上次活动以来的时间 (以毫秒为单位), 否则如果会话是新的, 则返回自会话创建以来的时间.
	 */
	long getTimeSinceLastActive();

	/**
	 * 禁用SockJS心跳, 可能是因为更高级别的协议已经为会话启用了心跳.
	 * 建议不要禁用此功能, 因为它可以帮助代理知道连接没有挂起.
	 */
	void disableHeartbeat();

}
