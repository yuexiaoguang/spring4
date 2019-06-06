package org.springframework.oxm.mime;

import java.io.IOException;
import javax.xml.transform.Result;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;

/**
 * {@link Marshaller}的子接口, 可以使用MIME附件来优化二进制数据的存储.
 * 附件可以添加为MTOM, XOP, 或SwA.
 */
public interface MimeMarshaller extends Marshaller {

	/**
	 * 使用给定的根将对象图编码到提供的{@link Result}中, 将二进制数据写入{@link MimeContainer}.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param result 要编组到的对象
	 * @param mimeContainer 用于将提取的二进制内容写入的MIME容器
	 * 
	 * @throws XmlMappingException 如果给定的对象无法编组到结果中
	 * @throws IOException 如果发生I/O异常
	 */
	void marshal(Object graph, Result result, MimeContainer mimeContainer) throws XmlMappingException, IOException;

}
