package org.springframework.test.context.support;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.util.Assert;

/**
 * 使用{@link ApplicationContextInitializer ApplicationContextInitializers}的实用方法.
 *
 * <p>虽然{@code ApplicationContextInitializerUtils}是在Spring Framework 4.1中首次引入的,
 * 但此类中方法的初始实现基于{@code ContextLoaderUtils}中的现有代码.
 */
abstract class ApplicationContextInitializerUtils {

	private static final Log logger = LogFactory.getLog(ApplicationContextInitializerUtils.class);


	/**
	 * 为所提供的{@code ContextConfigurationAttributes}列表解析合并的{@code ApplicationContextInitializer}类的集合.
	 * <p>请注意, {@link ContextConfiguration @ContextConfiguration}的
	 * {@link ContextConfiguration#inheritInitializers inheritInitializers}标志将被考虑在内.
	 * 具体来说, 如果{@code inheritInitializers}标志设置为 {@code true},
	 * 用于由提供的配置属性表示的类层次结构中的给定级别,
	 * 则在给定级别定义的上下文初始化器类将与更高级别中定义的上下文初始化器类合并.
	 * 
	 * @param configAttributesList 要处理的配置属性列表;
	 * 不能为 {@code null}或<em>为空</em>; 必须<em>自下而上</em>排序 (i.e., 好像我们正在遍历类层次结构)
	 * 
	 * @return 合并的上下文初始化器的集合, 包括适当的超类 (never {@code null})
	 */
	static Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> resolveInitializerClasses(
			List<ContextConfigurationAttributes> configAttributesList) {

		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be empty");
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses =
				new LinkedHashSet<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace("Processing context initializers for configuration attributes " + configAttributes);
			}
			initializerClasses.addAll(Arrays.asList(configAttributes.getInitializers()));
			if (!configAttributes.isInheritInitializers()) {
				break;
			}
		}

		return initializerClasses;
	}

}
