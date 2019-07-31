package org.springframework.http.converter.json;

import com.fasterxml.jackson.databind.ser.FilterProvider;

/**
 * 要通过{@link MappingJackson2HttpMessageConverter}序列化的POJO的简单保存器,
 * 以及要传递给转换器的更多序列化指令.
 *
 * <p>在服务器端, 此包装器添加了{@code ResponseBodyInterceptor},
 * 在内容协商选择要使用的转换器之后, 但在写入之前.
 *
 * <p>在客户端，只需包装POJO并将其传递给{@code RestTemplate}.
 */
public class MappingJacksonValue {

	private Object value;

	private Class<?> serializationView;

	private FilterProvider filters;

	private String jsonpFunction;


	/**
	 * @param value 要序列化的对象
	 */
	public MappingJacksonValue(Object value) {
		this.value = value;
	}


	/**
	 * 修改要序列化的POJO.
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 返回需要序列化的POJO.
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * 设置序列化POJO使用的序列化视图.
	 */
	public void setSerializationView(Class<?> serializationView) {
		this.serializationView = serializationView;
	}

	/**
	 * 返回要使用的序列化视图.
	 */
	public Class<?> getSerializationView() {
		return this.serializationView;
	}

	/**
	 * 设置序列化POJO使用的Jackson过滤器提供者.
	 */
	public void setFilters(FilterProvider filters) {
		this.filters = filters;
	}

	/**
	 * 返回要使用的Jackson过滤器提供者.
	 */
	public FilterProvider getFilters() {
		return this.filters;
	}

	/**
	 * 设置JSONP函数名称的名称.
	 * 
	 * @deprecated Will be removed as of Spring Framework 5.1, use
	 * <a href="https://docs.spring.io/spring/docs/4.3.x/spring-framework-reference/html/cors.html">CORS</a> instead.
	 */
	@Deprecated
	public void setJsonpFunction(String functionName) {
		this.jsonpFunction = functionName;
	}

	/**
	 * 返回配置的JSONP函数名称.
	 * 
	 * @deprecated Will be removed as of Spring Framework 5.1, use
	 * <a href="https://docs.spring.io/spring/docs/4.3.x/spring-framework-reference/html/cors.html">CORS</a> instead.
	 */
	@Deprecated
	public String getJsonpFunction() {
		return this.jsonpFunction;
	}

}
