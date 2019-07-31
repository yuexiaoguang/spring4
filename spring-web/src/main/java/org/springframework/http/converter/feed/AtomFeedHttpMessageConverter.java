package org.springframework.http.converter.feed;

import com.rometools.rome.feed.atom.Feed;

import org.springframework.http.MediaType;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter}的实现, 可以读写Atom feeds.
 * 具体来说, 此转换器可以处理来自<a href="https://github.com/rometools/rome">ROME</a>项目的{@link Feed}对象.
 *
 * <p>><b>NOTE: 从Spring 4.1开始, 这是基于ROME版本1.5的{@code com.rometools}变体. 请升级构建依赖项.</b>
 *
 * <p>默认情况下, 此转换器读取和写入媒体类型 ({@code application/atom+xml}).
 * 这可以通过{@link #setSupportedMediaTypes supportedMediaTypes}属性覆盖.
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
