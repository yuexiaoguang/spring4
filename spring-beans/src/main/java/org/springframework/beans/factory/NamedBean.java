package org.springframework.beans.factory;

/**
 * {@link BeanNameAware}的对应部分. 返回对象的bean名称.
 *
 * <p>可以引入此接口以避免在与Spring IoC和Spring AOP一起使用的对象中对bean名称的脆弱依赖.
 */
public interface NamedBean {

	/**
	 * 返回此bean在Spring bean工厂中的名称.
	 */
	String getBeanName();

}
