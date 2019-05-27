package org.springframework.scripting.groovy;

import groovy.lang.GroovyObject;

/**
 * {@link GroovyScriptFactory}使用的策略允许自定义创建的{@link GroovyObject}.
 *
 * <p>这对于允许编写DSL, 替换丢失的方法等非常有用.
 * 例如, 可以指定自定义{@link groovy.lang.MetaClass}.
 */
public interface GroovyObjectCustomizer {

	/**
	 * 自定义提供的{@link GroovyObject}.
	 * <p>例如, 这可用于设置自定义元类以处理缺少的方法.
	 * 
	 * @param goo 要自定义的{@code GroovyObject}
	 */
	void customize(GroovyObject goo);

}
