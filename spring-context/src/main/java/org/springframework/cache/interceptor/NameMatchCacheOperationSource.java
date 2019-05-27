package org.springframework.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * 简单的{@link CacheOperationSource}实现, 允许通过注册名称匹配属性.
 */
@SuppressWarnings("serial")
public class NameMatchCacheOperationSource implements CacheOperationSource, Serializable {

	/**
	 * Logger available to subclasses.
	 * <p>静态以实现最佳序列化.
	 */
	protected static final Log logger = LogFactory.getLog(NameMatchCacheOperationSource.class);


	/** Key是方法名称; 值是 TransactionAttributes */
	private Map<String, Collection<CacheOperation>> nameMap = new LinkedHashMap<String, Collection<CacheOperation>>();


	/**
	 * 设置名称/属性 map, 包括方法名称 (e.g. "myMethod") 和CacheOperation实例 (或要转换为CacheOperation实例的字符串).
	 */
	public void setNameMap(Map<String, Collection<CacheOperation>> nameMap) {
		for (Map.Entry<String, Collection<CacheOperation>> entry : nameMap.entrySet()) {
			addCacheMethod(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 为可缓存方法添加属性.
	 * <p>方法名称可以是完全匹配, 或者是用于匹配多个方法的模式 "xxx*", "*xxx" or "*xxx*".
	 * 
	 * @param methodName 方法名称
	 * @param ops 与该方法相关的操作
	 */
	public void addCacheMethod(String methodName, Collection<CacheOperation> ops) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding method [" + methodName + "] with cache operations [" + ops + "]");
		}
		this.nameMap.put(methodName, ops);
	}

	@Override
	public Collection<CacheOperation> getCacheOperations(Method method, Class<?> targetClass) {
		// 查找直接名称匹配
		String methodName = method.getName();
		Collection<CacheOperation> ops = this.nameMap.get(methodName);

		if (ops == null) {
			// 查找最具体的名称匹配.
			String bestNameMatch = null;
			for (String mappedName : this.nameMap.keySet()) {
				if (isMatch(methodName, mappedName)
						&& (bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
					ops = this.nameMap.get(mappedName);
					bestNameMatch = mappedName;
				}
			}
		}

		return ops;
	}

	/**
	 * 返回给定的方法名称是否与映射的名称匹配.
	 * <p>默认实现检查 "xxx*", "*xxx"和 "*xxx*"匹配, 以及直接相等.
	 * 可以在子类中重写.
	 * 
	 * @param methodName 类的方法名称
	 * @param mappedName 描述符中的名称
	 * 
	 * @return 如果名称匹配
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NameMatchCacheOperationSource)) {
			return false;
		}
		NameMatchCacheOperationSource otherTas = (NameMatchCacheOperationSource) other;
		return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
	}

	@Override
	public int hashCode() {
		return NameMatchCacheOperationSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.nameMap;
	}
}
