package org.springframework.http;

/**
 * 表示HTTP请求和响应消息的基础接口.
 * 由{@link HttpHeaders}组成, 通过{@link #getHeaders()}检索.
 */
public interface HttpMessage {

	/**
	 * 返回此消息的header.
	 * 
	 * @return 相应的HttpHeader对象 (never {@code null})
	 */
	HttpHeaders getHeaders();

}
