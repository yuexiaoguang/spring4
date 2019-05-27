package org.springframework.web.servlet.config.annotation;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Assist with the registration of a single redirect view controller.
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
	 * Set the specific redirect 3xx status code to use.
	 * <p>If not set, {@link org.springframework.web.servlet.view.RedirectView}
	 * will select {@code HttpStatus.MOVED_TEMPORARILY (302)} by default.
	 */
	public RedirectViewControllerRegistration setStatusCode(HttpStatus statusCode) {
		Assert.isTrue(statusCode.is3xxRedirection(), "Not a redirect status code");
		this.redirectView.setStatusCode(statusCode);
		return this;
	}

	/**
	 * Whether to interpret a given redirect URL that starts with a slash ("/")
	 * as relative to the current ServletContext, i.e. as relative to the web
	 * application root.
	 * <p>Default is {@code true}.
	 */
	public RedirectViewControllerRegistration setContextRelative(boolean contextRelative) {
		this.redirectView.setContextRelative(contextRelative);
		return this;
	}

	/**
	 * Whether to propagate the query parameters of the current request through
	 * to the target redirect URL.
	 * <p>Default is {@code false}.
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
