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
 * An {@link AbstractTemplateView} subclass based on Groovy XML/XHTML markup templates.
 *
 * <p>Spring's Groovy Markup Template support requires Groovy 2.3.1 and higher.
 */
public class GroovyMarkupView extends AbstractTemplateView {

	private MarkupTemplateEngine engine;


	/**
	 * Set the MarkupTemplateEngine to use in this view.
	 * <p>If not set, the engine is auto-detected by looking up a single
	 * {@link GroovyMarkupConfig} bean in the web application context and using
	 * it to obtain the configured {@code MarkupTemplateEngine} instance.
	 */
	public void setTemplateEngine(MarkupTemplateEngine engine) {
		this.engine = engine;
	}

	/**
	 * Invoked at startup.
	 * If no {@link #setTemplateEngine(MarkupTemplateEngine) templateEngine} has
	 * been manually set, this method looks up a {@link GroovyMarkupConfig} bean
	 * by type and uses it to obtain the Groovy Markup template engine.
	 */
	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext();
		if (this.engine == null) {
			setTemplateEngine(autodetectMarkupTemplateEngine());
		}
	}

	/**
	 * Autodetect a MarkupTemplateEngine via the ApplicationContext.
	 * Called if a MarkupTemplateEngine has not been manually configured.
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
	 * Return a template compiled by the configured Groovy Markup template engine
	 * for the given view URL.
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
