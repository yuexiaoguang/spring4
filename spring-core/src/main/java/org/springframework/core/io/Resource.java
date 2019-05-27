package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * 从底层资源的实际类型(如文件或类路径资源)中抽象出来的资源描述符的接口.
 *
 * <p>如果InputStream以物理形式存在, 则可以为每个资源打开, 但只能为某些资源返回URL或File句柄.
 * 实际行为是特定于实现的.
 */
public interface Resource extends InputStreamSource {

	/**
	 * 确定此资源是否实际以物理形式存在.
	 * <p>此方法执行确定的存在性检查, 而{@code Resource}句柄的存在仅保证有效的描述符句柄.
	 */
	boolean exists();

	/**
	 * 指示是否可以通过{@link #getInputStream()}读取此资源的内容.
	 * <p>对于典型的资源描述符, 将是{@code true};
	 * 请注意, 尝试读取实际内容时, 仍然可能失败.
	 * 但是, {@code false}值明确指示无法读取资源内容.
	 */
	boolean isReadable();

	/**
	 * 指示此资源是否表示具有打开的流的句柄.
	 * 如果{@code true}, 则无法多次读取InputStream, 必须读取并关闭它以避免资源泄漏.
	 * <p>对于典型的资源描述符, 将为{@code false}.
	 */
	boolean isOpen();

	/**
	 * 返回此资源的URL句柄.
	 * 
	 * @throws IOException 如果资源无法解析为URL, 即资源不可用作描述符
	 */
	URL getURL() throws IOException;

	/**
	 * 返回此资源的URI句柄.
	 * 
	 * @throws IOException 如果资源无法解析为URI, 即资源不可用作描述符
	 */
	URI getURI() throws IOException;

	/**
	 * 返回此资源的File句柄.
	 * 
	 * @throws java.io.FileNotFoundException 如果资源无法解析为绝对文件路径, 即资源在文件系统中不可用
	 * @throws IOException 在一般解析/读取失败的情况下
	 */
	File getFile() throws IOException;

	/**
	 * 确定此资源的内容长度.
	 * 
	 * @throws IOException 如果资源无法解析 (在文件系统中或作为一些其他已知的物理资源类型)
	 */
	long contentLength() throws IOException;

	/**
	 * 确定此资源的上次修改时间戳.
	 * 
	 * @throws IOException 如果资源无法解析 (在文件系统中或作为一些其他已知的物理资源类型)
	 */
	long lastModified() throws IOException;

	/**
	 * 创建相对于此资源的资源.
	 * 
	 * @param relativePath 相对路径 (相对于此资源)
	 * 
	 * @return 相对资源的资源句柄
	 * @throws IOException 如果无法确定相对资源
	 */
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * 确定此资源的文件名, i.e. 通常是路径的最后一部分: 例如, "myfile.txt".
	 * <p>如果此类资源没有文件名, 则返回{@code null}.
	 */
	String getFilename();

	/**
	 * 返回此资源的描述, 以便在使用资源时用于错误输出.
	 * <p>还鼓励实现从其{@code toString}方法返回此值.
	 */
	String getDescription();

}
