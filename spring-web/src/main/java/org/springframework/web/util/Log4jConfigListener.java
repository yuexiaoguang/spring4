package org.springframework.web.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 用于在Web环境中自定义log4j初始化的Bootstrap监听器.
 * 委托给{@link Log4jWebConfigurer} (有关配置详情, 请参阅其javadoc).
 *
 * <b>WARNING: 假设扩展的WAR文件</b>, 用于加载配置文件和写入日志文件.
 * 如果要保持WAR未展开或在WAR目录中不需要特定于应用程序的日志文件, 请不要在应用程序中使用log4j设置
 * (因此, 请勿使用Log4jConfigListener或Log4jConfigServlet).
 * 相反, 使用全局的VM范围的log4j设置 (例如, 在JBoss中) 或JDK 1.4的{@code java.util.logging} (也是全局的).
 *
 * <p>使用自定义log4j初始化时, 应在{@code web.xml}中的ContextLoaderListener之前注册此监听器.
 *
 * @deprecated 从Spring 4.2.1开始, 支持 Apache Log4j 2 (遵循Apache的log4j 1.x的EOL声明)
 */
@Deprecated
public class Log4jConfigListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		Log4jWebConfigurer.initLogging(event.getServletContext());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		Log4jWebConfigurer.shutdownLogging(event.getServletContext());
	}

}
