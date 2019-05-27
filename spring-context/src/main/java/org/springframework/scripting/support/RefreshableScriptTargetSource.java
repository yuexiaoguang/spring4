package org.springframework.scripting.support;

import org.springframework.aop.target.dynamic.BeanFactoryRefreshableTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * {@link BeanFactoryRefreshableTargetSource}的子类, 用于确定是否需要通过给定的{@link ScriptFactory}进行刷新.
 */
public class RefreshableScriptTargetSource extends BeanFactoryRefreshableTargetSource {

	private final ScriptFactory scriptFactory;

	private final ScriptSource scriptSource;

	private final boolean isFactoryBean;


	/**
	 * @param beanFactory 从中获取脚本bean的BeanFactory
	 * @param beanName 目标bean的名称
	 * @param scriptFactory 要委托的ScriptFactory, 用于确定是否需要刷新
	 * @param scriptSource 脚本定义的ScriptSource
	 * @param isFactoryBean 目标脚本是否定义了FactoryBean
	 */
	public RefreshableScriptTargetSource(BeanFactory beanFactory, String beanName,
			ScriptFactory scriptFactory, ScriptSource scriptSource, boolean isFactoryBean) {

		super(beanFactory, beanName);
		Assert.notNull(scriptFactory, "ScriptFactory must not be null");
		Assert.notNull(scriptSource, "ScriptSource must not be null");
		this.scriptFactory = scriptFactory;
		this.scriptSource = scriptSource;
		this.isFactoryBean = isFactoryBean;
	}


	/**
	 * 通过调用ScriptFactory的{@code requiresScriptedObjectRefresh}方法确定是否需要刷新.
	 */
	@Override
	protected boolean requiresRefresh() {
		return this.scriptFactory.requiresScriptedObjectRefresh(this.scriptSource);
	}

	/**
	 * 获取新的目标对象, 必要时检索FactoryBean.
	 */
	@Override
	protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
		return super.obtainFreshBean(beanFactory,
				(this.isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName));
	}

}
