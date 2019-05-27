package org.springframework.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.util.Assert;

/**
 * 比较器, 能够根据抛出的异常类型的深度对异常进行排序.
 */
public class ExceptionDepthComparator implements Comparator<Class<? extends Throwable>> {

	private final Class<? extends Throwable> targetException;


	/**
	 * @param exception 按深度排序时比较的目标异常
	 */
	public ExceptionDepthComparator(Throwable exception) {
		Assert.notNull(exception, "Target exception must not be null");
		this.targetException = exception.getClass();
	}

	/**
	 * @param exceptionType 按深度排序时比较的目标异常类型
	 */
	public ExceptionDepthComparator(Class<? extends Throwable> exceptionType) {
		Assert.notNull(exceptionType, "Target exception type must not be null");
		this.targetException = exceptionType;
	}


	@Override
	public int compare(Class<? extends Throwable> o1, Class<? extends Throwable> o2) {
		int depth1 = getDepth(o1, this.targetException, 0);
		int depth2 = getDepth(o2, this.targetException, 0);
		return (depth1 - depth2);
	}

	private int getDepth(Class<?> declaredException, Class<?> exceptionToMatch, int depth) {
		if (exceptionToMatch.equals(declaredException)) {
			// Found it!
			return depth;
		}
		// 如果已经尽可能远, 却找不到它...
		if (exceptionToMatch == Throwable.class) {
			return Integer.MAX_VALUE;
		}
		return getDepth(declaredException, exceptionToMatch.getSuperclass(), depth + 1);
	}


	/**
	 * 从给定目标异常的给定异常类型中获取最接近的匹配.
	 * 
	 * @param exceptionTypes 异常类型的集合
	 * @param targetException 要查找匹配项的目标异常
	 * 
	 * @return 给定集合中最接近的匹配异常类型
	 */
	public static Class<? extends Throwable> findClosestMatch(
			Collection<Class<? extends Throwable>> exceptionTypes, Throwable targetException) {

		Assert.notEmpty(exceptionTypes, "Exception types must not be empty");
		if (exceptionTypes.size() == 1) {
			return exceptionTypes.iterator().next();
		}
		List<Class<? extends Throwable>> handledExceptions =
				new ArrayList<Class<? extends Throwable>>(exceptionTypes);
		Collections.sort(handledExceptions, new ExceptionDepthComparator(targetException));
		return handledExceptions.get(0);
	}
}
