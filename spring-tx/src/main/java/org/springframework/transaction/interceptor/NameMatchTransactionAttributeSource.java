package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * 简单的{@link TransactionAttributeSource}实现, 允许通过注册名称匹配属性.
 */
@SuppressWarnings("serial")
public class NameMatchTransactionAttributeSource implements TransactionAttributeSource, Serializable {

	/**
	 * Logger available to subclasses.
	 * <p>Static for optimal serialization.
	 */
	protected static final Log logger = LogFactory.getLog(NameMatchTransactionAttributeSource.class);

	/** 方法名称作为键; 值是TransactionAttributes */
	private Map<String, TransactionAttribute> nameMap = new HashMap<String, TransactionAttribute>();


	/**
	 * 设置名称/属性映射, 包括方法名称 (e.g. "myMethod")和TransactionAttribute实例(或要转换为TransactionAttribute实例的字符串).
	 */
	public void setNameMap(Map<String, TransactionAttribute> nameMap) {
		for (Map.Entry<String, TransactionAttribute> entry : nameMap.entrySet()) {
			addTransactionalMethod(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 将给定属性解析为名称/属性映射.
	 * 将方法名称视为键, 将字符串属性定义视为值,
	 * 可通过TransactionAttributeEditor解析为TransactionAttribute实例.
	 */
	public void setProperties(Properties transactionAttributes) {
		TransactionAttributeEditor tae = new TransactionAttributeEditor();
		Enumeration<?> propNames = transactionAttributes.propertyNames();
		while (propNames.hasMoreElements()) {
			String methodName = (String) propNames.nextElement();
			String value = transactionAttributes.getProperty(methodName);
			tae.setAsText(value);
			TransactionAttribute attr = (TransactionAttribute) tae.getValue();
			addTransactionalMethod(methodName, attr);
		}
	}

	/**
	 * 为事务方法添加属性.
	 * <p>方法名称可以精确匹配, 或者是模式"xxx*", "*xxx" 或 "*xxx*", 用于匹配多个方法.
	 * 
	 * @param methodName 方法名称
	 * @param attr 方法关联的属性
	 */
	public void addTransactionalMethod(String methodName, TransactionAttribute attr) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding transactional method [" + methodName + "] with attribute [" + attr + "]");
		}
		this.nameMap.put(methodName, attr);
	}


	@Override
	public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
		if (!ClassUtils.isUserLevelMethod(method)) {
			return null;
		}

		// 寻找直接名称匹配.
		String methodName = method.getName();
		TransactionAttribute attr = this.nameMap.get(methodName);

		if (attr == null) {
			// 寻找最具体的名称匹配.
			String bestNameMatch = null;
			for (String mappedName : this.nameMap.keySet()) {
				if (isMatch(methodName, mappedName) &&
						(bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
					attr = this.nameMap.get(mappedName);
					bestNameMatch = mappedName;
				}
			}
		}

		return attr;
	}

	/**
	 * 给定的方法名称是否匹配映射的名称.
	 * <p>默认实现检查"xxx*", "*xxx" 和 "*xxx*"匹配, 以及直接相等. 可以在子类中重写.
	 * 
	 * @param methodName 类的方法名
	 * @param mappedName 描述符中的名称
	 * 
	 * @return 名称是否匹配
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NameMatchTransactionAttributeSource)) {
			return false;
		}
		NameMatchTransactionAttributeSource otherTas = (NameMatchTransactionAttributeSource) other;
		return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
	}

	@Override
	public int hashCode() {
		return NameMatchTransactionAttributeSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.nameMap;
	}

}
