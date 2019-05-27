package org.springframework.web.socket.sockjs.frame;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

/**
 * A Jackson 2.6+ codec for encoding and decoding SockJS messages.
 *
 * <p>It customizes Jackson's default properties with the following ones:
 * <ul>
 * <li>{@link MapperFeature#DEFAULT_VIEW_INCLUSION} is disabled</li>
 * <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is disabled</li>
 * </ul>
 *
 * <p>Note that Jackson's JSR-310 and Joda-Time support modules will be registered automatically
 * when available (and when Java 8 and Joda-Time themselves are available, respectively).
 */
public class Jackson2SockJsMessageCodec extends AbstractSockJsMessageCodec {

	private final ObjectMapper objectMapper;


	public Jackson2SockJsMessageCodec() {
		this.objectMapper = Jackson2ObjectMapperBuilder.json().build();
	}

	public Jackson2SockJsMessageCodec(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}


	@Override
	public String[] decode(String content) throws IOException {
		return this.objectMapper.readValue(content, String[].class);
	}

	@Override
	public String[] decodeInputStream(InputStream content) throws IOException {
		return this.objectMapper.readValue(content, String[].class);
	}

	@Override
	protected char[] applyJsonQuoting(String content) {
		return JsonStringEncoder.getInstance().quoteAsString(content);
	}

}
