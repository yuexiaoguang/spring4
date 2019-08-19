package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * 允许在读取主体并将其转换为Object之前自定义请求,
 * 并允许在将结果作为{@code @RequestBody}或{@code HttpEntity}方法参数传递给控制器​​方法之前处理生成的Object.
 *
 * <p>此约定的实现可以直接在{@code RequestMappingHandlerAdapter}注册,
 * 或者更可能使用{@code @ControllerAdvice}注解, 在这种情况下, 它们会被自动检测到.
 */
public interface RequestBodyAdvice {

	/**
	 * 首先调用以确定此拦截器是否适用.
	 * 
	 * @param methodParameter 方法参数
	 * @param targetType 目标类型, 不一定与方法参数类型相同, e.g. 用于{@code HttpEntity<String>}.
	 * @param converterType 选定的转换器类型
	 * 
	 * @return 是否应该调用此拦截器
	 */
	boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * 如果正文为空, 第二个 (或最后一个)调用.
	 * 
	 * @param body 在调用第一个增强之前设置为{@code null}
	 * @param inputMessage 请求
	 * @param parameter 方法参数
	 * @param targetType 目标类型, 不一定与方法参数类型相同, e.g. 用于{@code HttpEntity<String>}.
	 * @param converterType 选定的转换器类型
	 * 
	 * @return 要使用的值或{@code null}, 如果需要参数, 则可能会引发{@code HttpMessageNotReadableException}
	 */
	Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * 在读取和转换请求正文之前, 第二个调用.
	 * 
	 * @param inputMessage 请求
	 * @param parameter 目标方法参数
	 * @param targetType 目标类型, 不一定与方法参数类型相同, e.g. 用于{@code HttpEntity<String>}.
	 * @param converterType 用于反序列化正文的转换器
	 * 
	 * @return 输入请求或新实例, never {@code null}
	 */
	HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException;

	/**
	 * 在请求主体转换为Object之后, 第三个 (和最后一个)调用.
	 * 
	 * @param body 在调用第一个增强之前, 设置为转换器对象
	 * @param inputMessage 请求
	 * @param parameter 目标方法参数
	 * @param targetType 目标类型, 不一定与方法参数类型相同, e.g. 用于{@code HttpEntity<String>}.
	 * @param converterType 用于反序列化正文的转换器
	 * 
	 * @return 相同的主体或新的实例
	 */
	Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType);

}
