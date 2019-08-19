package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;
import org.springframework.web.multipart.support.RequestPartServletServerHttpRequest;

/**
 * 解析以下方法参数:
 * <ul>
 * <li>使用{@code @RequestPart}注解
 * <li>类型为{@link MultipartFile}与Spring的{@link MultipartResolver}抽象相结合
 * <li>类型为{@code javax.servlet.http.Part}与Servlet 3.0 multipart请求一起使用
 * </ul>
 *
 * <p>当参数使用{@code @RequestPart}注解时, part的内容将通过{@link HttpMessageConverter}传递,
 * 以解析方法参数, 并考虑请求部分的'Content-Type'.
 * 这与 @{@link RequestBody}根据常规请求的内容解析参数的方式类似.
 *
 * <p>如果参数未使用注解或未指定part的名称, 则它是从方法参数的名称派生的.
 *
 * <p>如果参数使用{@code @javax.validation.Valid}注解, 则可以应用自动验证.
 * 如果验证失败, 则会引发{@link MethodArgumentNotValidException}, 如果配置了
 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver},
 * 则返回400响应状态码.
 */
public class RequestPartMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {

	public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	/**
	 * 转换器和{@code Request~} 和{@code ResponseBodyAdvice}.
	 */
	public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters,
			List<Object> requestResponseBodyAdvice) {

		super(messageConverters, requestResponseBodyAdvice);
	}


	/**
	 * 支持以下值:
	 * <ul>
	 * <li>使用{@code @RequestPart}注解
	 * <li>类型为{@link MultipartFile}, 除非使用{@code @RequestParam}注解
	 * <li>类型为{@code javax.servlet.http.Part}, 除非使用{@code @RequestParam}注解
	 * </ul>
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(RequestPart.class)) {
			return true;
		}
		else {
			if (parameter.hasParameterAnnotation(RequestParam.class)) {
				return false;
			}
			return MultipartResolutionDelegate.isMultipartArgument(parameter.nestedIfOptional());
		}
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
		RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
		boolean isRequired = ((requestPart == null || requestPart.required()) && !parameter.isOptional());

		String name = getPartName(parameter, requestPart);
		parameter = parameter.nestedIfOptional();
		Object arg = null;

		Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, servletRequest);
		if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
			arg = mpArg;
		}
		else {
			try {
				HttpInputMessage inputMessage = new RequestPartServletServerHttpRequest(servletRequest, name);
				arg = readWithMessageConverters(inputMessage, parameter, parameter.getNestedGenericParameterType());
				WebDataBinder binder = binderFactory.createBinder(request, arg, name);
				if (arg != null) {
					validateIfApplicable(binder, parameter);
					if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
						throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
					}
				}
				mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
			}
			catch (MissingServletRequestPartException ex) {
				if (isRequired) {
					throw ex;
				}
			}
			catch (MultipartException ex) {
				if (isRequired) {
					throw ex;
				}
			}
		}

		if (arg == null && isRequired) {
			if (!MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
				throw new MultipartException("Current request is not a multipart request");
			}
			else {
				throw new MissingServletRequestPartException(name);
			}
		}
		return adaptArgumentIfNecessary(arg, parameter);
	}

	private String getPartName(MethodParameter methodParam, RequestPart requestPart) {
		String partName = (requestPart != null ? requestPart.name() : "");
		if (partName.isEmpty()) {
			partName = methodParam.getParameterName();
			if (partName == null) {
				throw new IllegalArgumentException("Request part name for argument type [" +
						methodParam.getNestedParameterType().getName() +
						"] not specified, and parameter name information not found in class file either.");
			}
		}
		return partName;
	}

}
