package org.springframework.oxm.mime;

import java.io.IOException;
import javax.xml.transform.Source;

import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

/**
 * {@link org.springframework.oxm.Unmarshaller}的子接口, 可以使用MIME附件优化二进制数据的存储.
 * 附件可以添加为 MTOM, XOP, 或 SwA.
 */
public interface MimeUnmarshaller extends Unmarshaller {

	/**
	 * 将给定的{@link Source}解组到对象图中, 从{@link MimeContainer}读取二进制附件.
	 * 
	 * @param source 要解组的源
	 * @param mimeContainer 用于从中读取提取的二进制内容的MIME容器
	 * 
	 * @return 对象图
	 * @throws XmlMappingException 如果给定的源无法映射到对象
	 * @throws IOException 如果发生I/O异常
	 */
	Object unmarshal(Source source, MimeContainer mimeContainer) throws XmlMappingException, IOException;

}
