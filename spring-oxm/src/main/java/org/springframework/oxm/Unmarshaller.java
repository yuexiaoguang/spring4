package org.springframework.oxm;

import java.io.IOException;
import javax.xml.transform.Source;

/**
 * 定义Object XML Mapping 解组器的约定.
 * 此接口的实现可以将给定的XML流反序列化为Object图.
 */
public interface Unmarshaller {

	/**
	 * 指示此解组器是否可以解组所提供类型的实例.
	 * 
	 * @param clazz 是否可以解组的类
	 * 
	 * @return {@code true} 如果这个解组器确实可以解组所提供的类; 否则{@code false}
	 */
	boolean supports(Class<?> clazz);

	/**
	 * 将给定的{@link Source}解组到对象图中.
	 * 
	 * @param source 要解组的源
	 * 
	 * @return 对象图
	 * @throws IOException 如果发生I/O错误
	 * @throws XmlMappingException 如果给定的源无法映射到对象
	 */
	Object unmarshal(Source source) throws IOException, XmlMappingException;

}
