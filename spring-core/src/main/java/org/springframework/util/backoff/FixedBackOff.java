package org.springframework.util.backoff;

/**
 * 一个简单的{@link BackOff}实现, 它提供两次尝试之间的固定间隔和最大重试次数.
 */
public class FixedBackOff implements BackOff {

	/**
	 * 默认恢复间隔: 5000 ms = 5 seconds.
	 */
	public static final long DEFAULT_INTERVAL = 5000;

	/**
	 * 表示无限次尝试.
	 */
	public static final long UNLIMITED_ATTEMPTS = Long.MAX_VALUE;

	private long interval = DEFAULT_INTERVAL;

	private long maxAttempts = UNLIMITED_ATTEMPTS;


	/**
	 * 创建一个间隔为{@value #DEFAULT_INTERVAL} ms 且无限次尝试的实例.
	 */
	public FixedBackOff() {
	}

	/**
	 * @param interval 两次尝试之间的间隔
	 * @param maxAttempts 最大尝试次数
	 */
	public FixedBackOff(long interval, long maxAttempts) {
		this.interval = interval;
		this.maxAttempts = maxAttempts;
	}


	/**
	 * 设置两次尝试之间的间隔, 以毫秒为单位.
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}

	/**
	 * 返回两次尝试之间的间隔, 以毫秒为单位.
	 */
	public long getInterval() {
		return interval;
	}

	/**
	 * 设置最大尝试次数.
	 */
	public void setMaxAttempts(long maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	/**
	 * 返回最大尝试次数.
	 */
	public long getMaxAttempts() {
		return maxAttempts;
	}

	@Override
	public BackOffExecution start() {
		return new FixedBackOffExecution();
	}


	private class FixedBackOffExecution implements BackOffExecution {

		private long currentAttempts = 0;

		@Override
		public long nextBackOff() {
			this.currentAttempts++;
			if (this.currentAttempts <= getMaxAttempts()) {
				return getInterval();
			}
			else {
				return STOP;
			}
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("FixedBackOff{");
			sb.append("interval=").append(FixedBackOff.this.interval);
			String attemptValue = (FixedBackOff.this.maxAttempts == Long.MAX_VALUE ?
					"unlimited" : String.valueOf(FixedBackOff.this.maxAttempts));
			sb.append(", currentAttempts=").append(this.currentAttempts);
			sb.append(", maxAttempts=").append(attemptValue);
			sb.append('}');
			return sb.toString();
		}
	}

}
