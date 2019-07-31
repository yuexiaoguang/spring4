package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpRequest;

/**
 * 表示客户端HTTP请求执行的上下文.
 *
 * <p>用于调用拦截器链中的下一个拦截器, 或者 - 如果调用的拦截器是最后一个 - 执行请求本身.
 */
public interface ClientHttpRequestExecution {

	/**
	 * 使用给定的请求属性和正文执行请求, 并返回响应.
	 * 
	 * @param request 请求, 包含方法, URI, 和header
	 * @param body 要执行的请求的主体
	 * 
	 * @return 响应
	 * @throws IOException
	 */
	ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException;

}
