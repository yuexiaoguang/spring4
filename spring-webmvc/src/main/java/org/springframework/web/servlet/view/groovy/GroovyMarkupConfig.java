package org.springframework.web.servlet.view.groovy;

import groovy.text.markup.MarkupTemplateEngine;

/**
 * Interface to be implemented by objects that configure and manage a Groovy
 * {@link MarkupTemplateEngine} for automatic lookup in a web environment.
 * Detected and used by {@link GroovyMarkupView}.
 */
public interface GroovyMarkupConfig {

	/**
	 * Return the Groovy {@link MarkupTemplateEngine} for the current
	 * web application context. May be unique to one servlet, or shared
	 * in the root context.
	 * @return the Groovy MarkupTemplateEngine engine
	 */
	MarkupTemplateEngine getTemplateEngine();

}
