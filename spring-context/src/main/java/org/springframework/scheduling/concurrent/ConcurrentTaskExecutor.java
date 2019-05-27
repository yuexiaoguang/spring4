package org.springframework.scheduling.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.enterprise.concurrent.ManagedExecutors;
import javax.enterprise.concurrent.ManagedTask;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 获取{@code java.util.concurrent.Executor}
 * 并为其公开Spring {@link org.springframework.core.task.TaskExecutor}的适配器.
 * 还检测扩展的{@code java.util.concurrent.ExecutorService},
 * 相应地调整{@link org.springframework.core.task.AsyncTaskExecutor}接口.
 *
 * <p>自动检测JSR-236 {@link javax.enterprise.concurrent.ManagedExecutorService},
 * 以便为它公开{@link javax.enterprise.concurrent.ManagedTask}适配器,
 * 公开基于{@link SchedulingAwareRunnable}的长时间运行提示, 和基于给定Runnable/Callable's {@code toString()}的身份名称.
 * 对于Java EE 7环境中的JSR-236样式查找, 请考虑使用 {@link DefaultManagedTaskExecutor}.
 *
 * <p>请注意, 有一个预构建的{@link ThreadPoolTask​​Executor}, 允许在bean风格中定义{@link java.util.concurrent.ThreadPoolExecutor},
 * 直接将它暴露为Spring {@link org.springframework.core.task.TaskExecutor}.
 * 这是原始ThreadPoolExecutor定义的一个方便的替代方法, 它具有对当前适配器类的单独定义.
 */
public class ConcurrentTaskExecutor implements AsyncListenableTaskExecutor, SchedulingTaskExecutor {

	private static Class<?> managedExecutorServiceClass;

	static {
		try {
			managedExecutorServiceClass = ClassUtils.forName(
					"javax.enterprise.concurrent.ManagedExecutorService",
					ConcurrentTaskScheduler.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// JSR-236 API not available...
			managedExecutorServiceClass = null;
		}
	}

	private Executor concurrentExecutor;

	private TaskExecutorAdapter adaptedExecutor;


	/**
	 * 使用单个线程执行器作为默认值.
	 */
	public ConcurrentTaskExecutor() {
		setConcurrentExecutor(null);
	}

	/**
	 * 使用给定的{@link java.util.concurrent.Executor}.
	 * <p>自动检测JSR-236 {@link javax.enterprise.concurrent.ManagedExecutorService},
	 * 以便为它公开{@link javax.enterprise.concurrent.ManagedTask}适配器.
	 * 
	 * @param concurrentExecutor 要委托给的{@link java.util.concurrent.Executor}
	 */
	public ConcurrentTaskExecutor(Executor concurrentExecutor) {
		setConcurrentExecutor(concurrentExecutor);
	}


	/**
	 * 指定要委托给的{@link java.util.concurrent.Executor}.
	 * <p>自动检测JSR-236 {@link javax.enterprise.concurrent.ManagedExecutorService},
	 * 以便为它公开{@link javax.enterprise.concurrent.ManagedTask}适配器.
	 */
	public final void setConcurrentExecutor(Executor concurrentExecutor) {
		if (concurrentExecutor != null) {
			this.concurrentExecutor = concurrentExecutor;
			if (managedExecutorServiceClass != null && managedExecutorServiceClass.isInstance(concurrentExecutor)) {
				this.adaptedExecutor = new ManagedTaskExecutorAdapter(concurrentExecutor);
			}
			else {
				this.adaptedExecutor = new TaskExecutorAdapter(concurrentExecutor);
			}
		}
		else {
			this.concurrentExecutor = Executors.newSingleThreadExecutor();
			this.adaptedExecutor = new TaskExecutorAdapter(this.concurrentExecutor);
		}
	}

	/**
	 * 返回此适配器委托给的{@link java.util.concurrent.Executor}.
	 */
	public final Executor getConcurrentExecutor() {
		return this.concurrentExecutor;
	}

	/**
	 * 指定要应用于即将执行的{@link Runnable}的自定义{@link TaskDecorator}.
	 * <p>请注意, 这样的装饰器不一定应用于用户提供的{@code Runnable}/{@code Callable},
	 * 而是应用于实际执行回调(可能是用户提供的任务的包装器).
	 * <p>主要用例是围绕任务的调用设置一些执行上下文, 或者为任务执行提供一些监视/统计.
	 */
	public final void setTaskDecorator(TaskDecorator taskDecorator) {
		this.adaptedExecutor.setTaskDecorator(taskDecorator);
	}


	@Override
	public void execute(Runnable task) {
		this.adaptedExecutor.execute(task);
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		this.adaptedExecutor.execute(task, startTimeout);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return this.adaptedExecutor.submit(task);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return this.adaptedExecutor.submit(task);
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		return this.adaptedExecutor.submitListenable(task);
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		return this.adaptedExecutor.submitListenable(task);
	}

	/**
	 * 该任务执行器更喜欢短时间运行任务.
	 */
	@Override
	public boolean prefersShortLivedTasks() {
		return true;
	}


	/**
	 * TaskExecutorAdapter子类, 用JSR-236 ManagedTask包装所有提供的Runnables和Callables,
	 * 公开基于{@link SchedulingAwareRunnable}的长时间运行提示和基于任务的{@code toString()}的身份名称.
	 */
	private static class ManagedTaskExecutorAdapter extends TaskExecutorAdapter {

		public ManagedTaskExecutorAdapter(Executor concurrentExecutor) {
			super(concurrentExecutor);
		}

		@Override
		public void execute(Runnable task) {
			super.execute(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public Future<?> submit(Runnable task) {
			return super.submit(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return super.submit(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public ListenableFuture<?> submitListenable(Runnable task) {
			return super.submitListenable(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}

		@Override
		public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
			return super.submitListenable(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
		}
	}


	/**
	 * 使用JSR-236 ManagedTask包装给定的Runnable/Callable的委托,
	 * 基于{@link SchedulingAwareRunnable}和给定的身份名称公开长时间运行的提示.
	 */
	protected static class ManagedTaskBuilder {

		public static Runnable buildManagedTask(Runnable task, String identityName) {
			Map<String, String> properties = new HashMap<String, String>(2);
			if (task instanceof SchedulingAwareRunnable) {
				properties.put(ManagedTask.LONGRUNNING_HINT,
						Boolean.toString(((SchedulingAwareRunnable) task).isLongLived()));
			}
			properties.put(ManagedTask.IDENTITY_NAME, identityName);
			return ManagedExecutors.managedTask(task, properties, null);
		}

		public static <T> Callable<T> buildManagedTask(Callable<T> task, String identityName) {
			Map<String, String> properties = new HashMap<String, String>(1);
			properties.put(ManagedTask.IDENTITY_NAME, identityName);
			return ManagedExecutors.managedTask(task, properties, null);
		}
	}

}
