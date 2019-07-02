package org.springframework.test.context.web;

import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * {@link AbstractGenericWebContextLoader}的具体实现, 它从Groovy脚本<em>和</em> XML配置文件加载bean定义.
 *
 * <p>使用后缀{@code "-context.xml"}和{@code "Context.groovy"}检测默认资源位置.
 */
public class GenericGroovyXmlWebContextLoader extends GenericXmlWebContextLoader {

	/**
	 * 使用{@link GroovyBeanDefinitionReader}从提供的{@code WebMergedContextConfiguration}中的位置
	 * 将bean定义加载到提供的{@link GenericWebApplicationContext context}中.
	 * 
	 * @param context 应该加载bean定义的上下文
	 * @param webMergedConfig 合并的上下文配置
	 */
	@Override
	protected void loadBeanDefinitions(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {
		new GroovyBeanDefinitionReader(context).loadBeanDefinitions(webMergedConfig.getLocations());
	}

	/**
	 * 返回{@code "-context.xml" 和 "Context.groovy"}以支持检测默认XML配置文件或Groovy脚本.
	 */
	@Override
	protected String[] getResourceSuffixes() {
		return new String[] { super.getResourceSuffix(), "Context.groovy" };
	}

	/**
	 * {@code GenericGroovyXmlWebContextLoader}支持Groovy和XML资源类型以检测默认值.
	 * 因此, 不支持此方法.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"GenericGroovyXmlWebContextLoader does not support the getResourceSuffix() method");
	}

}
