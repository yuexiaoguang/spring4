package org.springframework.cache.interceptor;

import java.util.Set;

/**
 * 所有缓存操作必须实现的基础接口.
 */
public interface BasicOperation {

	/**
	 * 返回与操作关联的缓存名称.
	 */
	Set<String> getCacheNames();

}
