package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

/**
 * 描述应用于整个资源内容的编码的资源描述符的接口.
 *
 * <p>如果使用该资源的客户端需要额外的解码功能来检索资源的内容, 则需要此信息.
 */
public interface EncodedResource extends Resource {

	/**
	 * 内容编码值, 如IANA注册中心所定义
	 * 
	 * @return 内容编码
	 */
	String getContentEncoding();

}
