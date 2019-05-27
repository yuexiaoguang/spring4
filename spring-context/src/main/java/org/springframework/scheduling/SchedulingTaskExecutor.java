package org.springframework.scheduling;

import org.springframework.core.task.AsyncTaskExecutor;

/**
 * {@link org.springframework.core.task.TaskExecutor}扩展, 公开与潜在任务提交者相关的调度特征.
 *
 * <p>鼓励调度客户端提交与正在使用的{@code TaskExecutor}实现的公开偏好相匹配的{@link Runnable Runnables}.
 *
 * <p>Note: 鼓励{@link SchedulingTaskExecutor}实现也实现{@link org.springframework.core.task.AsyncListenableTaskExecutor}接口.
 * 这不是必需的, 由于依赖于Spring 4.0的新{@link org.springframework.util.concurrent.ListenableFuture}接口,
 * 这将使第三方执行器实现无法与Spring 4.0和Spring 3保持兼容.
 */
public interface SchedulingTaskExecutor extends AsyncTaskExecutor {

	/**
	 * 这个{@code TaskExecutor}是否更喜欢短期任务而不是长期任务?
	 * <p>{@code SchedulingTaskExecutor}实现可以指示它是否更喜欢提交的任务, 以便在单个任务执行中执行尽可能少的工作.
	 * 例如, 提交的任务可能会将重复的循环分解为单独的子任务, 然后再提交后续任务.
	 * <p>这应该被视为暗示. 当然{@code TaskExecutor}客户端可以自由地忽略这个标志和{@code SchedulingTaskExecutor}接口.
	 * 但是, 线程池通常会首选短期任务, 以便能够执行更细粒度的调度.
	 * 
	 * @return {@code true} 如果这个{@code TaskExecutor}更喜欢短期任务
	 */
	boolean prefersShortLivedTasks();

}
