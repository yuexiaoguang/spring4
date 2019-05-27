package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

/**
 * {@link ObjectFactory}的变体, 专为注入点而设计, 允许程序化可选性和宽松的不唯一处理.
 */
public interface ObjectProvider<T> extends ObjectFactory<T> {

	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）.
	 * <p>允许指定显式构造参数, 沿着 {@link BeanFactory#getBean(String, Object...)}的形式.
	 * 
	 * @param args 创建相应实例时使用的参数
	 * 
	 * @return bean的一个实例
	 * @throws BeansException 如果出现创建错误
	 */
	T getObject(Object... args) throws BeansException;

	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）.
	 * 
	 * @return bean的一个实例, 或{@code null}不可用
	 * @throws BeansException 如果出现创建错误
	 */
	T getIfAvailable() throws BeansException;

	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）.
	 * 
	 * @return bean的一个实例, 或{@code null}不可用、不唯一 (i.e. 发现多个候选者, 但都没有被标记为主要的)
	 * @throws BeansException 如果出现创建错误
	 */
	T getIfUnique() throws BeansException;

}
