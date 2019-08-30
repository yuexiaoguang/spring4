package org.springframework.web.portlet.multipart;

import java.util.List;
import javax.portlet.ActionRequest;
import javax.portlet.PortletContext;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.portlet.PortletFileUpload;

import org.springframework.util.Assert;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.commons.CommonsFileUploadSupport;
import org.springframework.web.portlet.context.PortletContextAware;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * <a href="http://commons.apache.org/proper/commons-fileupload">Apache Commons FileUpload</a>
 * 1.2或更高版本的{@link PortletMultipartResolver}实现.
 *
 * <p>提供"maxUploadSize", "maxInMemorySize" 和 "defaultEncoding"设置作为bean属性 (继承自{@link CommonsFileUploadSupport}).
 * 有关默认值和可接受的值的详细信息, 请参阅相应的PortletFileUpload / DiskFileItemFactory属性
 * ("sizeMax", "sizeThreshold", "headerEncoding").
 *
 * <p>将临时文件保存到portlet容器的临时目录中.
 * 需要由应用程序上下文<i>或</i>通过构造函数初始化, 该构造函数使用PortletContext(用于独立使用).
 */
public class CommonsPortletMultipartResolver extends CommonsFileUploadSupport
		implements PortletMultipartResolver, PortletContextAware {

	private boolean resolveLazily = false;


	/**
	 * 用作bean的构造函数.
	 * 通过PortletContextAware接口 (通常是ApplicationContext)传入的PortletContext确定portlet容器的临时目录.
	 */
	public CommonsPortletMultipartResolver() {
		super();
	}

	/**
	 * 独立使用的构造函数.
	 * 通过给定的PortletContext确定portlet容器的临时目录.
	 * 
	 * @param portletContext 要使用的PortletContext
	 */
	public CommonsPortletMultipartResolver(PortletContext portletContext) {
		this();
		setPortletContext(portletContext);
	}


	/**
	 * 设置是否在文件或参数访问时延迟地解析multipart请求.
	 * <p>默认为"false", 立即解析multipart元素, 在{@link #resolveMultipart}调用时抛出相应的异常.
	 * 对于延迟multipart解析, 将其切换为"true", 一旦应用程序尝试获取multipart文件或参数, 则抛出解析异常.
	 */
	public void setResolveLazily(boolean resolveLazily) {
		this.resolveLazily = resolveLazily;
	}

	/**
	 * 初始化底层{@code org.apache.commons.fileupload.portlet.PortletFileUpload}实例.
	 * 可以重写以使用自定义子类, e.g. 用于测试.
	 * 
	 * @return 新的PortletFileUpload实例
	 */
	@Override
	protected FileUpload newFileUpload(FileItemFactory fileItemFactory) {
		return new PortletFileUpload(fileItemFactory);
	}

	@Override
	public void setPortletContext(PortletContext portletContext) {
		if (!isUploadTempDirSpecified()) {
			getFileItemFactory().setRepository(PortletUtils.getTempDir(portletContext));
		}
	}


	@Override
	public boolean isMultipart(ActionRequest request) {
		return (request != null && PortletFileUpload.isMultipartContent(request));
	}

	@Override
	public MultipartActionRequest resolveMultipart(final ActionRequest request) throws MultipartException {
		Assert.notNull(request, "Request must not be null");
		if (this.resolveLazily) {
			return new DefaultMultipartActionRequest(request) {
				@Override
				protected void initializeMultipart() {
					MultipartParsingResult parsingResult = parseRequest(request);
					setMultipartFiles(parsingResult.getMultipartFiles());
					setMultipartParameters(parsingResult.getMultipartParameters());
					setMultipartParameterContentTypes(parsingResult.getMultipartParameterContentTypes());
				}
			};
		}
		else {
			MultipartParsingResult parsingResult = parseRequest(request);
			return new DefaultMultipartActionRequest(request, parsingResult.getMultipartFiles(),
					parsingResult.getMultipartParameters(), parsingResult.getMultipartParameterContentTypes());
		}
	}

	/**
	 * 解析给定的portlet请求, 解析其multipart元素.
	 * 
	 * @param request 要解析的请求
	 * 
	 * @return 解析后的结果
	 * @throws MultipartException 如果multipart解析失败
	 */
	protected MultipartParsingResult parseRequest(ActionRequest request) throws MultipartException {
		String encoding = determineEncoding(request);
		FileUpload fileUpload = prepareFileUpload(encoding);
		try {
			List<FileItem> fileItems = ((PortletFileUpload) fileUpload).parseRequest(request);
			return parseFileItems(fileItems, encoding);
		}
		catch (FileUploadBase.SizeLimitExceededException ex) {
			throw new MaxUploadSizeExceededException(fileUpload.getSizeMax(), ex);
		}
		catch (FileUploadBase.FileSizeLimitExceededException ex) {
			throw new MaxUploadSizeExceededException(fileUpload.getFileSizeMax(), ex);
		}
		catch (FileUploadException ex) {
			throw new MultipartException("Failed to parse multipart portlet request", ex);
		}
	}

	/**
	 * 确定给定请求的编码.
	 * 可以在子类中重写.
	 * <p>默认实现检查请求编码, 然后回退到为此解析器指定的默认编码.
	 * 
	 * @param request 当前的portlet请求
	 * 
	 * @return 请求的编码 (never {@code null})
	 */
	protected String determineEncoding(ActionRequest request) {
		String encoding = request.getCharacterEncoding();
		if (encoding == null) {
			encoding = getDefaultEncoding();
		}
		return encoding;
	}

	@Override
	public void cleanupMultipart(MultipartActionRequest request) {
		if (request != null) {
			try {
				cleanupFileItems(request.getMultiFileMap());
			}
			catch (Throwable ex) {
				logger.warn("Failed to perform multipart cleanup for portlet request", ex);
			}
		}
	}

}
