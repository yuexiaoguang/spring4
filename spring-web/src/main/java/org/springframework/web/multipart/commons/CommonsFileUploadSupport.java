package org.springframework.web.multipart.commons;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

/**
 * 使用Apache Commons FileUpload 1.2或更高版本的multipart解析器的基类.
 *
 * <p>为multipart请求提供通用配置属性和解析功能, 使用Spring CommonsMultipartFile实例的Map作为上传文件的表示,
 * 并使用基于String的参数Map作为上传表单字段的表示.
 *
 * <p>子类为Servlet或Portlet环境实现具体的解析策略: 分别参见CommonsMultipartResolver 和 CommonsPortletMultipartResolver.
 * 此基类不依赖于这些API中的任何一个, 从而将通用功能分解出来.
 */
public abstract class CommonsFileUploadSupport {

	protected final Log logger = LogFactory.getLog(getClass());

	private final DiskFileItemFactory fileItemFactory;

	private final FileUpload fileUpload;

	private boolean uploadTempDirSpecified = false;

	private boolean preserveFilename = false;


	/**
	 * 使用其对应的FileItemFactory和FileUpload实例实例化新的CommonsFileUploadSupport.
	 */
	public CommonsFileUploadSupport() {
		this.fileItemFactory = newFileItemFactory();
		this.fileUpload = newFileUpload(getFileItemFactory());
	}


	/**
	 * 返回底层{@code org.apache.commons.fileupload.disk.DiskFileItemFactory}实例.
	 * 几乎没有必要访问它.
	 * 
	 * @return 底层DiskFileItemFactory实例
	 */
	public DiskFileItemFactory getFileItemFactory() {
		return this.fileItemFactory;
	}

	/**
	 * 返回底层{@code org.apache.commons.fileupload.FileUpload}实例. 几乎没有必要访问它.
	 * 
	 * @return 底层FileUpload 实例
	 */
	public FileUpload getFileUpload() {
		return this.fileUpload;
	}

	/**
	 * 设置允许上传的最大大小 (以字节为单位).
	 * -1 表示不限制 (默认).
	 * 
	 * @param maxUploadSize 允许的最大上传大小
	 */
	public void setMaxUploadSize(long maxUploadSize) {
		this.fileUpload.setSizeMax(maxUploadSize);
	}

	/**
	 * 设置允许上传的单个文件的最大大小 (以字节为单位).
	 * -1 表示不限制 (默认).
	 * 
	 * @param maxUploadSizePerFile 每个文件的最大上传大小
	 */
	public void setMaxUploadSizePerFile(long maxUploadSizePerFile) {
		this.fileUpload.setFileSizeMax(maxUploadSizePerFile);
	}

	/**
	 * 设置上传写入磁盘之前允许的最大大小 (以字节为单位).
	 * 仍然会收到超过此数量的上传文件, 但它们不会存储在内存中. 根据Commons FileUpload, 默认值为10240.
	 * 
	 * @param maxInMemorySize 允许的最大内存大小
	 */
	public void setMaxInMemorySize(int maxInMemorySize) {
		this.fileItemFactory.setSizeThreshold(maxInMemorySize);
	}

	/**
	 * 设置用于解析请求的默认字符编码, 应用于各个部分的header和表单字段.
	 * 根据Servlet规范, 默认值为ISO-8859-1.
	 * <p>如果请求指定了自身的字符编码, 请求编码将覆盖此设置.
	 * 这也允许在调用{@code ServletRequest.setCharacterEncoding}方法的过滤器中一般覆盖字符编码.
	 * 
	 * @param defaultEncoding 要使用的字符编码
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.fileUpload.setHeaderEncoding(defaultEncoding);
	}

	/**
	 * 确定用于解析请求的默认编码.
	 */
	protected String getDefaultEncoding() {
		String encoding = getFileUpload().getHeaderEncoding();
		if (encoding == null) {
			encoding = WebUtils.DEFAULT_CHARACTER_ENCODING;
		}
		return encoding;
	}

