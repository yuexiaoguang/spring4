package org.springframework.cache.jcache.interceptor;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * 由{@link JCacheOperationSource}驱动的切面, 用于包含可缓存方法的缓存增强bean.
 */
@SuppressWarnings("serial")
public class BeanFactoryJCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	private JCacheOperationSource cacheOperationSource;

	private final JCacheOperationSourcePointcut pointcut = new JCacheOperationSourcePointcut() {
		@Override
		protected JCacheOperationSource getCacheOperationSource() {
			return cacheOperationSource;
		}
	};

	/**
	 * 设置用于查找缓存属性的缓存操作属性源.
	 * 这应该通常与缓存拦截器本身上的源引用集合相同.
	 */
	public void setCacheOperationSource(JCacheOperationSource cacheOperationSource) {
		this.cacheOperationSource = cacheOperationSource;
	}

	/**
	 * 设置用于此切点的{@link org.springframework.aop.ClassFilter}.
	 * 默认{@link org.springframework.aop.ClassFilter#TRUE}.
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}