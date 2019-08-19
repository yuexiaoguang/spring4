package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContextException;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.web.servlet.HandlerMapping}接口的抽象实现,
 * 通过内省应用程序上下文中所有定义的bean来检测处理器bean的URL映射.
 */
public abstract class AbstractDetectingUrlHandlerMapping extends AbstractUrlHandlerMapping {

	private boolean detectHandlersInAncestorContexts = false;


	/**
	 * 设置是否在祖先ApplicationContexts中检测处理器bean.
	 * <p>默认为"false": 将仅检测当前ApplicationContext中的处理器bean,
	 * i.e. 仅在定义此HandlerMapping本身的上下文中 (通常是当前的DispatcherServlet的上下文).
	 * <p>切换此标志以检测祖先上下文中的处理器bean (通常是Spring根WebApplicationContext).
	 */
	public void setDetectHandlersInAncestorContexts(boolean detectHandlersInAncestorContexts) {
		this.detectHandlersInAncestorContexts = detectHandlersInAncestorContexts;
	}


	/**
	 * 除了超类的初始化之外, 还调用{@link #detectHandlers()}方法.
	 */
	@Override
	public void initApplicationContext() throws ApplicationContextException {
		super.initApplicationContext();
		detectHandlers();
	}

	/**
	 * 注册当前ApplicationContext中找到的所有处理器.
	 * <p>处理器的实际URL确定取决于具体的{@link #determineUrlsForHandler(String)}实现.
	 * 没有确定的URL的bean根本不被认为是处理程序.
	 * 
	 * @throws org.springframework.beans.BeansException 如果处理器无法注册
	 */
	protected void detectHandlers() throws BeansException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for URL mappings in application context: " + getApplicationContext());
		}
		String[] beanNames = (this.detectHandlersInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(getApplicationContext(), Object.class) :
				getApplicationContext().getBeanNamesForType(Object.class));

		// 获取可以确定URL的任何bean名称.
		for (String beanName : beanNames) {
			String[] urls = determineUrlsForHandler(beanName);
			if (!ObjectUtils.isEmpty(urls)) {
				// 找到的URL路径: 将其视为处理器.
				registerHandler(urls, beanName);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Rejected bean name '" + beanName + "': no URL paths identified");
				}
			}
		}
	}


	/**
	 * 确定给定处理器bean的URL.
	 * 
	 * @param beanName 候选bean的名称
	 * 
	 * @return bean确定的URL, 或{@code null}或空数组
	 */
	protected abstract String[] determineUrlsForHandler(String beanName);

}
