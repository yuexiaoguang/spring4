package org.springframework.web.socket.server.standard;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationObjectSupport;

/**
 * 检测{@link javax.websocket.server.ServerEndpointConfig}类型的bean, 并使用标准Java WebSocket运行时注册.
 * 还检测带{@link ServerEndpoint}注解的bean并注册它们.
 * 虽然不是必需的, 但带注解的端点可能应将其{@code configurator}属性设置为{@link SpringConfigurator}.
 *
 * <p>使用此类时, 通过在Spring配置中声明它, 应该可以关闭Servlet容器对WebSocket端点的扫描.
 * 这可以在{@code web.xml}中{@code <absolute-ordering>}元素的帮助下完成.
 */
public class ServerEndpointExporter extends WebApplicationObjectSupport
		implements InitializingBean, SmartInitializingSingleton {

	private List<Class<?>> annotatedEndpointClasses;

	private ServerContainer serverContainer;


	/**
	 * 显式列出应在启动时注册的带注解的端点类型.
	 * 如果要关闭一个Servlet容器的端点扫描, 这个端点通过所有第三方jar, 并依赖Spring配置, 那么就可以这样做了.
	 * 
	 * @param annotatedEndpointClasses 带{@link ServerEndpoint}注解的类型
	 */
	public void setAnnotatedEndpointClasses(Class<?>... annotatedEndpointClasses) {
		this.annotatedEndpointClasses = Arrays.asList(annotatedEndpointClasses);
	}

	/**
	 * 设置用于端点注册的JSR-356 {@link ServerContainer}.
	 * 如果没有设置, 将通过{@code ServletContext}检索容器.
	 */
	public void setServerContainer(ServerContainer serverContainer) {
		this.serverContainer = serverContainer;
	}

	/**
	 * 返回用于端点注册的JSR-356 {@link ServerContainer}.
	 */
	protected ServerContainer getServerContainer() {
		return this.serverContainer;
	}

	@Override
	protected void initServletContext(ServletContext servletContext) {
		if (this.serverContainer == null) {
			this.serverContainer =
					(ServerContainer) servletContext.getAttribute("javax.websocket.server.ServerContainer");
		}
	}

	@Override
	protected boolean isContextRequired() {
		return false;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.state(getServerContainer() != null, "javax.websocket.server.ServerContainer not available");
	}

	@Override
	public void afterSingletonsInstantiated() {
		registerEndpoints();
	}


	/**
	 * 实际注册端点. 由{@link #afterSingletonsInstantiated()}调用.
	 */
	protected void registerEndpoints() {
		Set<Class<?>> endpointClasses = new LinkedHashSet<Class<?>>();
		if (this.annotatedEndpointClasses != null) {
			endpointClasses.addAll(this.annotatedEndpointClasses);
		}

		ApplicationContext context = getApplicationContext();
		if (context != null) {
			String[] endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
			for (String beanName : endpointBeanNames) {
				endpointClasses.add(context.getType(beanName));
			}
		}

		for (Class<?> endpointClass : endpointClasses) {
			registerEndpoint(endpointClass);
		}

		if (context != null) {
			Map<String, ServerEndpointConfig> endpointConfigMap = context.getBeansOfType(ServerEndpointConfig.class);
			for (ServerEndpointConfig endpointConfig : endpointConfigMap.values()) {
				registerEndpoint(endpointConfig);
			}
		}
	}

	private void registerEndpoint(Class<?> endpointClass) {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Registering @ServerEndpoint class: " + endpointClass);
			}
			getServerContainer().addEndpoint(endpointClass);
		}
		catch (DeploymentException ex) {
			throw new IllegalStateException("Failed to register @ServerEndpoint class: " + endpointClass, ex);
		}
	}

	private void registerEndpoint(ServerEndpointConfig endpointConfig) {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Registering ServerEndpointConfig: " + endpointConfig);
			}
			getServerContainer().addEndpoint(endpointConfig);
		}
		catch (DeploymentException ex) {
			throw new IllegalStateException("Failed to register ServerEndpointConfig: " + endpointConfig, ex);
		}
	}

}
