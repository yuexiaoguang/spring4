package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory}实现,
 * 使用<a href="http://netty.io/">Netty 4</a>创建请求.
 *
 * <p>允许使用预先配置的{@link EventLoopGroup}实例: 对于跨多个客户端共享很有用.
 *
 * <p>请注意, 此实现始终关闭每个请求的HTTP连接.
 */
public class Netty4ClientHttpRequestFactory implements ClientHttpRequestFactory,
		AsyncClientHttpRequestFactory, InitializingBean, DisposableBean {

	/**
	 * 默认的最大响应大小.
	 */
	public static final int DEFAULT_MAX_RESPONSE_SIZE = 1024 * 1024 * 10;


	private final EventLoopGroup eventLoopGroup;

	private final boolean defaultEventLoopGroup;

	private int maxResponseSize = DEFAULT_MAX_RESPONSE_SIZE;

	private SslContext sslContext;

	private int connectTimeout = -1;

	private int readTimeout = -1;

	private volatile Bootstrap bootstrap;


	public Netty4ClientHttpRequestFactory() {
		int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
		this.eventLoopGroup = new NioEventLoopGroup(ioWorkerCount);
		this.defaultEventLoopGroup = true;
	}

	/**
	 * <p><b>NOTE:</b> 该工厂<strong>不会</strong> {@linkplain EventLoopGroup#shutdownGracefully() 关闭}组;
	 * 这样做成为调用者的责任.
	 */
	public Netty4ClientHttpRequestFactory(EventLoopGroup eventLoopGroup) {
		Assert.notNull(eventLoopGroup, "EventLoopGroup must not be null");
		this.eventLoopGroup = eventLoopGroup;
		this.defaultEventLoopGroup = false;
	}


	/**
	 * 设置默认的最大响应大小.
	 * <p>默认{@link #DEFAULT_MAX_RESPONSE_SIZE}.
	 */
	public void setMaxResponseSize(int maxResponseSize) {
		this.maxResponseSize = maxResponseSize;
	}

	/**
	 * 设置SSL上下文. 配置后, 它用于在通道管道中创建和插入{@link io.netty.handler.ssl.SslHandler}.
	 * <p>如果未提供任何客户端, 则配置默认客户端SslContext.
	 */
	public void setSslContext(SslContext sslContext) {
		this.sslContext = sslContext;
	}

	/**
	 * 设置底层连接超时 (以毫秒为单位).
	 * 值0指定无限超时.
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 设置底层URLConnection的读取超时 (以毫秒为单位).
	 * 值0指定无限超时.
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.sslContext == null) {
			this.sslContext = getDefaultClientSslContext();
		}
	}

	private SslContext getDefaultClientSslContext() {
		try {
			return SslContextBuilder.forClient().build();
		}
		catch (SSLException ex) {
			throw new IllegalStateException("Could not create default client SslContext", ex);
		}
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	private Netty4ClientHttpRequest createRequestInternal(URI uri, HttpMethod httpMethod) {
		return new Netty4ClientHttpRequest(getBootstrap(uri), uri, httpMethod);
	}

	private Bootstrap getBootstrap(URI uri) {
		boolean isSecure = (uri.getPort() == 443 || "https".equalsIgnoreCase(uri.getScheme()));
		if (isSecure) {
			return buildBootstrap(uri, true);
		}
		else {
			if (this.bootstrap == null) {
				this.bootstrap = buildBootstrap(uri, false);
			}
			return this.bootstrap;
		}
	}

	private Bootstrap buildBootstrap(final URI uri, final boolean isSecure) {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(this.eventLoopGroup).channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel channel) throws Exception {
						configureChannel(channel.config());
						ChannelPipeline pipeline = channel.pipeline();
						if (isSecure) {
							Assert.notNull(sslContext, "sslContext should not be null");
							pipeline.addLast(sslContext.newHandler(channel.alloc(), uri.getHost(), uri.getPort()));
						}
						pipeline.addLast(new HttpClientCodec());
						pipeline.addLast(new HttpObjectAggregator(maxResponseSize));
						if (readTimeout > 0) {
							pipeline.addLast(new ReadTimeoutHandler(readTimeout,
									TimeUnit.MILLISECONDS));
						}
					}
				});
		return bootstrap;
	}

	/**
	 * 用于更改给定{@link SocketChannelConfig}的属性的模板方法.
	 * <p>默认实现基于set属性设置连接超时.
	 * 
	 * @param config 通道配置
	 */
	protected void configureChannel(SocketChannelConfig config) {
		if (this.connectTimeout >= 0) {
			config.setConnectTimeoutMillis(this.connectTimeout);
		}
	}


	@Override
	public void destroy() throws InterruptedException {
		if (this.defaultEventLoopGroup) {
			// 如果在构造函数中创建它, 清理EventLoopGroup
			this.eventLoopGroup.shutdownGracefully().sync();
		}
	}

}
