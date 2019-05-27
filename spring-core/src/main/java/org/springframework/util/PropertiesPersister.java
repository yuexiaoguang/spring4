package org.springframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * 用于持久化{@code java.util.Properties}的策略接口, 允许可插入的解析策略.
 *
 * <p>默认实现是DefaultPropertiesPersister, 提供{@code java.util.Properties}的本机解析,
 * 但允许从任何Reader读取并写入任何Writer (允许指定属性文件的编码).
 */
public interface PropertiesPersister {

	/**
	 * 将给定InputStream的属性加载到给定的Properties对象中.
	 * 
	 * @param props 要加载到的Properties对象
	 * @param is 要加载的InputStream
	 * 
	 * @throws IOException 发生I/O 错误
	 */
	void load(Properties props, InputStream is) throws IOException;

	/**
	 * 将给定Reader中的属性加载到给定的Properties对象中.
	 * 
	 * @param props 要加载到的Properties对象
	 * @param reader 要加载的Reader
	 * 
	 * @throws IOException 发生I/O 错误
	 */
	void load(Properties props, Reader reader) throws IOException;

	/**
	 * 将给定Properties对象的内容写入给定的OutputStream.
	 * 
	 * @param props 要存储的Properties对象
	 * @param os 要写入的OutputStream
	 * @param header 属性列表的描述
	 * 
	 * @throws IOException 发生I/O 错误
	 */
	void store(Properties props, OutputStream os, String header) throws IOException;

	/**
	 * 将给定Properties对象的内容写入给定的Writer.
	 * 
	 * @param props 要存储的Properties对象
	 * @param writer 要写入的Writer
	 * @param header 属性列表的描述
	 * 
	 * @throws IOException 发生I/O 错误
	 */
	void store(Properties props, Writer writer, String header) throws IOException;

	/**
	 * 将给定XML InputStream中的属性加载到给定的Properties对象中.
	 * 
	 * @param props 要加载到的Properties对象
	 * @param is 要加载的InputStream
	 * 
	 * @throws IOException 发生I/O 错误
	 */
	void loadFromXml(Properties props, InputStream is) throws IOException;

	/**
	 * 将给定Properties对象的内容写入给定的XML OutputStream.
	 * 
	 * @param props 要存储的Properties对象
	 * @param os 要写入的OutputStream
	 * @param header 属性列表的描述
	 * 
	 * @throws IOException 发生I/O 错误
	 */
	void storeToXml(Properties props, OutputStream os, String header) throws IOException;

	/**
	 * 将给定Properties对象的内容写入给定的XML OutputStream.
	 * 
	 * @param props 要存储的Properties对象
	 * @param os 要写入的OutputStream
	 * @param encoding 要使用的编码
	 * @param header 属性列表的描述
	 * 
	 * @throws IOException 发生I/O 错误
	 */
	void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException;

}
