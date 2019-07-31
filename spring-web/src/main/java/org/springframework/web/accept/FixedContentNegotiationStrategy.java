package org.springframework.web.accept;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 返回一个固定的内容类型的{@code ContentNegotiationStrategy}.
 */
public class FixedContentNegotiationStrategy implements ContentNegotiationStrategy {

	private static final Log logger = LogFactory.getLog(FixedContentNegotiationStrategy.class);

	private final List<MediaType> contentType;


	public FixedContentNegotiationStrategy(MediaType contentType) {
		this.contentType = Collections.singletonList(contentType);
	}


	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("Requested media types: " + this.contentType);
		}
		return this.contentType;
	}

}
