package org.springframework.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 类路径资源的{@link Resource}实现.
 * 使用给定的{@link ClassLoader}或给定的{@link Class}来加载资源.
 *
 * <p>如果类路径资源驻留在文件系统中, 则支持解析为{@code java.io.File}, 但不支持JAR中的资源.
 * 始终支持解析为URL.
 */
public class ClassPathResource extends AbstractFileResolvingResource {

	private final String path;

	private ClassLoader classLoader;

	private Class<?> clazz;


	/**
	 * 为{@code ClassLoader}用法创建一个新的{@code ClassPathResource}.
	 * 将删除前导斜杠, 因为ClassLoader资源访问方法将不接受它.
	 * <p>线程上下文类加载器将用于加载资源.
	 * 
	 * @param path 类路径中的绝对路径
	 */
	public ClassPathResource(String path) {
		this(path, (ClassLoader) null);
	}

	/**
	 * 为{@code ClassLoader}用法创建一个新的{@code ClassPathResource}.
	 * 将删除前导斜杠, 因为ClassLoader资源访问方法将不接受它.
	 * 
	 * @param path 类路径中的绝对路径
	 * @param classLoader 用于加载资源的类加载器, 或{@code null} 表示线程上下文类加载器
	 */
	public ClassPathResource(String path, ClassLoader classLoader) {
		Assert.notNull(path, "Path must not be null");
		String pathToUse = StringUtils.cleanPath(path);
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		this.path = pathToUse;
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 为{@code Class}用法创建一个新的{@code ClassPathResource}.
	 * 路径可以是相对于给定的类; 也可以是类路径中的绝对路径, 通过前导斜杠.
	 * 
	 * @param path 类路径中的相对或绝对路径
	 * @param clazz 用于加载资源的类
	 */
	public ClassPathResource(String path, Class<?> clazz) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.clazz = clazz;
	}

	/**
	 * 使用可选的{@code ClassLoader}和{@code Class}创建一个新的{@code ClassPathResource}.
	 * 
	 * @param path 类路径中的相对或绝对路径
	 * @param classLoader 用于加载资源的类加载器
	 * @param clazz 用于加载资源的类
	 */
	protected ClassPathResource(String path, ClassLoader classLoader, Class<?> clazz) {
		this.path = StringUtils.cleanPath(path);
		this.classLoader = classLoader;
		this.clazz = clazz;
	}


	/**
	 * 返回此资源的路径 (作为类路径中的资源路径).
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * 返回将从中获取此资源的ClassLoader.
	 */
	public final ClassLoader getClassLoader() {
		return (this.clazz != null ? this.clazz.getClassLoader() : this.classLoader);
	}


	/**
	 * 此实现检查资源URL的解析.
	 */
	@Override
	public boolean exists() {
		return (resolveURL() != null);
	}

	/**
	 * 解析底层类路径资源的URL.
	 * 
	 * @return 已解析的URL, 或{@code null}
	 */
	protected URL resolveURL() {
		if (this.clazz != null) {
			return this.clazz.getResource(this.path);
		}
		else if (this.classLoader != null) {
			return this.classLoader.getResource(this.path);
		}
		else {
			return ClassLoader.getSystemResource(this.path);
		}
	}

	/**
	 * 此实现为给定的类路径资源打开一个InputStream.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is;
		if (this.clazz != null) {
			is = this.clazz.getResourceAsStream(this.path);
		}
		else if (this.classLoader != null) {
			is = this.classLoader.getResourceAsStream(this.path);
		}
		else {
			is = ClassLoader.getSystemResourceAsStream(this.path);
		}
		if (is == null) {
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return is;
	}

	/**
	 * 此实现返回底层类路径资源的URL.
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = resolveURL();
		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * 此实现创建一个ClassPathResource, 相对于此描述符的底层资源的路径应用给定路径.
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return (this.clazz != null ? new ClassPathResource(pathToUse, this.clazz) :
				new ClassPathResource(pathToUse, this.classLoader));
	}

	/**
	 * 此实现返回此类路径资源引用的文件的名称.
	 */
	@Override
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	/**
	 * 此实现返回包含类路径位置的描述.
	 */
	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder("class path resource [");
		String pathToUse = path;
		if (this.clazz != null && !pathToUse.startsWith("/")) {
			builder.append(ClassUtils.classPackageAsResourcePath(this.clazz));
			builder.append('/');
		}
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		builder.append(pathToUse);
		builder.append(']');
		return builder.toString();
	}


	/**
	 * 此实现比较底层类路径位置.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ClassPathResource) {
			ClassPathResource otherRes = (ClassPathResource) obj;
			return (this.path.equals(otherRes.path) &&
					ObjectUtils.nullSafeEquals(this.classLoader, otherRes.classLoader) &&
					ObjectUtils.nullSafeEquals(this.clazz, otherRes.clazz));
		}
		return false;
	}

	/**
	 * 此实现返回底层类路径位置的哈希码.
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}
}
