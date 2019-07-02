package org.springframework.test.context.support;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;

/**
 * {@link AbstractGenericContextLoader}的具体实现, 它从XML资源中读取bean定义.
 *
 * <p>使用后缀{@code "-context.xml"}检测默认资源位置.
 */
public class GenericXmlContextLoader extends AbstractGenericContextLoader {

	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		return new XmlBeanDefinitionReader(context);
	}

	/**
	 * 返回{@code "-context.xml"}以支持检测默认的XML配置文件.
	 */
	@Override
	protected String getResourceSuffix() {
		return "-context.xml";
	}

	/**
	 * 确保提供的{@link MergedContextConfiguration}不包含{@link MergedContextConfiguration#getClasses() classes}.
	 */
	@Override
	protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		if (mergedConfig.hasClasses()) {
			String msg = String.format(
				"Test class [%s] has been configured with @ContextConfiguration's 'classes' attribute %s, "
						+ "but %s does not support annotated classes.", mergedConfig.getTestClass().getName(),
				ObjectUtils.nullSafeToString(mergedConfig.getClasses()), getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
