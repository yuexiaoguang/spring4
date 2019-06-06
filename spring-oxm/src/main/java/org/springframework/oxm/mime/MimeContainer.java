package org.springframework.oxm.mime;

import javax.activation.DataHandler;

/**
 * 表示MIME附件的容器.
 * 具体实现可能会调整SOAPMessage或电子邮件消息.
 */
public interface MimeContainer {

	/**
	 * 指示此容器是否为XOP包.
	 * 
	 * @return {@code true} 当满足
	 * <a href="http://www.w3.org/TR/2005/REC-xop10-20050125/#identifying_xop_documents">Identifying XOP Documents</a>
	 * 中指定的约束时
	 */
	boolean isXopPackage();

	/**
	 * 将此消息转换为XOP包.
	 * 
	 * @return {@code true} 当消息实际上是XOP包时
	 */
	boolean convertToXopPackage();

	/**
	 * 将给定的数据处理器添加为此容器的附件.
	 * 
	 * @param contentId  附件的内容ID
	 * @param dataHandler 包含附件数据的数据处理器
	 */
	void addAttachment(String contentId, DataHandler dataHandler);

	/**
	 * 返回具有给定内容ID的附件, 或{@code null}.
	 * 
	 * @param contentId 内容id
	 * 
	 * @return 附件
	 */
	DataHandler getAttachment(String contentId);

}
