package org.springframework.web.context.request;

import org.springframework.beans.factory.ObjectFactory;

/**
 * 会话支持的{@link org.springframework.beans.factory.config.Scope}实现.
 *
 * <p>依赖于线程绑定的{@link RequestAttributes}实例, 可以通过{@link RequestContextListener},
 * {@link org.springframework.web.filter.RequestContextFilter}
 * 或者{@link org.springframework.web.servlet.DispatcherServlet}导出.
 *
 * <p>此{@code Scope}也适用于Portlet环境, 通过备用{@code RequestAttributes}实现
 * (Spring的{@link org.springframework.web.portlet.DispatcherPortlet}开箱即用).
 */
public class SessionScope extends AbstractRequestAttributesScope {

	private final int scope;


	/**
	 * 创建一个新的SessionScope, 将属性存储在本地隔离的会话中
	 * (或默认会话, 如果全局会话和特定于组件的会话之间没有区别).
	 */
	public SessionScope() {
		this.scope = RequestAttributes.SCOPE_SESSION;
	}

	/**
	 * 创建一个新的SessionScope, 指定是否在全局会话中存储属性, 前提是这种区别可用.
	 * <p>这种区别对于Portlet环境很重要, 其中有两个会话概念: "portlet scope"和"application scope".
	 * 如果启用此标志, 则对象将被放入"application scope"会话中; 否则它们将最终进入"portlet scope"会话 (典型的默认值).
	 * <p>在Servlet环境中, 该标志实际上被忽略.
	 * 
	 * @param globalSession {@code true} 以全局会话为目标; {@code false} 如果特定于组件的会话作为目标
	 */
	public SessionScope(boolean globalSession) {
		this.scope = (globalSession ? RequestAttributes.SCOPE_GLOBAL_SESSION : RequestAttributes.SCOPE_SESSION);
	}


	@Override
	protected int getScope() {
		return this.scope;
	}

	@Override
	public String getConversationId() {
		return RequestContextHolder.currentRequestAttributes().getSessionId();
	}

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		Object mutex = RequestContextHolder.currentRequestAttributes().getSessionMutex();
		synchronized (mutex) {
			return super.get(name, objectFactory);
		}
	}

	@Override
	public Object remove(String name) {
		Object mutex = RequestContextHolder.currentRequestAttributes().getSessionMutex();
		synchronized (mutex) {
			return super.remove(name);
		}
	}

}
