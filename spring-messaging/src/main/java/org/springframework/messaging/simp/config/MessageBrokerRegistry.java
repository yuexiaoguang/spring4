package org.springframework.messaging.simp.config;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;

/**
 * 用于配置消息代理选项的注册表.
 */
public class MessageBrokerRegistry {

	private final SubscribableChannel clientInboundChannel;

	private final MessageChannel clientOutboundChannel;

	private SimpleBrokerRegistration simpleBrokerRegistration;

	private StompBrokerRelayRegistration brokerRelayRegistration;

	private final ChannelRegistration brokerChannelRegistration = new ChannelRegistration();

	private String[] applicationDestinationPrefixes;

	private String userDestinationPrefix;

	private PathMatcher pathMatcher;

	private Integer cacheLimit;


	public MessageBrokerRegistry(SubscribableChannel clientInboundChannel, MessageChannel clientOutboundChannel) {
		Assert.notNull(clientInboundChannel, "Inbound channel must not be null");
		Assert.notNull(clientOutboundChannel, "Outbound channel must not be null");
		this.clientInboundChannel = clientInboundChannel;
		this.clientOutboundChannel = clientOutboundChannel;
	}


	/**
	 * 启用简单的消息代理, 并配置一个或多个前缀以过滤以代理为目标的目标 (e.g. 前缀为"/topic"的目标).
	 */
	public SimpleBrokerRegistration enableSimpleBroker(String... destinationPrefixes) {
		this.simpleBrokerRegistration = new SimpleBrokerRegistration(
				this.clientInboundChannel, this.clientOutboundChannel, destinationPrefixes);
		return this.simpleBrokerRegistration;
	}

	/**
	 * 启用S​​TOMP代理中继, 并配置消息代理支持的目标前缀.
	 * 检查消息代理的STOMP文档以获取支持的目标.
	 */
	public StompBrokerRelayRegistration enableStompBrokerRelay(String... destinationPrefixes) {
		this.brokerRelayRegistration = new StompBrokerRelayRegistration(
				this.clientInboundChannel, this.clientOutboundChannel, destinationPrefixes);
		return this.brokerRelayRegistration;
	}

	/**
	 * 自定义用于将消息从应用程序发送到消息代理的通道.
	 * 默认情况下, 从应用程序到消息代理的消息是同步发送的, 这意味着发送消息的应用程序代码将查明消息是否无法通过异常发送.
	 * 但是, 如果此处使用任务执行器属性配置代理通道, 则可以更改此设置.
	 */
	public ChannelRegistration configureBrokerChannel() {
		return this.brokerChannelRegistration;
	}

	protected ChannelRegistration getBrokerChannelRegistration() {
		return this.brokerChannelRegistration;
	}

	protected String getUserDestinationBroadcast() {
		return (this.brokerRelayRegistration != null ?
				this.brokerRelayRegistration.getUserDestinationBroadcast() : null);
	}

	protected String getUserRegistryBroadcast() {
		return (this.brokerRelayRegistration != null ?
				this.brokerRelayRegistration.getUserRegistryBroadcast() : null);
	}

	/**
	 * 配置一个或多个前缀以过滤以应用程序带注解的方法为目标的目标.
	 * 例如, 以"/app"为前缀的目标可以通过带注解的方法处理, 而其他目标可以以消息代理为目标 (e.g. "/topic", "/queue").
	 * <p>处理消息时, 将从目标中删除匹配前缀以形成查找路径.
	 * 这意味着注解不应包含目标前缀.
	 * <p>没有尾部斜杠的前缀将自动附加一个.
	 */
	public MessageBrokerRegistry setApplicationDestinationPrefixes(String... prefixes) {
		this.applicationDestinationPrefixes = prefixes;
		return this;
	}

	protected Collection<String> getApplicationDestinationPrefixes() {
		return (this.applicationDestinationPrefixes != null ?
				Arrays.asList(this.applicationDestinationPrefixes) : null);
	}

	/**
	 * 配置用于标识用户目标的前缀.
	 * 用户目标使用户能够订阅其会话中唯一的队列名称, 以及其他人将消息发送到这些唯一的, 特定于用户的队列的能力.
	 * <p>例如, 当用户尝试订阅"/user/queue/position-updates"时, 目标可能会被转换为"/queue/position-updatesi9oqdfzo",
	 * 从而产生一个唯一的队列名称, 该名称不会与任何其他尝试执行此操作的用户发生冲突.
	 * 随后, 当消息发送到"/user/{username}/queue/position-updates"时, 目标将转换为"/queue/position-updatesi9oqdfzo".
	 * <p>用于标识此类目标的默认前缀是 "/user/".
	 */
	public MessageBrokerRegistry setUserDestinationPrefix(String destinationPrefix) {
		this.userDestinationPrefix = destinationPrefix;
		return this;
	}

	protected String getUserDestinationPrefix() {
		return this.userDestinationPrefix;
	}

	/**
	 * 配置PathMatcher以用于将传入消息的目标与{@code @MessageMapping}和{@code @SubscribeMapping}方法进行匹配.
	 * <p>默认{@link org.springframework.util.AntPathMatcher}.
	 * 但是, 应用程序可能会提供{@code AntPathMatcher}实例, 自定义使用"." (通常用于消息),
	 * 而不是"/"作为路径分隔符, 或提供完全不同的PathMatcher实现.
	 * <p>请注意, 配置的PathMatcher仅用于在配置的前缀后匹配目标部分.
	 * 例如, 给定应用程序目标前缀"/app"和目标"/app/price.stock.**", 消息可能映射到具有"price" 和 "stock.**"作为其类型和方法级别的控制器.
	 * <p>启用简单代理时, 此处配置的PathMatcher还用于在代理消息时匹配消息目标.
	 */
	public MessageBrokerRegistry setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}

	protected PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 配置缓存限制以应用到代理的注册.
	 * <p>目前, 这仅适用于订阅注册表中的目标缓存. 默认缓存限制为 1024.
	 */
	public MessageBrokerRegistry setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
		return this;
	}


	protected SimpleBrokerMessageHandler getSimpleBroker(SubscribableChannel brokerChannel) {
		if (this.simpleBrokerRegistration == null && this.brokerRelayRegistration == null) {
			enableSimpleBroker();
		}
		if (this.simpleBrokerRegistration != null) {
			SimpleBrokerMessageHandler handler = this.simpleBrokerRegistration.getMessageHandler(brokerChannel);
			handler.setPathMatcher(this.pathMatcher);
			handler.setCacheLimit(this.cacheLimit);
			return handler;
		}
		return null;
	}

	protected StompBrokerRelayMessageHandler getStompBrokerRelay(SubscribableChannel brokerChannel) {
		if (this.brokerRelayRegistration != null) {
			return this.brokerRelayRegistration.getMessageHandler(brokerChannel);
		}
		return null;
	}

}
