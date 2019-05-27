package org.springframework.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.util.ResourceUtils;

/**
 * 将URL解析为文件引用的资源的抽象基类, 例如{@link UrlResource}或{@link ClassPathResource}.
 *
 * <p>检测URL中的 "file"协议以及JBoss "vfs"协议, 相应地解析文件系统引用.
 */
public abstract class AbstractFileResolvingResource extends AbstractResource {

	/**
	 * 此实现返回底层类路径资源的File引用, 前提是它引用文件系统中的文件.
	 */
	@Override
	public File getFile() throws IOException {
		URL url = getURL();
		if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
			return VfsResourceDelegate.getResource(url).getFile();
		}
		return ResourceUtils.getFile(url, getDescription());
	}

	/**
	 * 此实现确定底层文件 (或jar文件, 如果是jar/zip中的资源).
	 */
	@Override
	protected File getFileForLastModifiedCheck() throws IOException {
		URL url = getURL();
		if (ResourceUtils.isJarURL(url)) {
			URL actualUrl = ResourceUtils.extractArchiveURL(url);
			if (actualUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				return VfsResourceDelegate.getResource(actualUrl).getFile();
			}
			return ResourceUtils.getFile(actualUrl, "Jar URL");
		}
		else {
			return getFile();
		}
	}

	/**
	 * 此实现返回给定URI标识资源的File引用, 前提是它引用文件系统中的文件.
	 */
	protected File getFile(URI uri) throws IOException {
		if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
			return VfsResourceDelegate.getResource(uri).getFile();
		}
		return ResourceUtils.getFile(uri, getDescription());
	}


	@Override
	public boolean exists() {
		try {
			URL url = getURL();
			if (ResourceUtils.isFileURL(url)) {
				// 继续文件系统解析
				return getFile().exists();
			}
			else {
				// 尝试URL连接content-length header
				URLConnection con = url.openConnection();
				customizeConnection(con);
				HttpURLConnection httpCon =
						(con instanceof HttpURLConnection ? (HttpURLConnection) con : null);
				if (httpCon != null) {
					int code = httpCon.getResponseCode();
					if (code == HttpURLConnection.HTTP_OK) {
						return true;
					}
					else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
						return false;
					}
				}
				if (con.getContentLength() >= 0) {
					return true;
				}
				if (httpCon != null) {
					// 没有HTTP OK状态, 也没有 content-length header: 放弃
					httpCon.disconnect();
					return false;
				}
				else {
					// Fall back to stream existence: can we open the stream?
					InputStream is = getInputStream();
					is.close();
					return true;
				}
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	public boolean isReadable() {
		try {
			URL url = getURL();
			if (ResourceUtils.isFileURL(url)) {
				// 继续文件系统解析
				File file = getFile();
				return (file.canRead() && !file.isDirectory());
			}
			else {
				return true;
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	public long contentLength() throws IOException {
		URL url = getURL();
		if (ResourceUtils.isFileURL(url)) {
			// 继续文件系统解析
			return getFile().length();
		}
		else {
			// 尝试URL连接 content-length header
			URLConnection con = url.openConnection();
			customizeConnection(con);
			return con.getContentLength();
		}
	}

	@Override
	public long lastModified() throws IOException {
		URL url = getURL();
		if (ResourceUtils.isFileURL(url) || ResourceUtils.isJarURL(url)) {
			// 继续文件系统解析
			try {
				return super.lastModified();
			}
			catch (FileNotFoundException ex) {
				// 防御性地回退到URL连接检查
			}
		}
		// 尝试URL连接 last-modified header
		URLConnection con = url.openConnection();
		customizeConnection(con);
		return con.getLastModified();
	}


	/**
	 * 自定义在{@link #exists()}, {@link #contentLength()} 或{@link #lastModified()}调用过程中获得的给定{@link URLConnection}.
	 * <p>调用{@link ResourceUtils#useCachesIfNecessary(URLConnection)}, 并委托给{@link #customizeConnection(HttpURLConnection)}.
	 * 可以在子类中重写.
	 * 
	 * @param con 要自定义的URLConnection
	 * 
	 * @throws IOException 如果从URLConnection方法抛出
	 */
	protected void customizeConnection(URLConnection con) throws IOException {
		ResourceUtils.useCachesIfNecessary(con);
		if (con instanceof HttpURLConnection) {
			customizeConnection((HttpURLConnection) con);
		}
	}

	/**
	 * 自定义在{@link #exists()}, {@link #contentLength()} 或{@link #lastModified()}调用过程中获得的给定{@link HttpURLConnection}.
	 * <p>默认设置请求方法"HEAD". 可以在子类中重写.
	 * 
	 * @param con 要自定义的HttpURLConnection
	 * 
	 * @throws IOException 如果从 HttpURLConnection方法抛出
	 */
	protected void customizeConnection(HttpURLConnection con) throws IOException {
		con.setRequestMethod("HEAD");
	}


	/**
	 * 内部委托类, 在运行时避免JBoss VFS API硬依赖.
	 */
	private static class VfsResourceDelegate {

		public static Resource getResource(URL url) throws IOException {
			return new VfsResource(VfsUtils.getRoot(url));
		}

		public static Resource getResource(URI uri) throws IOException {
			return new VfsResource(VfsUtils.getRoot(uri));
		}
	}
}
