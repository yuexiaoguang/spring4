package org.springframework.web.servlet.view.groovy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.view.AbstractTemplateView;
import org.springframework.web.util.NestedServletException;

/**
 * 基于Groovy XML/XHTML标记模板的{@link AbstractTemplateView}子类.
 *
 * <p>Spring的Groovy标记模板支持需要Groovy 2.3.1及更高版本.
 */
public class GroovyMarkupView extends AbstractTemplateView {

	private MarkupTemplateEngine engine;


	/**
	 * 设置在此视图中使用的MarkupTemplateEngine.
	 * <p>如果未设置, 则通过在Web应用程序上下文中查找单个{@link GroovyMarkupConfig} bean,
	 * 并使用它来获取配置的{@code MarkupTemplateEngine}实例来自动检测引擎.
	 */
	public void setTemplateEngine(MarkupTemplateEngine engine) {
		this.engine = engine;
	}

	/**
	 * 在启动时调用.
	 * 如果没有手动设置 {@link #setTemplateEngine(MarkupTemplateEngine) templateEngine},
	 * 则此方法按类型查找{@link GroovyMarkupConfig} bean, 并使用它来获取Groovy标记模板引擎.
	 */
	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext();
		if (this.engine == null) {
			setTemplateEngine(autodetectMarkupTemplateEngine());
		}
	}

	/**
	 * 通过ApplicationContext自动检测MarkupTemplateEngine.
	 * 如果尚未手动配置MarkupTemplateEngine, 则调用此方法.
	 */
	protected MarkupTemplateEngine autodetectMarkupTemplateEngine() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(getApplicationContext(),
					GroovyMarkupConfig.class, true, false).getTemplateEngine();
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException("Expected a single GroovyMarkupConfig bean in the current " +
					"Servlet web application context or the parent root context: GroovyMarkupConfigurer is " +
					"the usual implementation. This bean may have any name.", ex);
		}
	}


	@Override
	public boolean checkResource(Locale locale) throws Exception {
		try {
			this.engine.resolveTemplate(getUrl());
		}
		catch (IOException ex) {
			return false;
		}
		return true;
	}

	@Override
	protected void renderMergedTemplateModel(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		Template template = getTemplate(getUrl());
		template.make(model).writeTo(new BufferedWriter(response.getWriter()));
	}

	/**
	 * 返回由配置的Groovy Markup模板引擎为给定视图URL编译的模板.
	 */
	protected Template getTemplate(String viewUrl) throws Exception {
		try {
			return this.engine.createTemplateByPath(viewUrl);
		}
		catch (ClassNotFoundException ex) {
			Throwable cause = (ex.getCause() != null ? ex.getCause() : ex);
			throw new NestedServletException(
					"Could not find class while rendering Groovy Markup view with name '" +
					getUrl() + "': " + ex.getMessage() + "'", cause);
		}
	}
}
