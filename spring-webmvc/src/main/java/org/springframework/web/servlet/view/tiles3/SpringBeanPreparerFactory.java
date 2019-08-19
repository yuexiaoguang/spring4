package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.ViewPreparer;

import org.springframework.web.context.WebApplicationContext;

/**
 * Tiles {@link org.apache.tiles.preparer.PreparerFactory}实现,
 * 使用preparer bean名称, 并从Spring ApplicationContext获取 preparer bean.
 * 在这种情况下, 完整的bean创建过程将控制在Spring应用程序上下文中, 允许使用scoped bean等.
 */
public class SpringBeanPreparerFactory extends AbstractSpringPreparerFactory {

	@Override
	protected ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException {
		return context.getBean(name, ViewPreparer.class);
	}

}
