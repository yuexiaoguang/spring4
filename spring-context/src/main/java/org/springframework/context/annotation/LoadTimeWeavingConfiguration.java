package org.springframework.context.annotation;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableLoadTimeWeaving.AspectJWeaving;
import org.springframework.context.weaving.AspectJWeavingEnabler;
import org.springframework.context.weaving.DefaultContextLoadTimeWeaver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * 注册{@link LoadTimeWeaver} bean的{@code @Configuration}类.
 *
 * <p>使用{@link EnableLoadTimeWeaving}注解时, 将自动导入此配置类.
 * 有关完整的使用详细信息, 请参阅{@code @EnableLoadTimeWeaving} javadoc.
 */
@Configuration
public class LoadTimeWeavingConfiguration implements ImportAware, BeanClassLoaderAware {

	private AnnotationAttributes enableLTW;

	private LoadTimeWeavingConfigurer ltwConfigurer;

	private ClassLoader beanClassLoader;


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableLTW = AnnotationConfigUtils.attributesFor(importMetadata, EnableLoadTimeWeaving.class);
		if (this.enableLTW == null) {
			throw new IllegalArgumentException(
					"@EnableLoadTimeWeaving is not present on importing class " + importMetadata.getClassName());
		}
	}

	@Autowired(required = false)
	public void setLoadTimeWeavingConfigurer(LoadTimeWeavingConfigurer ltwConfigurer) {
		this.ltwConfigurer = ltwConfigurer;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Bean(name = ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public LoadTimeWeaver loadTimeWeaver() {
		LoadTimeWeaver loadTimeWeaver = null;

		if (this.ltwConfigurer != null) {
			// 用户提供了自定义LoadTimeWeaver实例
			loadTimeWeaver = this.ltwConfigurer.getLoadTimeWeaver();
		}

		if (loadTimeWeaver == null) {
			// 没有提供自定义LoadTimeWeaver -> 回退到默认值
			loadTimeWeaver = new DefaultContextLoadTimeWeaver(this.beanClassLoader);
		}

		AspectJWeaving aspectJWeaving = this.enableLTW.getEnum("aspectjWeaving");
		switch (aspectJWeaving) {
			case DISABLED:
				// 禁用AJ 织入 -> do nothing
				break;
			case AUTODETECT:
				if (this.beanClassLoader.getResource(AspectJWeavingEnabler.ASPECTJ_AOP_XML_RESOURCE) == null) {
					// 类路径中不存在aop.xml -> 视为'disabled'
					break;
				}
				// 类路径中存在aop.xml -> enable
				AspectJWeavingEnabler.enableAspectJWeaving(loadTimeWeaver, this.beanClassLoader);
				break;
			case ENABLED:
				AspectJWeavingEnabler.enableAspectJWeaving(loadTimeWeaver, this.beanClassLoader);
				break;
		}

		return loadTimeWeaver;
	}

}
