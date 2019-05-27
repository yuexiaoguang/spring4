package org.springframework.scheduling;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

/**
 * 任务调度器接口, 它根据不同类型的触发器抽象{@link Runnable Runnables}的调度.
 *
 * <p>此接口与{@link SchedulingTaskExecutor}分开, 因为它通常代表不同类型的后端,
 * i.e. 具有不同特征和功能的线程池.
 * 如果实现可以处理两种执行特征, 则实现可以实现这两种接口.
 *
 * <p>默认实现是{@link org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler},
 * 包装本机{@link java.util.concurrent.ScheduledExecutorService}并添加扩展的触发器功能.
 *
 * <p>此接口大致相当于Java EE 6环境中支持的 JSR-236 {@code ManagedScheduledExecutorService}.
 * 但是, 在Spring 3.0发布时, JSR-236接口尚未以官方形式发布.
 */
public interface TaskScheduler {

	/**
	 * 调度给定的{@link Runnable}, 每当触发器指示下一个执行时间时调用它.
	 * <p>一旦调度器关闭或返回的{@link ScheduledFuture}被取消, 执行将结束.
	 * 
	 * @param task 触发器触发时要执行的Runnable
	 * @param trigger {@link Trigger}接口的实现,
	 * e.g. 包含cron表达式的{@link org.springframework.scheduling.support.CronTrigger}对象
	 * 
	 * @return 表示任务未完成的{@link ScheduledFuture}; 如果给定的Trigger对象从未触发, 则为{@code null}
	 * (i.e. 从{@link Trigger#nextExecutionTime}返回{@code null})
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因未接受给定任务 (e.g. 池超载处理策略或池正在关闭)
	 */
	ScheduledFuture<?> schedule(Runnable task, Trigger trigger);

	/**
	 * 调度给定的{@link Runnable}, 在指定的执行时间调用它.
	 * <p>一旦调度器关闭或返回的{@link ScheduledFuture}被取消, 执行将结束.
	 * 
	 * @param task 触发器触发时要执行的Runnable
	 * @param startTime 任务所需的执行时间
	 * (过去, 任务将立即执行, i.e. 尽快执行)
	 * 
	 * @return 表示任务未完成的{@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因未接受给定任务(e.g. 池超载处理策略或池正在关闭)
	 */
	ScheduledFuture<?> schedule(Runnable task, Date startTime);

	/**
	 * 调度给定的{@link Runnable}, 在指定的执行时间调用它, 以及在给定的时间段内调用它.
	 * <p>一旦调度器关闭或返回的{@link ScheduledFuture}被取消, 执行将结束.
	 * 
	 * @param task 触发器触发时要执行的Runnable
	 * @param startTime 任务所需的第一个执行时间
	 * (过去, 任务将立即执行, i.e. 尽快执行)
	 * @param period 连续执行任务之间的间隔(以毫秒为单位)
	 * 
	 * @return 表示任务未完成的{@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因未接受给定任务(e.g. 池超载处理策略或池正在关闭)
	 */
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period);

	/**
	 * 调度给定的{@link Runnable}, 尽快开始并在给定的时间段内调用它.
	 * <p>一旦调度器关闭或返回的{@link ScheduledFuture}被取消, 执行将结束.
	 * 
	 * @param task 触发器触发时要执行的Runnable
	 * @param period 连续执行任务之间的间隔 (以毫秒为单位)
	 * 
	 * @return 表示任务未完成的{@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因未接受给定任务(e.g. 池超载处理策略或池正在关闭)
	 */
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period);

	/**
	 * 调度给定的{@link Runnable}, 在指定的执行时间调用它, 然后在一次执行完成和下一次执行开始之间给定延迟.
	 * <p>一旦调度器关闭或返回的{@link ScheduledFuture}被取消, 执行将结束.
	 * 
	 * @param task 触发器触发时要执行的Runnable
	 * @param startTime 任务所需的第一个执行时间
	 * (过去, 任务将立即执行, i.e. 尽快执行)
	 * @param delay 一次执行完成和下一次执行开始之间的延迟 (以毫秒为单位)
	 * 
	 * @return 表示任务未完成的{@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因未接受给定任务(e.g. 池超载处理策略或池正在关闭)
	 */
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay);

	/**
	 * 调度给定的{@link Runnable}, 尽快启动, 并在一次执行完成和下一次执行开始之间的给定延迟之后调用它.
	 * <p>一旦调度器关闭或返回的{@link ScheduledFuture}被取消, 执行将结束.
	 * 
	 * @param task 触发器触发时要执行的Runnable
	 * @param delay 连续执行任务之间的间隔 (以毫秒为单位)
	 * 
	 * @return 表示任务未完成的{@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因未接受给定任务(e.g. 池超载处理策略或池正在关闭)
	 */
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay);

}
