package org.springframework.test.context.web;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * {@link AbstractGenericWebContextLoader}的具体实现, 它从XML资源加载bean定义.
 *
 * <p>使用后缀{@code "-context.xml"}检测默认资源位置.
 */
public class GenericXmlWebContextLoader extends AbstractGenericWebContextLoader {

	/**
	 * 使用{@link XmlBeanDefinitionReader}从提供的{@code WebMergedContextConfiguration}中的位置,
	 * 将bean定义加载到提供的{@link GenericWebApplicationContext context}中.
	 */
	@Override
	protected void loadBeanDefinitions(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {
		new XmlBeanDefinitionReader(context).loadBeanDefinitions(webMergedConfig.getLocations());
	}

	/**
	 * 返回{@code "-context.xml"}以支持检测默认的XML配置文件.
	 */
	@Override
	protected String getResourceSuffix() {
		return "-context.xml";
	}

	/**
	 * 确保提供的{@link WebMergedContextConfiguration}不包含{@link MergedContextConfiguration#getClasses() classes}.
	 */
	@Override
	protected void validateMergedContextConfiguration(WebMergedContextConfiguration webMergedConfig) {
		if (webMergedConfig.hasClasses()) {
			String msg = String.format(
				"Test class [%s] has been configured with @ContextConfiguration's 'classes' attribute %s, "
						+ "but %s does not support annotated classes.", webMergedConfig.getTestClass().getName(),
				ObjectUtils.nullSafeToString(webMergedConfig.getClasses()), getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}
}
