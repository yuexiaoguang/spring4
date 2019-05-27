package org.springframework.beans.factory;

/**
 * 要在bean工厂中知道其bean名称的bean实现的接口.
 * 请注意, 通常不建议对象依赖于其bean名称, 因为这代表了对外部配置的潜在脆弱依赖性,
 * 以及可能不必要的依赖于Spring API.
 *
 * <p>有关所有Bean生命周期方法的列表, see the {@link BeanFactory BeanFactory javadocs}.
 */
public interface BeanNameAware extends Aware {

	/**
	 * 在创建此bean的bean工厂中设置bean的名称.
	 * <p>在普通bean属性的填充之后, 但在init回调之前调用, 例如{@link InitializingBean#afterPropertiesSet()} 或自定义init方法.
	 * 
	 * @param name 工厂中bean的名称.
	 * 请注意, 此名称是工厂中使用的实际bean名称, 可能与最初指定的名称不同:
	 * 特别是对于内部bean名称, 通过附加“#...”后缀, 实际的bean名称可能是唯一的.
	 * 使用 {@link BeanFactoryUtils#originalBeanName(String)}方法提取原始bean名称 (没有后缀).
	 */
	void setBeanName(String name);

}
