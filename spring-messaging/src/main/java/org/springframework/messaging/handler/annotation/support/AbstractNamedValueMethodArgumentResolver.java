package org.springframework.messaging.handler.annotation.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * 用于从命名值解析方法参数的抽象基类.
 * 消息header, 和路径变量是命名值的示例.
 * 每个都可以有一个名称, 一个必需的标志和一个默认值.
 *
 * <p>子类定义如何执行以下操作:
 * <ul>
 * <li>获取方法参数的命名值信息
 * <li>将名称解析为参数值
 * <li>在需要参数值时处理缺少的参数值
 * <li>(可选)处理已解析的值
 * </ul>
 *
 * <p>默认值字符串可以包含 ${...} 占位符和Spring Expression Language {@code #{...}} 表达式.
 * 为此, 必须将{@link ConfigurableBeanFactory}提供给类构造函数.
 *
 * <p>如果{@link ConversionService}与方法参数类型不匹配, 则可以使用{@link ConversionService}将类型转换应用于已解析的参数值.
 */
public abstract class AbstractNamedValueMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final ConversionService conversionService;

	private final ConfigurableBeanFactory configurableBeanFactory;

	private final BeanExpressionContext expressionContext;

	private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache =
			new ConcurrentHashMap<MethodParameter, NamedValueInfo>(256);


	/**
	 * @param cs 用于转换值以匹配目标方法参数类型的转换服务
	 * @param beanFactory 用于在默认值中解析{@code ${...}}占位符和 {@code #{...}} SpEL表达式的bean工厂,
	 * 或{@code null} 如果默认值不包含表达式
	 */
	protected AbstractNamedValueMethodArgumentResolver(ConversionService cs, ConfigurableBeanFactory beanFactory) {
		this.conversionService = (cs != null ? cs : DefaultConversionService.getSharedInstance());
		this.configurableBeanFactory = beanFactory;
		this.expressionContext = (beanFactory != null ? new BeanExpressionContext(beanFactory, null) : null);
	}


	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
		MethodParameter nestedParameter = parameter.nestedIfOptional();

		Object resolvedName = resolveStringValue(namedValueInfo.name);
		if (resolvedName == null) {
			throw new IllegalArgumentException(
					"Specified name must not resolve to null: [" + namedValueInfo.name + "]");
		}

		Object arg = resolveArgumentInternal(nestedParameter, message, resolvedName.toString());
		if (arg == null) {
			if (namedValueInfo.defaultValue != null) {
				arg = resolveStringValue(namedValueInfo.defaultValue);
			}
			else if (namedValueInfo.required && !nestedParameter.isOptional()) {
				handleMissingValue(namedValueInfo.name, nestedParameter, message);
			}
			arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
		}
		else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
			arg = resolveStringValue(namedValueInfo.defaultValue);
		}

		if (parameter != nestedParameter || !ClassUtils.isAssignableValue(parameter.getParameterType(), arg)) {
			arg = this.conversionService.convert(arg, TypeDescriptor.forObject(arg), new TypeDescriptor(parameter));
		}

		handleResolvedValue(arg, namedValueInfo.name, parameter, message);

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
				throw new IllegalArgumentException("Name for argument type [" + parameter.getParameterType().getName() +
						"] not available, and parameter name information not found in class file either.");
			}
		}
		String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
		return new NamedValueInfo(name, info.required, defaultValue);
	}

	/**
	 * 解析给定的注解值, 可能包含占位符和表达式.
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
	 * @param parameter 要解析为参数值的方法参数
	 * @param message 当前请求
	 * @param name 要解析的值的名称
	 * 
	 * @return 已解析的参数. May be {@code null}
	 * @throws Exception 错误
	 */
	protected abstract Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name)
			throws Exception;

	/**
	 * 在需要命名值时调用, 但{@link #resolveArgumentInternal(MethodParameter, Message, String)}
	 * 返回{@code null}, 并且没有默认值. 在这种情况下, 子类通常会抛出异常.
	 * 
	 * @param name 值的名称
	 * @param parameter 方法参数
	 * @param message 正在处理的消息
	 */
	protected abstract void handleMissingValue(String name, MethodParameter parameter, Message<?> message);

	/**
	 * {@code null}导致{@code boolean}的{@code false}值, 或其他基本类型的异常.
	 */
	private Object handleNullValue(String name, Object value, Class<?> paramType) {
		if (value == null) {
			if (Boolean.TYPE.equals(paramType)) {
				return Boolean.FALSE;
			}
			else if (paramType.isPrimitive()) {
				throw new IllegalStateException("Optional " + paramType + " parameter '" + name +
						"' is present but cannot be translated into a null value due to being " +
						"declared as a primitive type. Consider declaring it as object wrapper " +
						"for the corresponding primitive type.");
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
	 * @param message 消息
	 */
	protected void handleResolvedValue(Object arg, String name, MethodParameter parameter, Message<?> message) {
	}


	/**
	 * 表示有关命名值的信息, 包括名称, 是否必须以及默认值.
	 */
	protected static class NamedValueInfo {

		private final String name;

		private final boolean required;

		private final String defaultValue;

		protected NamedValueInfo(String name, boolean required, String defaultValue) {
			this.name = name;
			this.required = required;
			this.defaultValue = defaultValue;
		}
	}
}
