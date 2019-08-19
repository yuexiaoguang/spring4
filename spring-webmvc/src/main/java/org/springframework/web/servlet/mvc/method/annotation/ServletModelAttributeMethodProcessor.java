package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Collections;
import java.util.Map;
import javax.servlet.ServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 特定于Servlet的{@link ModelAttributeMethodProcessor},
 * 它通过类型为{@link ServletRequestDataBinder}的WebDataBinder应用数据绑定.
 *
 * <p>如果名称与模型属性名称匹配并且存在适当的类型转换策略,
 * 还会添加后退策略, 以从URI模板变量或请求参数实例化模型属性.
 */
public class ServletModelAttributeMethodProcessor extends ModelAttributeMethodProcessor {

	/**
	 * @param annotationNotRequired 如果为"true", 则非简单方法参数和返回值被视为具有或不具有{@code @ModelAttribute}注解的模型属性
	 */
	public ServletModelAttributeMethodProcessor(boolean annotationNotRequired) {
		super(annotationNotRequired);
	}


	/**
	 * 如果名称与模型属性名称匹配并且存在适当的类型转换策略,
	 * 则从URI模板变量或请求参数实例化模型属性.
	 * 如果这些都不是true, 委托回基类.
	 */
	@Override
	protected final Object createAttribute(String attributeName, MethodParameter parameter,
			WebDataBinderFactory binderFactory, NativeWebRequest request) throws Exception {

		String value = getRequestValueForAttribute(attributeName, request);
		if (value != null) {
			Object attribute = createAttributeFromRequestValue(
					value, attributeName, parameter, binderFactory, request);
			if (attribute != null) {
				return attribute;
			}
		}

		return super.createAttribute(attributeName, parameter, binderFactory, request);
	}

	/**
	 * 从请求中获取一个值, 该值可用于通过从String到目标类型的类型转换来实例化model属性.
	 * <p>默认实现首先查找属性名称以匹配URI变量, 然后查找请求参数.
	 * 
	 * @param attributeName 模型属性名称
	 * @param request 当前的请求
	 * 
	 * @return 尝试转换的请求值, 或{@code null}
	 */
	protected String getRequestValueForAttribute(String attributeName, NativeWebRequest request) {
		Map<String, String> variables = getUriTemplateVariables(request);
		String variableValue = variables.get(attributeName);
		if (StringUtils.hasText(variableValue)) {
			return variableValue;
		}
		String parameterValue = request.getParameter(attributeName);
		if (StringUtils.hasText(parameterValue)) {
			return parameterValue;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	protected final Map<String, String> getUriTemplateVariables(NativeWebRequest request) {
		Map<String, String> variables = (Map<String, String>) request.getAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		return (variables != null ? variables : Collections.<String, String>emptyMap());
	}

	/**
	 * 使用类型转换从String请求值 (e.g. URI模板变量, 请求参数) 创建模型属性.
	 * <p>仅当存在可以执行转换的已注册{@link Converter}时, 默认实现才会转换.
	 * 
	 * @param sourceValue 从中创建模型属性的源值
	 * @param attributeName 属性名称 (never {@code null})
	 * @param parameter 方法参数
	 * @param binderFactory 用于创建WebDataBinder实例
	 * @param request 当前的请求
	 * 
	 * @return 创建的模型属性, 或{@code null} 如果找不到合适的转换
	 */
	protected Object createAttributeFromRequestValue(String sourceValue, String attributeName,
			MethodParameter parameter, WebDataBinderFactory binderFactory, NativeWebRequest request)
			throws Exception {

		DataBinder binder = binderFactory.createBinder(request, null, attributeName);
		ConversionService conversionService = binder.getConversionService();
		if (conversionService != null) {
			TypeDescriptor source = TypeDescriptor.valueOf(String.class);
			TypeDescriptor target = new TypeDescriptor(parameter);
			if (conversionService.canConvert(source, target)) {
				return binder.convertIfNecessary(sourceValue, parameter.getParameterType(), parameter);
			}
		}
		return null;
	}

	/**
	 * 此实现在绑定之前将{@link WebDataBinder}向下转型为{@link ServletRequestDataBinder}.
	 */
	@Override
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
		ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
		servletBinder.bind(servletRequest);
	}

}
