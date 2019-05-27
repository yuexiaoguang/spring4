package org.springframework.messaging.handler.invocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 缓存异常处理方法映射, 并提供查找应处理异常的方法的选项.
 * 如果多个方法匹配, 则使用{@link ExceptionDepthComparator}对它们进行排序, 并返回最高匹配项.
 */
public abstract class AbstractExceptionHandlerMethodResolver {

	private static final Method NO_METHOD_FOUND =
			ClassUtils.getMethodIfAvailable(System.class, "currentTimeMillis");

	private final Map<Class<? extends Throwable>, Method> mappedMethods =
			new ConcurrentHashMap<Class<? extends Throwable>, Method>(16);

	private final Map<Class<? extends Throwable>, Method> exceptionLookupCache =
			new ConcurrentHashMap<Class<? extends Throwable>, Method>(16);


	/**
	 * 接受异常到方法的映射.
	 */
	protected AbstractExceptionHandlerMethodResolver(Map<Class<? extends Throwable>, Method> mappedMethods) {
		Assert.notNull(mappedMethods, "Mapped Methods must not be null");
		this.mappedMethods.putAll(mappedMethods);
	}

	/**
	 * 提取此方法处理的异常.
	 * 此实现在方法签名中查找Throwable的子类.
	 * 该方法是静态的, 以确保从子类构造函数的安全使用.
	 */
	@SuppressWarnings("unchecked")
	protected static List<Class<? extends Throwable>> getExceptionsFromMethodSignature(Method method) {
		List<Class<? extends Throwable>> result = new ArrayList<Class<? extends Throwable>>();
		for (Class<?> paramType : method.getParameterTypes()) {
			if (Throwable.class.isAssignableFrom(paramType)) {
				result.add((Class<? extends Throwable>) paramType);
			}
		}
		if (result.isEmpty()) {
			throw new IllegalStateException("No exception types mapped to " + method);
		}
		return result;
	}


	/**
	 * 包含的类型是否具有任何异常映射.
	 */
	public boolean hasExceptionMappings() {
		return !this.mappedMethods.isEmpty();
	}

	/**
	 * 查找处理给定的异常的{@link Method}.
	 * 如果找到多个匹配项, 使用{@link ExceptionDepthComparator}.
	 * 
	 * @param exception 异常
	 * 
	 * @return 处理异常的方法, 或{@code null}
	 */
	public Method resolveMethod(Exception exception) {
		Method method = resolveMethodByExceptionType(exception.getClass());
		if (method == null) {
			Throwable cause = exception.getCause();
			if (cause != null) {
				method = resolveMethodByExceptionType(cause.getClass());
			}
		}
		return method;
	}

	/**
	 * 查找处理给定的异常类型的{@link Method}.
	 * 如果{@link Exception}实例不可用 (e.g. 对于工具), 这可能很有用.
	 * 
	 * @param exceptionType 异常类型
	 * 
	 * @return 处理异常的方法, 或{@code null}
	 */
	public Method resolveMethodByExceptionType(Class<? extends Throwable> exceptionType) {
		Method method = this.exceptionLookupCache.get(exceptionType);
		if (method == null) {
			method = getMappedMethod(exceptionType);
			this.exceptionLookupCache.put(exceptionType, method != null ? method : NO_METHOD_FOUND);
		}
		return method != NO_METHOD_FOUND ? method : null;
	}

	/**
	 * 返回映射到给定异常类型的{@link Method}, 或{@code null}.
	 */
	private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
		List<Class<? extends Throwable>> matches = new ArrayList<Class<? extends Throwable>>();
		for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
			if (mappedException.isAssignableFrom(exceptionType)) {
				matches.add(mappedException);
			}
		}
		if (!matches.isEmpty()) {
			Collections.sort(matches, new ExceptionDepthComparator(exceptionType));
			return this.mappedMethods.get(matches.get(0));
		}
		else {
			return null;
		}
	}
}
