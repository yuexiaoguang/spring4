package org.springframework.messaging.tcp;

/**
 * 连接失败后确定重新连接尝试频率的约定.
 */
public interface ReconnectStrategy {

	/**
	 * 返回下一次尝试重新连接的时间.
	 * 
	 * @param attemptCount 已经进行了多少次重新连接尝试
	 * 
	 * @return 以毫秒为单位的时间或{@code null}停止
	 */
	Long getTimeToNextAttempt(int attemptCount);

}
