package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/**
 * 协助注册预先配置了状态码和/或视图的简单自动控制器.
 */
public class ViewControllerRegistry {

	private ApplicationContext applicationContext;

	private final List<ViewControllerRegistration> registrations = new ArrayList<ViewControllerRegistration>(4);

	private final List<RedirectViewControllerRegistration> redirectRegistrations =
			new ArrayList<RedirectViewControllerRegistration>(10);

	private int order = 1;


	public ViewControllerRegistry(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Deprecated
	public ViewControllerRegistry() {
	}


	/**
	 * 将视图控制器映射到给定的URL路径 (或模式), 以便使用预先配置的状态码和视图呈现响应.
	 * <p>允许使用{@code "/admin/**"} 或 {@code "/articles/{articlename:\\w+}"}等模式.
	 * 有关语法的更多详细信息, 请参阅{@link org.springframework.util.AntPathMatcher}.
	 */
	public ViewControllerRegistration addViewController(String urlPath) {
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
		registration.setApplicationContext(this.applicationContext);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 将视图控制器映射到给定的URL路径 (或模式), 以便重定向到另一个URL.
	 * 默认情况下, 重定向URL应该相对于当前的ServletContext, i.e. 相对于Web应用程序根目录.
	 */
	public RedirectViewControllerRegistration addRedirectViewController(String urlPath, String redirectUrl) {
		RedirectViewControllerRegistration registration = new RedirectViewControllerRegistration(urlPath, redirectUrl);
		registration.setApplicationContext(this.applicationContext);
		this.redirectRegistrations.add(registration);
		return registration;
	}

	/**
	 * 将一个简单的控制器映射到给定的URL路径 (或模式), 以便将响应状态设置为给定的代码, 而不渲染正文.
	 */
	public void addStatusController(String urlPath, HttpStatus statusCode) {
		ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
		registration.setApplicationContext(this.applicationContext);
		registration.setStatusCode(statusCode);
		registration.getViewController().setStatusOnly(true);
		this.registrations.add(registration);
	}

	/**
	 * 指定用于映射视图控制器的{@code HandlerMapping}的顺序, 相对于Spring MVC中配置的其他处理器映射.
	 * <p>默认为1, i.e. 在带注解的按0排序的控制器之后.
	 */
	public void setOrder(int order) {
		this.order = order;
	}


	/**
	 * 返回包含已注册的视图控制器映射的{@code HandlerMapping}, 或者{@code null}表示没有注册.
	 */
	protected SimpleUrlHandlerMapping buildHandlerMapping() {
		if (this.registrations.isEmpty() && this.redirectRegistrations.isEmpty()) {
			return null;
		}

		Map<String, Object> urlMap = new LinkedHashMap<String, Object>();
		for (ViewControllerRegistration registration : this.registrations) {
			urlMap.put(registration.getUrlPath(), registration.getViewController());
		}
		for (RedirectViewControllerRegistration registration : this.redirectRegistrations) {
			urlMap.put(registration.getUrlPath(), registration.getViewController());
		}

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setUrlMap(urlMap);
		handlerMapping.setOrder(this.order);
		return handlerMapping;
	}

	/**
	 * @deprecated as of 4.3.12, in favor of {@link #buildHandlerMapping()}
	 */
	@Deprecated
	protected AbstractHandlerMapping getHandlerMapping() {
		return buildHandlerMapping();
	}

	@Deprecated
	protected void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

}
