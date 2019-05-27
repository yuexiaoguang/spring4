package org.springframework.messaging.simp;

/**
 * 在简单消息协议(如STOMP)中找到的各种消息的通用表示.
 */
public enum SimpMessageType {

	CONNECT,

	CONNECT_ACK,

	MESSAGE,

	SUBSCRIBE,

	UNSUBSCRIBE,

	HEARTBEAT,

	DISCONNECT,

	DISCONNECT_ACK,

	OTHER;

}
