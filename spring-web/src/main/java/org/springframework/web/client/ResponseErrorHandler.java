package org.springframework.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;

/**
 * {@link RestTemplate}用于确定特定响应是否有错误的策略接口.
 */
public interface ResponseErrorHandler {

	/**
	 * 指示给定的响应是否有错误.
	 * <p>实现通常会检查响应的{@link ClientHttpResponse#getStatusCode() HttpStatus}.
	 * 
	 * @param response 要检查的响应
	 * 
	 * @return {@code true}如果响应有错误; 否则{@code false}
	 * @throws IOException
	 */
	boolean hasError(ClientHttpResponse response) throws IOException;

	/**
	 * 处理给定响应中的错误.
	 * <p>只有在{@link #hasError(ClientHttpResponse)}返回{@code true}时才会调用此方法.
	 * 
	 * @param response 有错误的响应
	 * 
	 * @throws IOException
	 */
	void handleError(ClientHttpResponse response) throws IOException;

}
