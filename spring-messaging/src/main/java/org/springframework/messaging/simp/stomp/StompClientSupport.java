package org.springframework.messaging.simp.stomp;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * STOMP客户端实现的基类.
 *
 * <p>子类可以使用任何库通过WebSocket或TCP进行连接.
 * 创建新连接时, 子类可以创建{@link DefaultStompSession}的实例,
 * 该实例扩展{@link org.springframework.messaging.tcp.TcpConnectionHandler}, 其生命周期方法子类必须调用.
 *
 * <p>实际上, {@code TcpConnectionHandler}和{@code TcpConnection}是任何子类
 * 在使用{@link StompEncoder}和{@link StompDecoder}编码和解码STOMP消息时必须适配的约定.
 */
public abstract class StompClientSupport {

	protected Log logger = LogFactory.getLog(getClass());

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private TaskScheduler taskScheduler;

	private long[] defaultHeartbeat = new long[] {10000, 10000};

	private long receiptTimeLimit = 15 * 1000;


	/**
	 * 设置{@link MessageConverter}, 用于根据对象类型和"content-type" header转换来自{@code byte[]}的传入和传出消息的有效负载.
	 * <p>默认{@link SimpleMessageConverter}.
	 * 
	 * @param messageConverter 要使用的消息转换器
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	/**
	 * 返回配置的{@link MessageConverter}.
	 */
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * 配置用于心跳和回执跟踪的调度器.
	 * <p><strong>Note:</strong> 某些传输具有心跳的内置支持, 因此不需要TaskScheduler.
	 * 但是, 如果需要, 回执确实需要配置TaskScheduler.
	 * <p>默认不设置.
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * 返回配置的TaskScheduler.
	 */
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * 配置STOMP CONNECT帧的"heart-beat" header的默认值.
	 * 第一个数字表示客户端写入或发送心跳的频率. 第二个是服务器应该写的频率.
	 * 值为0表示没有心跳.
	 * <p>默认"10000,10000", 但子类可以覆盖该默认值, 例如如果它们需要首先配置TaskScheduler, 则将其设置为"0,0".
	 * 
	 * @param heartbeat CONNECT "heart-beat" header的值
	 * 
	 * @see <a href="http://stomp.github.io/stomp-specification-1.2.html#Heart-beating">
	 * http://stomp.github.io/stomp-specification-1.2.html#Heart-beating</a>
	 */
	public void setDefaultHeartbeat(long[] heartbeat) {
		if (heartbeat == null || heartbeat.length != 2 || heartbeat[0] < 0 || heartbeat[1] < 0) {
			throw new IllegalArgumentException("Invalid heart-beat: " + Arrays.toString(heartbeat));
		}
		this.defaultHeartbeat = heartbeat;
	}

	/**
	 * 返回配置的默认心跳值 (never {@code null}).
	 */
	public long[] getDefaultHeartbeat() {
		return this.defaultHeartbeat;
	}

	/**
	 * 确定是否启用心跳.
	 * <p>如果{@link #setDefaultHeartbeat defaultHeartbeat}设置为"0,0", 则返回{@code false}, 否则返回{@code true}.
	 */
	public boolean isDefaultHeartbeatEnabled() {
		long[] heartbeat = getDefaultHeartbeat();
		return (heartbeat[0] != 0 && heartbeat[1] != 0);
	}

	/**
	 * 配置回执过期时间, 以毫秒为单位.
	 * <p>默认 15,000 (15 seconds).
	 */
	public void setReceiptTimeLimit(long receiptTimeLimit) {
		Assert.isTrue(receiptTimeLimit > 0, "Receipt time limit must be larger than zero");
		this.receiptTimeLimit = receiptTimeLimit;
	}

	/**
	 * 返回配置的回执时间限制.
	 */
	public long getReceiptTimeLimit() {
		return this.receiptTimeLimit;
	}


	/**
	 * 用于创建和配置新会话的工厂方法.
	 * 
	 * @param connectHeaders STOMP CONNECT帧的header
	 * @param handler STOMP会话的处理器
	 * 
	 * @return 创建的会话
	 */
	protected ConnectionHandlingStompSession createSession(StompHeaders connectHeaders, StompSessionHandler handler) {
		connectHeaders = processConnectHeaders(connectHeaders);
		DefaultStompSession session = new DefaultStompSession(handler, connectHeaders);
		session.setMessageConverter(getMessageConverter());
		session.setTaskScheduler(getTaskScheduler());
		session.setReceiptTimeLimit(getReceiptTimeLimit());
		return session;
	}

	/**
	 * 进一步初始化StompHeaders, 例如, 如有必要, 设置心跳heart-beat header.
	 * 
	 * @param connectHeaders 要编辑的header
	 * 
	 * @return 编辑后的header
	 */
	protected StompHeaders processConnectHeaders(StompHeaders connectHeaders) {
		connectHeaders = (connectHeaders != null ? connectHeaders : new StompHeaders());
		if (connectHeaders.getHeartbeat() == null) {
			connectHeaders.setHeartbeat(getDefaultHeartbeat());
		}
		return connectHeaders;
	}

}
