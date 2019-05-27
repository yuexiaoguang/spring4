package org.springframework.scripting.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;

public abstract class LangNamespaceUtils {

	/**
	 * 内部管理的{@link ScriptFactoryPostProcessor}在{@link BeanDefinitionRegistry}中注册的唯一名称.
	 */
	private static final String SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME =
			"org.springframework.scripting.config.scriptFactoryPostProcessor";


	/**
	 * 如果{@link ScriptFactoryPostProcessor}尚未注册,
	 * 在提供的{@link BeanDefinitionRegistry}中注册{@link ScriptFactoryPostProcessor} bean定义.
	 * 
	 * @param registry 用于注册脚本处理器的{@link BeanDefinitionRegistry}
	 * 
	 * @return {@link ScriptFactoryPostProcessor} bean定义 (新的或已经注册的)
	 */
	public static BeanDefinition registerScriptFactoryPostProcessorIfNecessary(BeanDefinitionRegistry registry) {
		BeanDefinition beanDefinition = null;
		if (registry.containsBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME)) {
			beanDefinition = registry.getBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME);
		}
		else {
			beanDefinition = new RootBeanDefinition(ScriptFactoryPostProcessor.class);
			registry.registerBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME, beanDefinition);
		}
		return beanDefinition;
	}

}
