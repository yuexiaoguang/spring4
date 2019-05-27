package org.springframework.scheduling;

/**
 * 扩展Runnable接口, 为长时间运行的操作添加特殊回调.
 *
 * <p>此接口与CommonJ Work接口紧密对应, 但保持独立以避免必需的CommonJ依赖项.
 *
 * <p>支持可调度任务的TaskExecutors检查提交的Runnable, 检测是​​否已实现此接口并做出适当的响应.
 */
public interface SchedulingAwareRunnable extends Runnable {

	/**
	 * 返回Runnable的操作是long-lived ({@code true}) 还是short-lived ({@code false}).
	 * <p>在前一种情况下, 任务不会从线程池中分配线程, 而是被视为长时间运行的后台线程.
	 * <p>这应该被视为暗示. 当然, TaskExecutor实现可以自由地忽略此标志和SchedulingAwareRunnable接口.
	 */
	boolean isLongLived();

}
