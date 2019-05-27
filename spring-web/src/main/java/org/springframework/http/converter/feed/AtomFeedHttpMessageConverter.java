package org.springframework.http.converter.feed;

import com.rometools.rome.feed.atom.Feed;

import org.springframework.http.MediaType;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write Atom feeds. Specifically, this converter can handle {@link Feed}
 * objects from the <a href="https://github.com/rometools/rome">ROME</a> project.
 *
 * <p>><b>NOTE: As of Spring 4.1, this is based on the {@code com.rometools}
 * variant of ROME, version 1.5. Please upgrade your build dependency.</b>
 *
 * <p>By default, this converter reads and writes the media type ({@code application/atom+xml}).
 * This can be overridden through the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 */
public class AtomFeedHttpMessageConverter extends AbstractWireFeedHttpMessageConverter<Feed> {

	public AtomFeedHttpMessageConverter() {
		super(new MediaType("application", "atom+xml"));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return Feed.class.isAssignableFrom(clazz);
	}

}
