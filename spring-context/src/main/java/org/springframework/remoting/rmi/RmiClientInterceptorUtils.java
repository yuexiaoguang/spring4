package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.StubNotFoundException;
import java.rmi.UnknownHostException;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_RESPONSE;
import org.omg.CORBA.SystemException;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.util.ReflectionUtils;

/**
 * 用于在RMI客户端中执行调用的分解方法.
 * 可以处理在RMI stub上工作的RMI和非RMI服务接口.
 *
 * <p>Note: 这是一个SPI类, 不适合应用程序使用.
 */
public abstract class RmiClientInterceptorUtils {

	private static final Log logger = LogFactory.getLog(RmiClientInterceptorUtils.class);


	/**
	 * 在给定的RMI stub上执行原始方法调用, 使反射异常按原样进行.
	 * 
	 * @param invocation the AOP MethodInvocation
	 * @param stub the RMI stub
	 * 
	 * @return 调用结果
	 * @throws InvocationTargetException 如果被反射抛出
	 */
	public static Object invokeRemoteMethod(MethodInvocation invocation, Object stub)
			throws InvocationTargetException {

		Method method = invocation.getMethod();
		try {
			if (method.getDeclaringClass().isInstance(stub)) {
				// directly implemented
				return method.invoke(stub, invocation.getArguments());
			}
			else {
				// not directly implemented
				Method stubMethod = stub.getClass().getMethod(method.getName(), method.getParameterTypes());
				return stubMethod.invoke(stub, invocation.getArguments());
			}
		}
		catch (InvocationTargetException ex) {
			throw ex;
		}
		catch (NoSuchMethodException ex) {
			throw new RemoteProxyFailureException("No matching RMI stub method found for: " + method, ex);
		}
		catch (Throwable ex) {
			throw new RemoteProxyFailureException("Invocation of RMI stub method failed: " + method, ex);
		}
	}

	/**
	 * 包装远程访问期间发生的给定异常为RemoteException 或Spring RemoteAccessException (如果方法签名不支持RemoteException).
	 * <p>仅针对远程访问异常调用此方法, 而不是针对目标服务本身抛出的异常!
	 * 
	 * @param method 调用的方法
	 * @param ex 发生的异常, 用作RemoteAccessException或RemoteException的原因
	 * @param message RemoteAccessException或RemoteException的消息
	 * 
	 * @return 抛出给调用者的异常
	 */
	public static Exception convertRmiAccessException(Method method, Throwable ex, String message) {
		if (logger.isDebugEnabled()) {
			logger.debug(message, ex);
		}
		if (ReflectionUtils.declaresException(method, RemoteException.class)) {
			return new RemoteException(message, ex);
		}
		else {
			return new RemoteAccessException(message, ex);
		}
	}

	/**
	 * 如果方法签名不支持RemoteException, 则将远程访问期间发生的给定RemoteException转换为Spring的RemoteAccessException.
	 * 否则, 返回原始的RemoteException.
	 * 
	 * @param method 调用的方法
	 * @param ex 发生的RemoteException
	 * @param serviceName 服务的名称 (用于调试)
	 * 
	 * @return 抛出给调用者的异常
	 */
	public static Exception convertRmiAccessException(Method method, RemoteException ex, String serviceName) {
		return convertRmiAccessException(method, ex, isConnectFailure(ex), serviceName);
	}

	/**
	 * 如果方法签名不支持RemoteException，则将远程访问期间发生的给定RemoteException转换为Spring的RemoteAccessException.
	 * 否则, 返回原始的RemoteException.
	 * 
	 * @param method 调用的方法
	 * @param ex 发生的RemoteException
	 * @param isConnectFailure 是否应将给定的异常视为连接失败
	 * @param serviceName 服务的名称 (用于调试)
	 * 
	 * @return 抛出给调用者的异常
	 */
	public static Exception convertRmiAccessException(
			Method method, RemoteException ex, boolean isConnectFailure, String serviceName) {

		if (logger.isDebugEnabled()) {
			logger.debug("Remote service [" + serviceName + "] threw exception", ex);
		}
		if (ReflectionUtils.declaresException(method, ex.getClass())) {
			return ex;
		}
		else {
			if (isConnectFailure) {
				return new RemoteConnectFailureException("Could not connect to remote service [" + serviceName + "]", ex);
			}
			else {
				return new RemoteAccessException("Could not access remote service [" + serviceName + "]", ex);
			}
		}
	}

	/**
	 * 确定给定的RMI异常是否表示连接失败.
	 * <p>将RMI的 ConnectException, ConnectIOException, UnknownHostException, NoSuchObjectException,
	 * StubNotFoundException视为连接失败.
	 * 
	 * @param ex 要检查的RMI异常
	 * 
	 * @return 是否应将异常视为连接失败
	 */
	public static boolean isConnectFailure(RemoteException ex) {
		return (ex instanceof ConnectException || ex instanceof ConnectIOException ||
				ex instanceof UnknownHostException || ex instanceof NoSuchObjectException ||
				ex instanceof StubNotFoundException || ex.getCause() instanceof SocketException ||
				isCorbaConnectFailure(ex.getCause()));
	}

	/**
	 * 检查给定的RMI异常根本原因是否表示CORBA连接失败.
	 * <p>这与IBM JVM相关, 特别是对于WebSphere EJB客户端.
	 * <p>See the
	 * <a href="http://www.redbooks.ibm.com/Redbooks.nsf/RedbookAbstracts/tips0243.html">IBM website</code>
	 * for details.
	 * 
	 * @param ex 要检查的RMI异常
	 */
	private static boolean isCorbaConnectFailure(Throwable ex) {
		return ((ex instanceof COMM_FAILURE || ex instanceof NO_RESPONSE) &&
				((SystemException) ex).completed == CompletionStatus.COMPLETED_NO);
	}

}
