package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonInputMessage;

/**
 * {@link RequestBodyAdvice}实现, 增加了对在Spring MVC {@code @HttpEntity}或{@code @RequestBody}方法参数上
 * 声明的Jackson {@code @JsonView}注解的支持.
 *
 * <p>注解中指定的反序列化视图将传递给
 * {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter},
 * 然后使用它来反序列化请求主体.
 *
 * <p>请注意, 尽管{@code @JsonView}允许指定多个类, 但只有一个类参数时才支持使用请求正文增强.
 * 考虑使用复合接口.
 *
 * <p>{@code @JsonView}参数级使用需要Jackson 2.5或更高版本.
 */
public class JsonViewRequestBodyAdvice extends RequestBodyAdviceAdapter {

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {

		return (AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType) &&
				methodParameter.getParameterAnnotation(JsonView.class) != null);
	}

	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter methodParameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> selectedConverterType) throws IOException {

		JsonView annotation = methodParameter.getParameterAnnotation(JsonView.class);
		Class<?>[] classes = annotation.value();
		if (classes.length != 1) {
			throw new IllegalArgumentException(
					"@JsonView only supported for request body advice with exactly 1 class argument: " + methodParameter);
		}
		return new MappingJacksonInputMessage(inputMessage.getBody(), inputMessage.getHeaders(), classes[0]);
	}

}
