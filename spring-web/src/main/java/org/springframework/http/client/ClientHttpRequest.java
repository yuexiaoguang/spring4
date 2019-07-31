package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRequest;

/**
 * 表示客户端HTTP请求.
 * 通过{@link ClientHttpRequestFactory}的实现创建.
 *
 * <p>可以{@linkplain #execute() 执行}{@code ClientHttpRequest}, 接收{@link ClientHttpResponse}.
 */
public interface ClientHttpRequest extends HttpRequest, HttpOutputMessage {

	/**
	 * 执行此请求, 生成可以读取的 {@link ClientHttpResponse}.
	 * 
	 * @return 执行的响应结果
	 * @throws IOException
	 */
	ClientHttpResponse execute() throws IOException;

}
