package org.springframework.cache.aspectj;

/**
 * 用于欺骗编译器在拦截器中抛出有效的已检查异常的实用程序.
 */
class AnyThrow {

	static void throwUnchecked(Throwable e) {
		AnyThrow.<RuntimeException>throwAny(e);
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void throwAny(Throwable e) throws E {
		throw (E) e;
	}
}
