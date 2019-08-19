package org.springframework.web.servlet.view.groovy;

import groovy.text.markup.MarkupTemplateEngine;

/**
 * 由配置和管理Groovy {@link MarkupTemplateEngine}的对象实现的接口, 以便在Web环境中自动查找.
 * 由{@link GroovyMarkupView}检测并使用.
 */
public interface GroovyMarkupConfig {

	/**
	 * 返回Groovy {@link MarkupTemplateEngine}以获取当前Web应用程序上下文.
	 * 可能对一个servlet是唯一的, 也可以在根上下文中共享.
	 * 
	 * @return Groovy MarkupTemplateEngine引擎
	 */
	MarkupTemplateEngine getTemplateEngine();

}
