package org.springframework.web.context;

import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;

/**
 * Web应用程序监听器, 用于清除ServletContext中剩余的一次性属性, i.e. 实现{@link DisposableBean}并且之前未删除的属性.
 * 这通常用于销毁"application"范围内的对象, 其生命周期意味着Web应用程序关闭阶段结束时的销毁.
 */
public class ContextCleanupListener implements ServletContextListener {

	private static final Log logger = LogFactory.getLog(ContextCleanupListener.class);


	@Override
	public void contextInitialized(ServletContextEvent event) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		cleanupAttributes(event.getServletContext());
	}


	/**
	 * 找到实现{@link DisposableBean}的所有ServletContext属性并销毁它们,
	 * 最终删除所有受影响的ServletContext属性.
	 * 
	 * @param sc 要检查的ServletContext
	 */
	static void cleanupAttributes(ServletContext sc) {
		Enumeration<String> attrNames = sc.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String attrName = attrNames.nextElement();
			if (attrName.startsWith("org.springframework.")) {
				Object attrValue = sc.getAttribute(attrName);
				if (attrValue instanceof DisposableBean) {
					try {
						((DisposableBean) attrValue).destroy();
					}
					catch (Throwable ex) {
						logger.error("Couldn't invoke destroy method of attribute with name '" + attrName + "'", ex);
					}
				}
			}
		}
	}
}
