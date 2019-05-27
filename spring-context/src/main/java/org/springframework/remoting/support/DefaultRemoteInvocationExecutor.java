package org.springframework.remoting.support;

import java.lang.reflect.InvocationTargetException;

import org.springframework.util.Assert;

/**
 * {@link RemoteInvocationExecutor}接口的默认实现.
 * 只需委托给{@link RemoteInvocation}的调用方法.
 */
public class DefaultRemoteInvocationExecutor implements RemoteInvocationExecutor {

	@Override
	public Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException{

		Assert.notNull(invocation, "RemoteInvocation must not be null");
		Assert.notNull(targetObject, "Target object must not be null");
		return invocation.invoke(targetObject);
	}

}
