package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.util.UUID;

import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Container for the base URL of a SockJS endpoint with additional helper methods
 * to derive related SockJS URLs as the {@link #getInfoUrl() info} URL and
 * {@link #getTransportUrl(TransportType) transport} URLs.
 */
public class SockJsUrlInfo {

	private static final IdGenerator idGenerator = new JdkIdGenerator();


	private final URI sockJsUrl;

	private String serverId;

	private String sessionId;

	private UUID uuid;


	public SockJsUrlInfo(URI sockJsUrl) {
		this.sockJsUrl = sockJsUrl;
	}


	public URI getSockJsUrl() {
		return this.sockJsUrl;
	}

	public String getServerId() {
		if (this.serverId == null) {
			this.serverId = String.valueOf(Math.abs(getUuid().getMostSignificantBits()) % 1000);
		}
		return this.serverId;
	}

	public String getSessionId() {
		if (this.sessionId == null) {
			this.sessionId = getUuid().toString().replace("-","");
		}
		return this.sessionId;
	}

	protected UUID getUuid() {
		if (this.uuid == null) {
			this.uuid = idGenerator.generateId();
		}
		return this.uuid;
	}

	public URI getInfoUrl() {
		return UriComponentsBuilder.fromUri(this.sockJsUrl)
				.scheme(getScheme(TransportType.XHR))
				.pathSegment("info")
				.build(true).toUri();
	}

	public URI getTransportUrl(TransportType transportType) {
		return UriComponentsBuilder.fromUri(this.sockJsUrl)
				.scheme(getScheme(transportType))
				.pathSegment(getServerId())
				.pathSegment(getSessionId())
				.pathSegment(transportType.toString())
				.build(true).toUri();
	}

	private String getScheme(TransportType transportType) {
		String scheme = this.sockJsUrl.getScheme();
		if (TransportType.WEBSOCKET.equals(transportType)) {
			if (!scheme.startsWith("ws")) {
				scheme = ("https".equals(scheme) ? "wss" : "ws");
			}
		}
		else {
			if (!scheme.startsWith("http")) {
				scheme = ("wss".equals(scheme) ? "https" : "http");
			}
		}
		return scheme;
	}


	@Override
	public String toString() {
		return "SockJsUrlInfo[url=" + this.sockJsUrl + "]";
	}

}
