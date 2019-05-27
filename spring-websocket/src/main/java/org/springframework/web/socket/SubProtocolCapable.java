package org.springframework.web.socket;

import java.util.List;

/**
 * An interface for WebSocket handlers that support sub-protocols as defined in RFC 6455.
 */
public interface SubProtocolCapable {

	/**
	 * Return the list of supported sub-protocols.
	 */
	List<String> getSubProtocols();

}
