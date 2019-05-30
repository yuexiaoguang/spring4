package org.springframework.orm.jpa.support;

import java.util.concurrent.Callable;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptorAdapter;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

/**
 * OpenSessionInViewFilter和OpenSessionInViewInterceptor中使用的异步Web请求的拦截器.
 *
 * 确保以下内容:
 * 1) 当"可调用处理"开始时, 会话被绑定/解除绑定
 * 2) 如果异步请求超时, 会话将关闭
 */
class AsyncRequestInterceptor extends CallableProcessingInterceptorAdapter implements DeferredResultProcessingInterceptor {

	private static final Log logger = LogFactory.getLog(AsyncRequestInterceptor.class);

	private final EntityManagerFactory emFactory;

	private final EntityManagerHolder emHolder;

	private volatile boolean timeoutInProgress;


	public AsyncRequestInterceptor(EntityManagerFactory emFactory, EntityManagerHolder emHolder) {
		this.emFactory = emFactory;
		this.emHolder = emHolder;
	}


	@Override
	public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
		bindSession();
	}

	public void bindSession() {
		this.timeoutInProgress = false;
		TransactionSynchronizationManager.bindResource(this.emFactory, this.emHolder);
	}

	@Override
	public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
		TransactionSynchronizationManager.unbindResource(this.emFactory);
	}

	@Override
	public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) {
		this.timeoutInProgress = true;
		return RESULT_NONE;  // 让其他拦截器有机会处理超时
	}

	@Override
	public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
		closeAfterTimeout();
	}

	private void closeAfterTimeout() {
		if (this.timeoutInProgress) {
			logger.debug("Closing JPA EntityManager after async request timeout");
			EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
		}
	}


	// Implementation of DeferredResultProcessingInterceptor methods

	@Override
	public <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult) {
	}

	@Override
	public <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult) {
	}

	@Override
	public <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult, Object result) {
	}

	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) {
		this.timeoutInProgress = true;
		return true;  // 让其他拦截器有机会处理超时
	}

	@Override
	public <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult) {
		closeAfterTimeout();
	}

}
