package org.springframework.cache.interceptor;

/**
 * 缓存操作的抽象调用.
 *
 * <p>不提供传输受检异常的方法, 但提供了一个特殊异常, 该异常应该用于包装底层调用引发的任何异常.
 * 调用者应特殊处理此问题类型.
 */
public interface CacheOperationInvoker {

	/**
	 * 调用此实例定义的缓存操作.
	 * 包装在{@link ThrowableWrapper}中调用期间抛出的任何异常.
	 * 
	 * @return 操作的结果
	 * @throws ThrowableWrapper 如果在调用操作时发生错误
	 */
	Object invoke() throws ThrowableWrapper;


	/**
	 * 包装在调用 {@link #invoke()}时抛出的任何异常.
	 */
	@SuppressWarnings("serial")
	class ThrowableWrapper extends RuntimeException {

		private final Throwable original;

		public ThrowableWrapper(Throwable original) {
			super(original.getMessage(), original);
			this.original = original;
		}

		public Throwable getOriginal() {
			return this.original;
		}
	}

}
