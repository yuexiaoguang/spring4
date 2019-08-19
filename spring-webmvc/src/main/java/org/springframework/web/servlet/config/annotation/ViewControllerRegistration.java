package org.springframework.web.servlet.config.annotation;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * 协助注册单个视图控制器.
 */
public class ViewControllerRegistration {

	private final String urlPath;

	private final ParameterizableViewController controller = new ParameterizableViewController();


	public ViewControllerRegistration(String urlPath) {
		Assert.notNull(urlPath, "'urlPath' is required.");
		this.urlPath = urlPath;
	}


	/**
	 * 设置响应状态码. 可选.
	 *
	 * <p>如果未设置, 响应状态将为200 (OK).
	 */
	public ViewControllerRegistration setStatusCode(HttpStatus statusCode) {
		this.controller.setStatusCode(statusCode);
		return this;
	}

	/**
	 * 设置要返回的视图名称. 可选.
	 *
	 * <p>如果未指定, 视图控制器将返回{@code null}作为视图名称,
	 * 在这种情况下, 配置的{@link RequestToViewNameTranslator}将选择视图名称.
	 * {@code DefaultRequestToViewNameTranslator}例如将"/foo/bar"转换为"foo/bar".
	 */
	public void setViewName(String viewName) {
		this.controller.setViewName(viewName);
	}

	protected void setApplicationContext(ApplicationContext applicationContext) {
		this.controller.setApplicationContext(applicationContext);
	}

	protected String getUrlPath() {
		return this.urlPath;
	}

	protected ParameterizableViewController getViewController() {
		return this.controller;
	}

}
