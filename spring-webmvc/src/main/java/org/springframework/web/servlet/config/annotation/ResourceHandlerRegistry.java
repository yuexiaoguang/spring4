package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UrlPathHelper;

/**
 * 存储资源处理器的注册, 以通过Spring MVC提供静态资源(如图像, css文件等), 包括设置缓存header以优化Web浏览器的高效加载.
 * 可以在Web应用程序根目录下的位置, 从类路径和其他位置提供资源.
 *
 * <p>要创建资源处理器, 使用{@link #addResourceHandler(String...)},
 * 提供应调用处理器以提供静态资源的URL路径模式 (e.g. {@code "/resources/**"}).
 *
 * <p>然后在返回的{@link ResourceHandlerRegistration}上使用其他方法添加一个或多个位置,
 * 以从中提供静态内容 (e.g. {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}})
 * 或指定服务资源的缓存周期.
 */
public class ResourceHandlerRegistry {

	private final ServletContext servletContext;

	private final ApplicationContext applicationContext;

	private final ContentNegotiationManager contentNegotiationManager;

	private final UrlPathHelper pathHelper;

	private final List<ResourceHandlerRegistration> registrations = new ArrayList<ResourceHandlerRegistration>();

	private int order = Ordered.LOWEST_PRECEDENCE - 1;


	/**
	 * @param applicationContext Spring应用程序上下文
	 * @param servletContext 相应的Servlet上下文
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext) {
		this(applicationContext, servletContext, null);
	}

	/**
	 * @param applicationContext Spring应用程序上下文
	 * @param servletContext 相应的Servlet上下文
	 * @param contentNegotiationManager 要使用的内容协商管理器
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext,
			ContentNegotiationManager contentNegotiationManager) {

		this(applicationContext, servletContext, contentNegotiationManager, null);
	}

	/**
	 * {@link #ResourceHandlerRegistry(ApplicationContext, ServletContext, ContentNegotiationManager)}的变体,
	 * 它还接受用于将请求映射到静态资源的{@link UrlPathHelper}.
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext,
			ContentNegotiationManager contentNegotiationManager, UrlPathHelper pathHelper) {

		Assert.notNull(applicationContext, "ApplicationContext is required");
		this.applicationContext = applicationContext;
		this.servletContext = servletContext;
		this.contentNegotiationManager = contentNegotiationManager;
		this.pathHelper = pathHelper;
	}


	/**
	 * 根据指定的URL路径模式添加用于提供静态资源的资源处理器.
	 * 将为每个与指定路径模式之一匹配的传入请求调用该处理器.
	 * <p>允许使用{@code "/static/**"} 或 {@code "/css/{filename:\\w+\\.css}"}等模式.
	 * 有关语法的更多详细信息, 请参阅{@link org.springframework.util.AntPathMatcher}.
	 * 
	 * @return 用于进一步配置注册的资源处理器的{@link ResourceHandlerRegistration}
	 */
	public ResourceHandlerRegistration addResourceHandler(String... pathPatterns) {
		ResourceHandlerRegistration registration = new ResourceHandlerRegistration(pathPatterns);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 是否已为给定路径模式注册了资源处理器.
	 */
	public boolean hasMappingForPattern(String pathPattern) {
		for (ResourceHandlerRegistration registration : this.registrations) {
			if (Arrays.asList(registration.getPathPatterns()).contains(pathPattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定相对于在Spring MVC应用程序上下文中配置的其他{@link HandlerMapping}进行资源处理的顺序.
	 * <p>默认为{@code Integer.MAX_VALUE-1}.
	 */
	public ResourceHandlerRegistry setOrder(int order) {
		this.order = order;
		return this;
	}

	/**
	 * 返回映射的资源处理器的处理器映射; 如果没有注册, 则为{@code null}.
	 */
	protected AbstractHandlerMapping getHandlerMapping() {
		if (this.registrations.isEmpty()) {
			return null;
		}

		Map<String, HttpRequestHandler> urlMap = new LinkedHashMap<String, HttpRequestHandler>();
		for (ResourceHandlerRegistration registration : this.registrations) {
			for (String pathPattern : registration.getPathPatterns()) {
				ResourceHttpRequestHandler handler = registration.getRequestHandler();
				if (this.pathHelper != null) {
					handler.setUrlPathHelper(this.pathHelper);
				}
				if (this.contentNegotiationManager != null) {
					handler.setContentNegotiationManager(this.contentNegotiationManager);
				}
				handler.setServletContext(this.servletContext);
				handler.setApplicationContext(this.applicationContext);
				try {
					handler.afterPropertiesSet();
				}
				catch (Throwable ex) {
					throw new BeanInitializationException("Failed to init ResourceHttpRequestHandler", ex);
				}
				urlMap.put(pathPattern, handler);
			}
		}

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(order);
		handlerMapping.setUrlMap(urlMap);
		return handlerMapping;
	}

}
