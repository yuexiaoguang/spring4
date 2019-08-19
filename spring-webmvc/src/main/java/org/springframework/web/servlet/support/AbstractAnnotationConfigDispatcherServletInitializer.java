package org.springframework.web.servlet.support;

import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * {@link org.springframework.web.WebApplicationInitializer}实现的基类,
 * 用于注册使用带注解的类配置的
 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet},
 * e.g. Spring的
 * {@link org.springframework.context.annotation.Configuration @Configuration} classes.
 *
 * <p>具体实现需要实现{@link #getRootConfigClasses()}
 * 和{@link #getServletConfigClasses()}以及{@link #getServletMappings()}.
 * {@link AbstractDispatcherServletInitializer}提供了更多模板和自定义方法.
 *
 * <p>对于使用基于Java的Spring配置的应用程序, 这是首选方法.
 */
public abstract class AbstractAnnotationConfigDispatcherServletInitializer
		extends AbstractDispatcherServletInitializer {

	/**
	 * {@inheritDoc}
	 * <p>此实现创建一个{@link AnnotationConfigWebApplicationContext},
	 * 为其提供{@link #getRootConfigClasses()}返回的带注解的类.
	 * 如果{@link #getRootConfigClasses()}返回{@code null}, 则返回{@code null}.
	 */
	@Override
	protected WebApplicationContext createRootApplicationContext() {
		Class<?>[] configClasses = getRootConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			AnnotationConfigWebApplicationContext rootAppContext = new AnnotationConfigWebApplicationContext();
			rootAppContext.register(configClasses);
			return rootAppContext;
		}
		else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>此实现创建一个{@link AnnotationConfigWebApplicationContext},
	 * 为其提供由{@link #getServletConfigClasses()}返回的带注解的类.
	 */
	@Override
	protected WebApplicationContext createServletApplicationContext() {
		AnnotationConfigWebApplicationContext servletAppContext = new AnnotationConfigWebApplicationContext();
		Class<?>[] configClasses = getServletConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			servletAppContext.register(configClasses);
		}
		return servletAppContext;
	}

	/**
	 * 指定要提供给{@linkplain #createRootApplicationContext() 根应用程序上下文}的
	 * {@link org.springframework.context.annotation.Configuration @Configuration}
	 * 和/或{@link org.springframework.stereotype.Component @Component}类.
	 * 
	 * @return 根应用程序上下文的配置类, 如果不希望创建和注册根上下文, 则为{@code null}
	 */
	protected abstract Class<?>[] getRootConfigClasses();

	/**
	 * 指定要提供给{@linkplain #createServletApplicationContext() 调度器servlet应用程序上下文}的
	 * {@link org.springframework.context.annotation.Configuration @Configuration}
	 * 和/或{@link org.springframework.stereotype.Component @Component}类.
	 * 
	 * @return 调度器servlet应用程序上下文的配置类, 或{@code null} 如果通过根配置类指定了所有配置.
	 */
	protected abstract Class<?>[] getServletConfigClasses();

}
