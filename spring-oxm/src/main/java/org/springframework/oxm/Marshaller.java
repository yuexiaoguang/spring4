package org.springframework.oxm;

import java.io.IOException;
import javax.xml.transform.Result;

/**
 * 定义对象XML映射Marshaller的约定.
 * 此接口的实现可以将给定对象序列化为XML流.
 *
 * <p>虽然{@code marshal}方法接受{@code java.lang.Object}作为其第一个参数,
 * 但大多数{@code Marshaller}实现无法处理任意{@code Object}.
 * 相反, 必须在marshaller中注册一个对象类, 或者具有一个公共基类.
 */
public interface Marshaller {

	/**
	 * 指示此编组器是否可以编组所提供类型的实例.
	 * 
	 * @param clazz 是否能够编组的类
	 * 
	 * @return {@code true} 如果这个marshaller确实可以编组提供的类的实例; 否则{@code false}
	 */
	boolean supports(Class<?> clazz);

	/**
	 * 将具有给定根的对象图编组到提供的{@link Result}中.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param result 要编组到的结果
	 * 
	 * @throws IOException 如果发生I/O错误
	 * @throws XmlMappingException 如果给定的对象无法编组到结果中
	 */
	void marshal(Object graph, Result result) throws IOException, XmlMappingException;

}