	/**
	 * 设置存储上传文件的临时目录.
	 * 默认值是Web应用程序的servlet容器的临时目录.
	 */
	public void setUploadTempDir(Resource uploadTempDir) throws IOException {
		if (!uploadTempDir.exists() && !uploadTempDir.getFile().mkdirs()) {
			throw new IllegalArgumentException("Given uploadTempDir [" + uploadTempDir + "] could not be created");
		}
		this.fileItemFactory.setRepository(uploadTempDir.getFile());
		this.uploadTempDirSpecified = true;
	}

	/**
	 * 返回存储上传文件的临时目录.
	 */
	protected boolean isUploadTempDirSpecified() {
		return this.uploadTempDirSpecified;
	}

	/**
	 * 设置是否保留客户端发送的文件名, 而不是删除{@link CommonsMultipartFile#getOriginalFilename()}中的路径信息.
	 * <p>默认为"false", 剥离可能在实际文件名前加的路径信息 e.g. 从Opera上传的文件.
	 * 将其切换为"true"以保留客户端指定的文件名, 包括潜在的路径分隔符.
	 */
	public void setPreserveFilename(boolean preserveFilename) {
		this.preserveFilename = preserveFilename;
	}


	/**
	 * Commons DiskFileItemFactory实例的工厂方法.
	 * <p>默认实现返回标准的DiskFileItemFactory.
	 * 可以重写以使用自定义子类, e.g. 用于测试.
	 * 
	 * @return 新的DiskFileItemFactory实例
	 */
	protected DiskFileItemFactory newFileItemFactory() {
		return new DiskFileItemFactory();
	}

	/**
	 * Commons FileUpload实例的工厂方法.
	 * <p><b>由子类实现.</b>
	 * 
	 * @param fileItemFactory 要构建的Commons FileItemFactory
	 * 
	 * @return Commons FileUpload实例
	 */
	protected abstract FileUpload newFileUpload(FileItemFactory fileItemFactory);


	/**
	 * 确定给定的编码的适当的FileUpload实例.
	 * <p>如果编码匹配, 则默认实现返回共享FileUpload实例, 否则创建具有除所需编码之外的相同配置的新FileUpload实例.
	 * 
	 * @param encoding 要使用的字符编码
	 * 
	 * @return 适当的FileUpload实例.
	 */
	protected FileUpload prepareFileUpload(String encoding) {
		FileUpload fileUpload = getFileUpload();
		FileUpload actualFileUpload = fileUpload;

		// 如果请求指定自己的编码与默认编码不匹配, 使用新的临时FileUpload实例.
		if (encoding != null && !encoding.equals(fileUpload.getHeaderEncoding())) {
			actualFileUpload = newFileUpload(getFileItemFactory());
			actualFileUpload.setSizeMax(fileUpload.getSizeMax());
			actualFileUpload.setFileSizeMax(fileUpload.getFileSizeMax());
			actualFileUpload.setHeaderEncoding(encoding);
		}

		return actualFileUpload;
	}

