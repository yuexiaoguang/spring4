package org.springframework.remoting.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.UsesSunHttpServer;

/**
 * {@link org.springframework.beans.factory.FactoryBean}创建一个简单的HTTP服务器, 基于Sun的JRE 1.6中包含的HTTP服务器.
 * 在初始化时启动HTTP服务器并在销毁时停止它.
 * 公开生成的{@link com.sun.net.httpserver.HttpServer}对象.
 *
 * <p>允许为特定的{@link #setContexts 上下文路径}注册{@link com.sun.net.httpserver.HttpHandler HttpHandlers}.
 * 或者, 在{@link com.sun.net.httpserver.HttpServer}本身上以编程方式注册这些特定于上下文的处理器.
 */
@UsesSunHttpServer
public class SimpleHttpServerFactoryBean implements FactoryBean<HttpServer>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private int port = 8080;

	private String hostname;

	private int backlog = -1;

	private int shutdownDelay = 0;

	private Executor executor;

	private Map<String, HttpHandler> contexts;

	private List<Filter> filters;

	private Authenticator authenticator;

	private HttpServer server;


	/**
	 * 指定HTTP服务器的端口. 默认 8080.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 指定要绑定到的HTTP服务器的主机名. 默认 localhost;
	 * 可以使用要绑定的特定网络地址覆盖.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * 指定HTTP服务器的TCP待办事项.
	 * 默认 -1, 表示系统的默认值.
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	/**
	 * 指定在关闭HTTP服务器时完成HTTP交换之前要等待的秒数. 默认 0.
	 */
	public void setShutdownDelay(int shutdownDelay) {
		this.shutdownDelay = shutdownDelay;
	}

	/**
	 * 设置JDK并发执行器以用于分派传入请求.
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * 注册{@link com.sun.net.httpserver.HttpHandler HttpHandlers}以获取特定的上下文路径.
	 * 
	 * @param contexts 将上下文路径作为键, 将HttpHandler对象作为值的Map
	 */
	public void setContexts(Map<String, HttpHandler> contexts) {
		this.contexts = contexts;
	}

	/**
	 * 注册常用的{@link com.sun.net.httpserver.Filter Filters}以应用于所有本地注册的{@link #setContexts contexts}.
	 */
	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	/**
	 * 注册常用的{@link com.sun.net.httpserver.Authenticator}以应用于所有本地注册的{@link #setContexts contexts}.
	 */
	public void setAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}


	@Override
	public void afterPropertiesSet() throws IOException {
		InetSocketAddress address = (this.hostname != null ?
				new InetSocketAddress(this.hostname, this.port) : new InetSocketAddress(this.port));
		this.server = HttpServer.create(address, this.backlog);
		if (this.executor != null) {
			this.server.setExecutor(this.executor);
		}
		if (this.contexts != null) {
			for (String key : this.contexts.keySet()) {
				HttpContext httpContext = this.server.createContext(key, this.contexts.get(key));
				if (this.filters != null) {
					httpContext.getFilters().addAll(this.filters);
				}
				if (this.authenticator != null) {
					httpContext.setAuthenticator(this.authenticator);
				}
			}
		}
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Starting HttpServer at address " + address);
		}
		this.server.start();
	}

	@Override
	public HttpServer getObject() {
		return this.server;
	}

	@Override
	public Class<? extends HttpServer> getObjectType() {
		return (this.server != null ? this.server.getClass() : HttpServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() {
		logger.info("Stopping HttpServer");
		this.server.stop(this.shutdownDelay);
	}

}
