package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.preparer.factory.PreparerFactory;
import org.apache.tiles.request.Request;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Tiles {@link org.apache.tiles.preparer.PreparerFactory}接口的抽象实现,
 * 获取当前的Spring WebApplicationContext, 并委托给
 * {@link #getPreparer(String, org.springframework.web.context.WebApplicationContext)}.
 */
public abstract class AbstractSpringPreparerFactory implements PreparerFactory {

	@Override
	public ViewPreparer getPreparer(String name, Request context) {
		WebApplicationContext webApplicationContext = (WebApplicationContext) context.getContext("request").get(
				DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (webApplicationContext == null) {
			webApplicationContext = (WebApplicationContext) context.getContext("application").get(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			if (webApplicationContext == null) {
				throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
			}
		}
		return getPreparer(name, webApplicationContext);
	}

	/**
	 * 根据给定的Spring WebApplicationContext, 获取给定的preparer名称的preparer实例.
	 * 
	 * @param name preparer的名字
	 * @param context 当前的Spring WebApplicationContext
	 * 
	 * @return preparer实例
	 * @throws TilesException
	 */
	protected abstract ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException;

}
