package org.springframework.aop.framework;

/**
 * 在{@link ProxyCreatorSupport}对象上注册的监听器. 允许在激活和更改增强时接收回调.
 */
public interface AdvisedSupportListener {

	/**
	 * 在创建第一个代理时调用.
	 * 
	 * @param advised AdvisedSupport对象
	 */
	void activated(AdvisedSupport advised);

	/**
	 * 在创建代理后更改增强时调用.
	 * 
	 * @param advised AdvisedSupport对象
	 */
	void adviceChanged(AdvisedSupport advised);

}
