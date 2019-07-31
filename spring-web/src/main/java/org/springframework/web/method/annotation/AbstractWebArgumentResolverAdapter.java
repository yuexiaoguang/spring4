package org.springframework.web.method.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 一个抽象基类, 使{@link WebArgumentResolver}适配{@link HandlerMethodArgumentResolver}约定.
 *
 * <p><strong>Note:</strong> 提供此类是为了向后兼容.
 * 但是建议重新编写{@code WebArgumentResolver}作为{@code HandlerMethodArgumentResolver}.
 * 由于{@link #supportsParameter}只能通过实际解析值来实现, 然后检查结果不是{@code WebArgumentResolver#UNRESOLVED},
 * 所引发的任何异常必须被吸收和忽略, 因为不清楚适配器是否不支持该参数或是否因内部原因而失败.
 * {@code HandlerMethodArgumentResolver}约定还提供对模型属性和{@code WebDataBinderFactory}的访问 (用于类型转换).
 */
public abstract class AbstractWebArgumentResolverAdapter implements HandlerMethodArgumentResolver {

	private final Log logger = LogFactory.getLog(getClass());

	private final WebArgumentResolver adaptee;


	public AbstractWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
		Assert.notNull(adaptee, "'adaptee' must not be null");
		this.adaptee = adaptee;
	}


	/**
	 * 实际解析值并检查解析后的值是不是{@link WebArgumentResolver#UNRESOLVED}, 忽略任何异常.
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		try {
			NativeWebRequest webRequest = getWebRequest();
			Object result = this.adaptee.resolveArgument(parameter, webRequest);
			if (result == WebArgumentResolver.UNRESOLVED) {
				return false;
			}
			else {
				return ClassUtils.isAssignableValue(parameter.getParameterType(), result);
			}
		}
		catch (Exception ex) {
			// ignore (see class-level doc)
			logger.debug("Error in checking support for parameter [" + parameter + "], message: " + ex.getMessage());
			return false;
		}
	}

	/**
	 * 委托给{@link WebArgumentResolver}实例.
	 * 
	 * @throws IllegalStateException 如果解析的值不能分配给method参数.
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		Class<?> paramType = parameter.getParameterType();
		Object result = this.adaptee.resolveArgument(parameter, webRequest);
		if (result == WebArgumentResolver.UNRESOLVED || !ClassUtils.isAssignableValue(paramType, result)) {
			throw new IllegalStateException(
					"Standard argument type [" + paramType.getName() + "] in method " + parameter.getMethod() +
					"resolved to incompatible value of type [" + (result != null ? result.getClass() : null) +
					"]. Consider declaring the argument type in a less specific fashion.");
		}
		return result;
	}


	/**
	 * 需要访问{@link #supportsParameter}中的NativeWebRequest.
	 */
	protected abstract NativeWebRequest getWebRequest();

}
