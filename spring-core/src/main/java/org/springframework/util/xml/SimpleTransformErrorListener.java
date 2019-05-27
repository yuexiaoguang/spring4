package org.springframework.util.xml;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;

/**
 * 简单的{@code javax.xml.transform.ErrorListener}实现:
 * 使用给定的Commons Logging logger实例记录警告, 并重新抛出错误以停止XML转换.
 */
public class SimpleTransformErrorListener implements ErrorListener {

	private final Log logger;


	/**
	 * 为给定的Commons Logging logger实例创建一个新的SimpleTransformErrorListener.
	 */
	public SimpleTransformErrorListener(Log logger) {
		this.logger = logger;
	}


	@Override
	public void warning(TransformerException ex) throws TransformerException {
		logger.warn("XSLT transformation warning", ex);
	}

	@Override
	public void error(TransformerException ex) throws TransformerException {
		logger.error("XSLT transformation error", ex);
	}

	@Override
	public void fatalError(TransformerException ex) throws TransformerException {
		throw ex;
	}

}
