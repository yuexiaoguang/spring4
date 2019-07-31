package org.springframework.web.multipart.commons;

import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

/**
 * <a href="http://commons.apache.org/proper/commons-fileupload">Apache Commons FileUpload</a> 1.2
 * 或更高版本的基于Servlet的{@link MultipartResolver}实现.
 *
 * <p>提供"maxUploadSize", "maxInMemorySize"和"defaultEncoding"设置作为bean属性 (继承自{@link CommonsFileUploadSupport}).
 * 有关默认值和接受值的详细信息, 请参阅相应的ServletFileUpload / DiskFileItemFactory属性
 * ("sizeMax", "sizeThreshold", "headerEncoding").
 *
 * <p>将临时文件保存到servlet容器的临时目录中.
 * 需要通过应用程序上下文<i>或</i>通过带有ServletContext的构造函数进行初始化 (用于独立使用).
 */
public class CommonsMultipartResolver extends CommonsFileUploadSupport
		implements MultipartResolver, ServletContextAware {

	private boolean resolveLazily = false;


	/**
	 * 用作bean的构造函数.
	 * 通过ServletContextAware接口 (通常是WebApplicationContext)传入的ServletContext确定servlet容器的临时目录.
	 */
	public CommonsMultipartResolver() {
		super();
	}

	/**
	 * 独立使用的构造函数.
	 * 通过给定的ServletContext确定servlet容器的临时目录.
	 * 
	 * @param servletContext 要使用的ServletContext
	 */
	public CommonsMultipartResolver(ServletContext servletContext) {
		this();
		setServletContext(servletContext);
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
	 * 初始化底层{@code org.apache.commons.fileupload.servlet.ServletFileUpload}实例.
	 * 可以重写以使用自定义子类, e.g. 用于测试.
	 * 
	 * @param fileItemFactory 要使用的Commons FileItemFactory
	 * 
	 * @return 新的ServletFileUpload实例
	 */
	@Override
	protected FileUpload newFileUpload(FileItemFactory fileItemFactory) {
		return new ServletFileUpload(fileItemFactory);
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (!isUploadTempDirSpecified()) {
			getFileItemFactory().setRepository(WebUtils.getTempDir(servletContext));
		}
	}


	@Override
	public boolean isMultipart(HttpServletRequest request) {
		return (request != null && ServletFileUpload.isMultipartContent(request));
	}

	@Override
	public MultipartHttpServletRequest resolveMultipart(final HttpServletRequest request) throws MultipartException {
		Assert.notNull(request, "Request must not be null");
		if (this.resolveLazily) {
			return new DefaultMultipartHttpServletRequest(request) {
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
			return new DefaultMultipartHttpServletRequest(request, parsingResult.getMultipartFiles(),
					parsingResult.getMultipartParameters(), parsingResult.getMultipartParameterContentTypes());
		}
	}

	/**
	 * 解析给定的servlet请求, 解析其multipart元素.
	 * 
	 * @param request 要解析的请求
	 * 
	 * @return 解析后的结果
	 * @throws MultipartException 如果解析multipart失败
	 */
	protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
		String encoding = determineEncoding(request);
		FileUpload fileUpload = prepareFileUpload(encoding);
		try {
			List<FileItem> fileItems = ((ServletFileUpload) fileUpload).parseRequest(request);
			return parseFileItems(fileItems, encoding);
		}
		catch (FileUploadBase.SizeLimitExceededException ex) {
			throw new MaxUploadSizeExceededException(fileUpload.getSizeMax(), ex);
		}
		catch (FileUploadBase.FileSizeLimitExceededException ex) {
			throw new MaxUploadSizeExceededException(fileUpload.getFileSizeMax(), ex);
		}
		catch (FileUploadException ex) {
			throw new MultipartException("Failed to parse multipart servlet request", ex);
		}
	}

	/**
	 * 确定给定请求的编码.
	 * 可以在子类中重写.
	 * <p>默认实现检查请求编码, 然后回退到为此解析器指定的默认编码.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 请求的编码 (never {@code null})
	 */
	protected String determineEncoding(HttpServletRequest request) {
		String encoding = request.getCharacterEncoding();
		if (encoding == null) {
			encoding = getDefaultEncoding();
		}
		return encoding;
	}

	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		if (!(request instanceof AbstractMultipartHttpServletRequest) ||
				((AbstractMultipartHttpServletRequest) request).isResolved()) {
			try {
				cleanupFileItems(request.getMultiFileMap());
			}
			catch (Throwable ex) {
				logger.warn("Failed to perform multipart cleanup for servlet request", ex);
			}
		}
	}

}
