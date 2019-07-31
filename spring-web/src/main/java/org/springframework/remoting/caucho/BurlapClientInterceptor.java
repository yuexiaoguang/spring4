package org.springframework.remoting.caucho;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.net.MalformedURLException;

import com.caucho.burlap.client.BurlapProxyFactory;
import com.caucho.burlap.client.BurlapRuntimeException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.remoting.support.UrlBasedRemoteAccessor;
import org.springframework.util.Assert;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor}用于访问Burlap服务.
 * 通过用户名和密码支持身份验证.
 * 服务URL必须是公开Burlap服务的HTTP URL.
 *
 * <p>Burlap是一种基于XML的轻量级RPC协议.
 * For information on Burlap, see the
 * <a href="http://www.caucho.com/burlap">Burlap website</a>
 *
 * <p>Note: 使用Spring的{@link BurlapServiceExporter}不需要使用此代理工厂访问的服务, 因为不涉及特殊处理.
 * 因此, 还可以使用Caucho的{@link com.caucho.burlap.server.BurlapServlet}访问已导出的服务.
 *
 * @deprecated 从Spring 4.0开始, 由于Burlap几年没有进展 (与其兄弟Hessian形成鲜明对比)
 */
@Deprecated
public class BurlapClientInterceptor extends UrlBasedRemoteAccessor implements MethodInterceptor {

	private BurlapProxyFactory proxyFactory = new BurlapProxyFactory();

	private Object burlapProxy;


	/**
	 * Set the BurlapProxyFactory instance to use.
	 * If not specified, a default BurlapProxyFactory will be created.
	 * <p>Allows to use an externally configured factory instance,
	 * in particular a custom BurlapProxyFactory subclass.
	 */
	public void setProxyFactory(BurlapProxyFactory proxyFactory) {
		this.proxyFactory = (proxyFactory != null ? proxyFactory : new BurlapProxyFactory());
	}

	/**
	 * Set the username that this factory should use to access the remote service.
	 * Default is none.
	 * <p>The username will be sent by Burlap via HTTP Basic Authentication.
	 * @see com.caucho.burlap.client.BurlapProxyFactory#setUser
	 */
	public void setUsername(String username) {
		this.proxyFactory.setUser(username);
	}

	/**
	 * Set the password that this factory should use to access the remote service.
	 * Default is none.
	 * <p>The password will be sent by Burlap via HTTP Basic Authentication.
	 * @see com.caucho.burlap.client.BurlapProxyFactory#setPassword
	 */
	public void setPassword(String password) {
		this.proxyFactory.setPassword(password);
	}

	/**
	 * Set whether overloaded methods should be enabled for remote invocations.
	 * Default is "false".
	 * @see com.caucho.burlap.client.BurlapProxyFactory#setOverloadEnabled
	 */
	public void setOverloadEnabled(boolean overloadEnabled) {
		this.proxyFactory.setOverloadEnabled(overloadEnabled);
	}


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		prepare();
	}

	/**
	 * Initialize the Burlap proxy for this interceptor.
	 * @throws RemoteLookupFailureException if the service URL is invalid
	 */
	public void prepare() throws RemoteLookupFailureException {
		try {
			this.burlapProxy = createBurlapProxy(this.proxyFactory);
		}
		catch (MalformedURLException ex) {
			throw new RemoteLookupFailureException("Service URL [" + getServiceUrl() + "] is invalid", ex);
		}
	}

	/**
	 * Create the Burlap proxy that is wrapped by this interceptor.
	 * @param proxyFactory the proxy factory to use
	 * @return the Burlap proxy
	 * @throws MalformedURLException if thrown by the proxy factory
	 * @see com.caucho.burlap.client.BurlapProxyFactory#create
	 */
	protected Object createBurlapProxy(BurlapProxyFactory proxyFactory) throws MalformedURLException {
		Assert.notNull(getServiceInterface(), "Property 'serviceInterface' is required");
		return proxyFactory.create(getServiceInterface(), getServiceUrl());
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (this.burlapProxy == null) {
			throw new IllegalStateException("BurlapClientInterceptor is not properly initialized - " +
					"invoke 'prepare' before attempting any operations");
		}

		ClassLoader originalClassLoader = overrideThreadContextClassLoader();
		try {
			return invocation.getMethod().invoke(this.burlapProxy, invocation.getArguments());
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			if (targetEx instanceof BurlapRuntimeException) {
				Throwable cause = targetEx.getCause();
				throw convertBurlapAccessException(cause != null ? cause : targetEx);
			}
			else if (targetEx instanceof UndeclaredThrowableException) {
				UndeclaredThrowableException utex = (UndeclaredThrowableException) targetEx;
				throw convertBurlapAccessException(utex.getUndeclaredThrowable());
			}
			else {
				throw targetEx;
			}
		}
		catch (Throwable ex) {
			throw new RemoteProxyFailureException(
					"Failed to invoke Burlap proxy for remote service [" + getServiceUrl() + "]", ex);
		}
		finally {
			resetThreadContextClassLoader(originalClassLoader);
		}
	}

	/**
	 * Convert the given Burlap access exception to an appropriate
	 * Spring RemoteAccessException.
	 * @param ex the exception to convert
	 * @return the RemoteAccessException to throw
	 */
	protected RemoteAccessException convertBurlapAccessException(Throwable ex) {
		if (ex instanceof ConnectException) {
			return new RemoteConnectFailureException(
					"Cannot connect to Burlap remote service at [" + getServiceUrl() + "]", ex);
		}
		else {
			return new RemoteAccessException(
				"Cannot access Burlap remote service at [" + getServiceUrl() + "]", ex);
		}
	}

}
