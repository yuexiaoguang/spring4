package org.springframework.remoting.jaxws;

import java.net.InetSocketAddress;
import java.util.List;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.UsesSunHttpServer;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

/**
 * 用于JAX-WS服务的简单导出器, 自动检测带注解的服务bean (通过JAX-WS {@link javax.jws.WebService}注解),
 * 并通过Sun JDK 1.6中包含的HTTP服务器导出它们.
 * 每个服务的完整地址将包含服务器的基址和附加的服务名称 (e.g. "http://localhost:8080/OrderService").
 *
 * <p>请注意, 此导出器仅适用于Sun的JDK 1.6或更高版本, 以及适用于Sun JDK中包含的Sun整个类库的JDK.
 * 对于可移植的JAX-WS导出器, 查看{@link SimpleJaxWsServiceExporter}.
 */
@UsesSunHttpServer
public class SimpleHttpServerJaxWsServiceExporter extends AbstractJaxWsServiceExporter {

	protected final Log logger = LogFactory.getLog(getClass());

	private HttpServer server;

	private int port = 8080;

	private String hostname;

	private int backlog = -1;

	private int shutdownDelay = 0;

	private String basePath = "/";

	private List<Filter> filters;

	private Authenticator authenticator;

	private boolean localServer = false;


	/**
	 * 指定注册Web服务上下文使用的现有HTTP服务器.
	 * 这通常是由通用Spring
	 * {@link org.springframework.remoting.support.SimpleHttpServerFactoryBean}管理的服务器.
	 * <p>或者, 通过{@link #setPort "port"}, {@link #setHostname "hostname"}
	 * 和{@link #setBacklog "backlog"}属性配置本地HTTP服务器 (或依赖于默认值).
	 */
	public void setServer(HttpServer server) {
		this.server = server;
	}

	/**
	 * 指定HTTP服务器的端口. 默认8080.
	 * <p>仅适用于本地配置的HTTP服务器.
	 * 指定{@link #setServer "server"}属性时忽略.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 指定要绑定到的HTTP服务器的主机名.
	 * 默认localhost; 可以使用要绑定的特定网络地址覆盖.
	 * <p>仅适用于本地配置的HTTP服务器.
	 * 指定{@link #setServer "server"}属性时忽略.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * 指定HTTP服务器的TCP backlog.
	 * 默认-1, 表示系统的默认值.
	 * <p>仅适用于本地配置的HTTP服务器.
	 * 指定{@link #setServer "server"}属性时忽略.
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	/**
	 * 指定在关闭HTTP服务器时完成HTTP交换之前要等待的秒数. 默认 0.
	 * <p>仅适用于本地配置的HTTP服务器.
	 * 指定{@link #setServer "server"}属性时忽略.
	 */
	public void setShutdownDelay(int shutdownDelay) {
		this.shutdownDelay = shutdownDelay;
	}

	/**
	 * 设置上下文发布的基本路径. Default is "/".
	 * <p>对于每个上下文发布路径, 服务名称将附加到此基址.
	 * E.g. 服务名称"OrderService" -> "/OrderService".
	 */
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	/**
	 * 注册要应用于所有检测到的带{@link javax.jws.WebService}注解的bean的公共{@link com.sun.net.httpserver.Filter Filters}.
	 */
	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	/**
	 * 注册要应用于所有检测到的带{@link javax.jws.WebService}注解的bean的公共{@link com.sun.net.httpserver.Authenticator}.
	 */
	public void setAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.server == null) {
			InetSocketAddress address = (this.hostname != null ?
					new InetSocketAddress(this.hostname, this.port) : new InetSocketAddress(this.port));
			HttpServer server = HttpServer.create(address, this.backlog);
			if (logger.isInfoEnabled()) {
				logger.info("Starting HttpServer at address " + address);
			}
			server.start();
			this.server = server;
			this.localServer = true;
		}
		super.afterPropertiesSet();
	}

	@Override
	protected void publishEndpoint(Endpoint endpoint, WebService annotation) {
		endpoint.publish(buildHttpContext(endpoint, annotation.serviceName()));
	}

	@Override
	protected void publishEndpoint(Endpoint endpoint, WebServiceProvider annotation) {
		endpoint.publish(buildHttpContext(endpoint, annotation.serviceName()));
	}

	/**
	 * 为给定端点构建HttpContext.
	 * 
	 * @param endpoint JAX-WS Provider Endpoint对象
	 * @param serviceName 给定的服务名称
	 * 
	 * @return 完全填充的HttpContext
	 */
	protected HttpContext buildHttpContext(Endpoint endpoint, String serviceName) {
		String fullPath = calculateEndpointPath(endpoint, serviceName);
		HttpContext httpContext = this.server.createContext(fullPath);
		if (this.filters != null) {
			httpContext.getFilters().addAll(this.filters);
		}
		if (this.authenticator != null) {
			httpContext.setAuthenticator(this.authenticator);
		}
		return httpContext;
	}

	/**
	 * 计算给定端点的完整端点路径.
	 * 
	 * @param endpoint JAX-WS Provider Endpoint对象
	 * @param serviceName 给定的服务名称
	 * 
	 * @return 完整的端点路径
	 */
	protected String calculateEndpointPath(Endpoint endpoint, String serviceName) {
		return this.basePath + serviceName;
	}


	@Override
	public void destroy() {
		super.destroy();
		if (this.localServer) {
			logger.info("Stopping HttpServer");
			this.server.stop(this.shutdownDelay);
		}
	}

}
