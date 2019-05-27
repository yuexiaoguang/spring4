package org.springframework.remoting.soap;

import javax.xml.namespace.QName;

import org.springframework.remoting.RemoteInvocationFailureException;

/**
 * RemoteInvocationFailureException子类, 提供SOAP错误的详细信息.
 */
@SuppressWarnings("serial")
public abstract class SoapFaultException extends RemoteInvocationFailureException {

	/**
	 * @param msg the detail message
	 * @param cause 使用的SOAP API的根本原因
	 */
	protected SoapFaultException(String msg, Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 返回SOAP错误代码.
	 */
	public abstract String getFaultCode();

	/**
	 * 将SOAP错误代码作为{@code QName}对象返回.
	 */
	public abstract QName getFaultCodeAsQName();

	/**
	 * 返回描述性的SOAP错误字符串.
	 */
	public abstract String getFaultString();

	/**
	 * 返回导致此错误的actor.
	 */
	public abstract String getFaultActor();

}
