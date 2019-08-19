package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 处理{@link WebAsyncTask}类型的返回值.
 */
public class AsyncTaskMethodReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

	private final BeanFactory beanFactory;


	public AsyncTaskMethodReturnValueHandler(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return WebAsyncTask.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		return (returnValue != null && returnValue instanceof WebAsyncTask);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		WebAsyncTask<?> webAsyncTask = (WebAsyncTask<?>) returnValue;
		webAsyncTask.setBeanFactory(this.beanFactory);
		WebAsyncUtils.getAsyncManager(webRequest).startCallableProcessing(webAsyncTask, mavContainer);
	}

}