	/**
	 * 将给定的Commons FileItems列表解析为Spring MultipartParsingResult,
	 * 包含Spring MultipartFile实例和multipart参数Map.
	 * 
	 * @param fileItems 要解析的Commons FileIterms
	 * @param encoding 用于表单字段的编码
	 * 
	 * @return the Spring MultipartParsingResult
	 */
	protected MultipartParsingResult parseFileItems(List<FileItem> fileItems, String encoding) {
		MultiValueMap<String, MultipartFile> multipartFiles = new LinkedMultiValueMap<String, MultipartFile>();
		Map<String, String[]> multipartParameters = new HashMap<String, String[]>();
		Map<String, String> multipartParameterContentTypes = new HashMap<String, String>();

		// 提取multipart文件和multipart参数.
		for (FileItem fileItem : fileItems) {
			if (fileItem.isFormField()) {
				String value;
				String partEncoding = determineEncoding(fileItem.getContentType(), encoding);
				if (partEncoding != null) {
					try {
						value = fileItem.getString(partEncoding);
					}
					catch (UnsupportedEncodingException ex) {
						if (logger.isWarnEnabled()) {
							logger.warn("Could not decode multipart item '" + fileItem.getFieldName() +
									"' with encoding '" + partEncoding + "': using platform default");
						}
						value = fileItem.getString();
					}
				}
				else {
					value = fileItem.getString();
				}
				String[] curParam = multipartParameters.get(fileItem.getFieldName());
				if (curParam == null) {
					// simple form field
					multipartParameters.put(fileItem.getFieldName(), new String[] {value});
				}
				else {
					// array of simple form fields
					String[] newParam = StringUtils.addStringToArray(curParam, value);
					multipartParameters.put(fileItem.getFieldName(), newParam);
				}
				multipartParameterContentTypes.put(fileItem.getFieldName(), fileItem.getContentType());
			}
			else {
				// multipart file field
				CommonsMultipartFile file = createMultipartFile(fileItem);
				multipartFiles.add(file.getName(), file);
				if (logger.isDebugEnabled()) {
					logger.debug("Found multipart file [" + file.getName() + "] of size " + file.getSize() +
							" bytes with original filename [" + file.getOriginalFilename() + "], stored " +
							file.getStorageDescription());
				}
			}
		}
		return new MultipartParsingResult(multipartFiles, multipartParameters, multipartParameterContentTypes);
	}

	/**
	 * 为给定的Commons {@link FileItem}创建一个{@link CommonsMultipartFile}包装器.
	 * 
	 * @param fileItem 要包装的Commons FileItem
	 * 
	 * @return 相应的CommonsMultipartFile (可能是自定义子类)
	 */
	protected CommonsMultipartFile createMultipartFile(FileItem fileItem) {
		CommonsMultipartFile multipartFile = new CommonsMultipartFile(fileItem);
		multipartFile.setPreserveFilename(this.preserveFilename);
		return multipartFile;
	}

	/**
	 * 清理在multipart解析期间创建的Spring MultipartFiles, 可能在磁盘上保存临时数据.
	 * <p>删除底层Commons FileItem实例.
	 * 
	 * @param multipartFiles MultipartFile实例的集合
	 */
	protected void cleanupFileItems(MultiValueMap<String, MultipartFile> multipartFiles) {
		for (List<MultipartFile> files : multipartFiles.values()) {
			for (MultipartFile file : files) {
				if (file instanceof CommonsMultipartFile) {
					CommonsMultipartFile cmf = (CommonsMultipartFile) file;
					cmf.getFileItem().delete();
					if (logger.isDebugEnabled()) {
						logger.debug("Cleaning up multipart file [" + cmf.getName() + "] with original filename [" +
								cmf.getOriginalFilename() + "], stored " + cmf.getStorageDescription());
					}
				}
			}
		}
	}

	private String determineEncoding(String contentTypeHeader, String defaultEncoding) {
		if (!StringUtils.hasText(contentTypeHeader)) {
			return defaultEncoding;
		}
		MediaType contentType = MediaType.parseMediaType(contentTypeHeader);
		Charset charset = contentType.getCharset();
		return (charset != null ? charset.name() : defaultEncoding);
	}


	/**
	 * Spring MultipartFiles的 Map和multipart参数Map的保存器.
	 */
	protected static class MultipartParsingResult {

		private final MultiValueMap<String, MultipartFile> multipartFiles;

		private final Map<String, String[]> multipartParameters;

		private final Map<String, String> multipartParameterContentTypes;

		public MultipartParsingResult(MultiValueMap<String, MultipartFile> mpFiles,
				Map<String, String[]> mpParams, Map<String, String> mpParamContentTypes) {

			this.multipartFiles = mpFiles;
			this.multipartParameters = mpParams;
			this.multipartParameterContentTypes = mpParamContentTypes;
		}

		public MultiValueMap<String, MultipartFile> getMultipartFiles() {
			return this.multipartFiles;
		}

		public Map<String, String[]> getMultipartParameters() {
			return this.multipartParameters;
		}

		public Map<String, String> getMultipartParameterContentTypes() {
			return this.multipartParameterContentTypes;
		}
	}

}
