package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

/**
 * 定义一个可以在调用时返回Object实例（可能是共享或独立）的工厂.
 *
 * <p>此接口通常用于封装通用工厂, 该工厂在每次调用时返回某个目标对象的新实例（原型）.
 *
 * <p>此接口类似于{@link FactoryBean}, 但后者的实现通常意味着在{@link BeanFactory}中定义为SPI实例,
 * 而这个类的实现通常意味着作为API提供给其他bean (通过注入).
 * 因此, {@code getObject()} 方法具有不同的异常处理行为.
 */
public interface ObjectFactory<T> {

	/**
	 * 返回此工厂管理的对象的实例（可能是共享的或独立的）.
	 * 
	 * @return 结果实例
	 * @throws BeansException 如果出现创建错误
	 */
	T getObject() throws BeansException;

}
