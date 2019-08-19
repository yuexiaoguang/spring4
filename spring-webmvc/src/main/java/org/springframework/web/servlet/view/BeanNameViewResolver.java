package org.springframework.web.servlet.view;

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link org.springframework.web.servlet.ViewResolver}的简单实现,
 * 它将视图名称解释为当前应用程序上下文中的bean名称, i.e. 通常在执行{@code DispatcherServlet}的XML文件中.
 *
 * <p>这个解析器对于小型应用程序很方便, 可以将从控制器到视图的所有定义保持在同一位置.
 * 对于较大的应用程序, {@link XmlViewResolver}将是更好的选择, 因为它将XML视图bean定义分离到专用的视图文件.
 *
 * <p>Note: 这个{@code ViewResolver}和{@link XmlViewResolver}都不支持国际化.
 * 如果需要为每个区域设置应用不同的视图资源, 请考虑{@link ResourceBundleViewResolver}.
 *
 * <p>Note: 这个{@code ViewResolver}实现了{@link Ordered}接口, 以便灵活地参与{@code ViewResolver}链接.
 * 例如, 可以通过此{@code ViewResolver}定义一些特殊视图 (其"order"值为0), 而所有剩余视图可以通过{@link UrlBasedViewResolver}解析.
 */
public class BeanNameViewResolver extends WebApplicationObjectSupport implements ViewResolver, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered


	/**
	 * 指定此ViewResolver bean的顺序值.
	 * <p>默认值为{@code Ordered.LOWEST_PRECEDENCE}, 表示无序.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public View resolveViewName(String viewName, Locale locale) throws BeansException {
		ApplicationContext context = getApplicationContext();
		if (!context.containsBean(viewName)) {
			if (logger.isDebugEnabled()) {
				logger.debug("No matching bean found for view name '" + viewName + "'");
			}
			// 允许ViewResolver链接...
			return null;
		}
		if (!context.isTypeMatch(viewName, View.class)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found matching bean for view name '" + viewName +
						"' - to be ignored since it does not implement View");
			}
			// 由于在这里查看一般的ApplicationContext, 接受这个不匹配并允许链接...
			return null;
		}
		return context.getBean(viewName, View.class);
	}

}
