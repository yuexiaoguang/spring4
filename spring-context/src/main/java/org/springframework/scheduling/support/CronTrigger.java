package org.springframework.scheduling.support;

import java.util.Date;
import java.util.TimeZone;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

/**
 * cron表达式的{@link Trigger}实现.
 * 包装了{@link CronSequenceGenerator}.
 */
public class CronTrigger implements Trigger {

	private final CronSequenceGenerator sequenceGenerator;


	/**
	 * 根据默认时区中提供的模式构建{@link CronTrigger}.
	 * 
	 * @param expression 按照空格分隔的时间字段列表, 遵循cron表达式约定
	 */
	public CronTrigger(String expression) {
		this.sequenceGenerator = new CronSequenceGenerator(expression);
	}

	/**
	 * 根据给定时区中提供的模式构建{@link CronTrigger}.
	 * 
	 * @param expression 按照空格分隔的时间字段列表, 遵循cron表达式约定
	 * @param timeZone 将生成触发时间的时区
	 */
	public CronTrigger(String expression, TimeZone timeZone) {
		this.sequenceGenerator = new CronSequenceGenerator(expression, timeZone);
	}


	/**
	 * 返回构建此触发器的cron模式.
	 */
	public String getExpression() {
		return this.sequenceGenerator.getExpression();
	}


	/**
	 * 根据给定的触发上下文确定下一个执行时间.
	 * <p>下一个执行时间是根据前一次执行的{@linkplain TriggerContext#lastCompletionTime 完成时间}计算的;
	 * 因此, 不会发生重叠执行.
	 */
	@Override
	public Date nextExecutionTime(TriggerContext triggerContext) {
		Date date = triggerContext.lastCompletionTime();
		if (date != null) {
			Date scheduled = triggerContext.lastScheduledExecutionTime();
			if (scheduled != null && date.before(scheduled)) {
				// 以前的任务显然执行得太早了...
				// 简单地使用最后计算的执行时间, 以防止在同一秒内意外重新触发.
				date = scheduled;
			}
		}
		else {
			date = new Date();
		}
		return this.sequenceGenerator.next(date);
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof CronTrigger &&
				this.sequenceGenerator.equals(((CronTrigger) other).sequenceGenerator)));
	}

	@Override
	public int hashCode() {
		return this.sequenceGenerator.hashCode();
	}

	@Override
	public String toString() {
		return this.sequenceGenerator.toString();
	}

}
