package org.springframework.core.io;

/**
 * 从封闭的'上下文'加载资源的扩展接口, e.g. 从{@link javax.servlet.ServletContext}
 * 或{@link javax.portlet.PortletContext}, 但也从普通类路径路径或相关文件系统路径
 * (指定时没有显式前缀, 因此相对于本地{@link ResourceLoader}的上下文).
 */
public interface ContextResource extends Resource {

	/**
	 * 返回封闭'上下文'中的路径.
	 * <p>这通常是相对于特定于上下文的根目录的路径, e.g. ServletContext根目录或PortletContext根目录.
	 */
	String getPathWithinContext();

}
