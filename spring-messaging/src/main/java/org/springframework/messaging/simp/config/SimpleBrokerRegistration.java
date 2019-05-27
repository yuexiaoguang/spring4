package org.springframework.messaging.simp.config;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.scheduling.TaskScheduler;

/**
 * 用于配置{@link SimpleBrokerMessageHandler}的注册类.
 */
public class SimpleBrokerRegistration extends AbstractBrokerRegistration {

	private TaskScheduler taskScheduler;

	private long[] heartbeat;

	private String selectorHeaderName = "selector";


	public SimpleBrokerRegistration(SubscribableChannel inChannel, MessageChannel outChannel, String[] prefixes) {
		super(inChannel, outChannel, prefixes);
	}


	/**
	 * 配置{@link org.springframework.scheduling.TaskScheduler}以用于提供心跳支持.
	 * 设置此属性还会将{@link #setHeartbeatValue heartbeatValue}设置为"10000, 10000".
	 * <p>默认不设置.
	 */
	public SimpleBrokerRegistration setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		return this;
	}

	/**
	 * 配置心跳设置的值.
	 * 第一个数字表示服务器写入或发送心跳的频率.
	 * 第二个是客户应该多久写一次. 0表示没有心跳.
	 * <p>默认设置为"0, 0", 除非{@link #setTaskScheduler taskScheduler}在这种情况下默认值为"10000,10000" (以毫秒为单位).
	 */
	public SimpleBrokerRegistration setHeartbeatValue(long[] heartbeat) {
		this.heartbeat = heartbeat;
		return this;
	}

	/**
	 * 配置订阅消息可以具有的header名称, 以便过滤与订阅匹配的消息.
	 * header值应该是一个Spring EL布尔表达式, 应用于与订阅匹配的消息的header.
	 * <p>例如:
	 * <pre>
	 * headers.foo == 'bar'
	 * </pre>
	 * <p>默认设置为"selector". 可以将其设置为其他名称, 或{@code null}以关闭对选择器header的支持.
	 * 
	 * @param selectorHeaderName 用于选择器header的名称
	 */
	public void setSelectorHeaderName(String selectorHeaderName) {
		this.selectorHeaderName = selectorHeaderName;
	}


	@Override
	protected SimpleBrokerMessageHandler getMessageHandler(SubscribableChannel brokerChannel) {
		SimpleBrokerMessageHandler handler = new SimpleBrokerMessageHandler(getClientInboundChannel(),
				getClientOutboundChannel(), brokerChannel, getDestinationPrefixes());
		if (this.taskScheduler != null) {
			handler.setTaskScheduler(this.taskScheduler);
		}
		if (this.heartbeat != null) {
			handler.setHeartbeatValue(this.heartbeat);
		}
		handler.setSelectorHeaderName(this.selectorHeaderName);
		return handler;
	}

}
