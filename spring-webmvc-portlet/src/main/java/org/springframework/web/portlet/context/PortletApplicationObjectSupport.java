package org.springframework.web.portlet.context;

import java.io.File;
import javax.portlet.PortletContext;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * 在Portlet ApplicationContext中运行的应用程序对象的便捷超类.
 * 提供getApplicationContext, getServletContext, 和getTempDir方法.
 */
public abstract class PortletApplicationObjectSupport extends ApplicationObjectSupport
		implements PortletContextAware {

	private PortletContext portletContext;


	@Override
	public void setPortletContext(PortletContext portletContext) {
		this.portletContext = portletContext;
	}


	/**
	 * 覆盖基类行为以强制在ApplicationContext中运行.
	 * 如果未在上下文中运行, 则所有访问器都将抛出IllegalStateException.
	 */
	@Override
	protected boolean isContextRequired() {
		return true;
	}

	/**
	 * 返回当前的PortletContext.
	 * 
	 * @throws IllegalStateException 如果没有在PortletContext中运行
	 */
	protected final PortletContext getPortletContext() throws IllegalStateException {
		if (this.portletContext == null) {
			throw new IllegalStateException(
					"PortletApplicationObjectSupport instance [" + this + "] does not run within a PortletContext");
		}
		return this.portletContext;
	}

	/**
	 * 返回当前Web应用程序的临时目录, 由servlet容器提供.
	 * 
	 * @return 表示临时目录的文件
	 * @throws IllegalStateException 如果没有在PortletContext中运行
	 */
	protected final File getTempDir() throws IllegalStateException {
		return PortletUtils.getTempDir(getPortletContext());
	}

}
