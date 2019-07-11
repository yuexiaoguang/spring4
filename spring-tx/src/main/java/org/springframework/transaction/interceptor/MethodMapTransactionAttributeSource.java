package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * 简单的{@link TransactionAttributeSource}实现, 允许在{@link Map}中按方法存储属性.
 */
public class MethodMapTransactionAttributeSource
		implements TransactionAttributeSource, BeanClassLoaderAware, InitializingBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 方法名到属性值的Map */
	private Map<String, TransactionAttribute> methodMap;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private boolean eagerlyInitialized = false;

	private boolean initialized = false;

	/** Map from Method to TransactionAttribute */
	private final Map<Method, TransactionAttribute> transactionAttributeMap =
			new HashMap<Method, TransactionAttribute>();

	/** Map from Method to name pattern used for registration */
	private final Map<Method, String> methodNameMap = new HashMap<Method, String>();


	/**
	 * 设置名称/属性Map, 包括"FQCN.method"方法名称 (e.g. "com.mycompany.mycode.MyClass.myMethod")
	 * 和{@link TransactionAttribute}实例 (或要转换为{@code TransactionAttribute}实例的字符串).
	 * <p>通过setter注入进行配置, 通常在Spring bean工厂中进行. 依赖于之后被调用的{@link #afterPropertiesSet()}.
	 * 
	 * @param methodMap 从方法名到属性值的{@link Map}
	 */
	public void setMethodMap(Map<String, TransactionAttribute> methodMap) {
		this.methodMap = methodMap;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * 实时初始化指定的{@link #setMethodMap(java.util.Map) "methodMap"}.
	 */
	@Override
	public void afterPropertiesSet() {
		initMethodMap(this.methodMap);
		this.eagerlyInitialized = true;
		this.initialized = true;
	}

	/**
	 * 初始化指定的{@link #setMethodMap(java.util.Map) "methodMap"}.
	 * 
	 * @param methodMap 从方法名到{@code TransactionAttribute}实例的Map
	 */
	protected void initMethodMap(Map<String, TransactionAttribute> methodMap) {
		if (methodMap != null) {
			for (Map.Entry<String, TransactionAttribute> entry : methodMap.entrySet()) {
				addTransactionalMethod(entry.getKey(), entry.getValue());
			}
		}
	}


	/**
	 * 为事务方法添加属性.
	 * <p>方法名称可以以 "*"结尾或以"*"开头, 以匹配多个方法.
	 * 
	 * @param name 类和方法名称, 用点分隔
	 * @param attr 与该方法关联的属性
	 * 
	 * @throws IllegalArgumentException 无效的名称
	 */
	public void addTransactionalMethod(String name, TransactionAttribute attr) {
		Assert.notNull(name, "Name must not be null");
		int lastDotIndex = name.lastIndexOf('.');
		if (lastDotIndex == -1) {
			throw new IllegalArgumentException("'" + name + "' is not a valid method name: format is FQN.methodName");
		}
		String className = name.substring(0, lastDotIndex);
		String methodName = name.substring(lastDotIndex + 1);
		Class<?> clazz = ClassUtils.resolveClassName(className, this.beanClassLoader);
		addTransactionalMethod(clazz, methodName, attr);
	}

	/**
	 * 为事务方法添加属性.
	 * 方法名称可以以"*"结尾或以"*"开头, 以匹配多个方法.
	 * 
	 * @param clazz 目标接口或类
	 * @param mappedName 映射的方法名
	 * @param attr 与该方法关联的属性
	 */
	public void addTransactionalMethod(Class<?> clazz, String mappedName, TransactionAttribute attr) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(mappedName, "Mapped name must not be null");
		String name = clazz.getName() + '.'  + mappedName;

		Method[] methods = clazz.getDeclaredMethods();
		List<Method> matchingMethods = new ArrayList<Method>();
		for (Method method : methods) {
			if (isMatch(method.getName(), mappedName)) {
				matchingMethods.add(method);
			}
		}
		if (matchingMethods.isEmpty()) {
			throw new IllegalArgumentException(
					"Couldn't find method '" + mappedName + "' on class [" + clazz.getName() + "]");
		}

		// 注册所有匹配的方法
		for (Method method : matchingMethods) {
			String regMethodName = this.methodNameMap.get(method);
			if (regMethodName == null || (!regMethodName.equals(name) && regMethodName.length() <= name.length())) {
				// 现在还没有已注册的方法名称或更具体的方法名称规范 -> (重新)注册方法.
				if (logger.isDebugEnabled() && regMethodName != null) {
					logger.debug("Replacing attribute for transactional method [" + method + "]: current name '" +
							name + "' is more specific than '" + regMethodName + "'");
				}
				this.methodNameMap.put(method, name);
				addTransactionalMethod(method, attr);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Keeping attribute for transactional method [" + method + "]: current name '" +
							name + "' is not more specific than '" + regMethodName + "'");
				}
			}
		}
	}

	/**
	 * 为事务方法添加属性.
	 * 
	 * @param method 方法
	 * @param attr 与该方法关联的属性
	 */
	public void addTransactionalMethod(Method method, TransactionAttribute attr) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(attr, "TransactionAttribute must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Adding transactional method [" + method + "] with attribute [" + attr + "]");
		}
		this.transactionAttributeMap.put(method, attr);
	}

	/**
	 * 返回给定的方法名称是否与映射的名称匹配.
	 * <p>默认实现检查 "xxx*", "*xxx"和"*xxx*"匹配, 以及直接相等.
	 * 
	 * @param methodName 类的方法名称
	 * @param mappedName 描述符中的名称
	 * 
	 * @return 是否匹配
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}


	@Override
	public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
		if (this.eagerlyInitialized) {
			return this.transactionAttributeMap.get(method);
		}
		else {
			synchronized (this.transactionAttributeMap) {
				if (!this.initialized) {
					initMethodMap(this.methodMap);
					this.initialized = true;
				}
				return this.transactionAttributeMap.get(method);
			}
		}
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodMapTransactionAttributeSource)) {
			return false;
		}
		MethodMapTransactionAttributeSource otherTas = (MethodMapTransactionAttributeSource) other;
		return ObjectUtils.nullSafeEquals(this.methodMap, otherTas.methodMap);
	}

	@Override
	public int hashCode() {
		return MethodMapTransactionAttributeSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.methodMap;
	}

}
