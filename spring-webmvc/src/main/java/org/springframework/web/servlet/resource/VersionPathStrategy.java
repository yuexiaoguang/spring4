package org.springframework.web.servlet.resource;

/**
 * 用于在其URL路径中提取和嵌入资源版本的策略.
*/
public interface VersionPathStrategy {

	/**
	 * 从请求路径中提取资源版本.
	 * 
	 * @param requestPath 要检查的请求路径
	 * 
	 * @return 版本字符串或{@code null}
	 */
	String extractVersion(String requestPath);

	/**
	 * 从请求路径中删除版本.
	 * 假设通过{@link #extractVersion(String)}提取给定版本.
	 * 
	 * @param requestPath 正在解析的资源的请求路径
	 * @param version 从{@link #extractVersion(String)}获得的版本
	 * 
	 * @return 已删除版本的请求路径
	 */
	String removeVersion(String requestPath, String version);

	/**
	 * 将版本添加到给定的请求路径.
	 * 
	 * @param requestPath the requestPath
	 * @param version 版本
	 * 
	 * @return 已添加版本字符串的requestPath
	 */
	String addVersion(String requestPath, String version);

}
