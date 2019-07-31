package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * 基于Netty 4的{@link ClientHttpResponse}实现.
 */
class Netty4ClientHttpResponse extends AbstractClientHttpResponse {

	private final ChannelHandlerContext context;

	private final FullHttpResponse nettyResponse;

	private final ByteBufInputStream body;

	private volatile HttpHeaders headers;


	public Netty4ClientHttpResponse(ChannelHandlerContext context, FullHttpResponse nettyResponse) {
		Assert.notNull(context, "ChannelHandlerContext must not be null");
		Assert.notNull(nettyResponse, "FullHttpResponse must not be null");
		this.context = context;
		this.nettyResponse = nettyResponse;
		this.body = new ByteBufInputStream(this.nettyResponse.content());
		this.nettyResponse.retain();
	}


	@Override
	@SuppressWarnings("deprecation")
	public int getRawStatusCode() throws IOException {
		return this.nettyResponse.getStatus().code();
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getStatusText() throws IOException {
		return this.nettyResponse.getStatus().reasonPhrase();
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			HttpHeaders headers = new HttpHeaders();
			for (Map.Entry<String, String> entry : this.nettyResponse.headers()) {
				headers.add(entry.getKey(), entry.getValue());
			}
			this.headers = headers;
		}
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.body;
	}

	@Override
	public void close() {
		this.nettyResponse.release();
		this.context.close();
	}

}
