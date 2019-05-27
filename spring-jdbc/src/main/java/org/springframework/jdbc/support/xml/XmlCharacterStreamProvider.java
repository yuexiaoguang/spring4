package org.springframework.jdbc.support.xml;

import java.io.IOException;
import java.io.Writer;

/**
 * 定义为XML输入提供{@code Writer}数据的接口.
 */
public interface XmlCharacterStreamProvider {

	/**
	 * 实现必须实现此方法, 以便为{@code Writer}提供XML内容.
	 * 
	 * @param writer 用于提供XML输入的{@code Writer}对象
	 * 
	 * @throws IOException 如果在提供XML时发生I/O错误
	 */
	void provideXml(Writer writer) throws IOException;

}
