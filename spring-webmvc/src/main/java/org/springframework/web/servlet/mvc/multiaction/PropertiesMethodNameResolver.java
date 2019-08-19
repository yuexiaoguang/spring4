package org.springframework.web.servlet.mvc.multiaction;

import java.util.Enumeration;
import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;

/**
 * {@link MethodNameResolver}接口最灵活的开箱即用实现.
 * 使用{@code java.util.Properties}来定义传入请求的URL与相应的方法名称之间的映射.
 * 这些属性可以保存在XML文档中.
 *
 * <p>Properties format is
 * {@code
 * /welcome.html=displayGenresPage
 * }
 * Note that method overloading isn't allowed, so there's no need to
 * specify arguments.
 *
 * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
 * and a various Ant-style pattern matches, e.g. a registered "/t*" matches
 * both "/test" and "/team". For details, see the AntPathMatcher javadoc.
 *
 * @deprecated 从4.3开始, 使用注解驱动的处理器方法
 */
@Deprecated
public class PropertiesMethodNameResolver extends AbstractUrlMethodNameResolver
		implements InitializingBean {

	private Properties mappings;

	private PathMatcher pathMatcher = new AntPathMatcher();


	/**
	 * Set explicit URL to method name mappings through a Properties object.
	 * @param mappings Properties with URL as key and method name as value
	 */
	public void setMappings(Properties mappings) {
		this.mappings = mappings;
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns. Default is AntPathMatcher.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.mappings == null || this.mappings.isEmpty()) {
			throw new IllegalArgumentException("'mappings' property is required");
		}
	}


	@Override
	protected String getHandlerMethodNameForUrlPath(String urlPath) {
		String methodName = this.mappings.getProperty(urlPath);
		if (methodName != null) {
			return methodName;
		}
		Enumeration<?> propNames = this.mappings.propertyNames();
		while (propNames.hasMoreElements()) {
			String registeredPath = (String) propNames.nextElement();
			if (this.pathMatcher.match(registeredPath, urlPath)) {
				return (String) this.mappings.get(registeredPath);
			}
		}
		return null;
	}

}
