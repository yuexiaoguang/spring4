package org.springframework.web.socket.sockjs.transport.handler;

import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.TransportHandler;

/**
 * {@link TransportHandler}实现的通用基类.
 */
public abstract class AbstractTransportHandler implements TransportHandler {

	protected static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	protected final Log logger = LogFactory.getLog(getClass());

	private SockJsServiceConfig serviceConfig;


	@Override
	public void initialize(SockJsServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public SockJsServiceConfig getServiceConfig() {
		return this.serviceConfig;
	}

}
