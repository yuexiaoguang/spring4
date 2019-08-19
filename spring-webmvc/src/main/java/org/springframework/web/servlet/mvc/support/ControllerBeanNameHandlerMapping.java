package org.springframework.web.servlet.mvc.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.web.servlet.HandlerMapping}的实现遵循一个简单的约定,
 * 用于从已注册的{@link org.springframework.web.servlet.mvc.Controller} bean
 * 以及带{@code @Controller}注解的bean的 <i>bean名称</i>生成URL路径映射.
 *
 * <p>This is similar to {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
 * but doesn't expect bean names to follow the URL convention: It turns plain bean names
 * into URLs by prepending a slash and optionally applying a specified prefix and/or suffix.
 * However, it only does so for well-known {@link #isControllerType controller types},
 * as listed above (analogous to {@link ControllerClassNameHandlerMapping}).
 *
 * @deprecated 从4.3开始, 使用注解驱动的处理器方法
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
