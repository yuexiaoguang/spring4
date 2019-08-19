package org.springframework.web.servlet.mvc.method.annotation;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * {@link ResponseBodyAdvice}实现, 增加了对在Spring MVC {@code @RequestMapping}
 * 或{@code @ExceptionHandler}方法上声明的Jackson {@code @JsonView}注解的支持.
 *
 * <p>注解中指定的序列化视图将传递给
 * {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter},
 * 然后将使用它来序列化响应正文.
 *
 * <p>请注意, 尽管{@code @JsonView}允许指定多个类, 但只有一个类参数时才支持使用响应正文增强.
 * 考虑使用复合接口.
 */
public class JsonViewResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return super.supports(returnType, converterType) && returnType.hasMethodAnnotation(JsonView.class);
	}

	@Override
	protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {

		JsonView annotation = returnType.getMethodAnnotation(JsonView.class);
		Class<?>[] classes = annotation.value();
		if (classes.length != 1) {
			throw new IllegalArgumentException(
					"@JsonView only supported for response body advice with exactly 1 class argument: " + returnType);
		}
		bodyContainer.setSerializationView(classes[0]);
	}

}
