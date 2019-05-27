package org.springframework.aop.framework.autoproxy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.TargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * 自动代理创建程序，通过名称列表标识要代理的bean.
 * 检查直接，“xxx *”和“* xxx”匹配.
 *
 * <p>通常, 将指定要应用于所有已标识bean的拦截器名称列表, 通过"interceptorNames"属性.
 */
@SuppressWarnings("serial")
public class BeanNameAutoProxyCreator extends AbstractAutoProxyCreator {

	private List<String> beanNames;


	/**
	 * 设置应自动包装代理的bean的名称.
	 * 名称可以通过以 "*"结尾来指定要匹配的前缀, e.g. "myBean,tx*" 将匹配名为“myBean”的bean和名称以“tx”开头的所有bean.
	 * <p><b>NOTE:</b> 如果是FactoryBean, 只有FactoryBean创建的对象才会被代理. 此默认行为适用于Spring 2.0.
	 * 如果您打算代理FactoryBean实例本身 (一个罕见的用例, 但Spring 1.2的默认行为), 指定FactoryBean的bean名称,
	 * 包括factory-bean前缀 "&": e.g. "&myFactoryBean".
	 */
	public void setBeanNames(String... beanNames) {
		Assert.notEmpty(beanNames, "'beanNames' must not be empty");
		this.beanNames = new ArrayList<String>(beanNames.length);
		for (String mappedName : beanNames) {
			this.beanNames.add(StringUtils.trimWhitespace(mappedName));
		}
	}


	/**
	 * 如果bean名称在配置的名称列表中，则标识为要代理的bean.
	 */
	@Override
	protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource targetSource) {
		if (this.beanNames != null) {
			for (String mappedName : this.beanNames) {
				if (FactoryBean.class.isAssignableFrom(beanClass)) {
					if (!mappedName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
						continue;
					}
					mappedName = mappedName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
				}
				if (isMatch(beanName, mappedName)) {
					return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
				}
				BeanFactory beanFactory = getBeanFactory();
				if (beanFactory != null) {
					String[] aliases = beanFactory.getAliases(beanName);
					for (String alias : aliases) {
						if (isMatch(alias, mappedName)) {
							return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
						}
					}
				}
			}
		}
		return DO_NOT_PROXY;
	}

	/**
	 * 如果给定的bean名称与映射的名称匹配，则返回.
	 * <p>默认实现检查 "xxx*", "*xxx", "*xxx*"匹配, 以及直接相等. 可以在子类中重写.
	 * 
	 * @param beanName 要检查的bean名称
	 * @param mappedName 已配置的名称列表中的名称
	 * 
	 * @return 如果名字匹配
	 */
	protected boolean isMatch(String beanName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, beanName);
	}
}
