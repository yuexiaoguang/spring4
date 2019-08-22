package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.security.Principal;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

/**
 * 通常向{@link Transport} 和 {@link AbstractClientSockJsSession session}实现,
 * 公开有关通过给定传输连接到SockJS服务器端点的请求的信息.
 *
 * <p>请注意, 通过{@link SockJsClient}连接的单个请求可能会导致{@link TransportRequest}的多个实例, 在成功建立连接之前每个传输一个.
 */
public interface TransportRequest {

	/**
	 * 返回有关SockJS URL的信息, 包括服务器和会话ID.
	 */
	SockJsUrlInfo getSockJsUrlInfo();

	/**
	 * 返回与连接请求一起发送的header.
	 */
	HttpHeaders getHandshakeHeaders();

	/**
	 * 返回添加到除握手请求之外的所有其他HTTP请求的header, 例如XHR接收和发送请求.
	 */
	HttpHeaders getHttpRequestHeaders();

	/**
	 * 返回给定传输的传输URL.
	 * <p>对于{@link XhrTransport}, 这是用于接收消息的URL.
	 */
	URI getTransportUrl();

	/**
	 * 返回与请求关联的用户.
	 */
	Principal getUser();

	/**
	 * 返回用于编码SockJS消息的消息编解码器.
	 */
	SockJsMessageCodec getMessageCodec();

	/**
	 * 如果在计算的重新传输超时期限内未完全建立SockJS会话, 则注册超时清除任务以调用.
	 */
	void addTimeoutTask(Runnable runnable);

}
