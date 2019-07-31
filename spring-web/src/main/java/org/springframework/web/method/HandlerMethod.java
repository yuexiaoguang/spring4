package org.springframework.web.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 封装有关由{@linkplain #getMethod() method}和{@linkplain #getBean() bean}组成的处理器方法的信息.
 * 提供对方法参数, 方法返回值, 方法注解等的方便访问.
 *
 * <p>可以使用bean实例或bean名称创建类 (e.g. lazy-init bean, prototype bean).
 * 使用{@link #createWithResolvedBean()}获取 {@code HandlerMethod}实例, 并通过关联的{@link BeanFactory}解析bean实例.
 */
public class HandlerMethod {

	/** Logger that is available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private final Object bean;

	private final BeanFactory beanFactory;

	private final Class<?> beanType;

	private final Method method;

	private final Method bridgedMethod;

	private final MethodParameter[] parameters;

	private HttpStatus responseStatus;

	private String responseStatusReason;

	private HandlerMethod resolvedFromHandlerMethod;


	public HandlerMethod(Object bean, Method method) {
		Assert.notNull(bean, "Bean is required");
		Assert.notNull(method, "Method is required");
		this.bean = bean;
		this.beanFactory = null;
		this.beanType = ClassUtils.getUserClass(bean);
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		this.parameters = initMethodParameters();
		evaluateResponseStatus();
	}

	/**
	 * @throws NoSuchMethodException 当无法找到方法时
	 */
	public HandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		Assert.notNull(bean, "Bean is required");
		Assert.notNull(methodName, "Method name is required");
		this.bean = bean;
		this.beanFactory = null;
		this.beanType = ClassUtils.getUserClass(bean);
		this.method = bean.getClass().getMethod(methodName, parameterTypes);
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(this.method);
		this.parameters = initMethodParameters();
		evaluateResponseStatus();
	}

	/**
	 * 稍后可以使用方法{@link #createWithResolvedBean()}来重新创建具有初始化bean的{@code HandlerMethod}.
	 */
	public HandlerMethod(String beanName, BeanFactory beanFactory, Method method) {
		Assert.hasText(beanName, "Bean name is required");
		Assert.notNull(beanFactory, "BeanFactory is required");
		Assert.notNull(method, "Method is required");
		this.bean = beanName;
		this.beanFactory = beanFactory;
		this.beanType = ClassUtils.getUserClass(beanFactory.getType(beanName));
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		this.parameters = initMethodParameters();
		evaluateResponseStatus();
	}

	/**
	 * 复制构造函数, 在子类中使用.
	 */
	protected HandlerMethod(HandlerMethod handlerMethod) {
		Assert.notNull(handlerMethod, "HandlerMethod is required");
		this.bean = handlerMethod.bean;
		this.beanFactory = handlerMethod.beanFactory;
		this.beanType = handlerMethod.beanType;
		this.method = handlerMethod.method;
		this.bridgedMethod = handlerMethod.bridgedMethod;
		this.parameters = handlerMethod.parameters;
		this.responseStatus = handlerMethod.responseStatus;
		this.responseStatusReason = handlerMethod.responseStatusReason;
		this.resolvedFromHandlerMethod = handlerMethod.resolvedFromHandlerMethod;
	}

	/**
	 * 使用已解析的处理器重新创建HandlerMethod.
	 */
	private HandlerMethod(HandlerMethod handlerMethod, Object handler) {
		Assert.notNull(handlerMethod, "HandlerMethod is required");
		Assert.notNull(handler, "Handler object is required");
		this.bean = handler;
		this.beanFactory = handlerMethod.beanFactory;
		this.beanType = handlerMethod.beanType;
		this.method = handlerMethod.method;
		this.bridgedMethod = handlerMethod.bridgedMethod;
		this.parameters = handlerMethod.parameters;
		this.responseStatus = handlerMethod.responseStatus;
		this.responseStatusReason = handlerMethod.responseStatusReason;
		this.resolvedFromHandlerMethod = handlerMethod;
	}


	private MethodParameter[] initMethodParameters() {
		int count = this.bridgedMethod.getParameterTypes().length;
		MethodParameter[] result = new MethodParameter[count];
		for (int i = 0; i < count; i++) {
			HandlerMethodParameter parameter = new HandlerMethodParameter(i);
			GenericTypeResolver.resolveParameterType(parameter, this.beanType);
			result[i] = parameter;
		}
		return result;
	}

	private void evaluateResponseStatus() {
		ResponseStatus annotation = getMethodAnnotation(ResponseStatus.class);
		if (annotation == null) {
			annotation = AnnotatedElementUtils.findMergedAnnotation(getBeanType(), ResponseStatus.class);
		}
		if (annotation != null) {
			this.responseStatus = annotation.code();
			this.responseStatusReason = annotation.reason();
		}
	}


	/**
	 * 返回此处理器方法的bean.
	 */
	public Object getBean() {
		return this.bean;
	}

	/**
	 * 返回此处理器方法的方法.
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * 此方法返回此处理器方法的处理器类型.
	 * <p>请注意, 如果bean类型是CGLIB生成的类, 则返回原始用户定义的类.
	 */
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * 如果bean方法是桥接方法, 则此方法返回桥接 (用户定义)方法.
	 * 否则返回与{@link #getMethod()}相同的方法.
	 */
	protected Method getBridgedMethod() {
		return this.bridgedMethod;
	}

	/**
	 * 返回此处理器方法的方法参数.
	 */
	public MethodParameter[] getMethodParameters() {
		return this.parameters;
	}

	/**
	 * 返回指定的响应状态.
	 */
	protected HttpStatus getResponseStatus() {
		return this.responseStatus;
	}

	/**
	 * 返回相关的响应状态原因.
	 */
	protected String getResponseStatusReason() {
		return this.responseStatusReason;
	}

	/**
	 * 返回HandlerMethod返回类型.
	 */
	public MethodParameter getReturnType() {
		return new HandlerMethodParameter(-1);
	}

	/**
	 * 返回实际的返回值类型.
	 */
	public MethodParameter getReturnValueType(Object returnValue) {
		return new ReturnValueMethodParameter(returnValue);
	}

	/**
	 * 如果方法返回类型为void, 则返回{@code true}, 否则返回{@code false}.
	 */
	public boolean isVoid() {
		return Void.TYPE.equals(getReturnType().getParameterType());
	}

	/**
	 * 如果在给定方法本身上找不到注解, 则在遍历其超级方法的底层方法上返回单个注解.
	 * <p>从Spring Framework 4.2.2开始, 还支持带有属性覆盖的<em>合并</em>组合注解.
	 * 
	 * @param annotationType 要内省方法的注解类型
	 * 
	 * @return 注解, 或{@code null}
	 */
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
	}

	/**
	 * 返回是否使用给定的注解类型声明参数.
	 * 
	 * @param annotationType 要查找的注解类型
	 */
	public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.hasAnnotation(this.method, annotationType);
	}

	/**
	 * 返回HandlerMethod, 从中通过{@link #createWithResolvedBean()}解析此HandlerMethod实例.
	 */
	public HandlerMethod getResolvedFromHandlerMethod() {
		return this.resolvedFromHandlerMethod;
	}

	/**
	 * 如果提供的实例包含bean名称而不是对象实例, 则在创建和返回{@link HandlerMethod}之前解析bean名称.
	 */
	public HandlerMethod createWithResolvedBean() {
		Object handler = this.bean;
		if (this.bean instanceof String) {
			String beanName = (String) this.bean;
			handler = this.beanFactory.getBean(beanName);
		}
		return new HandlerMethod(this, handler);
	}

	/**
	 * 返回此处理器方法的简短表示, 以用于日志消息.
	 */
	public String getShortLogMessage() {
		int args = this.method.getParameterTypes().length;
		return getBeanType().getName() + "#" + this.method.getName() + "[" + args + " args]";
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HandlerMethod)) {
			return false;
		}
		HandlerMethod otherMethod = (HandlerMethod) other;
		return (this.bean.equals(otherMethod.bean) && this.method.equals(otherMethod.method));
	}

	@Override
	public int hashCode() {
		return (this.bean.hashCode() * 31 + this.method.hashCode());
	}

	@Override
	public String toString() {
		return this.method.toGenericString();
	}


	/**
	 * 具有HandlerMethod特定行为的MethodParameter.
	 */
	protected class HandlerMethodParameter extends SynthesizingMethodParameter {

		public HandlerMethodParameter(int index) {
			super(HandlerMethod.this.bridgedMethod, index);
		}

		protected HandlerMethodParameter(HandlerMethodParameter original) {
			super(original);
		}

		@Override
		public Class<?> getContainingClass() {
			return HandlerMethod.this.getBeanType();
		}

		@Override
		public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
			return HandlerMethod.this.getMethodAnnotation(annotationType);
		}

		@Override
		public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
			return HandlerMethod.this.hasMethodAnnotation(annotationType);
		}

		@Override
		public HandlerMethodParameter clone() {
			return new HandlerMethodParameter(this);
		}
	}


	/**
	 * 基于实际返回值的HandlerMethod返回类型的MethodParameter.
	 */
	private class ReturnValueMethodParameter extends HandlerMethodParameter {

		private final Object returnValue;

		public ReturnValueMethodParameter(Object returnValue) {
			super(-1);
			this.returnValue = returnValue;
		}

		protected ReturnValueMethodParameter(ReturnValueMethodParameter original) {
			super(original);
			this.returnValue = original.returnValue;
		}

		@Override
		public Class<?> getParameterType() {
			return (this.returnValue != null ? this.returnValue.getClass() : super.getParameterType());
		}

		@Override
		public ReturnValueMethodParameter clone() {
			return new ReturnValueMethodParameter(this);
		}
	}

}
