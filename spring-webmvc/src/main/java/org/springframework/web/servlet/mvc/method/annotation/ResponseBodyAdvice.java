package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * 允许在执行{@code @ResponseBody}或{@code ResponseEntity}控制器方法之后,
 * 但在使用{@code HttpMessageConverter}写入正文之前自定义响应.
 *
 * <p>实现可以直接使用{@code RequestMappingHandlerAdapter}和{@code ExceptionHandlerExceptionResolver}注册,
 * 或者更可能使用{@code @ControllerAdvice}注解, 在这种情况下, 它们将被两者自动检测到.
 */
public interface ResponseBodyAdvice<T> {

	/**
	 * 此组件是否支持给定的控制器方法返回类型和所选的{@code HttpMessageConverter}类型.
	 * 
	 * @param returnType 返回类型
	 * @param converterType 选定的转换器类型
	 * 
	 * @return {@code true} 如果应该调用{@link #beforeBodyWrite}; 否则{@code false}
	 */
	boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * 在选择{@code HttpMessageConverter}并且在调用其write方法之前调用.
	 * 
	 * @param body 要写入的主体
	 * @param returnType 控制器方法的返回类型
	 * @param selectedContentType 通过内容协商选择的内容类型
	 * @param selectedConverterType 要写入响应的选择的转换器类型
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * 
	 * @return 传入的主体或修改过的 (可能是新的)实例
	 */
	T beforeBodyWrite(T body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response);

}
