package org.springframework.messaging.simp.broker;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link MessageHandler}的抽象基类, 用于将消息代理到已注册的订阅者.
 */
public abstract class AbstractBrokerMessageHandler
		implements MessageHandler, ApplicationEventPublisherAware, SmartLifecycle {

	protected final Log logger = LogFactory.getLog(getClass());

	private final SubscribableChannel clientInboundChannel;

	private final MessageChannel clientOutboundChannel;

	private final SubscribableChannel brokerChannel;

	private final Collection<String> destinationPrefixes;

	private ApplicationEventPublisher eventPublisher;

	private AtomicBoolean brokerAvailable = new AtomicBoolean(false);

	private final BrokerAvailabilityEvent availableEvent = new BrokerAvailabilityEvent(true, this);

	private final BrokerAvailabilityEvent notAvailableEvent = new BrokerAvailabilityEvent(false, this);

	private boolean autoStartup = true;

	private volatile boolean running = false;

	private final Object lifecycleMonitor = new Object();

	private final ChannelInterceptor unsentDisconnectInterceptor = new UnsentDisconnectChannelInterceptor();


	/**
	 * 没有目标前缀的构造函数 (匹配所有目标).
	 * 
	 * @param inboundChannel 用于从客户端 (e.g. WebSocket客户端)接收消息的通道
	 * @param outboundChannel 用于向客户端 (e.g. WebSocket客户端)发送消息的通道
	 * @param brokerChannel 应用程序向代理发送消息的通道
	 */
	public AbstractBrokerMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel) {

		this(inboundChannel, outboundChannel, brokerChannel, Collections.<String>emptyList());
	}

	/**
	 * 具有目标前缀的构造方法, 以匹配消息的目标.
	 * 
	 * @param inboundChannel 用于从客户端 (e.g. WebSocket客户端)接收消息的通道
	 * @param outboundChannel 用于向客户端 (e.g. WebSocket客户端)发送消息的通道
	 * @param brokerChannel 应用程序向代理发送消息的通道
	 * @param destinationPrefixes 用于过滤消息的前缀
	 */
	public AbstractBrokerMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel, Collection<String> destinationPrefixes) {

		Assert.notNull(inboundChannel, "'inboundChannel' must not be null");
		Assert.notNull(outboundChannel, "'outboundChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");

		this.clientInboundChannel = inboundChannel;
		this.clientOutboundChannel = outboundChannel;
		this.brokerChannel = brokerChannel;

		destinationPrefixes = (destinationPrefixes != null ? destinationPrefixes : Collections.<String>emptyList());
		this.destinationPrefixes = Collections.unmodifiableCollection(destinationPrefixes);
	}


	public SubscribableChannel getClientInboundChannel() {
		return this.clientInboundChannel;
	}

	public MessageChannel getClientOutboundChannel() {
		return this.clientOutboundChannel;
	}

	public SubscribableChannel getBrokerChannel() {
		return this.brokerChannel;
	}

	public Collection<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.eventPublisher = publisher;
	}

	public ApplicationEventPublisher getApplicationEventPublisher() {
		return this.eventPublisher;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}


	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			logger.info("Starting...");
			this.clientInboundChannel.subscribe(this);
			this.brokerChannel.subscribe(this);
			if (this.clientInboundChannel instanceof InterceptableChannel) {
				((InterceptableChannel) this.clientInboundChannel).addInterceptor(0, this.unsentDisconnectInterceptor);
			}
			startInternal();
			this.running = true;
			logger.info("Started.");
		}
	}

	protected void startInternal() {
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			logger.info("Stopping...");
			stopInternal();
			this.clientInboundChannel.unsubscribe(this);
			this.brokerChannel.unsubscribe(this);
			if (this.clientInboundChannel instanceof InterceptableChannel) {
				((InterceptableChannel) this.clientInboundChannel).removeInterceptor(this.unsentDisconnectInterceptor);
			}
			this.running = false;
			logger.info("Stopped.");
		}
	}

	protected void stopInternal() {
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	/**
	 * 检查此消息处理器当前是否正在运行.
	 * <p>请注意, 即使此消息处理器正在运行, {@link #isBrokerAvailable()}标志仍然可以在打开和关闭之间独立交替,
	 * 具体取决于具体的子类实现.
	 */
	@Override
	public final boolean isRunning() {
		return this.running;
	}

	/**
	 * 消息代理是否当前可用并且能够处理消息.
	 * <p>请注意, 这是{@link #isRunning()}标志的补充, 该标志指示此消息处理器是否正在运行.
	 * 换句话说, 消息处理器必须首先运行, 然后{@code #isBrokerAvailable()}标志仍然可以在打开和关闭之间独立交替, 具体取决于具体的子类实现.
	 * <p>应用程序组件可以实现{@code org.springframework.context.ApplicationListener&lt;BrokerAvailabilityEvent&gt;}
	 * 以在代理可用和不可用时接收通知.
	 */
	public boolean isBrokerAvailable() {
		return this.brokerAvailable.get();
	}


	@Override
	public void handleMessage(Message<?> message) {
		if (!this.running) {
			if (logger.isTraceEnabled()) {
				logger.trace(this + " not running yet. Ignoring " + message);
			}
			return;
		}
		handleMessageInternal(message);
	}

	protected abstract void handleMessageInternal(Message<?> message);


	protected boolean checkDestinationPrefix(String destination) {
		if (destination == null || CollectionUtils.isEmpty(this.destinationPrefixes)) {
			return true;
		}
		for (String prefix : this.destinationPrefixes) {
			if (destination.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	protected void publishBrokerAvailableEvent() {
		boolean shouldPublish = this.brokerAvailable.compareAndSet(false, true);
		if (this.eventPublisher != null && shouldPublish) {
			if (logger.isInfoEnabled()) {
				logger.info(this.availableEvent);
			}
			this.eventPublisher.publishEvent(this.availableEvent);
		}
	}

	protected void publishBrokerUnavailableEvent() {
		boolean shouldPublish = this.brokerAvailable.compareAndSet(true, false);
		if (this.eventPublisher != null && shouldPublish) {
			if (logger.isInfoEnabled()) {
				logger.info(this.notAvailableEvent);
			}
			this.eventPublisher.publishEvent(this.notAvailableEvent);
		}
	}


	/**
	 * 检测未发送的DISCONNECT消息并进行处理.
	 */
	private class UnsentDisconnectChannelInterceptor extends ChannelInterceptorAdapter {

		@Override
		public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
			if (!sent) {
				SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
				if (SimpMessageType.DISCONNECT.equals(messageType)) {
					logger.debug("Detected unsent DISCONNECT message. Processing anyway.");
					handleMessage(message);
				}
			}
		}
	}
}
