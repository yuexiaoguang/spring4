package org.springframework.messaging.simp.config;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.Reactor2TcpClient;
import org.springframework.util.Assert;

/**
 * 用于配置{@link StompBrokerRelayMessageHandler}的注册类.
 */
public class StompBrokerRelayRegistration extends AbstractBrokerRegistration {

	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String clientLogin = "guest";

	private String clientPasscode = "guest";

	private String systemLogin = "guest";

	private String systemPasscode = "guest";

	private Long systemHeartbeatSendInterval;

	private Long systemHeartbeatReceiveInterval;

	private String virtualHost;

	private TcpOperations<byte[]> tcpClient;

	private boolean autoStartup = true;

	private String userDestinationBroadcast;

	private String userRegistryBroadcast;


	public StompBrokerRelayRegistration(SubscribableChannel clientInboundChannel,
			MessageChannel clientOutboundChannel, String[] destinationPrefixes) {

		super(clientInboundChannel, clientOutboundChannel, destinationPrefixes);
	}


	/**
	 * 设置STOMP消息代理主机.
	 */
	public StompBrokerRelayRegistration setRelayHost(String relayHost) {
		Assert.hasText(relayHost, "relayHost must not be empty");
		this.relayHost = relayHost;
		return this;
	}

	/**
	 * 设置STOMP消息代理端口.
	 */
	public StompBrokerRelayRegistration setRelayPort(int relayPort) {
		this.relayPort = relayPort;
		return this;
	}

	/**
	 * 设置在客户端创建与STOMP代理的连接时使用的登录名.
	 * <p>默认"guest".
	 */
	public StompBrokerRelayRegistration setClientLogin(String login) {
		Assert.hasText(login, "clientLogin must not be empty");
		this.clientLogin = login;
		return this;
	}

	/**
	 * 设置客户端创建与STOMP代理的连接时使用的密码.
	 * <p>默认"guest".
	 */
	public StompBrokerRelayRegistration setClientPasscode(String passcode) {
		Assert.hasText(passcode, "clientPasscode must not be empty");
		this.clientPasscode = passcode;
		return this;
	}

	/**
	 * 设置用于从应用程序内向STOMP代理发送消息的共享"系统"连接的登录名,
	 * i.e. 与特定客户端会话无关的消息 (e.g. REST/HTTP请求处理方法).
	 * <p>默认"guest".
	 */
	public StompBrokerRelayRegistration setSystemLogin(String login) {
		Assert.hasText(login, "systemLogin must not be empty");
		this.systemLogin = login;
		return this;
	}

	/**
	 * 设置用于从应用程序内向STOMP代理发送消息的共享"系统"连接的密码,
	 * i.e. 与特定客户端会话无关的消息 (e.g. REST/HTTP请求处理方法).
	 * <p>默认"guest".
	 */
	public StompBrokerRelayRegistration setSystemPasscode(String passcode) {
		Assert.hasText(passcode, "systemPasscode must not be empty");
		this.systemPasscode = passcode;
		return this;
	}

	/**
	 * 设置"系统"中继会话将在没有任何其他数据发送的情况下, 向STOMP代理发送心跳的间隔, 以毫秒为单位.
	 * 值为零将阻止将心跳发送到代理.
	 * <p>默认 10000.
	 */
	public StompBrokerRelayRegistration setSystemHeartbeatSendInterval(long systemHeartbeatSendInterval) {
		this.systemHeartbeatSendInterval = systemHeartbeatSendInterval;
		return this;
	}

	/**
	 * 设置"系统"中继会话在没有任何其他数据的情况下, 期望从STOMP代理接收心跳的最大间隔, 以毫秒为单位.
	 * 值为零将配置中继会话以期望不从代理接收心跳.
	 * <p>默认 10000.
	 */
	public StompBrokerRelayRegistration setSystemHeartbeatReceiveInterval(long heartbeatReceiveInterval) {
		this.systemHeartbeatReceiveInterval = heartbeatReceiveInterval;
		return this;
	}

	/**
	 * 设置要在STOMP CONNECT帧中使用的"host" header的值.
	 * 配置此属性后, 将向发送到STOMP代理的每个STOMP帧添加"host" header.
	 * 这可能是有用的, 例如在云环境中, 建立TCP连接的实际主机与提供基于云的STOMP服务的主机不同.
	 * <p>默认未设置.
	 */
	public StompBrokerRelayRegistration setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
		return this;
	}

	/**
	 * 配置TCP客户端以管理与STOMP代理的TCP连接.
	 * 默认使用{@link Reactor2TcpClient}.
	 * <p><strong>Note:</strong> 使用此属性时,
	 * 指定的任何{@link #setRelayHost(String) host}或{@link #setRelayPort(int) port}都会被忽略.
	 */
	public void setTcpClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
	}

	/**
	 * 配置刷新Spring ApplicationContext时是否应自动启动{@link StompBrokerRelayMessageHandler}.
	 * <p>默认 {@code true}.
	 */
	public StompBrokerRelayRegistration setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
		return this;
	}

	/**
	 * 设置目标以将消息广播到仍未解析的用户目标, 因为用户似乎没有连接.
	 * 在多应用程序服务器方案中, 这为其他应用程序服务器提供了尝试的机会.
	 * <p>默认不设置.
	 * 
	 * @param destination 广播未解析的消息的目标, e.g. "/topic/unresolved-user-destination"
	 */
	public StompBrokerRelayRegistration setUserDestinationBroadcast(String destination) {
		this.userDestinationBroadcast = destination;
		return this;
	}

	protected String getUserDestinationBroadcast() {
		return this.userDestinationBroadcast;
	}

	/**
	 * 设置目标以将本地用户注册表的内容广播到其他服务器, 并监听此类广播.
	 * 在多应用程序服务器方案中, 这允许每个服务器的用户注册表了解连接到其他服务器的用户.
	 * <p>默认不设置.
	 * 
	 * @param destination 广播用户注册表详细信息的目标, e.g. "/topic/simp-user-registry".
	 */
	public StompBrokerRelayRegistration setUserRegistryBroadcast(String destination) {
		this.userRegistryBroadcast = destination;
		return this;
	}

	protected String getUserRegistryBroadcast() {
		return this.userRegistryBroadcast;
	}


	protected StompBrokerRelayMessageHandler getMessageHandler(SubscribableChannel brokerChannel) {

		StompBrokerRelayMessageHandler handler = new StompBrokerRelayMessageHandler(
				getClientInboundChannel(), getClientOutboundChannel(),
				brokerChannel, getDestinationPrefixes());

		handler.setRelayHost(this.relayHost);
		handler.setRelayPort(this.relayPort);

		handler.setClientLogin(this.clientLogin);
		handler.setClientPasscode(this.clientPasscode);

		handler.setSystemLogin(this.systemLogin);
		handler.setSystemPasscode(this.systemPasscode);

		if (this.systemHeartbeatSendInterval != null) {
			handler.setSystemHeartbeatSendInterval(this.systemHeartbeatSendInterval);
		}
		if (this.systemHeartbeatReceiveInterval != null) {
			handler.setSystemHeartbeatReceiveInterval(this.systemHeartbeatReceiveInterval);
		}
		if (this.virtualHost != null) {
			handler.setVirtualHost(this.virtualHost);
		}
		if (this.tcpClient != null) {
			handler.setTcpClient(this.tcpClient);
		}

		handler.setAutoStartup(this.autoStartup);

		return handler;
	}

}
