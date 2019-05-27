package org.springframework.scripting.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler}, 支持动态语言的对象的连接, 例如 Groovy, JRuby and BeanShell.
 * 以下是一个示例 (来自参考文档), 详细说明了Groovy支持bean的连接:
 *
 * <pre class="code">
 * &lt;lang:groovy id="messenger"
 *     refresh-check-delay="5000"
 *     script-source="classpath:Messenger.groovy"&gt;
 * &lt;lang:property name="message" value="I Can Do The Frug"/&gt;
 * &lt;/lang:groovy&gt;
 * </pre>
 */
public class LangNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerScriptBeanDefinitionParser("groovy", "org.springframework.scripting.groovy.GroovyScriptFactory");
		registerScriptBeanDefinitionParser("jruby", "org.springframework.scripting.jruby.JRubyScriptFactory");
		registerScriptBeanDefinitionParser("bsh", "org.springframework.scripting.bsh.BshScriptFactory");
		registerScriptBeanDefinitionParser("std", "org.springframework.scripting.support.StandardScriptFactory");
		registerBeanDefinitionParser("defaults", new ScriptingDefaultsParser());
	}

	private void registerScriptBeanDefinitionParser(String key, String scriptFactoryClassName) {
		registerBeanDefinitionParser(key, new ScriptBeanDefinitionParser(scriptFactoryClassName));
	}

}
