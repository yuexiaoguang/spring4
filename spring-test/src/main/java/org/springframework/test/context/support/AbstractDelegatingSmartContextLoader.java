package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;

/**
 * {@code AbstractDelegatingSmartContextLoader}作为{@link SmartContextLoader} SPI实现的抽象基类,
 * 委托给一组<em>候选</em> SmartContextLoaders (i.e., 一个支持XML配置文件或Groovy脚本, 一个支持带注解的类),
 * 以确定哪个上下文加载器适合给定测试类的配置.
 * 每个候选都有机会{@linkplain #processContextConfiguration process} {@link ContextConfigurationAttributes},
 * 用于带{@link ContextConfiguration @ContextConfiguration}注解的测试类层次结构中的每个类,
 * 并且支持合并和处理配置的候选将用于实际{@linkplain #loadContext 加载}上下文.
 *
 * <p>对<em>基于XML的加载器</em>的任何引用都可以解释为仅支持XML配置文件的上下文加载器,
 * 或同时支持XML配置文件和Groovy脚本的上下文加载器.
 *
 * <p>在测试类上放置一个空的{@code @ContextConfiguration}注解,
 * 表示应该检测默认资源位置(e.g., XML配置文件或Groovy脚本)
 * 或默认 {@linkplain org.springframework.context.annotation.Configuration 配置类}.
 * 此外，如果未通过{@code @ContextConfiguration}显式声明特定的{@link ContextLoader}或{@link SmartContextLoader},
 * 则{@code AbstractDelegatingSmartContextLoader}的具体子类将用作默认加载器,
 * 从而为基于路径的资源位置 (e.g., XML配置文件和Groovy脚本) 或带注解的类提供自动支持, 但不能同时提供.
 *
 * <p>从Spring 3.2开始, 测试类可以选择性地既不声明基于路径的资源位置, 也不声明带注释的类, 而是仅声明
 * {@linkplain ContextConfiguration#initializers 应用上下文初始化器}.
 * 在这种情况下, 仍将尝试检测默认值, 但它们的缺失不会导致异常.
 */
public abstract class AbstractDelegatingSmartContextLoader implements SmartContextLoader {

	private static final Log logger = LogFactory.getLog(AbstractDelegatingSmartContextLoader.class);


	/**
	 * 获取支持XML配置文件和/或Groovy脚本的委托{@code SmartContextLoader}.
	 */
	protected abstract SmartContextLoader getXmlLoader();

	/**
	 * 获取支持带注解的类的委托{@code SmartContextLoader}.
	 */
	protected abstract SmartContextLoader getAnnotationConfigLoader();


	// ContextLoader

