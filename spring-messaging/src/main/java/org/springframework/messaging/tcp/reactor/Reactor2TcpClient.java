package org.springframework.messaging.tcp.reactor;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.reactivestreams.Publisher;
import reactor.Environment;
import reactor.core.config.ConfigurationReader;
import reactor.core.config.DispatcherConfiguration;
import reactor.core.config.ReactorConfiguration;
import reactor.core.support.NamedDaemonThreadFactory;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.fn.Supplier;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;
import reactor.io.buffer.Buffer;
import reactor.io.codec.Codec;
import reactor.io.net.ChannelStream;
import reactor.io.net.NetStreams;
import reactor.io.net.NetStreams.TcpClientFactory;
import reactor.io.net.ReactorChannelHandler;
import reactor.io.net.Reconnect;
import reactor.io.net.Spec.TcpClientSpec;
import reactor.io.net.config.ClientSocketOptions;
import reactor.io.net.impl.netty.NettyClientSocketOptions;
import reactor.io.net.impl.netty.tcp.NettyTcpClient;
import reactor.io.net.tcp.TcpClient;
import reactor.rx.Promise;
import reactor.rx.Promises;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.action.Signal;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 基于项目Reactor的TCP客户端支持的{@link org.springframework.messaging.tcp.TcpOperations}的实现.
 *
 * <p>此实现包装为N {@link #connect}调用创建的 N Reactor {@code TcpClient}实例, i.e. 每个连接一个实例.
 */
public class Reactor2TcpClient<P> implements TcpOperations<P> {

	@SuppressWarnings("rawtypes")
	public static final Class<NettyTcpClient> REACTOR_TCP_CLIENT_TYPE = NettyTcpClient.class;

	private static final Method eventLoopGroupMethod = initEventLoopGroupMethod();


	private final EventLoopGroup eventLoopGroup;

	private final Environment environment;

	private final TcpClientFactory<Message<P>, Message<P>> tcpClientSpecFactory;

	private final List<TcpClient<Message<P>, Message<P>>> tcpClients =
			new ArrayList<TcpClient<Message<P>, Message<P>>>();

	private boolean stopping;


	/**
	 * 使用默认{@link reactor.core.dispatch.SynchronousDispatcher}
	 * 创建{@link TcpClientSpec TcpClientSpec}工厂, i.e. 依赖于Netty线程.
	 * 可以使用{@code reactor.tcp.ioThreadCount} System属性调整Netty线程的数量.
	 * 网络I/O 线程将在活动客户端之间共享.
	 *
	 * @param host 要连接的主机
	 * @param port 要连接的端口
	 * @param codec 用于编码和解码TCP流的编解码器
	 */
	public Reactor2TcpClient(final String host, final int port, final Codec<Buffer, Message<P>, Message<P>> codec) {
		this(new FixedAddressSupplier(host, port), codec);
	}

	/**
	 * {@link #Reactor2TcpClient(String, int, Codec)}的变体, 它接受任意数量地址的供应商, 而不仅仅是一个主机和端口.
	 * 当前主机不可用后, 这可以用于{@link #connect(TcpConnectionHandler, ReconnectStrategy) 重新连接}到不同的地址.
	 *
	 * @param addressSupplier 用于连接的地址供应商
	 * @param codec 用于编码和解码TCP流的编解码器
	 */
	public Reactor2TcpClient(final Supplier<InetSocketAddress> addressSupplier,
			final Codec<Buffer, Message<P>, Message<P>> codec) {

		// Reactor 2.0.5 要求 NioEventLoopGroup vs 2.0.6+ 需要 EventLoopGroup
		final NioEventLoopGroup nioEventLoopGroup = initEventLoopGroup();
		this.eventLoopGroup = nioEventLoopGroup;
		this.environment = new Environment(new SynchronousDispatcherConfigReader());

		this.tcpClientSpecFactory = new TcpClientFactory<Message<P>, Message<P>>() {
			@Override
			public TcpClientSpec<Message<P>, Message<P>> apply(TcpClientSpec<Message<P>, Message<P>> spec) {
				return spec
						.env(environment)
						.codec(codec)
						.connect(addressSupplier)
						.options(createClientSocketOptions());
			}

			private ClientSocketOptions createClientSocketOptions() {
				return (ClientSocketOptions) ReflectionUtils.invokeMethod(eventLoopGroupMethod,
						new NettyClientSocketOptions(), nioEventLoopGroup);
			}
		};
	}

	/**
	 * 使用预配置的{@link TcpClientSpec} {@link Function}工厂.
	 * 这可能用于将SSL或特定网络参数添加到生成的客户端配置.
	 *
	 * <p><strong>NOTE:</strong> 如果客户端配置了创建线程的调度器, 则您负责清除它们, e.g. 通过{@link reactor.core.Dispatcher#shutdown}.
	 *
	 * @param tcpClientSpecFactory 用于每个客户端创建的TcpClientSpec {@link Function}
	 */
	public Reactor2TcpClient(TcpClientFactory<Message<P>, Message<P>> tcpClientSpecFactory) {
		Assert.notNull(tcpClientSpecFactory, "'tcpClientClientFactory' must not be null");
		this.tcpClientSpecFactory = tcpClientSpecFactory;
		this.eventLoopGroup = null;
		this.environment = null;
	}


	public static NioEventLoopGroup initEventLoopGroup() {
		int ioThreadCount;
		try {
			ioThreadCount = Integer.parseInt(System.getProperty("reactor.tcp.ioThreadCount"));
		}
		catch (Throwable ex) {
			ioThreadCount = -1;
		}
		if (ioThreadCount <= 0) {
			ioThreadCount = Runtime.getRuntime().availableProcessors();
		}
		return new NioEventLoopGroup(ioThreadCount, new NamedDaemonThreadFactory("reactor-tcp-io"));
	}


	@Override
	public ListenableFuture<Void> connect(final TcpConnectionHandler<P> connectionHandler) {
		Assert.notNull(connectionHandler, "TcpConnectionHandler must not be null");

		final TcpClient<Message<P>, Message<P>> tcpClient;
		final Runnable cleanupTask;
		synchronized (this.tcpClients) {
			if (this.stopping) {
				IllegalStateException ex = new IllegalStateException("Shutting down.");
				connectionHandler.afterConnectFailure(ex);
				return new PassThroughPromiseToListenableFutureAdapter<Void>(Promises.<Void>error(ex));
			}
			tcpClient = NetStreams.tcpClient(REACTOR_TCP_CLIENT_TYPE, this.tcpClientSpecFactory);
			this.tcpClients.add(tcpClient);
			cleanupTask = new Runnable() {
				@Override
				public void run() {
					synchronized (tcpClients) {
						tcpClients.remove(tcpClient);
					}
				}
			};
		}

		Promise<Void> promise = tcpClient.start(
				new MessageChannelStreamHandler<P>(connectionHandler, cleanupTask));

		return new PassThroughPromiseToListenableFutureAdapter<Void>(
				promise.onError(new Consumer<Throwable>() {
					@Override
					public void accept(Throwable ex) {
						cleanupTask.run();
						connectionHandler.afterConnectFailure(ex);
					}
				})
		);
	}

	@Override
	public ListenableFuture<Void> connect(TcpConnectionHandler<P> connectionHandler, ReconnectStrategy strategy) {
		Assert.notNull(connectionHandler, "TcpConnectionHandler must not be null");
		Assert.notNull(strategy, "ReconnectStrategy must not be null");

		final TcpClient<Message<P>, Message<P>> tcpClient;
		Runnable cleanupTask;
		synchronized (this.tcpClients) {
			if (this.stopping) {
				IllegalStateException ex = new IllegalStateException("Shutting down.");
				connectionHandler.afterConnectFailure(ex);
				return new PassThroughPromiseToListenableFutureAdapter<Void>(Promises.<Void>error(ex));
			}
			tcpClient = NetStreams.tcpClient(REACTOR_TCP_CLIENT_TYPE, this.tcpClientSpecFactory);
			this.tcpClients.add(tcpClient);
			cleanupTask = new Runnable() {
				@Override
				public void run() {
					synchronized (tcpClients) {
						tcpClients.remove(tcpClient);
					}
				}
			};
		}

		Stream<Tuple2<InetSocketAddress, Integer>> stream = tcpClient.start(
				new MessageChannelStreamHandler<P>(connectionHandler, cleanupTask),
				new ReactorReconnectAdapter(strategy));

		return new PassThroughPromiseToListenableFutureAdapter<Void>(stream.next().after());
	}

	@Override
	public ListenableFuture<Void> shutdown() {
		synchronized (this.tcpClients) {
			this.stopping = true;
		}

		Promise<Void> promise = Streams.from(this.tcpClients)
				.flatMap(new Function<TcpClient<Message<P>, Message<P>>, Promise<Void>>() {
					@Override
					public Promise<Void> apply(final TcpClient<Message<P>, Message<P>> client) {
						return client.shutdown().onComplete(new Consumer<Promise<Void>>() {
							@Override
							public void accept(Promise<Void> voidPromise) {
								tcpClients.remove(client);
							}
						});
					}
				})
				.next();

		if (this.eventLoopGroup != null) {
			final Promise<Void> eventLoopPromise = Promises.prepare();
			promise.onComplete(new Consumer<Promise<Void>>() {
				@Override
				public void accept(Promise<Void> voidPromise) {
					eventLoopGroup.shutdownGracefully().addListener(new FutureListener<Object>() {
						@Override
						public void operationComplete(Future<Object> future) throws Exception {
							if (future.isSuccess()) {
								eventLoopPromise.onComplete();
							}
							else {
								eventLoopPromise.onError(future.cause());
							}
						}
					});
				}
			});
			promise = eventLoopPromise;
		}

		if (this.environment != null) {
			promise.onComplete(new Consumer<Promise<Void>>() {
				@Override
				public void accept(Promise<Void> voidPromise) {
					environment.shutdown();
				}
			});
		}

		return new PassThroughPromiseToListenableFutureAdapter<Void>(promise);
	}


	private static Method initEventLoopGroupMethod() {
		for (Method method : NettyClientSocketOptions.class.getMethods()) {
			if (method.getName().equals("eventLoopGroup") && method.getParameterTypes().length == 1) {
				return method;
			}
		}
		throw new IllegalStateException("No compatible Reactor version found");
	}


	private static class FixedAddressSupplier implements Supplier<InetSocketAddress> {

		private final InetSocketAddress address;

		FixedAddressSupplier(String host, int port) {
			this.address = new InetSocketAddress(host, port);
		}

		@Override
		public InetSocketAddress get() {
			return this.address;
		}
	}


	private static class SynchronousDispatcherConfigReader implements ConfigurationReader {

		@Override
		public ReactorConfiguration read() {
			return new ReactorConfiguration(
					Collections.<DispatcherConfiguration>emptyList(), "sync", new Properties());
		}
	}


	private static class MessageChannelStreamHandler<P>
			implements ReactorChannelHandler<Message<P>, Message<P>, ChannelStream<Message<P>, Message<P>>> {

		private final TcpConnectionHandler<P> connectionHandler;

		private final Runnable cleanupTask;

		public MessageChannelStreamHandler(TcpConnectionHandler<P> connectionHandler, Runnable cleanupTask) {
			this.connectionHandler = connectionHandler;
			this.cleanupTask = cleanupTask;
		}

		@Override
		public Publisher<Void> apply(ChannelStream<Message<P>, Message<P>> channelStream) {
			Promise<Void> closePromise = Promises.prepare();
			this.connectionHandler.afterConnected(new Reactor2TcpConnection<P>(channelStream, closePromise));
			channelStream
					.finallyDo(new Consumer<Signal<Message<P>>>() {
						@Override
						public void accept(Signal<Message<P>> signal) {
							cleanupTask.run();
							if (signal.isOnError()) {
								connectionHandler.handleFailure(signal.getThrowable());
							}
							else if (signal.isOnComplete()) {
								connectionHandler.afterConnectionClosed();
							}
						}
					})
					.consume(new Consumer<Message<P>>() {
						@Override
						public void accept(Message<P> message) {
							connectionHandler.handleMessage(message);
						}
					});

			return closePromise;
		}
	}


	private static class ReactorReconnectAdapter implements Reconnect {

		private final ReconnectStrategy strategy;

		public ReactorReconnectAdapter(ReconnectStrategy strategy) {
			this.strategy = strategy;
		}

		@Override
		public Tuple2<InetSocketAddress, Long> reconnect(InetSocketAddress address, int attempt) {
			return Tuple.of(address, this.strategy.getTimeToNextAttempt(attempt));
		}
	}

}
