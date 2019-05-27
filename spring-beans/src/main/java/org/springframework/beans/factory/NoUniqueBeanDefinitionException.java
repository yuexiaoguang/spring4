package org.springframework.beans.factory;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.util.StringUtils;

/**
 * 当{@code BeanFactory}被要求提供一个bean实例, 预期只有一个匹配的bean, 但找到了多个匹配的候选者时, 抛出异常.
 */
@SuppressWarnings("serial")
public class NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException {

	private int numberOfBeansFound;

	private Collection<String> beanNamesFound;


	/**
	 * @param type 不唯一bean的必需类型
	 * @param numberOfBeansFound 匹配的bean的数量
	 * @param message detailed message describing the problem
	 */
	public NoUniqueBeanDefinitionException(Class<?> type, int numberOfBeansFound, String message) {
		super(type, message);
		this.numberOfBeansFound = numberOfBeansFound;
	}

	/**
	 * @param type 不唯一bean的必需类型
	 * @param beanNamesFound 所有匹配的bean的名称 (as a Collection)
	 */
	public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound) {
		this(type, beanNamesFound.size(), "expected single matching bean but found " + beanNamesFound.size() + ": " +
				StringUtils.collectionToCommaDelimitedString(beanNamesFound));
		this.beanNamesFound = beanNamesFound;
	}

	/**
	 * @param type 不唯一bean的必需类型
	 * @param beanNamesFound 所有匹配的bean的名称 (as an array)
	 */
	public NoUniqueBeanDefinitionException(Class<?> type, String... beanNamesFound) {
		this(type, Arrays.asList(beanNamesFound));
	}


	/**
	 * 返回当只需要一个匹配的bean时, 找到的bean数量.
	 * 对于 NoUniqueBeanDefinitionException, 通常大于 1.
	 */
	@Override
	public int getNumberOfBeansFound() {
		return this.numberOfBeansFound;
	}

	/**
	 * 返回当只需要一个匹配的bean时, 找到的所有bean的名称.
	 * 请注意, 如果在构造时未指定, 则可能是{@code null}.
	 */
	public Collection<String> getBeanNamesFound() {
		return this.beanNamesFound;
	}

}
