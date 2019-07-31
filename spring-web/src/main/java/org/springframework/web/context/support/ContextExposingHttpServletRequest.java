package org.springframework.web.context.support;

import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * HttpServletRequest装饰器, 在访问属性后通过延迟检查,
 * 使给定WebApplicationContext中的所有Spring bean可作为请求属性访问.
 */
public class ContextExposingHttpServletRequest extends HttpServletRequestWrapper {

	private final WebApplicationContext webApplicationContext;

	private final Set<String> exposedContextBeanNames;

	private Set<String> explicitAttributes;


	/**
	 * @param originalRequest 原始HttpServletRequest
	 * @param context 此请求运行的WebApplicationContext
	 */
	public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context) {
		this(originalRequest, context, null);
	}

	/**
	 * @param originalRequest 原始HttpServletRequest
	 * @param context 此请求运行的WebApplicationContext
	 * @param exposedContextBeanNames 上下文中应该暴露的bean的名称
	 * (如果这是非null, 则只有此Set中的bean才有资格作为属性公开)
	 */
	public ContextExposingHttpServletRequest(
			HttpServletRequest originalRequest, WebApplicationContext context, Set<String> exposedContextBeanNames) {

		super(originalRequest);
		Assert.notNull(context, "WebApplicationContext must not be null");
		this.webApplicationContext = context;
		this.exposedContextBeanNames = exposedContextBeanNames;
	}


	/**
	 * 返回此请求运行的WebApplicationContext.
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}


	@Override
	public Object getAttribute(String name) {
		if ((this.explicitAttributes == null || !this.explicitAttributes.contains(name)) &&
				(this.exposedContextBeanNames == null || this.exposedContextBeanNames.contains(name)) &&
				this.webApplicationContext.containsBean(name)) {
			return this.webApplicationContext.getBean(name);
		}
		else {
			return super.getAttribute(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value) {
		super.setAttribute(name, value);
		if (this.explicitAttributes == null) {
			this.explicitAttributes = new HashSet<String>(8);
		}
		this.explicitAttributes.add(name);
	}

}
