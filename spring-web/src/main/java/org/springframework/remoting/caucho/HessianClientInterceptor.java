package org.springframework.remoting.caucho;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.net.MalformedURLException;

import com.caucho.hessian.HessianException;
import com.caucho.hessian.client.HessianConnectionException;
import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.SerializerFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.remoting.support.UrlBasedRemoteAccessor;
import org.springframework.util.Assert;

/**
 * 用于访问Hessian服务的{@link org.aopalliance.intercept.MethodInterceptor}.
 * 通过用户名和密码支持身份验证.
 * 服务URL必须是公开Hessian服务的HTTP URL.
 *
 * <p>Hessian是一种轻量级的二进制RPC协议.
 * For information on Hessian, see the
 * <a href="http://www.caucho.com/hessian">Hessian website</a>
 * <b>Note: 从Spring 4.0开始, 这个导出器需要Hessian 4.0或更高版本.</b>
 *
 * <p>Note: 使用Spring的{@link HessianServiceExporter}不需要使用此代理工厂访问服务, 因为不涉及特殊处理.
 * 因此, 还可以使用Caucho的{@link com.caucho.hessian.server.HessianServlet}访问已导出的服务.
 */
public class HessianClientInterceptor extends UrlBasedRemoteAccessor implements MethodInterceptor {

	private HessianProxyFactory proxyFactory = new HessianProxyFactory();

	private Object hessianProxy;


	/**
	 * 设置要使用的HessianProxyFactory实例.
	 * 如果未指定, 将创建默认的HessianProxyFactory.
	 * <p>允许使用外部配置的工厂实例, 特别是自定义的HessianProxyFactory子类.
	 */
	public void setProxyFactory(HessianProxyFactory proxyFactory) {
		this.proxyFactory = (proxyFactory != null ? proxyFactory : new HessianProxyFactory());
	}

	/**
	 * 指定要使用的Hessian SerializerFactory.
	 * <p>这通常作为{@code com.caucho.hessian.io.SerializerFactory}类型的内部bean定义传入, 并应用了自定义bean属性值.
	 */
	public void setSerializerFactory(SerializerFactory serializerFactory) {
		this.proxyFactory.setSerializerFactory(serializerFactory);
	}

	/**
	 * 设置是否为每个序列化集合发送Java集合类型. 默认"true".
	 */
	public void setSendCollectionType(boolean sendCollectionType) {
		this.proxyFactory.getSerializerFactory().setSendCollectionType(sendCollectionType);
	}

	/**
	 * 设置是否允许非可序列化类型作为Hessian参数和返回值. 默认"true".
	 */
	public void setAllowNonSerializable(boolean allowNonSerializable) {
		this.proxyFactory.getSerializerFactory().setAllowNonSerializable(allowNonSerializable);
	}

	/**
	 * 设置是否应为远程调用启用重载方法.
	 * 默认"false".
	 */
	public void setOverloadEnabled(boolean overloadEnabled) {
		this.proxyFactory.setOverloadEnabled(overloadEnabled);
	}

	/**
	 * 设置此工厂用于访问远程服务的用户名.
	 * 默认无.
	 * <p>用户名将由Hessian通过HTTP Basic Authentication发送.
	 */
	public void setUsername(String username) {
		this.proxyFactory.setUser(username);
	}

	/**
	 * 设置此工厂用于访问远程服务的密码.
	 * 默认无.
	 * <p>密码将由Hessian通过HTTP Basic Authentication发送.
	 */
	public void setPassword(String password) {
		this.proxyFactory.setPassword(password);
	}

	/**
	 * 设置是否应启用Hessian的调试模式.
	 * 默认"false".
	 */
	public void setDebug(boolean debug) {
		this.proxyFactory.setDebug(debug);
	}

	/**
	 * 设置是否使用chunked post发送Hessian请求.
	 */
	public void setChunkedPost(boolean chunkedPost) {
		this.proxyFactory.setChunkedPost(chunkedPost);
	}

	/**
	 * 指定用于Hessian客户端的自定义HessianConnectionFactory.
	 */
	public void setConnectionFactory(HessianConnectionFactory connectionFactory) {
		this.proxyFactory.setConnectionFactory(connectionFactory);
	}

