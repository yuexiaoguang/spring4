package org.springframework.transaction.annotation;

import java.lang.reflect.AnnotatedElement;

import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * 用于解析已知事务注解类型的策略接口.
 * {@link AnnotationTransactionAttributeSource}委托给支持特定的注解类型的解析器,
 * 例如Spring自己的{@link Transactional}, JTA 1.2的{@link javax.transaction.Transactional}
 * 或EJB3的{@link javax.ejb.TransactionAttribute}.
 */
public interface TransactionAnnotationParser {

	/**
	 * 基于此解析器理解的注解类型, 解析给定方法或类的事务属性.
	 * <p>这实际上将已知的事务注解解析为Spring的元数据属性类. 如果方法/类不是事务的, 则返回{@code null}.
	 * 
	 * @param element 带注解的方法或类
	 * 
	 * @return 已配置的事务属性, 或{@code null}
	 */
	TransactionAttribute parseTransactionAnnotation(AnnotatedElement element);

}
