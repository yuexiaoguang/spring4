package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link WebSocketHandlerRegistry}, 它将{@link WebSocketHandler}映射到用于Servlet容器的URL.
 */
public class ServletWebSocketHandlerRegistry implements WebSocketHandlerRegistry {

	private final List<ServletWebSocketHandlerRegistration> registrations =
			new ArrayList<ServletWebSocketHandlerRegistration>();

	private TaskScheduler sockJsTaskScheduler;

	private int order = 1;

	private UrlPathHelper urlPathHelper;


	public ServletWebSocketHandlerRegistry(ThreadPoolTaskScheduler sockJsTaskScheduler) {
		this.sockJsTaskScheduler = sockJsTaskScheduler;
	}

	@Override
	public WebSocketHandlerRegistration addHandler(WebSocketHandler webSocketHandler, String... paths) {
		ServletWebSocketHandlerRegistration registration =
				new ServletWebSocketHandlerRegistration(this.sockJsTaskScheduler);
		registration.addHandler(webSocketHandler, paths);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 设置生成的{@link SimpleUrlHandlerMapping}的顺序, 相对于Spring MVC中配置的其他处理器映射.
	 * <p>默认为 1.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}

	/**
	 * 设置在用于映射握手请求的{@code SimpleUrlHandlerMapping}上配置的UrlPathHelper.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * 返回映射的{@link HttpRequestHandler}的{@link HandlerMapping}.
	 */
	public AbstractHandlerMapping getHandlerMapping() {
		Map<String, Object> urlMap = new LinkedHashMap<String, Object>();
		for (ServletWebSocketHandlerRegistration registration : this.registrations) {
			MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
			for (HttpRequestHandler httpHandler : mappings.keySet()) {
				for (String pattern : mappings.get(httpHandler)) {
					urlMap.put(pattern, httpHandler);
				}
			}
		}
		WebSocketHandlerMapping hm = new WebSocketHandlerMapping();
		hm.setUrlMap(urlMap);
		hm.setOrder(this.order);
		if (this.urlPathHelper != null) {
			hm.setUrlPathHelper(this.urlPathHelper);
		}
		return hm;
	}

}
