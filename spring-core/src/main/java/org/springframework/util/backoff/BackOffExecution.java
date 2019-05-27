package org.springframework.util.backoff;

/**
 * 表示特定的回退执行.
 *
 * <p>实现不需要是线程安全的.
 */
public interface BackOffExecution {

	/**
	 * {@link #nextBackOff()}的返回值, 表示不应重试该操作.
	 */
	long STOP = -1;

	/**
	 * 返回在重试操作之前等待的毫秒数或{@link #STOP} ({@value #STOP}) 以指示不应再进行该操作的尝试.
	 */
	long nextBackOff();

}
