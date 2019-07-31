package org.springframework.web.accept;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@code ContentNegotiationStrategy}实现的基类, 逐步将请求解析为媒体类型.
 *
 * <p>首先必须从请求(e.g. 文件扩展名, 查询参数)中提取键(e.g. "json", "pdf").
 * 然后必须通过存储此类映射的基类 {@link MappingMediaTypeFileExtensionResolver}将键解析为媒体类型.
 *
 * <p>方法{@link #handleNoMatch}允许子类插入查找媒体类型的其他方式
 * (e.g. 通过Java Activation框架, 或{@link javax.servlet.ServletContext#getMimeType}.
 * 然后通过基类解析的媒体类型被添加到基类{@link MappingMediaTypeFileExtensionResolver}, i.e. 缓存用于新的查找.
 */
public abstract class AbstractMappingContentNegotiationStrategy extends MappingMediaTypeFileExtensionResolver
		implements ContentNegotiationStrategy {

	/**
	 * 文件扩展名到媒体类型的映射.
	 */
	public AbstractMappingContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}


	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
			throws HttpMediaTypeNotAcceptableException {

		return resolveMediaTypeKey(webRequest, getMediaTypeKey(webRequest));
	}

	/**
	 * {@link #resolveMediaTypes(NativeWebRequest)}的替代方法, 接受已经提取的键.
	 */
	public List<MediaType> resolveMediaTypeKey(NativeWebRequest webRequest, String key)
			throws HttpMediaTypeNotAcceptableException {

		if (StringUtils.hasText(key)) {
			MediaType mediaType = lookupMediaType(key);
			if (mediaType != null) {
				handleMatch(key, mediaType);
				return Collections.singletonList(mediaType);
			}
			mediaType = handleNoMatch(webRequest, key);
			if (mediaType != null) {
				addMapping(key, mediaType);
				return Collections.singletonList(mediaType);
			}
		}
		return Collections.emptyList();
	}


	/**
	 * 从请求中提取键以用于查找媒体类型.
	 * 
	 * @return 要查找的键, 或{@code null}
	 */
	protected abstract String getMediaTypeKey(NativeWebRequest request);

	/**
	 * 通过{@link #lookupMediaType}成功解析键时, 覆盖以提供处理.
	 */
	protected void handleMatch(String key, MediaType mediaType) {
	}

	/**
	 * 当通过{@link #lookupMediaType}未能解析键时, 覆盖以提供处理.
	 * 子类可以采取进一步措施来确定媒体类型.
	 * 如果从此方法返回MediaType, 它将被添加到基类的缓存中.
	 */
	protected MediaType handleNoMatch(NativeWebRequest request, String key)
			throws HttpMediaTypeNotAcceptableException {

		return null;
	}

}
