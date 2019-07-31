package org.springframework.web.cors;

import javax.servlet.http.HttpServletRequest;

/**
 * 由类(通常是HTTP请求处理器)实现的接口, 它根据提供的请求提供{@link CorsConfiguration}实例.
 */
public interface CorsConfigurationSource {

	/**
	 * 根据传入的请求返回{@link CorsConfiguration}.
	 * 
	 * @return 关联的{@link CorsConfiguration}, 或{@code null}
	 */
	CorsConfiguration getCorsConfiguration(HttpServletRequest request);

}
