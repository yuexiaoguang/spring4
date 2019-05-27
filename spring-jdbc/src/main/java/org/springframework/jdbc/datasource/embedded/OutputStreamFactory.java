package org.springframework.jdbc.datasource.embedded;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 内部帮助器, 用于将虚拟OutputStream暴露给嵌入式数据库, 如 Derby, 从而阻止创建日志文件.
 */
public class OutputStreamFactory {

	/**
	 * 返回{@link java.io.OutputStream}, 忽略提供给它的所有数据.
	 */
	public static OutputStream getNoopOutputStream() {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				// 忽略输出
			}
		};
	}
}
