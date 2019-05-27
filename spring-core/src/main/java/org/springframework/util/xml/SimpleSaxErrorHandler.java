package org.springframework.util.xml;

import org.apache.commons.logging.Log;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 简单的{@code org.xml.sax.ErrorHandler}实现:
 * 使用给定的 Commons Logging logger实例记录警告, 并重新抛出错误以停止XML转换.
 */
public class SimpleSaxErrorHandler implements ErrorHandler {

	private final Log logger;


	/**
	 * 为给定的 Commons Logging logger实例创建一个新的SimpleSaxErrorHandler.
	 */
	public SimpleSaxErrorHandler(Log logger) {
		this.logger = logger;
	}


	@Override
	public void warning(SAXParseException ex) throws SAXException {
		logger.warn("Ignored XML validation warning", ex);
	}

	@Override
	public void error(SAXParseException ex) throws SAXException {
		throw ex;
	}

	@Override
	public void fatalError(SAXParseException ex) throws SAXException {
		throw ex;
	}

}
