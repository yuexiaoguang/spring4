package org.springframework.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@code java.io.File}句柄的{@link Resource}实现.
 * 支持解析{@code File}以及{@code URL}.
 * 实现扩展的{@link WritableResource}接口.
 */
public class FileSystemResource extends AbstractResource implements WritableResource {

	private final File file;

	private final String path;


	/**
	 * 从{@link File}句柄创建一个新的{@code FileSystemResource}.
	 * <p>Note: 通过{@link #createRelative}构建相关资源时, 相对路径将在同一目录级别应用:
	 * e.g. new File("C:/dir1"), 相对路径 "dir2" -> "C:/dir2"!
	 * 如果在给定根目录下构建相对路径, 使用构造函数{@link #FileSystemResource(String) 文件路径} 将根斜杠附加到根路径:
	 * "C:/dir1/", 表示此目录作为所有相对路径的根目录.
	 * 
	 * @param file File句柄
	 */
	public FileSystemResource(File file) {
		Assert.notNull(file, "File must not be null");
		this.file = file;
		this.path = StringUtils.cleanPath(file.getPath());
	}

	/**
	 * 从文件路径创建新的{@code FileSystemResource}.
	 * <p>Note: 通过{@link #createRelative}构建相关资源时, 这里指定的资源库路径是否以斜杠结尾会有所不同.
	 * 在 "C:/dir1/"的情况下, 将在该根目录下构建相对路径: e.g. 相对路径 "dir2" -> "C:/dir1/dir2".
	 * 在"C:/dir1"的情况下, 相对路径将应用于同一目录级别: 相对路径 "dir2" -> "C:/dir2".
	 * 
	 * @param path 文件路径
	 */
	public FileSystemResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.file = new File(path);
		this.path = StringUtils.cleanPath(path);
	}


	/**
	 * 返回此资源的文件路径.
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * 此实现返回底层文件是否存在.
	 */
	@Override
	public boolean exists() {
		return this.file.exists();
	}

	/**
	 * 此实现检查底层文件是否标记为可读 (并且对应于包含内容的实际文件, 而不是目录).
	 */
	@Override
	public boolean isReadable() {
		return (this.file.canRead() && !this.file.isDirectory());
	}

	/**
	 * 此实现为底层文件打开FileInputStream.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(this.file);
	}

	/**
	 * 此实现检查底层文件是否标记为可写 (并且对应于包含内容的实际文件, 而不是目录).
	 */
	@Override
	public boolean isWritable() {
		return (this.file.canWrite() && !this.file.isDirectory());
	}

	/**
	 * 此实现为底层文件打开FileOutputStream.
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		return new FileOutputStream(this.file);
	}

	/**
	 * 此实现返回底层文件的URL.
	 */
	@Override
	public URL getURL() throws IOException {
		return this.file.toURI().toURL();
	}

	/**
	 * 此实现返回底层文件的URI.
	 */
	@Override
	public URI getURI() throws IOException {
		return this.file.toURI();
	}

	/**
	 * 此实现返回底层File引用.
	 */
	@Override
	public File getFile() {
		return this.file;
	}

	/**
	 * 此实现返回底层文件的长度.
	 */
	@Override
	public long contentLength() throws IOException {
		return this.file.length();
	}

	/**
	 * 此实现创建FileSystemResource, 应用相对于此资源描述符的底层文件的路径的给定路径.
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new FileSystemResource(pathToUse);
	}

	/**
	 * 此实现返回文件的名称.
	 */
	@Override
	public String getFilename() {
		return this.file.getName();
	}

	/**
	 * 此实现返回包含文件的绝对路径的描述.
	 */
	@Override
	public String getDescription() {
		return "file [" + this.file.getAbsolutePath() + "]";
	}


	/**
	 * 此实现比较底层文件引用.
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this ||
			(obj instanceof FileSystemResource && this.path.equals(((FileSystemResource) obj).path)));
	}

	/**
	 * 此实现返回底层File引用的哈希码.
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}
}
