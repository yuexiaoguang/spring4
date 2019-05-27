package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;

/**
 * Strategy interface used by {@link TransactionInterceptor} for metadata retrieval.
 *
 * <p>Implementations know how to source transaction attributes, whether from configuration,
 * metadata attributes at source level (such as Java 5 annotations), or anywhere else.
 */
public interface TransactionAttributeSource {

	/**
	 * Return the transaction attribute for the given method,
	 * or {@code null} if the method is non-transactional.
	 * @param method the method to introspect
	 * @param targetClass the target class. May be {@code null},
	 * in which case the declaring class of the method must be used.
	 * @return TransactionAttribute the matching transaction attribute,
	 * or {@code null} if none found
	 */
	TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass);

}
