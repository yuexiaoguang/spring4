package org.springframework.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.springframework.core.NestedIOException;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * {@link Resource}实现的便捷基类, 预先实现典型行为.
 *
 * <p>"exists"方法将检查是否可以打开File 或 InputStream;
 * "isOpen"将总是返回 false; "getURL" 和 "getFile"抛出异常; "toString"将返回描述.
 */
public abstract class AbstractResource implements Resource {

	/**
	 * 此实现检查是否可以打开文件, 然后回退到是否可以打开InputStream.
	 * 这将涵盖目录和内容资源.
	 */
	@Override
	public boolean exists() {
		// 尝试文件存在: 是否可以在文件系统中找到该文件?
		try {
			return getFile().exists();
		}
		catch (IOException ex) {
			// 回退到流存在: 是否可以打开流?
			try {
				InputStream is = getInputStream();
				is.close();
				return true;
			}
			catch (Throwable isEx) {
				return false;
			}
		}
	}

	/**
	 * 此实现总是返回{@code true}.
	 */
	@Override
	public boolean isReadable() {
		return true;
	}

	/**
	 * 此实现总是返回{@code false}.
	 */
	@Override
	public boolean isOpen() {
		return false;
	}

	/**
	 * 假设无法将资源解析为URL, 此实现会抛出FileNotFoundException.
	 */
	@Override
	public URL getURL() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
	}

	/**
	 * 此实现基于{@link #getURL()}返回的URL构建URI.
	 */
	@Override
	public URI getURI() throws IOException {
		URL url = getURL();
		try {
			return ResourceUtils.toURI(url);
		}
		catch (URISyntaxException ex) {
			throw new NestedIOException("Invalid URI [" + url + "]", ex);
		}
	}

	/**
	 * 假设无法将资源解析为绝对文件路径, 此实现会抛出FileNotFoundException.
	 */
	@Override
	public File getFile() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
	}

	/**
	 * 此实现读取整个InputStream以计算内容长度.
	 * 子类几乎总是能够提供更优化的版本, e.g. 检查文件长度.
	 */
	@Override
	public long contentLength() throws IOException {
		InputStream is = getInputStream();
		Assert.state(is != null, "Resource InputStream must not be null");
		try {
			long size = 0;
			byte[] buf = new byte[255];
			int read;
			while ((read = is.read(buf)) != -1) {
				size += read;
			}
			return size;
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ex) {
			}
		}
	}

	/**
	 * 此实现检查底层文件的时间戳.
	 */
	@Override
	public long lastModified() throws IOException {
		long lastModified = getFileForLastModifiedCheck().lastModified();
		if (lastModified == 0L) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for resolving its last-modified timestamp");
		}
		return lastModified;
	}

	/**
	 * 确定用于时间戳检查的文件.
	 * <p>默认实现委托给{@link #getFile()}.
	 * 
	 * @return 用于时间戳检查的文件 (never {@code null})
	 * 
	 * @throws FileNotFoundException 如果资源无法解析为绝对文件路径, i.e. 在文件系统中不可用
	 * @throws IOException 在一般解析/读取失败的情况下
	 */
	protected File getFileForLastModifiedCheck() throws IOException {
		return getFile();
	}

	/**
	 * 假设无法为此资源创建相对资源, 此实现将抛出FileNotFoundException.
	 */
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
	}

	/**
	 * 假设此资源类型没有文件名, 此实现始终返回{@code null}.
	 */
	@Override
	public String getFilename() {
		return null;
	}


	/**
	 * 此实现返回此资源的描述.
	 */
	@Override
	public String toString() {
		return getDescription();
	}

	/**
	 * 此实现比较描述字符串.
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this ||
			(obj instanceof Resource && ((Resource) obj).getDescription().equals(getDescription())));
	}

	/**
	 * 此实现返回描述的哈希码.
	 */
	@Override
	public int hashCode() {
		return getDescription().hashCode();
	}
}
