package org.springframework.web.context.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletContext;

import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link javax.servlet.ServletContext}资源的{@link org.springframework.core.io.Resource}实现,
 * 解释Web应用程序根目录中的相对路径.
 *
 * <p>始终支持流访问和URL访问, 但仅在扩展Web应用程序存档时允许{@code java.io.File}访问.
 */
public class ServletContextResource extends AbstractFileResolvingResource implements ContextResource {

	private final ServletContext servletContext;

	private final String path;


	/**
	 * <p>Servlet规范要求资源路径以斜杠开头, 即使许多容器也接受路径而没有前导斜杠.
	 * 因此, 如果给定路径尚未以斜杠开头, 则它将以斜杠为前缀.
	 * 
	 * @param servletContext 从中加载的ServletContext
	 * @param path 资源的路径
	 */
	public ServletContextResource(ServletContext servletContext, String path) {
		// check ServletContext
		Assert.notNull(servletContext, "Cannot resolve ServletContextResource without ServletContext");
		this.servletContext = servletContext;

		// check path
		Assert.notNull(path, "Path is required");
		String pathToUse = StringUtils.cleanPath(path);
		if (!pathToUse.startsWith("/")) {
			pathToUse = "/" + pathToUse;
		}
		this.path = pathToUse;
	}


	/**
	 * 返回此资源的ServletContext.
	 */
	public final ServletContext getServletContext() {
		return this.servletContext;
	}

	/**
	 * 返回此资源的路径.
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * 此实现检查{@code ServletContext.getResource}.
	 */
	@Override
	public boolean exists() {
		try {
			URL url = this.servletContext.getResource(this.path);
			return (url != null);
		}
		catch (MalformedURLException ex) {
			return false;
		}
	}

	/**
	 * 此实现委托给{@code ServletContext.getResourceAsStream}, 在不可读资源 (e.g. 目录)的情况下返回{@code null}.
	 */
	@Override
	public boolean isReadable() {
		InputStream is = this.servletContext.getResourceAsStream(this.path);
		if (is != null) {
			try {
				is.close();
			}
			catch (IOException ex) {
				// ignore
			}
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 此实现委托给{@code ServletContext.getResourceAsStream}, 但如果找不到资源则抛出FileNotFoundException.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = this.servletContext.getResourceAsStream(this.path);
		if (is == null) {
			throw new FileNotFoundException("Could not open " + getDescription());
		}
		return is;
	}

	/**
	 * 此实现委托给{@code ServletContext.getResource}, 但如果找不到资源则抛出FileNotFoundException.
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = this.servletContext.getResource(this.path);
		if (url == null) {
			throw new FileNotFoundException(
					getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * 此实现解析"file:" URL或者委托给{@code ServletContext.getRealPath}, 如果找不到或者无法解析则抛出FileNotFoundException.
	 */
	@Override
	public File getFile() throws IOException {
		URL url = this.servletContext.getResource(this.path);
		if (url != null && ResourceUtils.isFileURL(url)) {
			// Proceed with file system resolution...
			return super.getFile();
		}
		else {
			String realPath = WebUtils.getRealPath(this.servletContext, this.path);
			return new File(realPath);
		}
	}

	/**
	 * 此实现创建ServletContextResource, 相对于此资源描述符的底层文件的路径应用给定路径.
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new ServletContextResource(this.servletContext, pathToUse);
	}

	/**
	 * 此实现返回此ServletContext资源引用的文件的名称.
	 */
	@Override
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	/**
	 * 此实现返回包含ServletContext资源位置的描述.
	 */
	@Override
	public String getDescription() {
		return "ServletContext resource [" + this.path + "]";
	}

	@Override
	public String getPathWithinContext() {
		return this.path;
	}


	/**
	 * 此实现比较底层的ServletContext资源位置.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ServletContextResource) {
			ServletContextResource otherRes = (ServletContextResource) obj;
			return (this.servletContext.equals(otherRes.servletContext) && this.path.equals(otherRes.path));
		}
		return false;
	}

	/**
	 * 此实现返回底层ServletContext资源位置的哈希码.
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
