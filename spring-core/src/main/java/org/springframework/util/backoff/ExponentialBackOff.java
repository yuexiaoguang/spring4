package org.springframework.util.backoff;

/**
 * {@link BackOff}实现, 增加每次重试尝试的回退时间.
 * 当间隔达到 {@link #setMaxInterval(long) 最大间隔}时, 它不再增加.
 * 一旦达到{@link #setMaxElapsedTime(long) 最大经过时间}, 就停止重试.
 *
 * <p>Example: 默认时间间隔为{@value #DEFAULT_INITIAL_INTERVAL} ms,
 * 默认乘数为{@value #DEFAULT_MULTIPLIER},
 * 默认最大时间间隔为{@value #DEFAULT_MAX_INTERVAL}.
 * 对于10次尝试, 序列如下:
 *
 * <pre>
 * request#     back off
 *
 *  1              2000
 *  2              3000
 *  3              4500
 *  4              6750
 *  5             10125
 *  6             15187
 *  7             22780
 *  8             30000
 *  9             30000
 * 10             30000
 * </pre>
 *
 * <p>请注意, 默认的最长经过时间是 {@link Long#MAX_VALUE}.
 * 使用{@link #setMaxElapsedTime(long)}来限制实例在返回{@link BackOffExecution#STOP}之前应该累积的最长时间.
 */
public class ExponentialBackOff implements BackOff {

	/**
	 * 默认的初始间隔.
	 */
	public static final long DEFAULT_INITIAL_INTERVAL = 2000L;

	/**
	 * 默认乘数 (将间隔增加 50%).
	 */
	public static final double DEFAULT_MULTIPLIER = 1.5;

	/**
	 * 默认的最大回退时间.
	 */
	public static final long DEFAULT_MAX_INTERVAL = 30000L;

	/**
	 * 默认的最长经过时间.
	 */
	public static final long DEFAULT_MAX_ELAPSED_TIME = Long.MAX_VALUE;


	private long initialInterval = DEFAULT_INITIAL_INTERVAL;

	private double multiplier = DEFAULT_MULTIPLIER;

	private long maxInterval = DEFAULT_MAX_INTERVAL;

	private long maxElapsedTime = DEFAULT_MAX_ELAPSED_TIME;


	/**
	 * 使用默认设置.
	 */
	public ExponentialBackOff() {
	}

	/**
	 * 使用提供的设置.
	 * 
	 * @param initialInterval 初始间隔, 以毫秒为单位
	 * @param multiplier 乘数 (应大于或等于1)
	 */
	public ExponentialBackOff(long initialInterval, double multiplier) {
		checkMultiplier(multiplier);
		this.initialInterval = initialInterval;
		this.multiplier = multiplier;
	}


	/**
	 * 初始间隔, 以毫秒为单位.
	 */
	public void setInitialInterval(long initialInterval) {
		this.initialInterval = initialInterval;
	}

	/**
	 * 初始间隔, 以毫秒为单位.
	 */
	public long getInitialInterval() {
		return initialInterval;
	}

	/**
	 * 每次重试尝试将当前间隔乘以的值.
	 */
	public void setMultiplier(double multiplier) {
		checkMultiplier(multiplier);
		this.multiplier = multiplier;
	}

	/**
	 * 每次重试尝试将当前间隔乘以的值.
	 */
	public double getMultiplier() {
		return multiplier;
	}

	/**
	 * 最大回退时间.
	 */
	public void setMaxInterval(long maxInterval) {
		this.maxInterval = maxInterval;
	}

	/**
	 * 最大回退时间.
	 */
	public long getMaxInterval() {
		return maxInterval;
	}

	/**
	 * 调用{@link BackOffExecution#nextBackOff()}返回{@link BackOffExecution#STOP}的最长经过时间, 以毫秒为单位.
	 */
	public void setMaxElapsedTime(long maxElapsedTime) {
		this.maxElapsedTime = maxElapsedTime;
	}

	/**
	 * 调用{@link BackOffExecution#nextBackOff()}返回{@link BackOffExecution#STOP}的最长经过时间, 以毫秒为单位.
	 */
	public long getMaxElapsedTime() {
		return maxElapsedTime;
	}

	@Override
	public BackOffExecution start() {
		return new ExponentialBackOffExecution();
	}

	private void checkMultiplier(double multiplier) {
		if (multiplier < 1) {
			throw new IllegalArgumentException("Invalid multiplier '" + multiplier + "'. Should be equal" +
					"or higher than 1. A multiplier of 1 is equivalent to a fixed interval");
		}
	}


	private class ExponentialBackOffExecution implements BackOffExecution {

		private long currentInterval = -1;

		private long currentElapsedTime = 0;

		@Override
		public long nextBackOff() {
			if (this.currentElapsedTime >= maxElapsedTime) {
				return STOP;
			}

			long nextInterval = computeNextInterval();
			this.currentElapsedTime += nextInterval;
			return nextInterval;
		}

		private long computeNextInterval() {
			long maxInterval = getMaxInterval();
			if (this.currentInterval >= maxInterval) {
				return maxInterval;
			}
			else if (this.currentInterval < 0) {
			 	long initialInterval = getInitialInterval();
				this.currentInterval = (initialInterval < maxInterval
						? initialInterval : maxInterval);
			}
			else {
				this.currentInterval = multiplyInterval(maxInterval);
			}
			return this.currentInterval;
		}

		private long multiplyInterval(long maxInterval) {
			long i = this.currentInterval;
			i *= getMultiplier();
			return (i > maxInterval ? maxInterval : i);
		}


		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("ExponentialBackOff{");
			sb.append("currentInterval=").append(this.currentInterval < 0 ? "n/a" : this.currentInterval + "ms");
			sb.append(", multiplier=").append(getMultiplier());
			sb.append('}');
			return sb.toString();
		}
	}
}
