package org.springframework.remoting.jaxws;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import org.springframework.remoting.soap.SoapFaultException;

/**
 * 用于JAX-WS {@link javax.xml.ws.soap.SOAPFaultException}类的Spring SoapFaultException适配器.
 */
@SuppressWarnings("serial")
public class JaxWsSoapFaultException extends SoapFaultException {

	/**
	 * @param original 要包装的原始JAX-WS SOAPFaultException
	 */
	public JaxWsSoapFaultException(SOAPFaultException original) {
		super(original.getMessage(), original);
	}

	/**
	 * 返回包装的JAX-WS SOAPFault.
	 */
	public final SOAPFault getFault() {
		return ((SOAPFaultException) getCause()).getFault();
	}


	@Override
	public String getFaultCode() {
		return getFault().getFaultCode();
	}

	@Override
	public QName getFaultCodeAsQName() {
		return getFault().getFaultCodeAsQName();
	}

	@Override
	public String getFaultString() {
		return getFault().getFaultString();
	}

	@Override
	public String getFaultActor() {
		return getFault().getFaultActor();
	}

}