	/**
	 * {@code AbstractDelegatingSmartContextLoader}不支持{@link ContextLoader#processLocations(Class, String...)}方法.
	 * 调用{@link #processContextConfiguration(ContextConfigurationAttributes)}.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	public final String[] processLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException(
				"DelegatingSmartContextLoaders do not support the ContextLoader SPI. " +
						"Call processContextConfiguration(ContextConfigurationAttributes) instead.");
	}

	/**
	 * {@code AbstractDelegatingSmartContextLoader}不支持{@link ContextLoader#loadContext(String...) }方法.
	 * 调用{@link #loadContext(MergedContextConfiguration)}.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	public final ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException(
				"DelegatingSmartContextLoaders do not support the ContextLoader SPI. " +
						"Call loadContext(MergedContextConfiguration) instead.");
	}


	// SmartContextLoader

	/**
	 * 委托给候选{@code SmartContextLoaders}处理提供的{@link ContextConfigurationAttributes}.
	 * <p>委托基于明确已知{@linkplain #getXmlLoader() XML配置文件和Groovy脚本}
	 * 和{@linkplain #getAnnotationConfigLoader() 带注解的类}的默认加载器的实现.
	 * 具体地, 委托算法如下:
	 * <ul>
	 * <li>如果提供的{@code ContextConfigurationAttributes}中的资源位置或带注解的类不为空,
	 * 则允许相应的候选加载器<em>按原样</em>处理配置, 而不检查默认值.</li>
	 * <li>否则, 将允许基于XML的加载器处理配置, 以检测默认资源位置.
	 * 如果基于XML的加载器检测到默认资源位置, 则会记录{@code info}消息.</li>
	 * <li>随后, 将允许基于注解的加载器处理配置, 以检测默认配置类.
	 * 如果基于注解的加载器检测到默认配置类, 则将记录{@code info}消息.</li>
	 * </ul>
	 * 
	 * @param configAttributes 要处理的上下文配置属性
	 * 
	 * @throws IllegalArgumentException 如果提供的配置属性为{@code null}, 或者提供的配置属性包括资源位置和带注解的类
	 * @throws IllegalStateException 如果基于XML的加载器检测到默认配置类;
	 * 如果基于注解的加载器检测到默认资源位置;
	 * 如果候选加载器都没有检测到提供的上下文配置的默认值;
	 * 或者两个候选加载器都检测到提供的上下文配置的默认值
	 */
	@Override
	public void processContextConfiguration(final ContextConfigurationAttributes configAttributes) {
		Assert.notNull(configAttributes, "configAttributes must not be null");
		Assert.isTrue(!(configAttributes.hasLocations() && configAttributes.hasClasses()), String.format(
				"Cannot process locations AND classes for context configuration %s: " +
				"configure one or the other, but not both.", configAttributes));

		// 如果原始位置或类不为空, 则无需使用默认检测检查; 只需让适当的加载器处理配置.
		if (configAttributes.hasLocations()) {
			delegateProcessing(getXmlLoader(), configAttributes);
		}
		else if (configAttributes.hasClasses()) {
			delegateProcessing(getAnnotationConfigLoader(), configAttributes);
		}
		else {
			// 否则尝试检测默认值...

			// 让XML加载器处理配置.
			delegateProcessing(getXmlLoader(), configAttributes);
			boolean xmlLoaderDetectedDefaults = configAttributes.hasLocations();

			if (xmlLoaderDetectedDefaults) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("%s detected default locations for context configuration %s.",
							name(getXmlLoader()), configAttributes));
				}
			}

			if (configAttributes.hasClasses()) {
				throw new IllegalStateException(String.format(
						"%s should NOT have detected default configuration classes for context configuration %s.",
						name(getXmlLoader()), configAttributes));
			}

			// 现在让注解配置加载器处理配置.
			delegateProcessing(getAnnotationConfigLoader(), configAttributes);

			if (configAttributes.hasClasses()) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format(
							"%s detected default configuration classes for context configuration %s.",
							name(getAnnotationConfigLoader()), configAttributes));
				}
			}

			if (!xmlLoaderDetectedDefaults && configAttributes.hasLocations()) {
				throw new IllegalStateException(String.format(
						"%s should NOT have detected default locations for context configuration %s.",
						name(getAnnotationConfigLoader()), configAttributes));
			}

			if (configAttributes.hasLocations() && configAttributes.hasClasses()) {
				String msg = String.format(
						"Configuration error: both default locations AND default configuration classes " +
						"were detected for context configuration %s; configure one or the other, but not both.",
						configAttributes);
				logger.error(msg);
				throw new IllegalStateException(msg);
			}
		}
	}

	/**
	 * 委托给适当的候选{@code SmartContextLoader}加载{@link ApplicationContext}.
	 * <p>委托基于明确已知{@linkplain #getXmlLoader() XML配置文件和Groovy脚本}
	 * 和{@linkplain #getAnnotationConfigLoader() 带注解的类}的默认加载器的实现.
	 * 具体地, 委托算法如下:
	 * <ul>
	 * <li>如果提供的{@code MergedContextConfiguration}中的资源位置不为空,
	 * 且带注解的类为空, 则基于XML的加载器将加载{@code ApplicationContext}.</li>
	 * <li>如果提供的{@code MergedContextConfiguration}中带注解的类不为空,
	 * 且资源位置为空, 则基于注解的加载器将加载{@code ApplicationContext}.</li>
	 * </ul>
	 * 
	 * @param mergedConfig 用于加载应用程序上下文的合并上下文配置
	 * 
	 * @throws IllegalArgumentException 如果提供的合并配置为{@code null}
	 * @throws IllegalStateException 如果候选加载器都不能从提供的合并上下文配置加载{@code ApplicationContext}
	 */
	@Override
	public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		Assert.notNull(mergedConfig, "MergedContextConfiguration must not be null");

		if (mergedConfig.hasLocations() && mergedConfig.hasClasses()) {
			throw new IllegalStateException(String.format(
					"Neither %s nor %s supports loading an ApplicationContext from %s: " +
					"declare either 'locations' or 'classes' but not both.", name(getXmlLoader()),
					name(getAnnotationConfigLoader()), mergedConfig));
		}

		SmartContextLoader[] candidates = {getXmlLoader(), getAnnotationConfigLoader()};
		for (SmartContextLoader loader : candidates) {
			// 确定每个加载器是否可以从mergedConfig加载上下文. 如果可以的话, 让它来; 否则, 继续迭代.
			if (supports(loader, mergedConfig)) {
				return delegateLoading(loader, mergedConfig);
			}
		}

		// 如果两个候选者都不支持基于资源的mergedConfig, 但是声明了ACI或定制器, 那么委托给注解配置加载器.
		if (!mergedConfig.getContextInitializerClasses().isEmpty() || !mergedConfig.getContextCustomizers().isEmpty()) {
			return delegateLoading(getAnnotationConfigLoader(), mergedConfig);
		}

		// else...
		throw new IllegalStateException(String.format(
				"Neither %s nor %s was able to load an ApplicationContext from %s.",
				name(getXmlLoader()), name(getAnnotationConfigLoader()), mergedConfig));
	}


	private static void delegateProcessing(SmartContextLoader loader, ContextConfigurationAttributes configAttributes) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Delegating to %s to process context configuration %s.",
					name(loader), configAttributes));
		}
		loader.processContextConfiguration(configAttributes);
	}

	private static ApplicationContext delegateLoading(SmartContextLoader loader, MergedContextConfiguration mergedConfig)
			throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Delegating to %s to load context from %s.", name(loader), mergedConfig));
		}
		return loader.loadContext(mergedConfig);
	}

	private boolean supports(SmartContextLoader loader, MergedContextConfiguration mergedConfig) {
		if (loader == getAnnotationConfigLoader()) {
			return (mergedConfig.hasClasses() && !mergedConfig.hasLocations());
		}
		else {
			return (mergedConfig.hasLocations() && !mergedConfig.hasClasses());
		}
	}

	private static String name(SmartContextLoader loader) {
		return loader.getClass().getSimpleName();
	}

}
