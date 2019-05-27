package org.springframework.core.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 支持写入资源的扩展接口.
 * 提供{@link #getOutputStream() OutputStream 访问器}.
 */
public interface WritableResource extends Resource {

	/**
	 * 返回是否可以修改此资源的内容,
	 * e.g. 通过{@link #getOutputStream()}或{@link #getFile()}.
	 * <p>对于典型的资源描述符, 将是{@code true};
	 * 请注意, 尝试写入实际内容时, 仍然可能失败.
	 * 但是, {@code false}的值是无法修改资源内容的明确指示.
	 */
	boolean isWritable();

	/**
	 * 返回底层资源的{@link OutputStream}, 允许(过度)写入其内容.
	 * 
	 * @throws IOException 如果流无法打开
	 */
	OutputStream getOutputStream() throws IOException;

}
