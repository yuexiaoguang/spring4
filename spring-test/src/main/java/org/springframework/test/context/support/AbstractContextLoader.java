package org.springframework.test.context.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * 抽象应用程序上下文加载器, 为{@link ContextLoader} SPI的所有具体实现提供基础.
 * 为{@link #processLocations 处理}资源位置提供基于<em>模板方法</em>的方法.
 *
 * <p>从Spring 3.1开始, {@code AbstractContextLoader}也为{@link SmartContextLoader} SPI的所有具体实现提供了基础.
 * 为了向后兼容{@code ContextLoader} SPI,
 * {@link #processContextConfiguration(ContextConfigurationAttributes)}委托给{@link #processLocations(Class, String...)}.
 */
public abstract class AbstractContextLoader implements SmartContextLoader {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final Log logger = LogFactory.getLog(AbstractContextLoader.class);


	// SmartContextLoader

	/**
	 * 为了向后兼容{@link ContextLoader} SPI, 默认实现只是委托给{@link #processLocations(Class, String...)},
	 * 并从提供的{@link ContextConfigurationAttributes 配置属性}中检索
	 * {@link ContextConfigurationAttributes#getDeclaringClass() 声明类}
	 * 和{@link ContextConfigurationAttributes#getLocations() 资源位置}.
	 * 然后在提供的配置属性中{@link ContextConfigurationAttributes#setLocations(String[]) 设置}处理的位置.
	 * <p>可以在子类中重写 &mdash; 例如, 处理带注解的类而不是资源位置.
	 */
	@Override
	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
		String[] processedLocations =
				processLocations(configAttributes.getDeclaringClass(), configAttributes.getLocations());
		configAttributes.setLocations(processedLocations);
	}

	/**
	 * 在读取bean定义之前, 准备由此{@code SmartContextLoader}创建的{@link ConfigurableApplicationContext}.
	 * <p>默认实现:
	 * <ul>
	 * <li>在上下文 {@link org.springframework.core.env.Environment Environment}中
	 * 从提供的{@code MergedContextConfiguration}设置<em>活动的bean定义配置文件</em>.</li>
	 * <li>为所有
	 * {@linkplain MergedContextConfiguration#getPropertySourceLocations() 资源位置}
	 * 和{@linkplain MergedContextConfiguration#getPropertySourceProperties() 内联属性}
	 * 添加{@link PropertySource PropertySources},
	 * 从提供的{@code MergedContextConfiguration}添加到上下文的{@code Environment}.</li>
	 * <li>确定通过{@code MergedContextConfiguration} 提供的上下文初始化器类,
	 * 并使用给定的应用程序上下文实例化和 {@linkplain ApplicationContextInitializer#initialize 调用}每个类.
	 * <ul>
	 * <li>实现{@link org.springframework.core.Ordered Ordered}
	 * 或使用{@link org.springframework.core.annotation.Order @Order}注解的
	 * 任何{@code ApplicationContextInitializers}将被适当地排序.</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param context 新创建的应用程序上下文
	 * @param mergedConfig 合并的上下文配置
	 */
	protected void prepareContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		context.getEnvironment().setActiveProfiles(mergedConfig.getActiveProfiles());
		TestPropertySourceUtils.addPropertiesFilesToEnvironment(context, mergedConfig.getPropertySourceLocations());
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, mergedConfig.getPropertySourceProperties());
		invokeApplicationContextInitializers(context, mergedConfig);
	}

	@SuppressWarnings("unchecked")
	private void invokeApplicationContextInitializers(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedConfig) {

		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses =
				mergedConfig.getContextInitializerClasses();
		if (initializerClasses.isEmpty()) {
			// 没有声明ApplicationContextInitializers -> nothing to do
			return;
		}

		List<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerInstances = new ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>>();
		Class<?> contextClass = context.getClass();

		for (Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>> initializerClass : initializerClasses) {
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			if (initializerContextClass != null && !initializerContextClass.isInstance(context)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
						"is not assignable from the type of application context used by this " +
						"context loader: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						contextClass.getName()));
			}
			initializerInstances.add((ApplicationContextInitializer<ConfigurableApplicationContext>) BeanUtils.instantiateClass(initializerClass));
		}

		AnnotationAwareOrderComparator.sort(initializerInstances);
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : initializerInstances) {
			initializer.initialize(context);
		}
	}

	/**
	 * 在将bean定义加载到上下文之后, 但在上下文刷新之前,
	 * 自定义由此{@code ContextLoader}创建的{@link ConfigurableApplicationContext}.
	 * <p>默认实现委托给已使用提供的{@code mergedConfig}注册的
	 * 所有{@link MergedContextConfiguration#getContextCustomizers 上下文定制器}.
	 * 
	 * @param context 新创建的应用程序上下文
	 * @param mergedConfig 合并的上下文配置
	 */
	protected void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		for (ContextCustomizer contextCustomizer : mergedConfig.getContextCustomizers()) {
			contextCustomizer.customizeContext(context, mergedConfig);
		}
	}


	// ContextLoader

	/**
	 * 如果提供的{@code locations}为{@code null}或<em>空</em>, 并且{@link #isGenerateDefaultLocations()}返回{@code true},
	 * 将{@link #generateDefaultLocations(Class) 生成} (i.e., 检测到)默认位置,
	 * 使用指定的{@link Class class}和配置的{@linkplain #getResourceSuffixes() 资源后缀};
	 * 否则, 提供的{@code locations}将被{@linkplain #modifyLocations 编辑}并返回.
	 * 
	 * @param clazz 与位置关联的类: 在生成默认位置时使用
	 * @param locations 用于加载应用程序上下文的未修改位置 (可以是{@code null}或为空)
	 * 
	 * @return 处理的应用程序上下文资源位置数组
	 */
	@Override
	public final String[] processLocations(Class<?> clazz, String... locations) {
		return (ObjectUtils.isEmpty(locations) && isGenerateDefaultLocations()) ?
				generateDefaultLocations(clazz) : modifyLocations(clazz, locations);
	}

	/**
	 * 根据提供的类生成默认的类路径资源位置数组.
	 * <p>例如, 如果提供的类是{@code com.example.MyTest},
	 * 则生成的位置将包含一个值为{@code "classpath:com/example/MyTest<suffix>"}的字符串,
	 * 其中{@code <suffix>}是第一个配置的{@linkplain #getResourceSuffixes() 资源后缀}的值,
	 * 其中生成的位置实际存在于类路径中.
	 * <p>从Spring 3.1开始, 此方法的实现遵循{@link SmartContextLoader} SPI中定义的契约.
	 * 具体来说, 此方法将<em>抢先</em>验证生成的默认位置是否确实存在.
	 * 如果它不存在, 此方法将记录警告并返回一个空数组.
	 * <p>子类可以覆盖此方法以实现不同的<em>默认位置生成</em>策略.
	 * 
	 * @param clazz 要为其生成默认位置的类
	 * 
	 * @return 一组默认的应用程序上下文资源位置
	 */
	protected String[] generateDefaultLocations(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		String[] suffixes = getResourceSuffixes();
		for (String suffix : suffixes) {
			Assert.hasText(suffix, "Resource suffix must not be empty");
			String resourcePath = ClassUtils.convertClassNameToResourcePath(clazz.getName()) + suffix;
			String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + resourcePath;
			ClassPathResource classPathResource = new ClassPathResource(resourcePath);
			if (classPathResource.exists()) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Detected default resource location \"%s\" for test class [%s]",
							prefixedResourcePath, clazz.getName()));
				}
				return new String[] {prefixedResourcePath};
			}
			else if (logger.isDebugEnabled()) {
				logger.debug(String.format("Did not detect default resource location for test class [%s]: " +
						"%s does not exist", clazz.getName(), classPathResource));
			}
		}

		if (logger.isInfoEnabled()) {
			logger.info(String.format("Could not detect default resource locations for test class [%s]: " +
					"no resource found for suffixes %s.", clazz.getName(), ObjectUtils.nullSafeToString(suffixes)));
		}

		return EMPTY_STRING_ARRAY;
	}

	/**
	 * 生成提供的位置数组的修改版本并将其返回.
	 * <p>默认实现委托给
	 * {@link TestContextResourceUtils#convertToClasspathResourcePaths}.
	 * <p>子类可以覆盖此方法以实现不同的<em>位置修改</em>策略.
	 * 
	 * @param clazz 与位置关联的类
	 * @param locations 要修改的资源位置
	 * 
	 * @return 一组修改过的应用程序上下文资源位置
	 */
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		return TestContextResourceUtils.convertToClasspathResourcePaths(clazz, locations);
	}

	/**
	 * 如果提供给{@link #processLocations(Class, String...)}的{@code locations}为{@code null}或为空,
	 * 则确定是否应生成<em>默认</em>资源位置.
	 * <p>从Spring 3.1开始, 此方法的语义已经重载, 包括检测默认资源位置或默认配置类.
	 * 因此，如果提供给{@link #processContextConfiguration(ContextConfigurationAttributes)}
	 * 的{@link ContextConfigurationAttributes 配置属性}中的{@code classes}为{@code null}或为空,
	 * 则此方法还可用于确定是否应检测默认配置类.
	 * <p>可以由子类重写以更改默认行为.
	 * 
	 * @return 默认总是{@code true}
	 */
	protected boolean isGenerateDefaultLocations() {
		return true;
	}

	/**
	 * 检测默认位置时, 获取附加到{@link ApplicationContext}资源位置的后缀.
	 * <p>默认实现只是将{@link #getResourceSuffix()}返回的值包装在单个元素数组中,
	 * 但是这可以被子类覆盖以支持多个后缀.
	 * 
	 * @return 资源后缀; never {@code null} or empty
	 */
	protected String[] getResourceSuffixes() {
		return new String[] {getResourceSuffix()};
	}

	/**
	 * 检测默认位置时, 获取附加到{@link ApplicationContext}资源位置的后缀.
	 * <p>子类必须提供此方法的实现, 该实现返回单个后缀.
	 * 或者, 子类可以提供此方法的<em>no-op</em>实现, 并覆盖{@link #getResourceSuffixes()}以提供多个自定义后缀.
	 * 
	 * @return 资源后缀; never {@code null} or empty
	 */
	protected abstract String getResourceSuffix();

}
