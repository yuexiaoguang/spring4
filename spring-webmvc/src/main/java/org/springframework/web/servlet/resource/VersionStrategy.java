package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

/**
 * {@link VersionPathStrategy}的扩展, 它添加了一种方法来确定{@link Resource}的实际版本.
*/
public interface VersionStrategy extends VersionPathStrategy {

	/**
	 * 确定给定资源的版本.
	 * 
	 * @param resource 要检查的资源
	 * 
	 * @return 版本 (never {@code null})
	 */
	String getResourceVersion(Resource resource);

}
