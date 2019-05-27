package org.springframework.beans.factory.annotation;

import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 执行与注解相关的bean查找的工具方法, 例如Spring的 {@link Qualifier @Qualifier} 注解.
 */
public abstract class BeanFactoryAnnotationUtils {

	/**
	 * 从给定的{@code BeanFactory}(e.g. 通过{@code <qualifier>}或{@code @Qualifier})中,
	 * 获取匹配给定限定符, 或者具有与给定限定符匹配的bean名称的{@code T}类型的bean.
	 * 
	 * @param beanFactory 从中获取目标bean的BeanFactory
	 * @param beanType 要检索的bean的类型
	 * @param qualifier 在多个bean匹配之间进行选择的限定符
	 * 
	 * @return 类型为{@code T}的匹配的bean (never {@code null})
	 * @throws NoUniqueBeanDefinitionException 如果找到多个类型为{@code T}的匹配的bean
	 * @throws NoSuchBeanDefinitionException 如果找不到类型为{@code T}的匹配的bean
	 * @throws BeansException 如果无法创建bean
	 */
	public static <T> T qualifiedBeanOfType(BeanFactory beanFactory, Class<T> beanType, String qualifier)
			throws BeansException {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			// Full qualifier matching supported.
			return qualifiedBeanOfType((ConfigurableListableBeanFactory) beanFactory, beanType, qualifier);
		}
		else if (beanFactory.containsBean(qualifier)) {
			// Fallback: target bean at least found by bean name.
			return beanFactory.getBean(qualifier, beanType);
		}
		else {
			throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() +
					" bean found for bean name '" + qualifier +
					"'! (Note: Qualifier matching not supported because given " +
					"BeanFactory does not implement ConfigurableListableBeanFactory.)");
		}
	}

	/**
	 * 从给定的{@code BeanFactory}中(e.g. 声明了{@code <qualifier>} 或{@code @Qualifier})获取{@code T}类型的匹配给定限定符的bean .
	 * 
	 * @param bf 从中获取目标bean的BeanFactory
	 * @param beanType 要检索的bean的类型
	 * @param qualifier 在多个bean匹配之间进行选择的限定符
	 * 
	 * @return 类型为{@code T}的匹配的bean (never {@code null})
	 */
	private static <T> T qualifiedBeanOfType(ConfigurableListableBeanFactory bf, Class<T> beanType, String qualifier) {
		String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(bf, beanType);
		String matchingBean = null;
		for (String beanName : candidateBeans) {
			if (isQualifierMatch(qualifier, beanName, bf)) {
				if (matchingBean != null) {
					throw new NoUniqueBeanDefinitionException(beanType, matchingBean, beanName);
				}
				matchingBean = beanName;
			}
		}
		if (matchingBean != null) {
			return bf.getBean(matchingBean, beanType);
		}
		else if (bf.containsBean(qualifier)) {
			// Fallback: target bean at least found by bean name - probably a manually registered singleton.
			return bf.getBean(qualifier, beanType);
		}
		else {
			throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() +
					" bean found for qualifier '" + qualifier + "' - neither qualifier match nor bean name match!");
		}
	}

	/**
	 * 检查指定的bean是否声明了给定名称的限定符.
	 * 
	 * @param qualifier 要匹配的限定符
	 * @param beanName 候选bean的名称
	 * @param bf 要从中检索命名bean的{@code BeanFactory}
	 * 
	 * @return {@code true} 如果bean定义(在XML情况下), 
	 * 或bean的工厂方法(在{@code @Bean}情况下)定义了匹配的限定符值 (通过{@code <qualifier>} 或 {@code @Qualifier})
	 */
	private static boolean isQualifierMatch(String qualifier, String beanName, ConfigurableListableBeanFactory bf) {
		if (bf.containsBean(beanName)) {
			try {
				BeanDefinition bd = bf.getMergedBeanDefinition(beanName);
				// Explicit qualifier metadata on bean definition? (typically in XML definition)
				if (bd instanceof AbstractBeanDefinition) {
					AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
					AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
					if ((candidate != null && qualifier.equals(candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY))) ||
							qualifier.equals(beanName) || ObjectUtils.containsElement(bf.getAliases(beanName), qualifier)) {
						return true;
					}
				}
				// Corresponding qualifier on factory method? (typically in configuration class)
				if (bd instanceof RootBeanDefinition) {
					Method factoryMethod = ((RootBeanDefinition) bd).getResolvedFactoryMethod();
					if (factoryMethod != null) {
						Qualifier targetAnnotation = AnnotationUtils.getAnnotation(factoryMethod, Qualifier.class);
						if (targetAnnotation != null) {
							return qualifier.equals(targetAnnotation.value());
						}
					}
				}
				// Corresponding qualifier on bean implementation class? (for custom user types)
				Class<?> beanType = bf.getType(beanName);
				if (beanType != null) {
					Qualifier targetAnnotation = AnnotationUtils.getAnnotation(beanType, Qualifier.class);
					if (targetAnnotation != null) {
						return qualifier.equals(targetAnnotation.value());
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore - can't compare qualifiers for a manually registered singleton object
			}
		}
		return false;
	}

}
