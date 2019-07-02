package org.springframework.test.context.support;

import java.util.Properties;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;

/**
 * {@link AbstractGenericContextLoader}的具体实现, 它从Java {@link Properties}资源中读取bean定义.
 */
public class GenericPropertiesContextLoader extends AbstractGenericContextLoader {

	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(final GenericApplicationContext context) {
		return new PropertiesBeanDefinitionReader(context);
	}

	/**
	 * Returns &quot;{@code -context.properties}&quot;.
	 */
	@Override
	protected String getResourceSuffix() {
		return "-context.properties";
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
