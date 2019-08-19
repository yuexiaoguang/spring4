package org.springframework.web.servlet.config.annotation;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 协助注册单个重定向视图控制器.
 */
public class RedirectViewControllerRegistration {

	private final String urlPath;

	private final RedirectView redirectView;

	private final ParameterizableViewController controller = new ParameterizableViewController();


	public RedirectViewControllerRegistration(String urlPath, String redirectUrl) {
		Assert.notNull(urlPath, "'urlPath' is required.");
		Assert.notNull(redirectUrl, "'redirectUrl' is required.");
		this.urlPath = urlPath;
		this.redirectView = new RedirectView(redirectUrl);
		this.redirectView.setContextRelative(true);
		this.controller.setView(this.redirectView);
	}


	/**
	 * 设置要使用的特定重定向3xx状态码.
	 * <p>如果没有设置, {@link org.springframework.web.servlet.view.RedirectView}
	 * 将默认选择{@code HttpStatus.MOVED_TEMPORARILY (302)}.
	 */
	public RedirectViewControllerRegistration setStatusCode(HttpStatus statusCode) {
		Assert.isTrue(statusCode.is3xxRedirection(), "Not a redirect status code");
		this.redirectView.setStatusCode(statusCode);
		return this;
	}

	/**
	 * 是否将以斜杠("/") 开头的给定重定向URL解释为相对于当前ServletContext, i.e. 相对于Web应用程序根目录.
	 * <p>默认{@code true}.
	 */
	public RedirectViewControllerRegistration setContextRelative(boolean contextRelative) {
		this.redirectView.setContextRelative(contextRelative);
		return this;
	}

	/**
	 * 是否将当前请求的查询参数传播到目标重定向URL.
	 * <p>默认{@code false}.
	 */
	public RedirectViewControllerRegistration setKeepQueryParams(boolean propagate) {
		this.redirectView.setPropagateQueryParams(propagate);
		return this;
	}

	protected void setApplicationContext(ApplicationContext applicationContext) {
		this.controller.setApplicationContext(applicationContext);
		this.redirectView.setApplicationContext(applicationContext);
	}

	protected String getUrlPath() {
		return this.urlPath;
	}

	protected ParameterizableViewController getViewController() {
		return this.controller;
	}

}
