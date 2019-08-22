package org.springframework.web.socket.sockjs;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;

/**
 * 处理来自SockJS客户端的HTTP请求的主要入口点.
 *
 * <p>在Servlet 3+ 容器中, 可以使用
 * {@link org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler}来调用此服务.
 * 处理servlet以及所涉及的所有过滤器必须通过ServletContext API启用异步支持,
 * 或者通过向web.xml中的servlet和过滤器声明添加{@code <async-support>true</async-support>}元素.
 */
public interface SockJsService {

	/**
	 * 处理SockJS HTTP请求.
	 * <p>有关预期的URL类型的详细信息, 请参阅
	 * <a href="http://sockjs.github.io/sockjs-protocol/sockjs-protocol-0.3.3.html">SockJS protocol</a>
	 * "Base URL", "Static URLs", 和"Session URLs"部分.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * @param sockJsPath SockJS服务前缀中路径的其余部分
	 * @param handler 将与SockJS客户端交换消息的处理器
	 * 
	 * @throws SockJsException 请求处理失败时引发;
	 * 通常, 尝试向客户端发送消息失败会自动关闭SockJS会话, 并引发{@link SockJsTransportFailureException};
	 * 尝试从客户端读取消息失败不会自动关闭会话, 并可能导致{@link SockJsMessageDeliveryException}或{@link SockJsException};
	 * 来自WebSocketHandler的异常可以在内部处理, 也可以通过{@link ExceptionWebSocketHandlerDecorator}或其他一些装饰器来处理.
	 * 使用{@link org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler}时会自动添加前者.
	 */
	void handleRequest(ServerHttpRequest request, ServerHttpResponse response, String sockJsPath,
			WebSocketHandler handler) throws SockJsException;

}
