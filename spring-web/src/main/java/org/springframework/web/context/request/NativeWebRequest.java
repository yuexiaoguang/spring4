package org.springframework.web.context.request;

/**
 * 扩展{@link WebRequest}接口, 以通用方式公开本机请求和响应对象.
 *
 * <p>主要用于框架内部使用, 特别是用于通用参数解析代码.
 */
public interface NativeWebRequest extends WebRequest {

	/**
	 * 返回底层本机请求对象.
	 */
	Object getNativeRequest();

	/**
	 * 返回底层本机响应对象.
	 */
	Object getNativeResponse();

	/**
	 * 返回底层本机请求对象.
	 * 
	 * @param requiredType 请求对象的所需类型
	 * 
	 * @return 匹配的请求对象, 或{@code null}
	 */
	<T> T getNativeRequest(Class<T> requiredType);

	/**
	 * 返回底层本机响应对象.
	 * 
	 * @param requiredType 响应对象的所需类型
	 * 
	 * @return 匹配的响应对象, 或{@code null}
	 */
	<T> T getNativeResponse(Class<T> requiredType);

}
