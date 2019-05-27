package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@code java.net.URL}定位器的{@link Resource}实现.
 * 支持解析{@code URL}, 以及{@code File}, 在{@code "file:"}协议的情况下.
 */
public class UrlResource extends AbstractFileResolvingResource {

	/**
	 * 原始URI; 用于URI和文件访问.
	 */
	private final URI uri;

	/**
	 * 原始 URL, 用于实际访问.
	 */
	private final URL url;

	/**
	 * 清理后的URL (带有规范化路径), 用于比较.
	 */
	private final URL cleanedUrl;


	/**
	 * @param uri URI
	 * 
	 * @throws MalformedURLException 如果给定的URL路径无效
	 */
	public UrlResource(URI uri) throws MalformedURLException {
		Assert.notNull(uri, "URI must not be null");
		this.uri = uri;
		this.url = uri.toURL();
		this.cleanedUrl = getCleanedUrl(this.url, uri.toString());
	}

	/**
	 * @param url URL
	 */
	public UrlResource(URL url) {
		Assert.notNull(url, "URL must not be null");
		this.url = url;
		this.cleanedUrl = getCleanedUrl(this.url, url.toString());
		this.uri = null;
	}

	/**
	 * <p>Note: 如有必要, 需要对给定路径进行预编码.
	 * 
	 * @param path URL路径
	 * 
	 * @throws MalformedURLException 如果给定的URL路径无效
	 */
	public UrlResource(String path) throws MalformedURLException {
		Assert.notNull(path, "Path must not be null");
		this.uri = null;
		this.url = new URL(path);
		this.cleanedUrl = getCleanedUrl(this.url, path);
	}

	/**
	 * <p>如有必要, 给定的部分将自动编码.
	 * 
	 * @param protocol 要使用的URL协议 (e.g. "jar" 或 "file" - 没有冒号); 也被称为"方案"
	 * @param location 位置 (e.g. 该协议中的文件路径); 也称为"方案特定部分"
	 * 
	 * @throws MalformedURLException 如果给定的URL路径无效
	 */
	public UrlResource(String protocol, String location) throws MalformedURLException  {
		this(protocol, location, null);
	}

	/**
	 * <p>如有必要, 给定的部分将自动编码.
	 * 
	 * @param protocol 要使用的URL协议 (e.g. "jar" 或 "file" - 没有冒号); 也被称为"方案"
	 * @param location 位置 (e.g. 该协议中的文件路径); 也称为"方案特定部分"
	 * @param fragment 该位置内的片段 (e.g. HTML页面上的锚点, 如下面的"#"分隔符后面)
	 * 
	 * @throws MalformedURLException 如果给定的URL规范无效
	 */
	public UrlResource(String protocol, String location, String fragment) throws MalformedURLException  {
		try {
			this.uri = new URI(protocol, location, fragment);
			this.url = this.uri.toURL();
			this.cleanedUrl = getCleanedUrl(this.url, this.uri.toString());
		}
		catch (URISyntaxException ex) {
			MalformedURLException exToThrow = new MalformedURLException(ex.getMessage());
			exToThrow.initCause(ex);
			throw exToThrow;
		}
	}


	/**
	 * 确定给定原始URL的已清理URL.
	 * 
	 * @param originalUrl 原始 URL
	 * @param originalPath 原始URL路径
	 * 
	 * @return 已清理的URL
	 */
	private URL getCleanedUrl(URL originalUrl, String originalPath) {
		try {
			return new URL(StringUtils.cleanPath(originalPath));
		}
		catch (MalformedURLException ex) {
			// 清理的URL路径无法转换为URL -> 采用原始URL.
			return originalUrl;
		}
	}

	/**
	 * 此实现为给定的URL打开一个InputStream.
	 * <p>它将{@code useCaches}标志设置为{@code false}, 主要是为了避免在Windows上锁定jar文件.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		URLConnection con = this.url.openConnection();
		ResourceUtils.useCachesIfNecessary(con);
		try {
			return con.getInputStream();
		}
		catch (IOException ex) {
			// 关闭HTTP连接 (如果适用).
			if (con instanceof HttpURLConnection) {
				((HttpURLConnection) con).disconnect();
			}
			throw ex;
		}
	}

	/**
	 * 此实现返回底层URL引用.
	 */
	@Override
	public URL getURL() throws IOException {
		return this.url;
	}

	/**
	 * 如果可能, 此实现将直接返回底层URI.
	 */
	@Override
	public URI getURI() throws IOException {
		if (this.uri != null) {
			return this.uri;
		}
		else {
			return super.getURI();
		}
	}

	/**
	 * 此实现返回底层URL/URI的File引用, 前提是它引用文件系统中的文件.
	 */
	@Override
	public File getFile() throws IOException {
		if (this.uri != null) {
			return super.getFile(this.uri);
		}
		else {
			return super.getFile();
		}
	}

	/**
	 * 此实现创建一个{@code UrlResource}, 相对于此资源描述符的底层URL的路径应用给定路径.
	 */
	@Override
	public Resource createRelative(String relativePath) throws MalformedURLException {
		if (relativePath.startsWith("/")) {
			relativePath = relativePath.substring(1);
		}
		return new UrlResource(new URL(this.url, relativePath));
	}

	/**
	 * 此实现返回此URL引用的文件的名称.
	 */
	@Override
	public String getFilename() {
		return StringUtils.getFilename(this.cleanedUrl.getPath());
	}

	/**
	 * 此实现返回包含URL的描述.
	 */
	@Override
	public String getDescription() {
		return "URL [" + this.url + "]";
	}


	/**
	 * 此实现比较底层URL引用.
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this ||
			(obj instanceof UrlResource && this.cleanedUrl.equals(((UrlResource) obj).cleanedUrl)));
	}

	/**
	 * 此实现返回底层URL引用的哈希码.
	 */
	@Override
	public int hashCode() {
		return this.cleanedUrl.hashCode();
	}
}
