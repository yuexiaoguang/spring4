package org.springframework.web.socket.sockjs.client;

import java.net.URI;

import org.springframework.http.HttpHeaders;

/**
 * 可以执行需要在SockJS会话启动之前执行的SockJS "Info"请求的组件, 以便检查服务器端点功能, 例如端点是否允许使用WebSocket.
 *
 * <p>通常{@link XhrTransport}实现也是此约定的实现.
 */
public interface InfoReceiver {

	/**
	 * 执行对 SockJS "Info" URL的HTTP请求, 并返回生成的JSON响应内容, 或引发异常.
	 * <p>请注意, 从4.2开始, 此方法接受{@code headers}参数.
	 * 
	 * @param infoUrl 从中获取SockJS服务器信息的URL
	 * @param headers 用于请求的header
	 * 
	 * @return 响应的主体
	 */
	String executeInfoRequest(URI infoUrl, HttpHeaders headers);

}
