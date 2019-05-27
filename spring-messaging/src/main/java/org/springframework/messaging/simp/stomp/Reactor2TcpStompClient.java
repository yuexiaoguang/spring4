package org.springframework.messaging.simp.stomp;

import io.netty.channel.EventLoopGroup;
import reactor.Environment;
import reactor.io.net.NetStreams.TcpClientFactory;
import reactor.io.net.Spec.TcpClientSpec;
import reactor.io.net.impl.netty.NettyClientSocketOptions;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.Reactor2TcpClient;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 使用{@link Reactor2TcpClient}的TCP客户端上的STOMP.
 */
public class Reactor2TcpStompClient extends StompClientSupport implements Lifecycle {

	private final TcpOperations<byte[]> tcpClient;

	private final EventLoopGroup eventLoopGroup;

	private final Environment environment;

	private volatile boolean running = false;


	/**
	 * 使用主机"127.0.0.1"和端口 61613.
	 */
	public Reactor2TcpStompClient() {
		this("127.0.0.1", 61613);
	}

	public Reactor2TcpStompClient(String host, int port) {
		this.eventLoopGroup = Reactor2TcpClient.initEventLoopGroup();
		this.environment = new Environment();
		this.tcpClient = new Reactor2TcpClient<byte[]>(
				new StompTcpClientSpecFactory(host, port, this.eventLoopGroup, this.environment));
	}

	/**
	 * 使用预配置的TCP客户端创建实例.
	 * 
	 * @param tcpClient 要使用的客户端
	 */
	public Reactor2TcpStompClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
		this.eventLoopGroup = null;
		this.environment = null;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			try {
				if (this.eventLoopGroup != null) {
					this.eventLoopGroup.shutdownGracefully().await(5000);
				}
				if (this.environment != null) {
					this.environment.shutdown();
				}
			}
			catch (InterruptedException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to shutdown gracefully", ex);
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	/**
	 * 在STOMP级别连接时, 连接并通知给定的{@link StompSessionHandler}.
	 * 
	 * @param handler STOMP会话的处理器
	 * 
	 * @return 可在准备使用时访问会话的ListenableFuture
	 */
	public ListenableFuture<StompSession> connect(StompSessionHandler handler) {
		return connect(null, handler);
	}

	/**
	 * {@link #connect(StompSessionHandler)}的重载版本, 它接受用于STOMP CONNECT帧的header.
	 * 
	 * @param connectHeaders 要添加到CONNECT帧的header
	 * @param handler STOMP会话的处理器
	 * 
	 * @return 可在准备使用时访问会话的ListenableFuture
	 */
	public ListenableFuture<StompSession> connect(StompHeaders connectHeaders, StompSessionHandler handler) {
		ConnectionHandlingStompSession session = createSession(connectHeaders, handler);
		this.tcpClient.connect(session);
		return session.getSessionFuture();
	}

	/**
	 * 关闭客户端并释放资源.
	 */
	public void shutdown() {
		this.tcpClient.shutdown();
	}


	private static class StompTcpClientSpecFactory implements TcpClientFactory<Message<byte[]>, Message<byte[]>> {

		private final String host;

		private final int port;

		private final NettyClientSocketOptions socketOptions;

		private final Environment environment;

		private final Reactor2StompCodec codec;


		StompTcpClientSpecFactory(String host, int port, EventLoopGroup group, Environment environment) {
			this.host = host;
			this.port = port;
			this.socketOptions = new NettyClientSocketOptions().eventLoopGroup(group);
			this.environment = environment;
			this.codec = new Reactor2StompCodec(new StompEncoder(), new StompDecoder());
		}

		@Override
		public TcpClientSpec<Message<byte[]>, Message<byte[]>> apply(
				TcpClientSpec<Message<byte[]>, Message<byte[]>> clientSpec) {

			return clientSpec
					.env(this.environment)
					.dispatcher(this.environment.getDispatcher(Environment.WORK_QUEUE))
					.connect(this.host, this.port)
					.codec(this.codec)
					.options(this.socketOptions);
		}
	}

}
