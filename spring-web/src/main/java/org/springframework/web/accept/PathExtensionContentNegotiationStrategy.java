package org.springframework.web.accept;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 一个{@code ContentNegotiationStrategy}, 它将请求路径中的文件扩展名解析为用于查找媒体类型的键.
 *
 * <p>如果在提供给构造函数的显式注册中找不到文件扩展名, 则Java Activation Framework (JAF)将用作回退机制.
 *
 * <p>自动检测并启用JAF, 但{@link #setUseJaf(boolean)}属性可能设置为false.
 */
public class PathExtensionContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

	private static final boolean JAF_PRESENT = ClassUtils.isPresent("javax.activation.FileTypeMap",
			PathExtensionContentNegotiationStrategy.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(PathExtensionContentNegotiationStrategy.class);

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private boolean useJaf = true;

	private boolean ignoreUnknownExtensions = true;


	/**
	 * 创建一个没有任何映射的实例.
	 * 如果通过Java Activation框架解析出任何扩展名, 则可以稍后添加映射.
	 */
	public PathExtensionContentNegotiationStrategy() {
		this(null);
	}

	/**
	 * 使用给定的文件扩展名和媒体类型映射创建实例.
	 */
	public PathExtensionContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
		this.urlPathHelper.setUrlDecode(false);
	}


	/**
	 * 配置{@code UrlPathHelper}以在{@link #getMediaTypeKey}中使用, 以便为目标请求URL路径派生查找路径.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 是否使用Java Activation Framework查找文件扩展名.
	 * <p>默认"true", 但取决于存在的JAF.
	 */
	public void setUseJaf(boolean useJaf) {
		this.useJaf = useJaf;
	}

	/**
	 * 是否忽略具有未知文件扩展名的请求.
	 * 设置为{@code false}将导致{@code HttpMediaTypeNotAcceptableException}.
	 * <p>默认{@code true}.
	 */
	public void setIgnoreUnknownExtensions(boolean ignoreUnknownExtensions) {
		this.ignoreUnknownExtensions = ignoreUnknownExtensions;
	}


	@Override
	protected String getMediaTypeKey(NativeWebRequest webRequest) {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			logger.warn("An HttpServletRequest is required to determine the media type key");
			return null;
		}
		String path = this.urlPathHelper.getLookupPathForRequest(request);
		String extension = UriUtils.extractFileExtension(path);
		return (StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ENGLISH) : null);
	}

	@Override
	protected MediaType handleNoMatch(NativeWebRequest webRequest, String extension)
			throws HttpMediaTypeNotAcceptableException {

		if (this.useJaf && JAF_PRESENT) {
			MediaType mediaType = ActivationMediaTypeFactory.getMediaType("file." + extension);
			if (mediaType != null && !MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
				return mediaType;
			}
		}
		if (this.ignoreUnknownExtensions) {
			return null;
		}
		throw new HttpMediaTypeNotAcceptableException(getAllMediaTypes());
	}

	/**
	 * 将文件扩展名解析为{@link MediaType}.
	 * 该方法首先查找显式注册的文件扩展名, 然后返回JAF.
	 * 
	 * @param resource 要查找的资源
	 * 
	 * @return 扩展名的MediaType, 或{@code null}
	 */
	public MediaType getMediaTypeForResource(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		MediaType mediaType = null;
		String filename = resource.getFilename();
		String extension = StringUtils.getFilenameExtension(filename);
		if (extension != null) {
			mediaType = lookupMediaType(extension);
		}
		if (mediaType == null && JAF_PRESENT) {
			mediaType = ActivationMediaTypeFactory.getMediaType(filename);
		}
		if (MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
			mediaType = null;
		}
		return mediaType;
	}


	/**
	 * 内部类, 以避免对JAF的硬编码依赖.
	 */
	private static class ActivationMediaTypeFactory {

		private static final FileTypeMap fileTypeMap;

		static {
			fileTypeMap = initFileTypeMap();
		}

		/**
		 * 从spring-context-support模块中查找扩展的 mime.types.
		 */
		private static FileTypeMap initFileTypeMap() {
			Resource resource = new ClassPathResource("org/springframework/mail/javamail/mime.types");
			if (resource.exists()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Loading JAF FileTypeMap from " + resource);
				}
				InputStream inputStream = null;
				try {
					inputStream = resource.getInputStream();
					return new MimetypesFileTypeMap(inputStream);
				}
				catch (IOException ex) {
					// ignore
				}
				finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						}
						catch (IOException ex) {
							// ignore
						}
					}
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Loading default Java Activation Framework FileTypeMap");
			}
			return FileTypeMap.getDefaultFileTypeMap();
		}

		public static MediaType getMediaType(String filename) {
			String mediaType = fileTypeMap.getContentType(filename);
			return (StringUtils.hasText(mediaType) ? MediaType.parseMediaType(mediaType) : null);
		}
	}

}
