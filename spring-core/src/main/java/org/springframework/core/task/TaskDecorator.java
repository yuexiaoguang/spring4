package org.springframework.core.task;

/**
 * 装饰器的回调接口, 应用于即将执行的任何{@link Runnable}.
 *
 * <p>请注意, 这样的装饰器不一定应用于用户提供的{@code Runnable}/{@code Callable},
 * 而是应用于实际执行回调(可能是用户提供的任务的包装器).
 *
 * <p>主要用例是围绕任务的调用设置一些执行上下文, 或者为任务执行提供一些监视/统计.
 */
public interface TaskDecorator {

	/**
	 * 装饰给定的{@code Runnable}, 返回一个可能被包装的{@code Runnable}来实际执行.
	 * 
	 * @param runnable 原始的{@code Runnable}
	 * 
	 * @return 已装饰的{@code Runnable}
	 */
	Runnable decorate(Runnable runnable);

}
