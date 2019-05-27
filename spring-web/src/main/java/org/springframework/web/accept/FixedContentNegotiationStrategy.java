package org.springframework.web.accept;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A {@code ContentNegotiationStrategy} that returns a fixed content type.
 */
public class FixedContentNegotiationStrategy implements ContentNegotiationStrategy {

	private static final Log logger = LogFactory.getLog(FixedContentNegotiationStrategy.class);

	private final List<MediaType> contentType;


	/**
	 * Create an instance with the given content type.
	 */
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
