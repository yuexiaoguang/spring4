package org.springframework.web.accept;

import java.util.Map;
import javax.servlet.ServletContext;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 扩展{@code PathExtensionContentNegotiationStrategy}, 还使用{@link ServletContext#getMimeType(String)}来解析文件扩展名.
 */
public class ServletPathExtensionContentNegotiationStrategy extends PathExtensionContentNegotiationStrategy {

	private final ServletContext servletContext;


	/**
	 * 通过{@link ServletContext#getMimeType(String)}或通过JAF解析扩展名时, 可以稍后添加映射.
	 */
	public ServletPathExtensionContentNegotiationStrategy(ServletContext context) {
		this(context, null);
	}

	/**
	 * 使用给定的extension-to-MediaType查找创建实例.
	 */
	public ServletPathExtensionContentNegotiationStrategy(
			ServletContext servletContext, Map<String, MediaType> mediaTypes) {

		super(mediaTypes);
		Assert.notNull(servletContext, "ServletContext is required");
		this.servletContext = servletContext;
	}


	/**
	 * 通过{@link ServletContext#getMimeType(String)}解析文件扩展名, 并委托给基类进行潜在的JAF查找.
	 */
	@Override
	protected MediaType handleNoMatch(NativeWebRequest webRequest, String extension)
			throws HttpMediaTypeNotAcceptableException {

		MediaType mediaType = null;
		if (this.servletContext != null) {
			String mimeType = this.servletContext.getMimeType("file." + extension);
			if (StringUtils.hasText(mimeType)) {
				mediaType = MediaType.parseMediaType(mimeType);
			}
		}
		if (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
			MediaType superMediaType = super.handleNoMatch(webRequest, extension);
			if (superMediaType != null) {
				mediaType = superMediaType;
			}
		}
		return mediaType;
	}

	/**
	 * 扩展基类{@link PathExtensionContentNegotiationStrategy#getMediaTypeForResource},
	 * 同时还可以查找ServletContext.
	 * 
	 * @param resource 要查找的资源
	 * 
	 * @return 扩展名的MediaType, 或{@code null}
	 */
	public MediaType getMediaTypeForResource(Resource resource) {
		MediaType mediaType = null;
		if (this.servletContext != null) {
			String mimeType = this.servletContext.getMimeType(resource.getFilename());
			if (StringUtils.hasText(mimeType)) {
				mediaType = MediaType.parseMediaType(mimeType);
			}
		}
		if (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
			MediaType superMediaType = super.getMediaTypeForResource(resource);
			if (superMediaType != null) {
				mediaType = superMediaType;
			}
		}
		return mediaType;
	}

}
