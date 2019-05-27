package org.springframework.aop;

import java.io.Serializable;

/**
 * 始终匹配的Pointcut实例.
 */
@SuppressWarnings("serial")
class TruePointcut implements Pointcut, Serializable {

	public static final TruePointcut INSTANCE = new TruePointcut();

	/**
	 * 强制Singleton模式.
	 */
	private TruePointcut() {
	}

	@Override
	public ClassFilter getClassFilter() {
		return ClassFilter.TRUE;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return MethodMatcher.TRUE;
	}

	/**
	 * 需要支持序列化. 在反序列化时替换规范实例, 保护Singleton 模式.
	 * 替代覆盖 {@code equals()}.
	 */
	private Object readResolve() {
		return INSTANCE;
	}

	@Override
	public String toString() {
		return "Pointcut.TRUE";
	}

}
