package org.springframework.web.method.annotation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletException;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 用于从命名值解析方法参数的抽象基类.
 * 请求参数, 请求header和路径变量是命名值的示例.
 * 每个都可以有一个名称, 一个必需的标志和一个默认值.
 *
 * <p>子类定义如何执行以下操作:
 * <ul>
 * <li>获取方法参数的命名值信息
 * <li>将名称解析为参数值
 * <li>在需要参数值时处理缺少的参数值
 * <li>可选地处理已解析的值
 * </ul>
 *
 * <p>默认值字符串可以包含 ${...}占位符和Spring Expression Language #{...}表达式.
 * 为此, 必须将{@link ConfigurableBeanFactory}提供给类构造函数.
 *
 * <p>创建{@link WebDataBinder}以将类型转换应用于已解析的参数值, 如果它与方法参数类型不匹配.
 */
public abstract class AbstractNamedValueMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final ConfigurableBeanFactory configurableBeanFactory;

	private final BeanExpressionContext expressionContext;

	private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache =
			new ConcurrentHashMap<MethodParameter, NamedValueInfo>(256);


	public AbstractNamedValueMethodArgumentResolver() {
		this.configurableBeanFactory = null;
		this.expressionContext = null;
	}

	/**
	 * @param beanFactory 用于在默认值中解析 ${...}占位符和 #{...} SpEL表达式的bean工厂;
	 * 或{@code null}, 如果默认值不包含表达式
	 */
	public AbstractNamedValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		this.configurableBeanFactory = beanFactory;
		this.expressionContext =
				(beanFactory != null ? new BeanExpressionContext(beanFactory, new RequestScope()) : null);
	}


	@Override
	public final Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
		MethodParameter nestedParameter = parameter.nestedIfOptional();

		Object resolvedName = resolveStringValue(namedValueInfo.name);
		if (resolvedName == null) {
			throw new IllegalArgumentException(
					"Specified name must not resolve to null: [" + namedValueInfo.name + "]");
		}

		Object arg = resolveName(resolvedName.toString(), nestedParameter, webRequest);
		if (arg == null) {
			if (namedValueInfo.defaultValue != null) {
				arg = resolveStringValue(namedValueInfo.defaultValue);
			}
			else if (namedValueInfo.required && !nestedParameter.isOptional()) {
				handleMissingValue(namedValueInfo.name, nestedParameter, webRequest);
			}
			arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
		}
		else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
			arg = resolveStringValue(namedValueInfo.defaultValue);
		}

		if (binderFactory != null) {
			WebDataBinder binder = binderFactory.createBinder(webRequest, null, namedValueInfo.name);
			try {
				arg = binder.convertIfNecessary(arg, parameter.getParameterType(), parameter);
			}
			catch (ConversionNotSupportedException ex) {
				throw new MethodArgumentConversionNotSupportedException(arg, ex.getRequiredType(),
						namedValueInfo.name, parameter, ex.getCause());
			}
			catch (TypeMismatchException ex) {
				throw new MethodArgumentTypeMismatchException(arg, ex.getRequiredType(),
						namedValueInfo.name, parameter, ex.getCause());

			}
		}

		handleResolvedValue(arg, namedValueInfo.name, parameter, mavContainer, webRequest);

		return arg;
	}

	/**
	 * 获取给定方法参数的命名值.
	 */
	private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
		NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
		if (namedValueInfo == null) {
			namedValueInfo = createNamedValueInfo(parameter);
			namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
			this.namedValueInfoCache.put(parameter, namedValueInfo);
		}
		return namedValueInfo;
	}

	/**
	 * 为给定的方法参数创建{@link NamedValueInfo}对象.
	 * 实现通常通过{@link MethodParameter#getParameterAnnotation(Class)}检索方法注解.
	 * 
	 * @param parameter 方法参数
	 * 
	 * @return 命名值信息
	 */
	protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

	/**
	 * 基于具有已清理值的给定NamedValueInfo创建新的NamedValueInfo.
	 */
	private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
		String name = info.name;
		if (info.name.isEmpty()) {
			name = parameter.getParameterName();
			if (name == null) {
				throw new IllegalArgumentException(
						"Name for argument type [" + parameter.getNestedParameterType().getName() +
						"] not available, and parameter name information not found in class file either.");
			}
		}
		String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
		return new NamedValueInfo(name, info.required, defaultValue);
	}

	/**
	 * 解析给定的注解指定的值, 可能包含占位符和表达式.
	 */
	private Object resolveStringValue(String value) {
		if (this.configurableBeanFactory == null) {
			return value;
		}
		String placeholdersResolved = this.configurableBeanFactory.resolveEmbeddedValue(value);
		BeanExpressionResolver exprResolver = this.configurableBeanFactory.getBeanExpressionResolver();
		if (exprResolver == null) {
			return value;
		}
		return exprResolver.evaluate(placeholdersResolved, this.expressionContext);
	}

	/**
	 * 将给定的参数类型和值名称解析为参数值.
	 * 
	 * @param name 要解析的值的名称
	 * @param parameter 解析为参数值的方法参数 (在声明{@link java.util.Optional}的情况下预先嵌套)
	 * @param request 当前的请求
	 * 
	 * @return 已解析的值 (may be {@code null})
	 * @throws Exception
	 */
	protected abstract Object resolveName(String name, MethodParameter parameter, NativeWebRequest request)
			throws Exception;

	/**
	 * 在需要命名值时调用, 但{@link #resolveName(String, MethodParameter, NativeWebRequest)}返回{@code null}并且没有默认值.
	 * 在这种情况下, 子类通常会抛出异常.
	 * 
	 * @param name 值的名称
	 * @param parameter 方法参数
	 * @param request 当前的请求
	 */
	protected void handleMissingValue(String name, MethodParameter parameter, NativeWebRequest request)
			throws Exception {

		handleMissingValue(name, parameter);
	}

	/**
	 * 在需要命名值时调用, 但{@link #resolveName(String, MethodParameter, NativeWebRequest)}返回{@code null}并且没有默认值.
	 * 在这种情况下, 子类通常会抛出异常.
	 * 
	 * @param name 值的名称
	 * @param parameter 方法参数
	 */
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new ServletRequestBindingException("Missing argument '" + name +
				"' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
	}

	/**
	 * {@code null}导致{@code boolean}的{@code false}值或其他原语的异常.
	 */
	private Object handleNullValue(String name, Object value, Class<?> paramType) {
		if (value == null) {
			if (Boolean.TYPE.equals(paramType)) {
				return Boolean.FALSE;
			}
			else if (paramType.isPrimitive()) {
				throw new IllegalStateException("Optional " + paramType.getSimpleName() + " parameter '" + name +
						"' is present but cannot be translated into a null value due to being declared as a " +
						"primitive type. Consider declaring it as object wrapper for the corresponding primitive type.");
			}
		}
		return value;
	}

	/**
	 * 在解析值后调用.
	 * 
	 * @param arg 已解析的参数值
	 * @param name 参数名称
	 * @param parameter 参数类型
	 * @param mavContainer the {@link ModelAndViewContainer} (may be {@code null})
	 * @param webRequest 当前的请求
	 */
	protected void handleResolvedValue(Object arg, String name, MethodParameter parameter,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
	}


	/**
	 * 表示有关命名值的信息, 包括名称, 是否必需, 以及默认值.
	 */
	protected static class NamedValueInfo {

		private final String name;

		private final boolean required;

		private final String defaultValue;

		public NamedValueInfo(String name, boolean required, String defaultValue) {
			this.name = name;
			this.required = required;
			this.defaultValue = defaultValue;
		}
	}

}
