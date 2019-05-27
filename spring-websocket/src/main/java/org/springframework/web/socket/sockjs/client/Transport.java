package org.springframework.web.socket.sockjs.client;

import java.util.List;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * A client-side implementation for a SockJS transport.
 */
public interface Transport {

	/**
	 * Return the SockJS transport types that this transport can be used for.
	 * In particular since from a client perspective there is no difference
	 * between XHR and XHR streaming, an {@code XhrTransport} could do both.
	 */
	List<TransportType> getTransportTypes();

	/**
	 * Connect the transport.
	 * @param request the transport request.
	 * @param webSocketHandler the application handler to delegate lifecycle events to.
	 * @return a future to indicate success or failure to connect.
	 */
	ListenableFuture<WebSocketSession> connect(TransportRequest request, WebSocketHandler webSocketHandler);

}
