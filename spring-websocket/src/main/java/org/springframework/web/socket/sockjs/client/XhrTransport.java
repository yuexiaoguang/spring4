package org.springframework.web.socket.sockjs.client;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;

/**
 * 使用HTTP请求来模拟WebSocket交互的SockJS {@link Transport}.
 * 基本{@code Transport}接口的{@code connect}方法用于接收来自服务器的消息,
 * 而{@link #executeSendRequest}方法用于发送消息.
 */
public interface XhrTransport extends Transport, InfoReceiver {

	/**
	 * {@code XhrTransport}支持"xhr_streaming"和"xhr" SockJS服务器传输.
	 * 从客户的角度来看， 没有实现差异.
	 * <p>默认情况下, {@code XhrTransport}将首先与"xhr_streaming"一起使用, 如果流无法连接, 然后与"xhr"一起使用.
	 * 在某些情况下, 抑制流式传输以便仅使用"xhr".
	 */
	boolean isXhrStreamingDisabled();

	/**
	 * 执行将消息发送到服务器的请求.
	 * <p>请注意, 从4.2开始, 此方法接受{@code headers}参数.
	 * 
	 * @param transportUrl 发送消息的URL.
	 * @param message 要发送的消息
	 */
	void executeSendRequest(URI transportUrl, HttpHeaders headers, TextMessage message);

}
