package org.springframework.aop.target;

/**
 * 简单的{@link org.springframework.aop.TargetSource} 实现,
 * 从{@link org.springframework.beans.factory.BeanFactory}中新获取指定的目标bean.
 *
 * <p>可以获取任何种类的目标bean: 单例, scoped, 原型.
 * 通常用于范围 bean.
 */
@SuppressWarnings("serial")
public class SimpleBeanTargetSource extends AbstractBeanFactoryBasedTargetSource {

	@Override
	public Object getTarget() throws Exception {
		return getBeanFactory().getBean(getTargetBeanName());
	}

}
