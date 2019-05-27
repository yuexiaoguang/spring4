package org.springframework.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.lang.UsesJava7;
import org.springframework.util.Assert;

/**
 * {@code java.nio.file.Path}句柄的{@link Resource}实现.
 * 支持解析File, 以及URL.
 * 实现扩展的{@link WritableResource}接口.
 */
@UsesJava7
public class PathResource extends AbstractResource implements WritableResource {

	private final Path path;


	/**
	 * 从Path句柄创建新的PathResource.
	 * <p>Note: 与{@link FileSystemResource}不同, 当通过{@link #createRelative}构建相关资源时,
	 * 相对路径将构建在给定的根目录<i>下面</i>:
	 * e.g. Paths.get("C:/dir1/"), 相对路径 "dir2" -> "C:/dir1/dir2"!
	 * 
	 * @param path Path句柄
	 */
	public PathResource(Path path) {
		Assert.notNull(path, "Path must not be null");
		this.path = path.normalize();
	}

	/**
	 * 从Path句柄创建新的PathResource.
	 * <p>Note: 与{@link FileSystemResource}不同, 当通过{@link #createRelative}构建相关资源时,
	 * 相对路径将构建在给定的根目录<i>下面</i>:
	 * e.g. Paths.get("C:/dir1/"), 相对路径 "dir2" -> "C:/dir1/dir2"!
	 * 
	 * @param path 路径
	 */
	public PathResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = Paths.get(path).normalize();
	}

	/**
	 * <p>Note: 与{@link FileSystemResource}不同, 当通过{@link #createRelative}构建相关资源时,
	 * 相对路径将构建在给定的根目录<i>下面</i>:
	 * e.g. Paths.get("C:/dir1/"), 相对路径 "dir2" -> "C:/dir1/dir2"!
	 * 
	 * @param uri 路径URI
	 */
	public PathResource(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		this.path = Paths.get(uri).normalize();
	}


	/**
	 * 返回此资源的文件路径.
	 */
	public final String getPath() {
		return this.path.toString();
	}

	/**
	 * 此实现返回底层文件是否存在.
	 */
	@Override
	public boolean exists() {
		return Files.exists(this.path);
	}

	/**
	 * 此实现检查底层文件是否标记为可读
	 * (并且对应于包含内容的实际文件, 而不是目录).
	 */
	@Override
	public boolean isReadable() {
		return (Files.isReadable(this.path) && !Files.isDirectory(this.path));
	}

	/**
	 * 此实现为底层文件打开InputStream.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		if (!exists()) {
			throw new FileNotFoundException(getPath() + " (no such file or directory)");
		}
		if (Files.isDirectory(this.path)) {
			throw new FileNotFoundException(getPath() + " (is a directory)");
		}
		return Files.newInputStream(this.path);
	}

	/**
	 * 此实现检查底层文件是否标记为可写
	 * (并且对应于包含内容的实际文件, 而不是目录).
	 */
	@Override
	public boolean isWritable() {
		return (Files.isWritable(this.path) && !Files.isDirectory(this.path));
	}

	/**
	 * 此实现打开底层文件的OutputStream.
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		if (Files.isDirectory(this.path)) {
			throw new FileNotFoundException(getPath() + " (is a directory)");
		}
		return Files.newOutputStream(this.path);
	}

	/**
	 * 此实现返回底层文件的URL.
	 */
	@Override
	public URL getURL() throws IOException {
		return this.path.toUri().toURL();
	}

	/**
	 * 此实现返回底层文件的URI.
	 */
	@Override
	public URI getURI() throws IOException {
		return this.path.toUri();
	}

	/**
	 * 此实现返回底层File引用.
	 */
	@Override
	public File getFile() throws IOException {
		try {
			return this.path.toFile();
		}
		catch (UnsupportedOperationException ex) {
			// 只有默认文件系统上的路径才能转换为File:
			// 对于无法进行转换的情况, 抛出异常.
			throw new FileNotFoundException(this.path + " cannot be resolved to absolute file path");
		}
	}

	/**
	 * 此实现返回底层文件的长度.
	 */
	@Override
	public long contentLength() throws IOException {
		return Files.size(this.path);
	}

	/**
	 * 此实现返回底层文件的时间戳.
	 */
	@Override
	public long lastModified() throws IOException {
		// 不能使用超类方法, 因为它使用转换到文件, 只有默认文件系统上的路径可以转换为文件...
		return Files.getLastModifiedTime(this.path).toMillis();
	}

	/**
	 * 此实现创建PathResource, 应用相对于此资源描述符的底层文件的路径的给定路径.
	 */
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return new PathResource(this.path.resolve(relativePath));
	}

	/**
	 * 此实现返回文件的名称.
	 */
	@Override
	public String getFilename() {
		return this.path.getFileName().toString();
	}

	@Override
	public String getDescription() {
		return "path [" + this.path.toAbsolutePath() + "]";
	}


	/**
	 * 此实现比较底层Path引用.
	 */
	@Override
	public boolean equals(Object obj) {
		return (this == obj ||
			(obj instanceof PathResource && this.path.equals(((PathResource) obj).path)));
	}

	/**
	 * 此实现返回底层Path引用的哈希码.
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}
}
