package org.springframework.aop.target;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * 动态{@link org.springframework.aop.TargetSource}实现的基类, 
 * 它们创建新的原型bean实例, 以支持池化, 或每次调用都创建新的实例策略.
 *
 * <p>这样的TargetSource必须在{@link BeanFactory}中运行, 因为它需要调用{@code getBean}方法来创建一个新的原型实例.
 * 因此, 这个基类继承 {@link AbstractBeanFactoryBasedTargetSource}.
 */
@SuppressWarnings("serial")
public abstract class AbstractPrototypeBasedTargetSource extends AbstractBeanFactoryBasedTargetSource {

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);

		// 检查目标bean是否定义为原型.
		if (!beanFactory.isPrototype(getTargetBeanName())) {
			throw new BeanDefinitionStoreException(
					"Cannot use prototype-based TargetSource against non-prototype bean with name '" +
					getTargetBeanName() + "': instances would not be independent");
		}
	}

	/**
	 * 子类应该调用此方法来创建新的原型实例.
	 * 
	 * @throws BeansException 如果bean创建失败
	 */
	protected Object newPrototypeInstance() throws BeansException {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new instance of bean '" + getTargetBeanName() + "'");
		}
		return getBeanFactory().getBean(getTargetBeanName());
	}

	/**
	 * 子类应该调用此方法来销毁过时的原型实例.
	 * 
	 * @param target 要销毁的bean实例
	 */
	protected void destroyPrototypeInstance(Object target) {
		if (logger.isDebugEnabled()) {
			logger.debug("Destroying instance of bean '" + getTargetBeanName() + "'");
		}
		if (getBeanFactory() instanceof ConfigurableBeanFactory) {
			((ConfigurableBeanFactory) getBeanFactory()).destroyBean(getTargetBeanName(), target);
		}
		else if (target instanceof DisposableBean) {
			try {
				((DisposableBean) target).destroy();
			}
			catch (Throwable ex) {
				logger.error("Couldn't invoke destroy method of bean with name '" + getTargetBeanName() + "'", ex);
			}
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		throw new NotSerializableException("A prototype-based TargetSource itself is not deserializable - " +
				"just a disconnected SingletonTargetSource is");
	}

	/**
	 * 在序列化中使用SingletonTargetSource替换此对象. 受保护的，否则不会为子类调用它.
	 * ({@code writeReplace()} 方法必须对要序列化的类可见.)
	 * <p>有了这个方法的实现, 没有必要将此类或子类中的非可序列化字段标记为 transient.
	 */
	protected Object writeReplace() throws ObjectStreamException {
		if (logger.isDebugEnabled()) {
			logger.debug("Disconnecting TargetSource [" + this + "]");
		}
		try {
			// 创建断开连接的 SingletonTargetSource.
			return new SingletonTargetSource(getTarget());
		}
		catch (Exception ex) {
			logger.error("Cannot get target for disconnecting TargetSource [" + this + "]", ex);
			throw new NotSerializableException(
					"Cannot get target for disconnecting TargetSource [" + this + "]: " + ex);
		}
	}

}
