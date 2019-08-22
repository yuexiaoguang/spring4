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
 * 用于编码和解码SockJS消息的Jackson 2.6+编解码器.
 *
 * <p>它通过以下方式定制Jackson的默认属性:
 * <ul>
 * <li>禁用{@link MapperFeature#DEFAULT_VIEW_INCLUSION}</li>
 * <li>禁用{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
 * </ul>
 *
 * <p>请注意，Jackson的JSR-310和Joda-Time支持模块将在可用时自动注册 (当Java 8和Joda-Time本身可用时).
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
