package org.springframework.web.socket.client;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WebSocket连接管理器的基类.
 * 给定要连接的URI, 提供连接到WebSocket服务器的声明式样式.
 * 如果{@link #autoStartup}属性设置为{@code true}, 刷新Spring ApplicationContext时发生连接,
 * 或者设置为{@code false}, 手动调用{@link #start()} 和 {@link #stop()}方法.
 */
public abstract class ConnectionManagerSupport implements SmartLifecycle {

	protected final Log logger = LogFactory.getLog(getClass());

	private final URI uri;

	private boolean autoStartup = false;

	private int phase = Integer.MAX_VALUE;

	private volatile boolean running = false;

	private final Object lifecycleMonitor = new Object();


	public ConnectionManagerSupport(String uriTemplate, Object... uriVariables) {
		this.uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(
				uriVariables).encode().toUri();
	}


	protected URI getUri() {
		return this.uri;
	}

	/**
	 * 设置在初始化此连接管理器并刷新Spring上下文后, 是否自动连接到远程端点.
	 * <p>默认为"false".
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * 返回'autoStartup'属性的值.
	 * 如果为"true", 则此端点连接管理器将在ContextRefreshedEvent上连接到远程端点.
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定应建立与远程端点的连接并随后关闭的阶段.
	 * 启动顺序从最低到最高, 关闭顺序与此相反.
	 * 默认为Integer.MAX_VALUE, 表示此端点连接工厂尽可能晚地连接并尽快关闭.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回此端点连接工厂将自动连接并停止的阶段.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}


	/**
	 * 启动WebSocket连接. 如果已连接, 则该方法无影响.
	 */
	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				startInternal();
			}
		}
	}

	protected void startInternal() {
		synchronized (this.lifecycleMonitor) {
			if (logger.isInfoEnabled()) {
				logger.info("Starting " + getClass().getSimpleName());
			}
			this.running = true;
			openConnection();
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				if (logger.isInfoEnabled()) {
					logger.info("Stopping " + getClass().getSimpleName());
				}
				try {
					stopInternal();
				}
				catch (Throwable ex) {
					logger.error("Failed to stop WebSocket connection", ex);
				}
				finally {
					this.running = false;
				}
			}
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	protected void stopInternal() throws Exception {
		if (isConnected()) {
			closeConnection();
		}
	}

	/**
	 * 返回此ConnectionManager是否已启动.
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}


	protected abstract void openConnection();

	protected abstract void closeConnection() throws Exception;

	protected abstract boolean isConnected();

}
