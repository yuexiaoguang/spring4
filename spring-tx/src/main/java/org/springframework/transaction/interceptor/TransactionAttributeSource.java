package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;

/**
 * {@link TransactionInterceptor}用于元数据检索的策略接口.
 *
 * <p>实现知道如何从源级别的配置, 元数据属性(例如Java 5注解)或其他任何地方获取事务属性.
 */
public interface TransactionAttributeSource {

	/**
	 * 返回给定方法的事务属性, 或{@code null} 如果方法是非事务性的.
	 * 
	 * @param method 要内省的方法
	 * @param targetClass 目标类. 可能是{@code null}, 在这种情况下必须使用方法的声明类.
	 * 
	 * @return 匹配事务属性的TransactionAttribute, 或{@code null}
	 */
	TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass);

}
