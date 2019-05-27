package org.springframework.cache.interceptor;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * 代理工厂bean, 用于简化声明式的缓存处理.
 * 这是标准AOP {@link org.springframework.aop.framework.ProxyFactoryBean}的一个方便的替代方案, 具有单独的{@link CacheInterceptor}定义.
 *
 * <p>此类旨在促进声明式的缓存划分:
 * 即, 使用高速缓存代理包装单个目标对象, 代理目标实现的所有接口.
 * 主要存在于第三方框架集成中.
 * <strong>用户应该支持{@code cache:} XML命名空间 {@link org.springframework.cache.annotation.Cacheable @Cacheable}注解.</strong>
 * 有关详细信息, 请参阅Spring参考文档的<a href="http://bit.ly/p9rIvx">declarative annotation-based caching</a>部分.
 */
@SuppressWarnings("serial")
public class CacheProxyFactoryBean extends AbstractSingletonProxyFactoryBean
		implements BeanFactoryAware, SmartInitializingSingleton {

	private final CacheInterceptor cacheInterceptor = new CacheInterceptor();

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * 设置一个或多个源以查找缓存操作.
	 */
	public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
		this.cacheInterceptor.setCacheOperationSources(cacheOperationSources);
	}

	/**
	 * 设置切点, i.e. 根据传递的方法和属性触发{@link CacheInterceptor}的条件调用的bean.
	 * <p>Note: 始终调用其他拦截器.
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.cacheInterceptor.setBeanFactory(beanFactory);
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.cacheInterceptor.afterSingletonsInstantiated();
	}


	@Override
	protected Object createMainInterceptor() {
		this.cacheInterceptor.afterPropertiesSet();
		return new DefaultPointcutAdvisor(this.pointcut, this.cacheInterceptor);
	}

}
