package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 对bean定义读取器实现有用的实用方法.
 * 主要供内部使用.
 */
public class BeanDefinitionReaderUtils {

	/**
	 * 生成的bean名称的分隔符.
	 * 如果类名或父级名称不是唯一的, "#1", "#2" 等将被追加, 直到名称变得唯一.
	 */
	public static final String GENERATED_BEAN_NAME_SEPARATOR = BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;


	/**
	 * 为给定的父名称和类名创建一个新的GenericBeanDefinition, 如果指定了ClassLoader, 则实时地加载bean类.
	 * 
	 * @param parentName 父级bean的名称
	 * @param className bean类的名称
	 * @param classLoader 用于加载bean类的ClassLoader ({@code null}按名称注册bean类)
	 * 
	 * @return bean定义
	 * @throws ClassNotFoundException 如果无法加载bean类
	 */
	public static AbstractBeanDefinition createBeanDefinition(
			String parentName, String className, ClassLoader classLoader) throws ClassNotFoundException {

		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setParentName(parentName);
		if (className != null) {
			if (classLoader != null) {
				bd.setBeanClass(ClassUtils.forName(className, classLoader));
			}
			else {
				bd.setBeanClassName(className);
			}
		}
		return bd;
	}

	/**
	 * 为给定的顶级bean定义生成bean名称, 在给定的bean工厂中是唯一的.
	 * 
	 * @param beanDefinition 用于生成bean名称的bean定义
	 * @param registry 定义将要注册的bean工厂 (检查现有的bean名称)
	 * 
	 * @return 生成的bean名称
	 * @throws BeanDefinitionStoreException 如果没有为给定的bean定义生成唯一的名称
	 */
	public static String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		return generateBeanName(beanDefinition, registry, false);
	}

	/**
	 * 为给定的bean定义生成bean名称, 在给定的bean工厂中是唯一的.
	 * 
	 * @param definition 用于生成bean名称的bean定义
	 * @param registry 定义将要注册的bean工厂 (检查现有的bean名称)
	 * @param isInnerBean 是否将给定的bean定义注册为内部bean或顶级bean
	 * (允许内部bean与顶级bean的特殊名称生成)
	 * 
	 * @return 生成的bean名称
	 * @throws BeanDefinitionStoreException 如果没有为给定的bean定义生成唯一的名称
	 */
	public static String generateBeanName(
			BeanDefinition definition, BeanDefinitionRegistry registry, boolean isInnerBean)
			throws BeanDefinitionStoreException {

		String generatedBeanName = definition.getBeanClassName();
		if (generatedBeanName == null) {
			if (definition.getParentName() != null) {
				generatedBeanName = definition.getParentName() + "$child";
			}
			else if (definition.getFactoryBeanName() != null) {
				generatedBeanName = definition.getFactoryBeanName() + "$created";
			}
		}
		if (!StringUtils.hasText(generatedBeanName)) {
			throw new BeanDefinitionStoreException("Unnamed bean definition specifies neither " +
					"'class' nor 'parent' nor 'factory-bean' - can't generate bean name");
		}

		String id = generatedBeanName;
		if (isInnerBean) {
			// Inner bean: generate identity hashcode suffix.
			id = generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(definition);
		}
		else {
			// Top-level bean: use plain class name.
			// Increase counter until the id is unique.
			int counter = -1;
			while (counter == -1 || registry.containsBeanDefinition(id)) {
				counter++;
				id = generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + counter;
			}
		}
		return id;
	}

	/**
	 * 使用给定的bean工厂注册给定的bean定义.
	 * 
	 * @param definitionHolder bean定义, 包括名称和别名
	 * @param registry 要注册的bean工厂
	 * 
	 * @throws BeanDefinitionStoreException 如果注册失败
	 */
	public static void registerBeanDefinition(
			BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		// 在主名称下注册bean定义.
		String beanName = definitionHolder.getBeanName();
		registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

		// Register aliases for bean name, if any.
		String[] aliases = definitionHolder.getAliases();
		if (aliases != null) {
			for (String alias : aliases) {
				registry.registerAlias(beanName, alias);
			}
		}
	}

	/**
	 * 使用生成的名称注册给定的bean定义, 在给定的bean工厂中是唯一的.
	 * 
	 * @param definition 用于生成bean名称的bean定义
	 * @param registry 要注册的bean工厂
	 * 
	 * @return 生成的bean名称
	 * @throws BeanDefinitionStoreException 如果没有为给定的bean定义生成唯一名称, 或者无法注册定义
	 */
	public static String registerWithGeneratedName(
			AbstractBeanDefinition definition, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		String generatedName = generateBeanName(definition, registry, false);
		registry.registerBeanDefinition(generatedName, definition);
		return generatedName;
	}

}
