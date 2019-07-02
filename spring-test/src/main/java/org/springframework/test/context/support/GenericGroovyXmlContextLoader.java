package org.springframework.test.context.support;

import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link AbstractGenericContextLoader}的具体实现, 它从Groovy脚本<em>和</em> XML配置文件中读取bean定义.
 *
 * <p>使用后缀{@code "-context.xml"} 和 {@code "Context.groovy"}检测默认资源位置.
 */
public class GenericGroovyXmlContextLoader extends GenericXmlContextLoader {

	/**
	 * 使用{@link GroovyBeanDefinitionReader}从提供的{@code MergedContextConfiguration}中的位置,
	 * 将bean定义加载到提供的{@link GenericApplicationContext 上下文}中.
	 * 
	 * @param context 应该加载bean定义的上下文
	 * @param mergedConfig 合并的上下文配置
	 */
	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		new GroovyBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
	}

	/**
	 * 返回{@code "-context.xml" 和 "Context.groovy"}, 以支持检测默认XML配置文件或Groovy脚本.
	 */
	@Override
	protected String[] getResourceSuffixes() {
		return new String[] { super.getResourceSuffix(), "Context.groovy" };
	}

	/**
	 * {@code GenericGroovyXmlContextLoader}支持Groovy和XML资源类型以检测默认值.
	 * 因此, 不支持此方法.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"GenericGroovyXmlContextLoader does not support the getResourceSuffix() method");
	}

}
