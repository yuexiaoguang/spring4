package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

/**
 * 描述其版本的资源描述符的接口, 其版本字符串可以从其内容和/或元数据派生.
 */
public interface VersionedResource extends Resource {

	String getVersion();

}
