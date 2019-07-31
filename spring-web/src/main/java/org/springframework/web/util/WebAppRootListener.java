package org.springframework.web.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 监听器, 它将系统属性设置为Web应用程序根目录.
 * 可以使用servlet上下文级别的"webAppRootKey" init参数 (i.e. web.xml中的context-param)
 * 定义系统属性的键, 默认的键为"webapp.root".
 *
 * <p>可用于支持系统属性替换的工具包 (i.e. System.getProperty值), 如日志文件位置中的log4j的 "${key}"语法.
 *
 * <p>Note: 这个监听器应放在{@code web.xml}中的ContextLoaderListener之前, 至少在用于log4j时.
 * Log4jConfigListener隐式设置系统属性, 因此除了它之外不需要这个监听器.
 *
 * <p><b>WARNING</b>: 一些容器, e.g. Tomcat, 不会将每个Web应用程序的系统属性分开.
 * 必须为每个Web应用程序使用唯一的"webAppRootKey" context-params, 以避免冲突.
 * 像Resin这样的其他容器会隔离每个Web应用程序的系统属性:
 * 在这里可以使用默认键 (i.e. 根本没有"webAppRootKey"上下文参数), 而无需担心.
 *
 * <p><b>WARNING</b>: 需要扩展包含Web应用程序的WAR文件, 以允许设置Web应用程序根系统属性.
 * 例如, 将WAR文件部署到WebLogic时, 默认不是这样. 不要在这样的环境中使用此监听器!
 */
public class WebAppRootListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		WebUtils.setWebAppRootSystemProperty(event.getServletContext());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		WebUtils.removeWebAppRootSystemProperty(event.getServletContext());
	}

}
