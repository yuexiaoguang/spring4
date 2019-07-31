package org.springframework.http.server;

import java.net.InetSocketAddress;
import java.security.Principal;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpRequest;

/**
 * 表示服务器端HTTP请求.
 */
public interface ServerHttpRequest extends HttpRequest, HttpInputMessage {

	/**
	 * 返回包含经过身份验证的用户名的{@link java.security.Principal}实例.
	 * 如果用户尚未通过身份验证, 则该方法返回<code>null</code>.
	 */
	Principal getPrincipal();

	/**
	 * 返回接收请求的地址.
	 */
	InetSocketAddress getLocalAddress();

	/**
	 * 返回远程客户端的地址.
	 */
	InetSocketAddress getRemoteAddress();

	/**
	 * 返回一个允许将请求置于异步模式的控件, 以便响应保持打开状态, 直到从当前或另一个线程显式关闭.
	 */
	ServerHttpAsyncRequestControl getAsyncRequestControl(ServerHttpResponse response);

}
