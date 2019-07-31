package org.springframework.web.method.annotation;

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

/**
 * 解析使用@{@link RequestParam}注解的方法参数, {@link MultipartFile}类型的参数,
 * 以及Spring的{@link MultipartResolver}抽象,
 * 与Servlet 3.0 multipart请求一起使用的{@code javax.servlet.http.Part}类型的参数.
 * 此解析器也可以在默认解析模式下创建, 其中未使用{@link RequestParam @RequestParam}注解的简单类型(int, long, etc.),
 * 也被视为请求参数, 参数名称派生自参数名称.
 *
 * <p>如果方法参数类型为{@link Map}, 则注解中指定的名称用于解析请求参数String值.
 * 然后通过类型转换将值转换为{@link Map}, 假设已经注册了合适的{@link Converter}或{@link PropertyEditor}.
 * 或者, 如果未指定请求参数名称, 则使用{@link RequestParamMapMethodArgumentResolver}来提供对Map形式的所有请求参数的访问.
 *
 * <p>调用{@link WebDataBinder}以将类型转换应用于尚未与方法参数类型匹配的已解析的请求header值.
 */
public class RequestParamMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver
		implements UriComponentsContributor {

	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);

	private final boolean useDefaultResolution;


	/**
	 * @param useDefaultResolution 在默认解析模式下, {@link BeanUtils#isSimpleProperty}中定义的简单类型的方法参数被视为请求参数,
	 * 即使它未被注解, 请求参数名称也是从方法参数名称派生的.
	 */
	public RequestParamMethodArgumentResolver(boolean useDefaultResolution) {
		this.useDefaultResolution = useDefaultResolution;
	}

	/**
	 * @param beanFactory 用于在默认值中解析 ${...}占位符和 #{...} SpEL表达式的bean工厂;
	 * 或{@code null}, 如果默认值不包含表达式
	 * @param useDefaultResolution 在默认解析模式下, {@link BeanUtils#isSimpleProperty}中定义的简单类型的方法参数被视为请求参数,
	 * 即使它未被注解, 请求参数名称也是从方法参数名称派生的.
	 */
	public RequestParamMethodArgumentResolver(ConfigurableBeanFactory beanFactory, boolean useDefaultResolution) {
		super(beanFactory);
		this.useDefaultResolution = useDefaultResolution;
	}


	/**
	 * 支持以下值:
	 * <ul>
	 * <li>使用@RequestParam注解的方法参数.
	 * 这排除了{@link Map}参数, 其中注解未指定名称.
	 * {@link Map}参数需要使用{@link RequestParamMapMethodArgumentResolver}.
	 * <li>类型为{@link MultipartFile}的参数, 除非使用@{@link RequestPart}注解.
	 * <li>类型为{@code javax.servlet.http.Part}的参数, 除非使用@{@link RequestPart}注解.
	 * <li>在默认解析模式下, 即使没有 @{@link RequestParam}注解的简单类型参数.
	 * </ul>
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(RequestParam.class)) {
			if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
				String paramName = parameter.getParameterAnnotation(RequestParam.class).name();
				return StringUtils.hasText(paramName);
			}
			else {
				return true;
			}
		}
		else {
			if (parameter.hasParameterAnnotation(RequestPart.class)) {
				return false;
			}
			parameter = parameter.nestedIfOptional();
			if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
				return true;
			}
			else if (this.useDefaultResolution) {
				return BeanUtils.isSimpleProperty(parameter.getNestedParameterType());
			}
			else {
				return false;
			}
		}
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
		return (ann != null ? new RequestParamNamedValueInfo(ann) : new RequestParamNamedValueInfo());
	}

	@Override
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
		MultipartHttpServletRequest multipartRequest =
				WebUtils.getNativeRequest(servletRequest, MultipartHttpServletRequest.class);

		Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, servletRequest);
		if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
			return mpArg;
		}

		Object arg = null;
		if (multipartRequest != null) {
			List<MultipartFile> files = multipartRequest.getFiles(name);
			if (!files.isEmpty()) {
				arg = (files.size() == 1 ? files.get(0) : files);
			}
		}
		if (arg == null) {
			String[] paramValues = request.getParameterValues(name);
			if (paramValues != null) {
				arg = (paramValues.length == 1 ? paramValues[0] : paramValues);
			}
		}
		return arg;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter, NativeWebRequest request)
			throws Exception {

		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
		if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
			if (!MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
				throw new MultipartException("Current request is not a multipart request");
			}
			else {
				throw new MissingServletRequestPartException(name);
			}
		}
		else {
			throw new MissingServletRequestParameterException(name,
					parameter.getNestedParameterType().getSimpleName());
		}
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		Class<?> paramType = parameter.getNestedParameterType();
		if (Map.class.isAssignableFrom(paramType) || MultipartFile.class == paramType ||
				"javax.servlet.http.Part".equals(paramType.getName())) {
			return;
		}

		RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
		String name = (requestParam == null || StringUtils.isEmpty(requestParam.name()) ?
				parameter.getParameterName() : requestParam.name());

		if (value == null) {
			if (requestParam != null) {
				if (!requestParam.required() || !requestParam.defaultValue().equals(ValueConstants.DEFAULT_NONE)) {
					return;
				}
			}
			builder.queryParam(name);
		}
		else if (value instanceof Collection) {
			for (Object element : (Collection<?>) value) {
				element = formatUriValue(conversionService, TypeDescriptor.nested(parameter, 1), element);
				builder.queryParam(name, element);
			}
		}
		else {
			builder.queryParam(name, formatUriValue(conversionService, new TypeDescriptor(parameter), value));
		}
	}

	protected String formatUriValue(ConversionService cs, TypeDescriptor sourceType, Object value) {
		if (value == null) {
			return null;
		}
		else if (value instanceof String) {
			return (String) value;
		}
		else if (cs != null) {
			return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
		}
		else {
			return value.toString();
		}
	}


	private static class RequestParamNamedValueInfo extends NamedValueInfo {

		public RequestParamNamedValueInfo() {
			super("", false, ValueConstants.DEFAULT_NONE);
		}

		public RequestParamNamedValueInfo(RequestParam annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