	/**
	 * 设置用于Hessian客户端的套接字连接超时.
	 */
	public void setConnectTimeout(long timeout) {
		this.proxyFactory.setConnectTimeout(timeout);
	}

	/**
	 * 设置等待Hessian服务回复时使用的超时.
	 */
	public void setReadTimeout(long timeout) {
		this.proxyFactory.setReadTimeout(timeout);
	}

	/**
	 * 设置是否应使用Hessian协议的版本2来解析请求和回复. 默认"false".
	 */
	public void setHessian2(boolean hessian2) {
		this.proxyFactory.setHessian2Request(hessian2);
		this.proxyFactory.setHessian2Reply(hessian2);
	}

	/**
	 * 设置是否应使用Hessian协议的版本2来解析请求. 默认"false".
	 */
	public void setHessian2Request(boolean hessian2) {
		this.proxyFactory.setHessian2Request(hessian2);
	}

	/**
	 * 设置是否应使用Hessian协议的版本2来解析回复. 默认"false".
	 */
	public void setHessian2Reply(boolean hessian2) {
		this.proxyFactory.setHessian2Reply(hessian2);
	}


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		prepare();
	}

	/**
	 * 初始化此拦截器的Hessian代理.
	 * 
	 * @throws RemoteLookupFailureException 如果服务URL无效
	 */
	public void prepare() throws RemoteLookupFailureException {
		try {
			this.hessianProxy = createHessianProxy(this.proxyFactory);
		}
		catch (MalformedURLException ex) {
			throw new RemoteLookupFailureException("Service URL [" + getServiceUrl() + "] is invalid", ex);
		}
	}

	/**
	 * 创建由此拦截器包装的Hessian代理.
	 * 
	 * @param proxyFactory 要使用的代理工厂
	 * 
	 * @return Hessian代理
	 * @throws MalformedURLException 如果由代理工厂抛出
	 */
	protected Object createHessianProxy(HessianProxyFactory proxyFactory) throws MalformedURLException {
		Assert.notNull(getServiceInterface(), "'serviceInterface' is required");
		return proxyFactory.create(getServiceInterface(), getServiceUrl(), getBeanClassLoader());
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (this.hessianProxy == null) {
			throw new IllegalStateException("HessianClientInterceptor is not properly initialized - " +
					"invoke 'prepare' before attempting any operations");
		}

		ClassLoader originalClassLoader = overrideThreadContextClassLoader();
		try {
			return invocation.getMethod().invoke(this.hessianProxy, invocation.getArguments());
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			// Hessian 4.0检查: 另一层InvocationTargetException.
			if (targetEx instanceof InvocationTargetException) {
				targetEx = ((InvocationTargetException) targetEx).getTargetException();
			}
			if (targetEx instanceof HessianConnectionException) {
				throw convertHessianAccessException(targetEx);
			}
			else if (targetEx instanceof HessianException || targetEx instanceof HessianRuntimeException) {
				Throwable cause = targetEx.getCause();
				throw convertHessianAccessException(cause != null ? cause : targetEx);
			}
			else if (targetEx instanceof UndeclaredThrowableException) {
				UndeclaredThrowableException utex = (UndeclaredThrowableException) targetEx;
				throw convertHessianAccessException(utex.getUndeclaredThrowable());
			}
			else {
				throw targetEx;
			}
		}
		catch (Throwable ex) {
			throw new RemoteProxyFailureException(
					"Failed to invoke Hessian proxy for remote service [" + getServiceUrl() + "]", ex);
		}
		finally {
			resetThreadContextClassLoader(originalClassLoader);
		}
	}

	/**
	 * 将给定的Hessian访问异常转换为适当的Spring RemoteAccessException.
	 * 
	 * @param ex 要转换的异常
	 * 
	 * @return 要抛出的RemoteAccessException
	 */
	protected RemoteAccessException convertHessianAccessException(Throwable ex) {
		if (ex instanceof HessianConnectionException || ex instanceof ConnectException) {
			return new RemoteConnectFailureException(
					"Cannot connect to Hessian remote service at [" + getServiceUrl() + "]", ex);
		}
		else {
			return new RemoteAccessException(
				"Cannot access Hessian remote service at [" + getServiceUrl() + "]", ex);
		}
	}

}
