package org.springframework.web.servlet.mvc.multiaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.util.WebUtils;

/**
 * {@link MethodNameResolver}的简单实现, 将URL映射到方法名称.
 * 虽然这是{@link MultiActionController}类使用的默认实现 (因为它不需要配置), 但对于大多数应用程序来说它有点天真.
 * 特别是, 通常不希望将URL与实现方法联系起来.
 *
 * <p>Maps the resource name after the last slash, ignoring an extension.
 * E.g. "/foo/bar/baz.html" to "baz", assuming a "/foo/bar/baz.html"
 * controller mapping to the corresponding MultiActionController handler.
 * method. Doesn't support wildcards.
 *
 * @deprecated 从4.3开始, 使用注解驱动的处理器方法
 */
@Deprecated
public class InternalPathMethodNameResolver extends AbstractUrlMethodNameResolver {

	private String prefix = "";

	private String suffix = "";

	/** Request URL path String --> method name String */
	private final Map<String, String> methodNameCache = new ConcurrentHashMap<String, String>(16);


	/**
	 * Specify a common prefix for handler method names.
	 * Will be prepended to the internal path found in the URL:
	 * e.g. internal path "baz", prefix "my" -> method name "mybaz".
	 */
	public void setPrefix(String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * Return the common prefix for handler method names.
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * Specify a common suffix for handler method names.
	 * Will be appended to the internal path found in the URL:
	 * e.g. internal path "baz", suffix "Handler" -> method name "bazHandler".
	 */
	public void setSuffix(String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * Return the common suffix for handler method names.
	 */
	protected String getSuffix() {
		return this.suffix;
	}


	/**
	 * Extracts the method name indicated by the URL path.
	 * @see #extractHandlerMethodNameFromUrlPath
	 * @see #postProcessHandlerMethodName
	 */
	@Override
	protected String getHandlerMethodNameForUrlPath(String urlPath) {
		String methodName = this.methodNameCache.get(urlPath);
		if (methodName == null) {
			methodName = extractHandlerMethodNameFromUrlPath(urlPath);
			methodName = postProcessHandlerMethodName(methodName);
			this.methodNameCache.put(urlPath, methodName);
		}
		return methodName;
	}

	/**
	 * Extract the handler method name from the given request URI.
	 * Delegates to {@code WebUtils.extractFilenameFromUrlPath(String)}.
	 * @param uri the request URI (e.g. "/index.html")
	 * @return the extracted URI filename (e.g. "index")
	 * @see org.springframework.web.util.WebUtils#extractFilenameFromUrlPath
	 */
	protected String extractHandlerMethodNameFromUrlPath(String uri) {
		return WebUtils.extractFilenameFromUrlPath(uri);
	}

	/**
	 * Build the full handler method name based on the given method name
	 * as indicated by the URL path.
	 * <p>The default implementation simply applies prefix and suffix.
	 * This can be overridden, for example, to manipulate upper case
	 * / lower case, etc.
	 * @param methodName the original method name, as indicated by the URL path
	 * @return the full method name to use
	 * @see #getPrefix()
	 * @see #getSuffix()
	 */
	protected String postProcessHandlerMethodName(String methodName) {
		return getPrefix() + methodName + getSuffix();
	}

}
