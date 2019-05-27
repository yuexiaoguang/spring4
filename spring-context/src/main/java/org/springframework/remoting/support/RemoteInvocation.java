package org.springframework.remoting.support;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.util.ClassUtils;

/**
 * 封装远程调用, 以可序列化的方式提供核心方法调用属性.
 * 用于RMI和基于HTTP的序列化调用器.
 *
 * <p>这是一个SPI类, 通常不由应用程序直接使用.
 * 可以为其他调用参数进行子类化.
 *
 * <p>{@link RemoteInvocation}和{@link RemoteInvocationResult}都设计用于标准Java序列化以及JavaBean样式序列化.
 */
public class RemoteInvocation implements Serializable {

	/** use serialVersionUID from Spring 1.1 for interoperability */
	private static final long serialVersionUID = 6876024250231820554L;


	private String methodName;

	private Class<?>[] parameterTypes;

	private Object[] arguments;

	private Map<String, Serializable> attributes;


	/**
	 * @param methodInvocation 要转换的AOP调用
	 */
	public RemoteInvocation(MethodInvocation methodInvocation) {
		this.methodName = methodInvocation.getMethod().getName();
		this.parameterTypes = methodInvocation.getMethod().getParameterTypes();
		this.arguments = methodInvocation.getArguments();
	}

	/**
	 * @param methodName 要调用的方法的名称
	 * @param parameterTypes 方法的参数类型
	 * @param arguments 调用的参数
	 */
	public RemoteInvocation(String methodName, Class<?>[] parameterTypes, Object[] arguments) {
		this.methodName = methodName;
		this.parameterTypes = parameterTypes;
		this.arguments = arguments;
	}

	/**
	 * 为JavaBean样式的反序列化创建新的RemoteInvocation (e.g. with Jackson).
	 */
	public RemoteInvocation() {
	}


	/**
	 * 设置目标方法的名称.
	 * <p>此setter用于JavaBean样式的反序列化.
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/**
	 * 返回目标方法的名称.
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * 设置目标方法的参数类型.
	 * <p>此setter用于JavaBean样式的反序列化.
	 */
	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	/**
	 * 返回目标方法的参数类型.
	 */
	public Class<?>[] getParameterTypes() {
		return this.parameterTypes;
	}

	/**
	 * 设置目标方法调用的参数.
	 * <p>此setter用于JavaBean样式的反序列化.
	 */
	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}

	/**
	 * 返回目标方法调用的参数.
	 */
	public Object[] getArguments() {
		return this.arguments;
	}


	/**
	 * 添加其他调用属性. 无需子类RemoteInvocation即可添加其他调用上下文.
	 * <p>属性键必须是唯一的, 并且不允许覆盖现有属性.
	 * <p>该实现避免了不必要地创建属性Map, 以最小化序列化大小.
	 * 
	 * @param key the attribute key
	 * @param value the attribute value
	 * 
	 * @throws IllegalStateException 如果Key已经绑定
	 */
	public void addAttribute(String key, Serializable value) throws IllegalStateException {
		if (this.attributes == null) {
			this.attributes = new HashMap<String, Serializable>();
		}
		if (this.attributes.containsKey(key)) {
			throw new IllegalStateException("There is already an attribute with key '" + key + "' bound");
		}
		this.attributes.put(key, value);
	}

	/**
	 * 检索给定键的属性.
	 * <p>该实现避免了不必要地创建属性Map, 以最小化序列化大小.
	 * 
	 * @param key the attribute key
	 * 
	 * @return 属性值, 或{@code null}如果未定义
	 */
	public Serializable getAttribute(String key) {
		if (this.attributes == null) {
			return null;
		}
		return this.attributes.get(key);
	}

	/**
	 * 设置属性Map. 仅限于此处用于特殊目的:
	 * 最好使用{@link #addAttribute}和{@link #getAttribute}.
	 * 
	 * @param attributes 属性Map
	 */
	public void setAttributes(Map<String, Serializable> attributes) {
		this.attributes = attributes;
	}

	/**
	 * 返回属性Map. 主要用于调试:
	 * 最好使用{@link #addAttribute}和{@link #getAttribute}.
	 * 
	 * @return 属性Map, 或{@code null}如果未创建
	 */
	public Map<String, Serializable> getAttributes() {
		return this.attributes;
	}


	/**
	 * 对给定的目标对象执行此调用.
	 * 通常在服务器上收到RemoteInvocation时调用.
	 * 
	 * @param targetObject 要调用的目标对象
	 * 
	 * @return 调用结果
	 * @throws NoSuchMethodException 如果方法名称无法解析
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致异常
	 */
	public Object invoke(Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		Method method = targetObject.getClass().getMethod(this.methodName, this.parameterTypes);
		return method.invoke(targetObject, this.arguments);
	}


	@Override
	public String toString() {
		return "RemoteInvocation: method name '" + this.methodName + "'; parameter types " +
				ClassUtils.classNamesToString(this.parameterTypes);
	}

}
