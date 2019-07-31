package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.InterceptingAsyncHttpAccessor;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 拦截客户端HTTP请求.
 * 可以使用{@link org.springframework.web.client.AsyncRestTemplate}
 * {@linkplain org.springframework.web.client.AsyncRestTemplate#setInterceptors 注册}此接口的实现,
 * 以修改传出的{@link HttpRequest}和/或注册以在
 * {@link org.springframework.util.concurrent.ListenableFutureAdapter}
 * 的帮助下修改传入的{@link ClientHttpResponse}.
 *
 * <p>拦截器的主要入口是{@link #intercept}.
 */
public interface AsyncClientHttpRequestInterceptor {

	/**
	 * 拦截给定的请求, 并返回异步的响应.
	 * 给定的{@link AsyncClientHttpRequestExecution}允许拦截器将请求传递给链中的下一个实体.
	 * <p>实现可能遵循这种模式:
	 * <ol>
	 * <li>检查{@linkplain HttpRequest 请求}和正文</li>
	 * <li>可选地{@linkplain org.springframework.http.client.support.HttpRequestWrapper 包装}请求, 以过滤HTTP属性的.</li>
	 * <li>可选地修改请求的正文.</li>
	 * <li>以下之一:
	 * <ul>
	 * <li>通过{@link ClientHttpRequestExecution}执行请求</li>
	 * <li>不要执行完全阻塞执行的请求</li>
	 * </ul>
	 * <li>可选地在{@link org.springframework.util.concurrent.ListenableFutureAdapter ListenableFutureAdapter}
	 * 的帮助下适配响应以过滤HTTP属性.</li>
	 * </ol>
	 * 
	 * @param request 请求, 包含方法, URI, 和header
	 * @param body 请求的主体
	 * @param execution 请求执行
	 * 
	 * @return 异步的响应
	 * @throws IOException
	 */
	ListenableFuture<ClientHttpResponse> intercept(HttpRequest request, byte[] body,
			AsyncClientHttpRequestExecution execution) throws IOException;

}
