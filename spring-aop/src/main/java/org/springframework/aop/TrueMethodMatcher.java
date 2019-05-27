package org.springframework.aop;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * 匹配所有方法的 MethodMatcher实例.
 */
@SuppressWarnings("serial")
class TrueMethodMatcher implements MethodMatcher, Serializable {

	public static final TrueMethodMatcher INSTANCE = new TrueMethodMatcher();


	/**
	 * 强制Singleton模式.
	 */
	private TrueMethodMatcher() {
	}


	@Override
	public boolean isRuntime() {
		return false;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, Object... args) {
		// 永远不应该调用，因为isRuntime返回false.
		throw new UnsupportedOperationException();
	}


	@Override
	public String toString() {
		return "MethodMatcher.TRUE";
	}

	/**
	 * 需要支持序列化. 在反序列化时替换规范实例, 保护Singleton 模式.
	 * 替代覆盖 {@code equals()}.
	 */
	private Object readResolve() {
		return INSTANCE;
	}

}
