package org.springframework.web.servlet.mvc.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

/**
 * Implementation of {@link org.springframework.web.servlet.HandlerMapping} that
 * follows a simple convention for generating URL path mappings from the <i>bean names</i>
 * of registered {@link org.springframework.web.servlet.mvc.Controller} beans
 * as well as {@code @Controller} annotated beans.
 *
 * <p>This is similar to {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
 * but doesn't expect bean names to follow the URL convention: It turns plain bean names
 * into URLs by prepending a slash and optionally applying a specified prefix and/or suffix.
 * However, it only does so for well-known {@link #isControllerType controller types},
 * as listed above (analogous to {@link ControllerClassNameHandlerMapping}).
 *
 * @deprecated as of 4.3, in favor of annotation-driven handler methods
 */
@Deprecated
public class ControllerBeanNameHandlerMapping extends AbstractControllerUrlHandlerMapping {

	private String urlPrefix = "";

	private String urlSuffix = "";


	/**
	 * Set an optional prefix to prepend to generated URL mappings.
	 * <p>By default this is an empty String. If you want a prefix like
	 * "/myapp/", you can set it for all beans mapped by this mapping.
	 */
	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = (urlPrefix != null ? urlPrefix : "");
	}

	/**
	 * Set an optional suffix to append to generated URL mappings.
	 * <p>By default this is an empty String. If you want a suffix like
	 * ".do", you can set it for all beans mapped by this mapping.
	 */
	public void setUrlSuffix(String urlSuffix) {
		this.urlSuffix = (urlSuffix != null ? urlSuffix : "");
	}


	@Override
	protected String[] buildUrlsForHandler(String beanName, Class<?> beanClass) {
		List<String> urls = new ArrayList<String>();
		urls.add(generatePathMapping(beanName));
		String[] aliases = getApplicationContext().getAliases(beanName);
		for (String alias : aliases) {
			urls.add(generatePathMapping(alias));
		}
		return StringUtils.toStringArray(urls);
	}

	/**
	 * Prepends a '/' if required and appends the URL suffix to the name.
	 */
	protected String generatePathMapping(String beanName) {
		String name = (beanName.startsWith("/") ? beanName : "/" + beanName);
		StringBuilder path = new StringBuilder();
		if (!name.startsWith(this.urlPrefix)) {
			path.append(this.urlPrefix);
		}
		path.append(name);
		if (!name.endsWith(this.urlSuffix)) {
			path.append(this.urlSuffix);
		}
		return path.toString();
	}

}
