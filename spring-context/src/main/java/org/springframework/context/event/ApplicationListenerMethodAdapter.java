package org.springframework.context.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link GenericApplicationListener}适配器, 它将事件处理委托给带 {@link EventListener}注解的方法.
 *
 * <p>委托给 {@link #processEvent(ApplicationEvent)}, 为子类提供偏转默认值的机会.
 * 如果需要, 展开{@link PayloadApplicationEvent}的内容, 以允许方法声明定义任意的事件类型.
 * 如果定义了条件, 则在调用底层方法之前对其进行评估.
 */
public class ApplicationListenerMethodAdapter implements GenericApplicationListener {

	protected final Log logger = LogFactory.getLog(getClass());

	private final String beanName;

	private final Method method;

	private final Class<?> targetClass;

	private final Method bridgedMethod;

	private final List<ResolvableType> declaredEventTypes;

	private final String condition;

	private final int order;

	private final AnnotatedElementKey methodKey;

	private ApplicationContext applicationContext;

	private EventExpressionEvaluator evaluator;


	public ApplicationListenerMethodAdapter(String beanName, Class<?> targetClass, Method method) {
		this.beanName = beanName;
		this.method = method;
		this.targetClass = targetClass;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);

		Method targetMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
		EventListener ann = AnnotatedElementUtils.findMergedAnnotation(targetMethod, EventListener.class);

		this.declaredEventTypes = resolveDeclaredEventTypes(method, ann);
		this.condition = (ann != null ? ann.condition() : null);
		this.order = resolveOrder(method);

		this.methodKey = new AnnotatedElementKey(method, targetClass);
	}


	private List<ResolvableType> resolveDeclaredEventTypes(Method method, EventListener ann) {
		int count = method.getParameterTypes().length;
		if (count > 1) {
			throw new IllegalStateException(
					"Maximum one parameter is allowed for event listener method: " + method);
		}
		if (ann != null && ann.classes().length > 0) {
			List<ResolvableType> types = new ArrayList<ResolvableType>(ann.classes().length);
			for (Class<?> eventType : ann.classes()) {
				types.add(ResolvableType.forClass(eventType));
			}
			return types;
		}
		else {
			if (count == 0) {
				throw new IllegalStateException(
						"Event parameter is mandatory for event listener method: " + method);
			}
			return Collections.singletonList(ResolvableType.forMethodParameter(method, 0));
		}
	}

	private int resolveOrder(Method method) {
		Order ann = AnnotatedElementUtils.findMergedAnnotation(method, Order.class);
		return (ann != null ? ann.value() : 0);
	}

	/**
	 * 初始化实例.
	 */
	void init(ApplicationContext applicationContext, EventExpressionEvaluator evaluator) {
		this.applicationContext = applicationContext;
		this.evaluator = evaluator;
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		processEvent(event);
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		for (ResolvableType declaredEventType : this.declaredEventTypes) {
			if (declaredEventType.isAssignableFrom(eventType)) {
				return true;
			}
			else if (PayloadApplicationEvent.class.isAssignableFrom(eventType.getRawClass())) {
				ResolvableType payloadType = eventType.as(PayloadApplicationEvent.class).getGeneric();
				if (declaredEventType.isAssignableFrom(payloadType)) {
					return true;
				}
			}
		}
		return eventType.hasUnresolvableGenerics();
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 处理指定的 {@link ApplicationEvent}, 检查条件是否匹配并处理非null结果.
	 */
	public void processEvent(ApplicationEvent event) {
		Object[] args = resolveArguments(event);
		if (shouldHandle(event, args)) {
			Object result = doInvoke(args);
			if (result != null) {
				handleResult(result);
			}
			else {
				logger.trace("No result object given - no result to handle");
			}
		}
	}

	/**
	 * 解析用于指定的 {@link ApplicationEvent}的方法参数.
	 * <p>这些参数将用于调用此实例处理的方法.
	 * 可以返回{@code null}以指示无法解析合适的参数, 因此不应该为指定的事件调用该方法.
	 */
	protected Object[] resolveArguments(ApplicationEvent event) {
		ResolvableType declaredEventType = getResolvableType(event);
		if (declaredEventType == null) {
			return null;
		}
		if (this.method.getParameterTypes().length == 0) {
			return new Object[0];
		}
		if (!ApplicationEvent.class.isAssignableFrom(declaredEventType.getRawClass()) &&
				event instanceof PayloadApplicationEvent) {
			return new Object[] {((PayloadApplicationEvent) event).getPayload()};
		}
		else {
			return new Object[] {event};
		}
	}

	protected void handleResult(Object result) {
		if (result.getClass().isArray()) {
			Object[] events = ObjectUtils.toObjectArray(result);
			for (Object event : events) {
				publishEvent(event);
			}
		}
		else if (result instanceof Collection<?>) {
			Collection<?> events = (Collection<?>) result;
			for (Object event : events) {
				publishEvent(event);
			}
		}
		else {
			publishEvent(result);
		}
	}

	private void publishEvent(Object event) {
		if (event != null) {
			Assert.notNull(this.applicationContext, "ApplicationContext must not be null");
			this.applicationContext.publishEvent(event);
		}
	}

	private boolean shouldHandle(ApplicationEvent event, Object[] args) {
		if (args == null) {
			return false;
		}
		String condition = getCondition();
		if (StringUtils.hasText(condition)) {
			Assert.notNull(this.evaluator, "EventExpressionEvaluator must no be null");
			EvaluationContext evaluationContext = this.evaluator.createEvaluationContext(
					event, this.targetClass, this.method, args, this.applicationContext);
			return this.evaluator.condition(condition, this.methodKey, evaluationContext);
		}
		return true;
	}

	/**
	 * 使用给定的参数值调用事件监听器方法.
	 */
	protected Object doInvoke(Object... args) {
		Object bean = getTargetBean();
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		try {
			return this.bridgedMethod.invoke(bean, args);
		}
		catch (IllegalArgumentException ex) {
			assertTargetBean(this.bridgedMethod, bean, args);
			throw new IllegalStateException(getInvocationErrorMessage(bean, ex.getMessage(), args), ex);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException(getInvocationErrorMessage(bean, ex.getMessage(), args), ex);
		}
		catch (InvocationTargetException ex) {
			// Throw underlying exception
			Throwable targetException = ex.getTargetException();
			if (targetException instanceof RuntimeException) {
				throw (RuntimeException) targetException;
			}
			else {
				String msg = getInvocationErrorMessage(bean, "Failed to invoke event listener method", args);
				throw new UndeclaredThrowableException(targetException, msg);
			}
		}
	}

	/**
	 * 返回要使用的目标bean实例.
	 */
	protected Object getTargetBean() {
		Assert.notNull(this.applicationContext, "ApplicationContext must no be null");
		return this.applicationContext.getBean(this.beanName);
	}

	/**
	 * 返回要使用的条件.
	 * <p>匹配{@link EventListener}注解的{@code condition}属性;
	 * 或组合注解的任何匹配属性, 该组合注解是{@code @EventListener}的元注解.
	 */
	protected String getCondition() {
		return this.condition;
	}

	/**
	 * 在给定的错误消息中添加其他详细信息, 例如bean类型和方法签名.
	 * 
	 * @param message 要将HandlerMethod详细信息附加到的错误消息
	 */
	protected String getDetailedErrorMessage(Object bean, String message) {
		StringBuilder sb = new StringBuilder(message).append("\n");
		sb.append("HandlerMethod details: \n");
		sb.append("Bean [").append(bean.getClass().getName()).append("]\n");
		sb.append("Method [").append(this.bridgedMethod.toGenericString()).append("]\n");
		return sb.toString();
	}

	/**
	 * 断言目标bean类是声明给定方法的类的实例.
	 * 在某些情况下, 事件处理时的实际bean实例可能是JDK动态代理 (延迟初始化, 原型bean等).
	 * 需要代理的事件监听器bean应该更喜欢基于类的代理机制.
	 */
	private void assertTargetBean(Method method, Object targetBean, Object[] args) {
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String msg = "The event listener method class '" + methodDeclaringClass.getName() +
					"' is not an instance of the actual bean class '" +
					targetBeanClass.getName() + "'. If the bean requires proxying " +
					"(e.g. due to @Transactional), please use class-based proxying.";
			throw new IllegalStateException(getInvocationErrorMessage(targetBean, msg, args));
		}
	}

	private String getInvocationErrorMessage(Object bean, String message, Object[] resolvedArgs) {
		StringBuilder sb = new StringBuilder(getDetailedErrorMessage(bean, message));
		sb.append("Resolved arguments: \n");
		for (int i = 0; i < resolvedArgs.length; i++) {
			sb.append("[").append(i).append("] ");
			if (resolvedArgs[i] == null) {
				sb.append("[null] \n");
			}
			else {
				sb.append("[type=").append(resolvedArgs[i].getClass().getName()).append("] ");
				sb.append("[value=").append(resolvedArgs[i]).append("]\n");
			}
		}
		return sb.toString();
	}


	private ResolvableType getResolvableType(ApplicationEvent event) {
		ResolvableType payloadType = null;
		if (event instanceof PayloadApplicationEvent) {
			PayloadApplicationEvent<?> payloadEvent = (PayloadApplicationEvent<?>) event;
			payloadType = payloadEvent.getResolvableType().as(PayloadApplicationEvent.class).getGeneric();
		}
		for (ResolvableType declaredEventType : this.declaredEventTypes) {
			if (!ApplicationEvent.class.isAssignableFrom(declaredEventType.getRawClass()) && payloadType != null) {
				if (declaredEventType.isAssignableFrom(payloadType)) {
					return declaredEventType;
				}
			}
			if (declaredEventType.getRawClass().isInstance(event)) {
				return declaredEventType;
			}
		}
		return null;
	}


	@Override
	public String toString() {
		return this.method.toGenericString();
	}

}
