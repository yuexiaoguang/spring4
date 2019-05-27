package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.interceptor.AbstractCacheInvoker;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.util.Assert;

/**
 * JSR-107缓存切面的基类, 例如{@link JCacheInterceptor}或AspectJ切面.
 *
 * <p>使用Spring缓存抽象进行与缓存相关的操作.
 * 不需要JSR-107 {@link javax.cache.Cache}或{@link javax.cache.CacheManager}来处理标准的JSR-107缓存注解.
 *
 * <p>用于确定缓存操作的{@link JCacheOperationSource}
 *
 * <p>如果它的{@code JCacheOperationSource}是可序列化的, 则缓存切面是可序列化的.
 */
public class JCacheAspectSupport extends AbstractCacheInvoker implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private JCacheOperationSource cacheOperationSource;

	private boolean initialized = false;

	private CacheResultInterceptor cacheResultInterceptor;

	private CachePutInterceptor cachePutInterceptor;

	private CacheRemoveEntryInterceptor cacheRemoveEntryInterceptor;

	private CacheRemoveAllInterceptor cacheRemoveAllInterceptor;


	public void setCacheOperationSource(JCacheOperationSource cacheOperationSource) {
		Assert.notNull(cacheOperationSource, "JCacheOperationSource must not be null");
		this.cacheOperationSource = cacheOperationSource;
	}

	/**
	 * 返回此缓存切面的CacheOperationSource.
	 */
	public JCacheOperationSource getCacheOperationSource() {
		return this.cacheOperationSource;
	}

	public void afterPropertiesSet() {
		Assert.state(getCacheOperationSource() != null, "The 'cacheOperationSource' property is required: " +
				"If there are no cacheable methods, then don't use a cache aspect.");
		Assert.state(getErrorHandler() != null, "The 'errorHandler' property is required");

		this.cacheResultInterceptor = new CacheResultInterceptor(getErrorHandler());
		this.cachePutInterceptor = new CachePutInterceptor(getErrorHandler());
		this.cacheRemoveEntryInterceptor = new CacheRemoveEntryInterceptor(getErrorHandler());
		this.cacheRemoveAllInterceptor = new CacheRemoveAllInterceptor(getErrorHandler());

		this.initialized = true;
	}


	protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
		// 检查切面是否已启用, 以应对自动拉入AJ的情况
		if (this.initialized) {
			Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
			JCacheOperation<?> operation = getCacheOperationSource().getCacheOperation(method, targetClass);
			if (operation != null) {
				CacheOperationInvocationContext<?> context =
						createCacheOperationInvocationContext(target, args, operation);
				return execute(context, invoker);
			}
		}

		return invoker.invoke();
	}

	@SuppressWarnings("unchecked")
	private CacheOperationInvocationContext<?> createCacheOperationInvocationContext(
			Object target, Object[] args, JCacheOperation<?> operation) {

		return new DefaultCacheInvocationContext<Annotation>(
				(JCacheOperation<Annotation>) operation, target, args);
	}

	@SuppressWarnings("unchecked")
	private Object execute(CacheOperationInvocationContext<?> context, CacheOperationInvoker invoker) {
		CacheOperationInvoker adapter = new CacheOperationInvokerAdapter(invoker);
		BasicOperation operation = context.getOperation();

		if (operation instanceof CacheResultOperation) {
			return this.cacheResultInterceptor.invoke(
					(CacheOperationInvocationContext<CacheResultOperation>) context, adapter);
		}
		else if (operation instanceof CachePutOperation) {
			return this.cachePutInterceptor.invoke(
					(CacheOperationInvocationContext<CachePutOperation>) context, adapter);
		}
		else if (operation instanceof CacheRemoveOperation) {
			return this.cacheRemoveEntryInterceptor.invoke(
					(CacheOperationInvocationContext<CacheRemoveOperation>) context, adapter);
		}
		else if (operation instanceof CacheRemoveAllOperation) {
			return this.cacheRemoveAllInterceptor.invoke(
					(CacheOperationInvocationContext<CacheRemoveAllOperation>) context, adapter);
		}
		else {
			throw new IllegalArgumentException("Cannot handle " + operation);
		}
	}

	/**
	 * 执行底层操作 (通常在缓存未命中的情况下) 并返回调用的结果.
	 * 如果发生异常, 它将被包装在{@code ThrowableWrapper}中:
	 * 可以处理或修改异常, 但<em>必须</em>包装在{@code ThrowableWrapper}中.
	 * 
	 * @param invoker 处理缓存的操作的调用器
	 * 
	 * @return 调用的结果
	 */
	protected Object invokeOperation(CacheOperationInvoker invoker) {
		return invoker.invoke();
	}


	private class CacheOperationInvokerAdapter implements CacheOperationInvoker {

		private final CacheOperationInvoker delegate;

		public CacheOperationInvokerAdapter(CacheOperationInvoker delegate) {
			this.delegate = delegate;
		}

		@Override
		public Object invoke() throws ThrowableWrapper {
			return invokeOperation(this.delegate);
		}
	}

}
