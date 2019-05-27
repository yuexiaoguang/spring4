package org.springframework.aop.target;

import org.springframework.beans.BeansException;

/**
 * {@link org.springframework.aop.TargetSource}实现, 为每个请求创建目标bean的新实例,
 * 在释放时销毁每个实例 (每次请求后).
 *
 * <p>从{@link org.springframework.beans.factory.BeanFactory}中获取bean实例.
 */
@SuppressWarnings("serial")
public class PrototypeTargetSource extends AbstractPrototypeBasedTargetSource {

	/**
	 * 为每个调用获取一个新的原型实例.
	 */
	@Override
	public Object getTarget() throws BeansException {
		return newPrototypeInstance();
	}

	/**
	 * 销毁给定的独立实例.
	 */
	@Override
	public void releaseTarget(Object target) {
		destroyPrototypeInstance(target);
	}

	@Override
	public String toString() {
		return "PrototypeTargetSource for target bean with name '" + getTargetBeanName() + "'";
	}

}
