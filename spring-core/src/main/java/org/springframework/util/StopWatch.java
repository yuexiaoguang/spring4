package org.springframework.util;

import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * 简单的秒表, 允许多个任务的计时, 暴露每个命名任务的总运行时间和运行时间.
 *
 * <p>隐藏{@code System.currentTimeMillis()}, 提高应用程序代码的可读性, 并降低计算错误的可能性.
 *
 * <p>请注意, 此对象不是设计为线程安全的, 并且不使用同步.
 *
 * <p>此类通常用于在概念验证和开发过程中验证性能, 而不是作为生产应用程序的一部分.
 */
public class StopWatch {

	/**
	 * 这个秒表的标识符.
	 * 当从多个秒表输出并需要在日志或控制台输出中区分它们时很方便.
	 */
	private final String id;

	private boolean keepTaskList = true;

	private final List<TaskInfo> taskList = new LinkedList<TaskInfo>();

	/** 当前任务的开始时间 */
	private long startTimeMillis;

	/** 秒表是否正在运行? */
	private boolean running;

	/** 当前任务的名称 */
	private String currentTaskName;

	private TaskInfo lastTaskInfo;

	private int taskCount;

	/** 总运行时间 */
	private long totalTimeMillis;


	/**
	 * 不开始任何任务.
	 */
	public StopWatch() {
		this("");
	}

	/**
	 * 不开始任何任务.
	 * 
	 * @param id 这个秒表的标识符. 当从多个秒表输出并需要区分它们时很方便.
	 */
	public StopWatch(String id) {
		this.id = id;
	}


	/**
	 * 返回此秒表的ID.
	 * 
	 * @return the id (默认情况下为空字符串)
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * 确定TaskInfo数组是否随着时间的推移而构建.
	 * 设置为"false", 当数百万个间隔使用StopWatch时, 否则任务信息结构将消耗过多的内存.
	 * 默认 "true".
	 */
	public void setKeepTaskList(boolean keepTaskList) {
		this.keepTaskList = keepTaskList;
	}


	/**
	 * 启动一个未命名的任务. 如果在不调用此方法的情况下调用 {@link #stop()} 或计时方法, 则结果未定义.
	 */
	public void start() throws IllegalStateException {
		start("");
	}

	/**
	 * 启动命名任务. 如果在不调用此方法的情况下调用 {@link #stop()} 或计时方法, 则结果未定义.
	 * 
	 * @param taskName 要启动的任务的名称
	 */
	public void start(String taskName) throws IllegalStateException {
		if (this.running) {
			throw new IllegalStateException("Can't start StopWatch: it's already running");
		}
		this.running = true;
		this.currentTaskName = taskName;
		this.startTimeMillis = System.currentTimeMillis();
	}

	/**
	 * 停止当前任务.
	 * 如果在不调用至少一对{@code start()} / {@code stop()}方法的情况下调用计时方法, 则结果是不确定的.
	 */
	public void stop() throws IllegalStateException {
		if (!this.running) {
			throw new IllegalStateException("Can't stop StopWatch: it's not running");
		}
		long lastTime = System.currentTimeMillis() - this.startTimeMillis;
		this.totalTimeMillis += lastTime;
		this.lastTaskInfo = new TaskInfo(this.currentTaskName, lastTime);
		if (this.keepTaskList) {
			this.taskList.add(lastTaskInfo);
		}
		++this.taskCount;
		this.running = false;
		this.currentTaskName = null;
	}

	/**
	 * 返回秒表当前是否正在运行.
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * 返回当前正在运行的任务的名称.
	 */
	public String currentTaskName() {
		return this.currentTaskName;
	}


	/**
	 * 返回上一个任务所花费的时间.
	 */
	public long getLastTaskTimeMillis() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task interval");
		}
		return this.lastTaskInfo.getTimeMillis();
	}

	/**
	 * 返回上一个任务的名称.
	 */
	public String getLastTaskName() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task name");
		}
		return this.lastTaskInfo.getTaskName();
	}

	/**
	 * 返回最后一个任务.
	 */
	public TaskInfo getLastTaskInfo() throws IllegalStateException {
		if (this.lastTaskInfo == null) {
			throw new IllegalStateException("No tasks run: can't get last task info");
		}
		return this.lastTaskInfo;
	}


	/**
	 * 返回所有任务的总时间, 以毫秒为单位.
	 */
	public long getTotalTimeMillis() {
		return this.totalTimeMillis;
	}

	/**
	 * 返回所有任务的总时间, 以秒为单位.
	 */
	public double getTotalTimeSeconds() {
		return this.totalTimeMillis / 1000.0;
	}

	/**
	 * 返回定时任务的数量.
	 */
	public int getTaskCount() {
		return this.taskCount;
	}

	/**
	 * 返回执行的任务的数据数组.
	 */
	public TaskInfo[] getTaskInfo() {
		if (!this.keepTaskList) {
			throw new UnsupportedOperationException("Task info is not being kept!");
		}
		return this.taskList.toArray(new TaskInfo[this.taskList.size()]);
	}


	/**
	 * 返回总运行时间的简短描述.
	 */
	public String shortSummary() {
		return "StopWatch '" + getId() + "': running time (millis) = " + getTotalTimeMillis();
	}

	/**
	 * 返回一个字符串, 其中包含描述所执行任务的表.
	 * 对于自定义报告, 调用 getTaskInfo() 并直接使用任务信息.
	 */
	public String prettyPrint() {
		StringBuilder sb = new StringBuilder(shortSummary());
		sb.append('\n');
		if (!this.keepTaskList) {
			sb.append("No task info kept");
		}
		else {
			sb.append("-----------------------------------------\n");
			sb.append("ms     %     Task name\n");
			sb.append("-----------------------------------------\n");
			NumberFormat nf = NumberFormat.getNumberInstance();
			nf.setMinimumIntegerDigits(5);
			nf.setGroupingUsed(false);
			NumberFormat pf = NumberFormat.getPercentInstance();
			pf.setMinimumIntegerDigits(3);
			pf.setGroupingUsed(false);
			for (TaskInfo task : getTaskInfo()) {
				sb.append(nf.format(task.getTimeMillis())).append("  ");
				sb.append(pf.format(task.getTimeSeconds() / getTotalTimeSeconds())).append("  ");
				sb.append(task.getTaskName()).append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * 返回描述所执行任务的信息.
	 * 对于自定义报告, 调用 getTaskInfo() 并直接使用任务信息.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(shortSummary());
		if (this.keepTaskList) {
			for (TaskInfo task : getTaskInfo()) {
				sb.append("; [").append(task.getTaskName()).append("] took ").append(task.getTimeMillis());
				long percent = Math.round((100.0 * task.getTimeSeconds()) / getTotalTimeSeconds());
				sb.append(" = ").append(percent).append("%");
			}
		}
		else {
			sb.append("; no task info kept");
		}
		return sb.toString();
	}


	/**
	 * 内部类, 用于保存关于秒表内执行的一个任务的数据.
	 */
	public static final class TaskInfo {

		private final String taskName;

		private final long timeMillis;

		TaskInfo(String taskName, long timeMillis) {
			this.taskName = taskName;
			this.timeMillis = timeMillis;
		}

		/**
		 * 返回此任务的名称.
		 */
		public String getTaskName() {
			return this.taskName;
		}

		/**
		 * 返回此任务所用的时间, 以毫秒为单位.
		 */
		public long getTimeMillis() {
			return this.timeMillis;
		}

		/**
		 * 返回此任务所用的时间, 以秒为单位.
		 */
		public double getTimeSeconds() {
			return (this.timeMillis / 1000.0);
		}
	}
}
