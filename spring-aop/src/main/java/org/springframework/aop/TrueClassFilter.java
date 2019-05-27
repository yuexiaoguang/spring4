package org.springframework.aop;

import java.io.Serializable;

/**
 * 与所有类匹配的ClassFilter实例.
 */
@SuppressWarnings("serial")
class TrueClassFilter implements ClassFilter, Serializable {

	public static final TrueClassFilter INSTANCE = new TrueClassFilter();

	/**
	 * 强制Singleton模式.
	 */
	private TrueClassFilter() {
	}

	@Override
	public boolean matches(Class<?> clazz) {
		return true;
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
		return "ClassFilter.TRUE";
	}

}
