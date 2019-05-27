package org.springframework.scheduling.support;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;

/**
 * 周期性任务执行的触发器.
 * 该周期可以应用为固定速率或固定延迟, 并且还可以配置初始延迟值.
 * 默认初始延迟为0, 默认行为为固定延迟
 * (i.e. 从每个<emphasis>完成</emphasis>时间开始测量连续执行之间的间隔).
 * 要测量每次执行的计划<emphasis>start</emphasis>时间之间的间隔, 将'fixedRate'属性设置为{@code true}.
 *
 * <p>请注意, TaskScheduler接口已经定义了以固定速率或固定延迟来调度任务的方法.
 * 两者都支持初始延迟的可选值. 应尽可能直接使用这些方法.
 * 此Trigger实现的值是它可以在依赖于Trigger抽象的组件中使用.
 * 例如, 允许定期触发, 基于cron的触发, 甚至自定义Trigger实现.
 */
public class PeriodicTrigger implements Trigger {

	private final long period;

	private final TimeUnit timeUnit;

	private volatile long initialDelay = 0;

	private volatile boolean fixedRate = false;


	/**
	 * 创建具有给定周期的触发器, 以毫秒为单位.
	 */
	public PeriodicTrigger(long period) {
		this(period, null);
	}

	/**
	 * 使用给定的期间和时间单位创建触发器.
	 * 时间单位不仅适用于周期, 还适用于任何'initialDelay'值, 如果稍后通过{@link #setInitialDelay(long)}在此Trigger上配置.
	 */
	public PeriodicTrigger(long period, TimeUnit timeUnit) {
		Assert.isTrue(period >= 0, "period must not be negative");
		this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
		this.period = this.timeUnit.toMillis(period);
	}


	/**
	 * 指定初始执行的延迟.
	 * 它将根据此触发器{@link TimeUnit}进行评估.
	 * 如果在实例化时未明确提供时间单位, 则默认为毫秒.
	 */
	public void setInitialDelay(long initialDelay) {
		this.initialDelay = this.timeUnit.toMillis(initialDelay);
	}

	/**
	 * 指定是否应在计划的开始时间之间, 还是在实际完成时间之间, 测量周期性间隔.
	 * 后者"固定延迟"行为是默认行为.
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}


	/**
	 * 返回任务应再次运行的时间.
	 */
	@Override
	public Date nextExecutionTime(TriggerContext triggerContext) {
		if (triggerContext.lastScheduledExecutionTime() == null) {
			return new Date(System.currentTimeMillis() + this.initialDelay);
		}
		else if (this.fixedRate) {
			return new Date(triggerContext.lastScheduledExecutionTime().getTime() + this.period);
		}
		return new Date(triggerContext.lastCompletionTime().getTime() + this.period);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PeriodicTrigger)) {
			return false;
		}
		PeriodicTrigger other = (PeriodicTrigger) obj;
		return (this.fixedRate == other.fixedRate && this.initialDelay == other.initialDelay &&
				this.period == other.period);
	}

	@Override
	public int hashCode() {
		return (this.fixedRate ? 17 : 29) + (int) (37 * this.period) + (int) (41 * this.initialDelay);
	}

}
