package org.springframework.messaging.tcp;

/**
 * 一种以固定间隔进行重新连接尝试的简单策略.
 */
public class FixedIntervalReconnectStrategy implements ReconnectStrategy {

	private final long interval;


	/**
	 * @param interval 尝试重新连接的频率, 以毫秒为单位
	 */
	public FixedIntervalReconnectStrategy(long interval) {
		this.interval = interval;
	}


	@Override
	public Long getTimeToNextAttempt(int attemptCount) {
		return this.interval;
	}

}
