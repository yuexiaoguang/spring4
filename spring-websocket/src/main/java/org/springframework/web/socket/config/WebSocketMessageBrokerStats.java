package org.springframework.web.socket.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

/**
 * 用于聚合来自用于Java配置的{@code @EnableWebSocketMessageBroker}和用于XML的{@code <websocket:message-broker>}的设置
 * 的关键基础结构组件的内部状态和计数器的信息的中心类.
 *
 * <p>默认情况下, 在INFO级别每15分钟记录一次聚合信息.
 * 可以通过{@link #setLoggingPeriod(long)}更改日志记录的频率.
 *
 * <p>该类通过上面的配置声明为Spring bean, 名称为"webSocketMessageBrokerStats",
 * 可以很容易地导出到JMX, e.g. 使用{@link org.springframework.jmx.export.MBeanExporter MBeanExporter}.
 */
public class WebSocketMessageBrokerStats {

	private static final Log logger = LogFactory.getLog(WebSocketMessageBrokerStats.class);


	private SubProtocolWebSocketHandler webSocketHandler;

	private StompSubProtocolHandler stompSubProtocolHandler;

	private StompBrokerRelayMessageHandler stompBrokerRelay;

	private ThreadPoolExecutor inboundChannelExecutor;

	private ThreadPoolExecutor outboundChannelExecutor;

	private ScheduledThreadPoolExecutor sockJsTaskScheduler;

	private ScheduledFuture<?> loggingTask;

	private long loggingPeriod = 30 * 60 * 1000;


	public void setSubProtocolWebSocketHandler(SubProtocolWebSocketHandler webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
		this.stompSubProtocolHandler = initStompSubProtocolHandler();
	}

	private StompSubProtocolHandler initStompSubProtocolHandler() {
		for (SubProtocolHandler handler : this.webSocketHandler.getProtocolHandlers()) {
			if (handler instanceof StompSubProtocolHandler) {
				return (StompSubProtocolHandler) handler;
			}
		}
		SubProtocolHandler defaultHandler = this.webSocketHandler.getDefaultProtocolHandler();
		if (defaultHandler != null && defaultHandler instanceof StompSubProtocolHandler) {
			return (StompSubProtocolHandler) defaultHandler;
		}
		return null;
	}

	public void setStompBrokerRelay(StompBrokerRelayMessageHandler stompBrokerRelay) {
		this.stompBrokerRelay = stompBrokerRelay;
	}

	public void setInboundChannelExecutor(ThreadPoolTaskExecutor inboundChannelExecutor) {
		this.inboundChannelExecutor = inboundChannelExecutor.getThreadPoolExecutor();
	}

	public void setOutboundChannelExecutor(ThreadPoolTaskExecutor outboundChannelExecutor) {
		this.outboundChannelExecutor = outboundChannelExecutor.getThreadPoolExecutor();
	}

	public void setSockJsTaskScheduler(ThreadPoolTaskScheduler sockJsTaskScheduler) {
		this.sockJsTaskScheduler = sockJsTaskScheduler.getScheduledThreadPoolExecutor();
		this.loggingTask = initLoggingTask(1 * 60 * 1000);
	}

	private ScheduledFuture<?> initLoggingTask(long initialDelay) {
		if (this.loggingPeriod > 0 && logger.isInfoEnabled()) {
			return this.sockJsTaskScheduler.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					logger.info(WebSocketMessageBrokerStats.this);
				}
			}, initialDelay, this.loggingPeriod, TimeUnit.MILLISECONDS);
		}
		return null;
	}

	/**
	 * 设置在INFO级别记录信息的频率, 以毫秒为单位.
	 * 如果设置为0或小于0, 则取消记录任务.
	 * <p>默认此属性设置为30分钟 (30 * 60 * 1000).
	 */
	public void setLoggingPeriod(long period) {
		if (this.loggingTask != null) {
			this.loggingTask.cancel(true);
		}
		this.loggingPeriod = period;
		this.loggingTask = initLoggingTask(0);
	}

	/**
	 * 返回配置的记录周期频率, 以毫秒为单位.
	 */
	public long getLoggingPeriod() {
		return this.loggingPeriod;
	}

	/**
	 * 获取有关WebSocket会话的统计信息.
	 */
	public String getWebSocketSessionStatsInfo() {
		return (this.webSocketHandler != null ? this.webSocketHandler.getStatsInfo() : "null");
	}

	/**
	 * 获取有关STOMP相关WebSocket消息处理的统计信息.
	 */
	public String getStompSubProtocolStatsInfo() {
		return (this.stompSubProtocolHandler != null ? this.stompSubProtocolHandler.getStatsInfo() : "null");
	}

	/**
	 * 获取有关STOMP代理中继的统计信息 (使用功能齐全的STOMP代理时).
	 */
	public String getStompBrokerRelayStatsInfo() {
		return (this.stompBrokerRelay != null ? this.stompBrokerRelay.getStatsInfo() : "null");
	}

	/**
	 * 获取有关执行器处理来自WebSocket客户端的传入消息的统计信息.
	 */
	public String getClientInboundExecutorStatsInfo() {
		return (this.inboundChannelExecutor != null ? getExecutorStatsInfo(this.inboundChannelExecutor) : "null");
	}

	/**
	 * 获取有关执行器处理发送到WebSocket客户端的传出消息的统计信息.
	 */
	public String getClientOutboundExecutorStatsInfo() {
		return (this.outboundChannelExecutor != null ? getExecutorStatsInfo(this.outboundChannelExecutor) : "null");
	}

	/**
	 * 获取有关SockJS任务定时器的统计信息.
	 */
	public String getSockJsTaskSchedulerStatsInfo() {
		return (this.sockJsTaskScheduler != null ? getExecutorStatsInfo(this.sockJsTaskScheduler) : "null");
	}

	private String getExecutorStatsInfo(Executor executor) {
		String str = executor.toString();
		return str.substring(str.indexOf("pool"), str.length() - 1);
	}

	public String toString() {
		return "WebSocketSession[" + getWebSocketSessionStatsInfo() + "]" +
				", stompSubProtocol[" + getStompSubProtocolStatsInfo() + "]" +
				", stompBrokerRelay[" + getStompBrokerRelayStatsInfo() + "]" +
				", inboundChannel[" + getClientInboundExecutorStatsInfo() + "]" +
				", outboundChannel" + getClientOutboundExecutorStatsInfo() + "]" +
				", sockJsScheduler[" + getSockJsTaskSchedulerStatsInfo() + "]";
	}

}
