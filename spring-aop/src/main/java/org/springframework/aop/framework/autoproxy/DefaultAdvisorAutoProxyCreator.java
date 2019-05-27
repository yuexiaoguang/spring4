package org.springframework.aop.framework.autoproxy;

import org.springframework.beans.factory.BeanNameAware;

/**
 * {@code BeanPostProcessor}实现，根据当前{@code BeanFactory}中的所有候选{@code Advisor}创建AOP代理.
 * 这个类是完全通用的; 它不包含任何特殊代码来处理任何特定切面, 例如使用池的切面.
 *
 * <p>有可能过滤出切面 - 例如, 在同一工厂中使用此类型的多个后处理器 - 通过设置 {@code usePrefix}属性为 true,
 * 在这种情况下，将只使用以DefaultAdvisorAutoProxyCreator的bean名称后跟一个点(如 "aapc.")开头的切面.
 * 可以通过设置{@code advisorBeanNamePrefix}属性从bean名称更改此缺省前缀.
 * 在这种情况下也将使用分隔符 (.).
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator implements BeanNameAware {

	/** bean名称的前缀和剩余部分之间的分隔符 */
	public final static String SEPARATOR = ".";


	private boolean usePrefix = false;

	private String advisorBeanNamePrefix;


	/**
	 * 设置是否仅包含bean名称中具有特定前缀的切面.
	 * <p>默认是 {@code false}, 包括{@code Advisor}类型的所有bean.
	 */
	public void setUsePrefix(boolean usePrefix) {
		this.usePrefix = usePrefix;
	}

	/**
	 * 返回是否仅包含bean名称中具有特定前缀的切面.
	 */
	public boolean isUsePrefix() {
		return this.usePrefix;
	}

	/**
	 * 设置bean名称的前缀，使其包含在此对象的自动代理中.
	 * 应设置此前缀以避免循环引用. 默认值是此对象的bean名称+点.
	 * 
	 * @param advisorBeanNamePrefix 排除前缀
	 */
	public void setAdvisorBeanNamePrefix(String advisorBeanNamePrefix) {
		this.advisorBeanNamePrefix = advisorBeanNamePrefix;
	}

	/**
	 * 返回bean名称的前缀，这将导致它们被包含在此对象的自动代理中.
	 */
	public String getAdvisorBeanNamePrefix() {
		return this.advisorBeanNamePrefix;
	}

	@Override
	public void setBeanName(String name) {
		// 如果未设置基础结构bean名称前缀, 覆盖它.
		if (this.advisorBeanNamePrefix == null) {
			this.advisorBeanNamePrefix = name + SEPARATOR;
		}
	}


	/**
	 * 考虑具有指定前缀的{@code Advisor} bean是否合格, 如果激活.
	 */
	@Override
	protected boolean isEligibleAdvisorBean(String beanName) {
		return (!isUsePrefix() || beanName.startsWith(getAdvisorBeanNamePrefix()));
	}
}
