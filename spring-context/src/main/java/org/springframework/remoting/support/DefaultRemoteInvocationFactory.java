package org.springframework.remoting.support;

import org.aopalliance.intercept.MethodInvocation;

/**
 * {@link RemoteInvocationFactory}接口的默认实现.
 * 只需创建一个新的标准{@link RemoteInvocation}对象.
 */
public class DefaultRemoteInvocationFactory implements RemoteInvocationFactory {

	@Override
	public RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return new RemoteInvocation(methodInvocation);
	}

}
