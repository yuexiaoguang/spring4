package org.springframework.http.converter.feed;

import com.rometools.rome.feed.rss.Channel;

import org.springframework.http.MediaType;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write RSS feeds. Specifically, this converter can handle {@link Channel}
 * objects from the <a href="https://github.com/rometools/rome">ROME</a> project.
 *
 * <p>><b>NOTE: As of Spring 4.1, this is based on the {@code com.rometools}
 * variant of ROME, version 1.5. Please upgrade your build dependency.</b>
 *
 * <p>By default, this converter reads and writes the media type ({@code application/rss+xml}).
 * This can be overridden through the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 */
public class RssChannelHttpMessageConverter extends AbstractWireFeedHttpMessageConverter<Channel> {

	public RssChannelHttpMessageConverter() {
		super(MediaType.APPLICATION_RSS_XML);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return Channel.class.isAssignableFrom(clazz);
	}

}
