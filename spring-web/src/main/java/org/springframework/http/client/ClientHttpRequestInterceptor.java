package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpRequest;

/**
 * 拦截客户端HTTP请求.
 * 可以使用
 * {@link org.springframework.web.client.RestTemplate RestTemplate}
 * {@linkplain org.springframework.web.client.RestTemplate#setInterceptors 注册}
 * 此接口的实现, 以修改传出的{@link ClientHttpRequest}和/或传入的{@link ClientHttpResponse}.
 *
 * <p>拦截器的主要入口点是
 * {@link #intercept(HttpRequest, byte[], ClientHttpRequestExecution)}.
 */
public interface ClientHttpRequestInterceptor {

	/**
	 * 拦截给定的请求, 并返回响应.
	 * 给定的{@link ClientHttpRequestExecution}允许拦截器传递请求并响应链中的下一个实体.
	 * <p>该方法的典型实现将遵循以下模式:
	 * <ol>
	 * <li>检查{@linkplain HttpRequest 请求}和正文</li>
	 * <li>可选地{@linkplain org.springframework.http.client.support.HttpRequestWrapper 包装}请求, 以过滤HTTP 属性.</li>
	 * <li>可选地修改请求的正文.</li>
	 * <li><strong>或者</strong>
	 * <ul>
	 * <li>使用
	 * {@link ClientHttpRequestExecution#execute(org.springframework.http.HttpRequest, byte[])}执行请求,</li>
	 * <strong>或者</strong>
	 * <li>不要执行完全阻塞执行的请求.</li>
	 * </ul>
	 * <li>可选地包装响应以过滤HTTP属性.</li>
	 * </ol>
	 * 
	 * @param request 请求, 包含方法, URI, 和header
	 * @param body 请求的正文
	 * @param execution 请求执行
	 * 
	 * @return 响应
	 * @throws IOException
	 */
	ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException;

}
