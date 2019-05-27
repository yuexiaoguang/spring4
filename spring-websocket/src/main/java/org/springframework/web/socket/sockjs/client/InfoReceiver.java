package org.springframework.web.socket.sockjs.client;

import java.net.URI;

import org.springframework.http.HttpHeaders;

/**
 * A component that can execute the SockJS "Info" request that needs to be
 * performed before the SockJS session starts in order to check server endpoint
 * capabilities such as whether the endpoint permits use of WebSocket.
 *
 * <p>Typically {@link XhrTransport} implementations are also implementations
 * of this contract.
 */
public interface InfoReceiver {

	/**
	 * Perform an HTTP request to the SockJS "Info" URL.
	 * and return the resulting JSON response content, or raise an exception.
	 * <p>Note that as of 4.2 this method accepts a {@code headers} parameter.
	 * @param infoUrl the URL to obtain SockJS server information from
	 * @param headers the headers to use for the request
	 * @return the body of the response
	 */
	String executeInfoRequest(URI infoUrl, HttpHeaders headers);

}
