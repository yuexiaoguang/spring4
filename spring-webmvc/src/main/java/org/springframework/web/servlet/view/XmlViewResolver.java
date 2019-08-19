package org.springframework.web.servlet.view;

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.View;

/**
 * {@link org.springframework.web.servlet.ViewResolver}实现,
 * 它在专用XML文件中使用bean定义进行视图定义, 由资源位置指定.
 * 该文件通常位于WEB-INF目录中; 默认为"/WEB-INF/views.xml".
 *
 * <p>此{@code ViewResolver}不支持其定义资源级别的国际化.
 * 如果需要为每个区域设置应用不同的视图资源, 请考虑{@link ResourceBundleViewResolver}.
 *
 * <p>Note: 这个{@code ViewResolver}实现了{@link Ordered}接口, 以便灵活地参与{@code ViewResolver}链接.
 * 例如, 可以通过此{@code ViewResolver}定义一些特殊视图 (其"order"值为0), 而所有剩余视图可以通过{@link UrlBasedViewResolver}解析.
 */
public class XmlViewResolver extends AbstractCachingViewResolver
		implements Ordered, InitializingBean, DisposableBean {

	/** 默认位置 */
	public static final String DEFAULT_LOCATION = "/WEB-INF/views.xml";


	private Resource location;

	private ConfigurableApplicationContext cachedFactory;

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered


	/**
	 * 设置定义视图bean的XML文件的位置.
	 * <p>默认为 "/WEB-INF/views.xml".
	 * 
	 * @param location XML文件的位置.
	 */
	public void setLocation(Resource location) {
		this.location = location;
	}

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

	/**
	 * 从XML文件预初始化工厂.
	 * 仅在启用缓存时才有效.
	 */
	@Override
	public void afterPropertiesSet() throws BeansException {
		if (isCache()) {
			initFactory();
		}
	}


	/**
	 * 此实现仅返回视图名称, 因为XmlViewResolver不支持本地化解析.
	 */
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName;
	}

	@Override
	protected View loadView(String viewName, Locale locale) throws BeansException {
		BeanFactory factory = initFactory();
		try {
			return factory.getBean(viewName, View.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Allow for ViewResolver chaining...
			return null;
		}
	}

	/**
	 * 从XML文件初始化视图bean工厂.
	 * 由于并行线程的访问而同步.
	 * 
	 * @throws BeansException 初始化错误
	 */
	protected synchronized BeanFactory initFactory() throws BeansException {
		if (this.cachedFactory != null) {
			return this.cachedFactory;
		}

		Resource actualLocation = this.location;
		if (actualLocation == null) {
			actualLocation = getApplicationContext().getResource(DEFAULT_LOCATION);
		}

		// 为视图创建子级ApplicationContext.
		GenericWebApplicationContext factory = new GenericWebApplicationContext();
		factory.setParent(getApplicationContext());
		factory.setServletContext(getServletContext());

		// 使用上下文感知实体解析器加载XML资源.
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.setEnvironment(getApplicationContext().getEnvironment());
		reader.setEntityResolver(new ResourceEntityResolver(getApplicationContext()));
		reader.loadBeanDefinitions(actualLocation);

		factory.refresh();

		if (isCache()) {
			this.cachedFactory = factory;
		}
		return factory;
	}


	/**
	 * 关闭上下文时关闭视图bean工厂.
	 */
	@Override
	public void destroy() throws BeansException {
		if (this.cachedFactory != null) {
			this.cachedFactory.close();
		}
	}

}
