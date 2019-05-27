package org.springframework.aop.target;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.aop.TargetSource}实现的基类,
 * 基于Spring {@link org.springframework.beans.factory.BeanFactory}, 委托给Spring管理的bean实例.
 *
 * <p>例如，子类可以创建原型实例或延迟地访问单例目标.
 * 有关具体策略，请参阅{@link LazyInitTargetSource}和{@link AbstractPrototypeBasedTargetSource}的子类.
 *
 * <p>基于BeanFactory的TargetSources是可序列化的. 这涉及断开当前目标并变成{@link SingletonTargetSource}.
 */
public abstract class AbstractBeanFactoryBasedTargetSource implements TargetSource, BeanFactoryAware, Serializable {

	/** use serialVersionUID from Spring 1.2.7 for interoperability */
	private static final long serialVersionUID = -4721607536018568393L;


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 将在每次调用时创建的目标bean的名称 */
	private String targetBeanName;

	/** Class of the target */
	private volatile Class<?> targetClass;

	/**
	 * 拥有此TargetSource的BeanFactory. 需要保留此引用，以便我们可以根据需要创建新的原型实例.
	 */
	private BeanFactory beanFactory;


	/**
	 * 在工厂中设置目标bean的名称.
	 * <p>目标bean不应该是单例, 否则将始终从工厂获得相同的实例, 导致与{@link SingletonTargetSource}提供的行为相同.
	 * 
	 * @param targetBeanName 拥有此拦截器的BeanFactory中目标bean的名称
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * 返回工厂中目标bean的名称.
	 */
	public String getTargetBeanName() {
		return this.targetBeanName;
	}

	/**
	 * 明确指定目标类, 避免任何类型的目标bean访问 (例如, 避免初始化FactoryBean实例).
	 * <p>默认是自动检测类型, 通过对BeanFactory的{@code getType}调用 (甚至是一个完整的{@code getBean}调用作为回调).
	 */
	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * 设置拥有BeanFactory. 需要保存一个引用，以便我们可以在每次调用时使用{@code getBean}方法.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.targetBeanName == null) {
			throw new IllegalStateException("Property 'targetBeanName' is required");
		}
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回拥有的BeanFactory.
	 */
	public BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	public Class<?> getTargetClass() {
		Class<?> targetClass = this.targetClass;
		if (targetClass != null) {
			return targetClass;
		}
		synchronized (this) {
			// 同步内全面检查, 仅输入一次BeanFactory交互算法...
			targetClass = this.targetClass;
			if (targetClass == null && this.beanFactory != null) {
				// 确定目标bean的类型.
				targetClass = this.beanFactory.getType(this.targetBeanName);
				if (targetClass == null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Getting bean with name '" + this.targetBeanName + "' for type determination");
					}
					Object beanInstance = this.beanFactory.getBean(this.targetBeanName);
					targetClass = beanInstance.getClass();
				}
				this.targetClass = targetClass;
			}
			return targetClass;
		}
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public void releaseTarget(Object target) throws Exception {
		// Nothing to do here.
	}


	/**
	 * 从其他AbstractBeanFactoryBasedTargetSource对象复制配置.
	 * 如果子类希望公开它，则应覆盖它.
	 * 
	 * @param other 从中复制配置的对象
	 */
	protected void copyFrom(AbstractBeanFactoryBasedTargetSource other) {
		this.targetBeanName = other.targetBeanName;
		this.targetClass = other.targetClass;
		this.beanFactory = other.beanFactory;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		AbstractBeanFactoryBasedTargetSource otherTargetSource = (AbstractBeanFactoryBasedTargetSource) other;
		return (ObjectUtils.nullSafeEquals(this.beanFactory, otherTargetSource.beanFactory) &&
				ObjectUtils.nullSafeEquals(this.targetBeanName, otherTargetSource.targetBeanName));
	}

	@Override
	public int hashCode() {
		int hashCode = getClass().hashCode();
		hashCode = 13 * hashCode + ObjectUtils.nullSafeHashCode(this.beanFactory);
		hashCode = 13 * hashCode + ObjectUtils.nullSafeHashCode(this.targetBeanName);
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(" for target bean '").append(this.targetBeanName).append("'");
		if (this.targetClass != null) {
			sb.append(" of type [").append(this.targetClass.getName()).append("]");
		}
		return sb.toString();
	}

}
