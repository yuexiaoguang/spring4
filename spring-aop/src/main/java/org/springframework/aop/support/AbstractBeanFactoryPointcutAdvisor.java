package org.springframework.aop.support;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.Assert;

/**
 * 基于BeanFactory的抽象PointcutAdvisor，允许将任何增强配置为对BeanFactory中的Advice bean的引用.
 *
 * <p>指定增强bean的名称, 而不是增强对象本身（如果在BeanFactory中运行）会在初始化时增加松散耦合,
 * 为了在切点实际匹配之前不初始化增强对象.
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanFactoryPointcutAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	private String adviceBeanName;

	private BeanFactory beanFactory;

	private transient volatile Advice advice;

	private transient volatile Object adviceMonitor = new Object();


	/**
	 * 指定此切面应引用的增强bean的名称.
	 * <p>首次访问此切面的增强时, 将获得指定bean的实例.
	 * 这个切面最多只能获得一个advice bean的单个实例, 在切面的生命周期中缓存实例.
	 */
	public void setAdviceBeanName(String adviceBeanName) {
		this.adviceBeanName = adviceBeanName;
	}

	/**
	 * 返回此切面引用的advice Bean的名称.
	 */
	public String getAdviceBeanName() {
		return this.adviceBeanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		resetAdviceMonitor();
	}

	private void resetAdviceMonitor() {
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			this.adviceMonitor = ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
		}
		else {
			this.adviceMonitor = new Object();
		}
	}

	/**
	 * 直接指定目标增强的特定实例, 避免{@link #getAdvice()}中的延迟解析.
	 * @since 3.1
	 */
	public void setAdvice(Advice advice) {
		synchronized (this.adviceMonitor) {
			this.advice = advice;
		}
	}

	@Override
	public Advice getAdvice() {
		Advice advice = this.advice;
		if (advice != null || this.adviceBeanName == null) {
			return advice;
		}

		Assert.state(this.beanFactory != null, "BeanFactory must be set to resolve 'adviceBeanName'");
		if (this.beanFactory.isSingleton(this.adviceBeanName)) {
			// 依靠工厂提供的单例语义.
			advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
			this.advice = advice;
			return advice;
		}
		else {
			// 工厂不保证单例 -> 让我们在本地锁定, 但重用工厂的单例锁, 以防我们的增强bean的延迟依赖关系发生隐式触发单例锁定...
			synchronized (this.adviceMonitor) {
				if (this.advice == null) {
					this.advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
				}
				return this.advice;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": advice ");
		if (this.adviceBeanName != null) {
			sb.append("bean '").append(this.adviceBeanName).append("'");
		}
		else {
			sb.append(this.advice);
		}
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依靠默认序列化, 只是在反序列化后初始化状态.
		ois.defaultReadObject();

		// Initialize transient fields.
		resetAdviceMonitor();
	}

}
