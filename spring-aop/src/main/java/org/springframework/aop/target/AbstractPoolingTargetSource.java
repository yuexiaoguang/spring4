package org.springframework.aop.target;

import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;

/**
 * 用于池化{@link org.springframework.aop.TargetSource}实现的抽象基类, 用于维护目标实例池,
 * 为每个方法调用从池中获取和释放目标对象.
 * 此抽象基类独立于具体的池化技术; 有关具体示例, 请参阅子类{@link CommonsPool2TargetSource}.
 *
 * <p>子类必须根据所选对象池实现{@link #getTarget}和{@link #releaseTarget}方法.
 * 从{@link AbstractPrototypeBasedTargetSource}继承的{@link #newPrototypeInstance()}方法可用于创建对象以便将它们放入池中.
 *
 * <p>子类还必须实现{@link PoolingConfig}接口中的一些监视方法. 
 * {@link #getPoolingConfigMixin()}方法通过IntroductionAdvisor在代理对象上提供这些统计信息.
 *
 * <p>此类实现{@link org.springframework.beans.factory.DisposableBean}接口, 以强制子类实现 {@link #destroy()}方法,
 * 关闭他们的对象池.
 */
@SuppressWarnings("serial")
public abstract class AbstractPoolingTargetSource extends AbstractPrototypeBasedTargetSource
		implements PoolingConfig, DisposableBean {

	/** The maximum size of the pool */
	private int maxSize = -1;


	/**
	 * Set the maximum size of the pool.
	 * Default is -1, indicating no size limit.
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * Return the maximum size of the pool.
	 */
	@Override
	public int getMaxSize() {
		return this.maxSize;
	}


	@Override
	public final void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);
		try {
			createPool();
		}
		catch (Throwable ex) {
			throw new BeanInitializationException("Could not create instance pool for TargetSource", ex);
		}
	}


	/**
	 * Create the pool.
	 * @throws Exception 避免对池API进行约束
	 */
	protected abstract void createPool() throws Exception;

	/**
	 * Acquire an object from the pool.
	 * 
	 * @return an object from the pool
	 * @throws Exception 可能需要处理来自池API的受检异常
	 */
	@Override
	public abstract Object getTarget() throws Exception;

	/**
	 * Return the given object to the pool.
	 * 
	 * @param target 必须通过调用{@code getTarget()}从池中获取的对象
	 * 
	 * @throws Exception 允许池API抛出异常
	 */
	@Override
	public abstract void releaseTarget(Object target) throws Exception;


	/**
	 * 返回一个IntroductionAdvisor, 提供有关此对象维护的池的统计信息.
	 */
	public DefaultIntroductionAdvisor getPoolingConfigMixin() {
		DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor(this);
		return new DefaultIntroductionAdvisor(dii, PoolingConfig.class);
	}

}
