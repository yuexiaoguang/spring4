package org.springframework.web.context.request;

import javax.faces.context.FacesContext;

import org.springframework.core.NamedInheritableThreadLocal;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.ClassUtils;

/**
 * Holder类以线程绑定的{@link RequestAttributes}对象的形式公开Web请求.
 * 如果{@code inheritable}标志设置为{@code true}, 则当前线程生成的任何子线程将继承该请求.
 *
 * <p>使用{@link RequestContextListener}或{@link org.springframework.web.filter.RequestContextFilter}公开当前的Web请求.
 * 请注意, {@link org.springframework.web.servlet.DispatcherServlet}
 * 和{@link org.springframework.web.portlet.DispatcherPortlet}已默认公开当前请求.
 */
public abstract class RequestContextHolder  {

	private static final boolean jsfPresent =
			ClassUtils.isPresent("javax.faces.context.FacesContext", RequestContextHolder.class.getClassLoader());

	private static final ThreadLocal<RequestAttributes> requestAttributesHolder =
			new NamedThreadLocal<RequestAttributes>("Request attributes");

	private static final ThreadLocal<RequestAttributes> inheritableRequestAttributesHolder =
			new NamedInheritableThreadLocal<RequestAttributes>("Request context");


	/**
	 * 重置当前线程的RequestAttributes.
	 */
	public static void resetRequestAttributes() {
		requestAttributesHolder.remove();
		inheritableRequestAttributesHolder.remove();
	}

	/**
	 * 将给定的RequestAttributes绑定到当前线程, <i>不</i>将其公开为子线程可继承.
	 * 
	 * @param attributes 要公开的RequestAttributes
	 */
	public static void setRequestAttributes(RequestAttributes attributes) {
		setRequestAttributes(attributes, false);
	}

	/**
	 * 将给定的RequestAttributes绑定到当前线程.
	 * 
	 * @param attributes 要公开的RequestAttributes, 或{@code null}以重置线程绑定上下文
	 * @param inheritable 是否将RequestAttributes公开为子线程可继承(使用{@link InheritableThreadLocal})
	 */
	public static void setRequestAttributes(RequestAttributes attributes, boolean inheritable) {
		if (attributes == null) {
			resetRequestAttributes();
		}
		else {
			if (inheritable) {
				inheritableRequestAttributesHolder.set(attributes);
				requestAttributesHolder.remove();
			}
			else {
				requestAttributesHolder.set(attributes);
				inheritableRequestAttributesHolder.remove();
			}
		}
	}

	/**
	 * 返回当前绑定到该线程的RequestAttributes.
	 * 
	 * @return 当前绑定到线程的 RequestAttributes, 或{@code null}
	 */
	public static RequestAttributes getRequestAttributes() {
		RequestAttributes attributes = requestAttributesHolder.get();
		if (attributes == null) {
			attributes = inheritableRequestAttributesHolder.get();
		}
		return attributes;
	}

	/**
	 * 返回当前绑定到该线程的RequestAttributes.
	 * <p>公开先前绑定的RequestAttributes实例. 如果有的话, 回退到当前的JSF FacesContext.
	 * 
	 * @return 当前绑定到该线程的RequestAttributes
	 * @throws IllegalStateException 如果没有RequestAttributes对象绑定到当前线程
	 */
	public static RequestAttributes currentRequestAttributes() throws IllegalStateException {
		RequestAttributes attributes = getRequestAttributes();
		if (attributes == null) {
			if (jsfPresent) {
				attributes = FacesRequestAttributesFactory.getFacesRequestAttributes();
			}
			if (attributes == null) {
				throw new IllegalStateException("No thread-bound request found: " +
						"Are you referring to request attributes outside of an actual web request, " +
						"or processing a request outside of the originally receiving thread? " +
						"If you are actually operating within a web request and still receive this message, " +
						"your code is probably running outside of DispatcherServlet/DispatcherPortlet: " +
						"In this case, use RequestContextListener or RequestContextFilter to expose the current request.");
			}
		}
		return attributes;
	}


	/**
	 * 内部类, 以避免硬编码的JSF依赖.
 	 */
	private static class FacesRequestAttributesFactory {

		public static RequestAttributes getFacesRequestAttributes() {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			return (facesContext != null ? new FacesRequestAttributes(facesContext) : null);
		}
	}

}
