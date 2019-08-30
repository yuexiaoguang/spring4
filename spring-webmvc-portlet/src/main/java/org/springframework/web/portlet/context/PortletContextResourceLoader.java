package org.springframework.web.portlet.context;

import javax.portlet.PortletContext;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * ResourceLoader实现, 将路径解析为PortletContext资源,
 * 以便在Portlet ApplicationContext外部使用 (例如, 在GenericPortletBean子类中).
 *
 * <p>在WebApplicationContext中, 资源路径由上下文实现自动解析为PortletContext资源.
 */
public class PortletContextResourceLoader extends DefaultResourceLoader {

	private final PortletContext portletContext;


	/**
	 * @param portletContext 用于加载资源的PortletContext
	 */
	public PortletContextResourceLoader(PortletContext portletContext) {
		this.portletContext = portletContext;
	}

	/**
	 * 此实现支持Web应用程序根目录下的文件路径.
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		return new PortletContextResource(this.portletContext, path);
	}

}
