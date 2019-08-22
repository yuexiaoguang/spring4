package org.springframework.web.socket.sockjs.client;

import java.util.List;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * SockJS传输的客户端实现.
 */
public interface Transport {

	/**
	 * 返回此传输可用于的SockJS传输类型.
	 * 特别是从客户的角度来看, XHR和XHR流之间没有区别, {@code XhrTransport}可以同时做到这两点.
	 */
	List<TransportType> getTransportTypes();

	/**
	 * 连接运输.
	 * 
	 * @param request 传输请求.
	 * @param webSocketHandler 将生命周期事件委托给的应用程序处理器.
	 * 
	 * @return 指示连接成功或失败的Future.
	 */
	ListenableFuture<WebSocketSession> connect(TransportRequest request, WebSocketHandler webSocketHandler);

}
