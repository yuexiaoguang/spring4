package org.springframework.cache.interceptor;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * 由 {@link CacheOperationSource}驱动的切面, 用于包含可缓存的方法的缓存增强bean.
 */
@SuppressWarnings("serial")
public class BeanFactoryCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	private CacheOperationSource cacheOperationSource;

	private final CacheOperationSourcePointcut pointcut = new CacheOperationSourcePointcut() {
		@Override
		protected CacheOperationSource getCacheOperationSource() {
			return cacheOperationSource;
		}
	};


	/**
	 * 设置用于查找缓存属性的缓存操作属性源.
	 * 应该通常与缓存拦截器本身上的源引用集相同.
	 */
	public void setCacheOperationSource(CacheOperationSource cacheOperationSource) {
		this.cacheOperationSource = cacheOperationSource;
	}

	/**
	 * 设置用于此切入点的{@link ClassFilter}.
	 * 默认 {@link ClassFilter#TRUE}.
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
