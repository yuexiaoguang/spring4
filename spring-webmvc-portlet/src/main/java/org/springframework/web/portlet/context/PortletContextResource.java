package org.springframework.web.portlet.context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.portlet.PortletContext;

import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * {@link javax.portlet.PortletContext}资源的{@link org.springframework.core.io.Resource}实现,
 * 解释portlet应用程序根目录中的相对路径.
 *
 * <p>始终支持流访问和URL访问, 但仅在展开portlet应用程序归档时允许{@code java.io.File}访问.
 */
public class PortletContextResource extends AbstractFileResolvingResource implements ContextResource {

	private final PortletContext portletContext;

	private final String path;


	/**
	 * <p>Portlet规范要求资源路径以斜杠开头, 即使许多容器也接受没有前导斜杠的路径.
	 * 因此, 如果给定路径尚未以斜杠开头, 则它将以斜杠为前缀.
	 * 
	 * @param portletContext 从中加载的PortletContext
	 * @param path 资源的路径
	 */
	public PortletContextResource(PortletContext portletContext, String path) {
		// check PortletContext
		Assert.notNull(portletContext, "Cannot resolve PortletContextResource without PortletContext");
		this.portletContext = portletContext;

		// check path
		Assert.notNull(path, "Path is required");
		String pathToUse = StringUtils.cleanPath(path);
		if (!pathToUse.startsWith("/")) {
			pathToUse = "/" + pathToUse;
		}
		this.path = pathToUse;
	}


	/**
	 * 返回此资源的PortletContext.
	 */
	public final PortletContext getPortletContext() {
		return this.portletContext;
	}

	/**
	 * 返回此资源的路径.
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * 此实现检查{@code PortletContext.getResource}.
	 */
	@Override
	public boolean exists() {
		try {
			URL url = this.portletContext.getResource(this.path);
			return (url != null);
		}
		catch (MalformedURLException ex) {
			return false;
		}
	}

	/**
	 * 此实现委托给{@code PortletContext.getResourceAsStream}, 在不可读资源(例如目录)的情况下返回{@code null}.
	 */
	@Override
	public boolean isReadable() {
		InputStream is = this.portletContext.getResourceAsStream(this.path);
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
	 * 此实现委托给{@code PortletContext.getResourceAsStream}, 如果找不到则抛出FileNotFoundException.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = this.portletContext.getResourceAsStream(this.path);
		if (is == null) {
			throw new FileNotFoundException("Could not open " + getDescription());
		}
		return is;
	}

	/**
	 * 此实现委托给{@code PortletContext.getResource}, 如果找不到则抛出FileNotFoundException.
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = this.portletContext.getResource(this.path);
		if (url == null) {
			throw new FileNotFoundException(
					getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * 此实现解析"file:" URL或者委托给到{@code PortletContext.getRealPath}, 如果找不到或者无法解析则抛出FileNotFoundException.
	 */
	@Override
	public File getFile() throws IOException {
		URL url = getURL();
		if (ResourceUtils.isFileURL(url)) {
			// Proceed with file system resolution...
			return super.getFile();
		}
		else {
			String realPath = PortletUtils.getRealPath(this.portletContext, this.path);
			return new File(realPath);
		}
	}

	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new PortletContextResource(this.portletContext, pathToUse);
	}

	@Override
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	@Override
	public String getDescription() {
		return "PortletContext resource [" + this.path + "]";
	}

	@Override
	public String getPathWithinContext() {
		return this.path;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof PortletContextResource) {
			PortletContextResource otherRes = (PortletContextResource) obj;
			return (this.portletContext.equals(otherRes.portletContext) && this.path.equals(otherRes.path));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
