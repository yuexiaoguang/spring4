package org.springframework.jdbc.support.xml;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 定义为XML输入提供{@code OutputStream}数据的接口.
 */
public interface XmlBinaryStreamProvider {

	/**
	 * 实现必须实现此方法, 以便为{@code OutputStream}提供XML内容.
	 * 
	 * @param outputStream 用于提供XML输入的{@code OutputStream}对象
	 * 
	 * @throws IOException 如果在提供XML时发生I/O错误
	 */
	void provideXml(OutputStream outputStream) throws IOException;

}
