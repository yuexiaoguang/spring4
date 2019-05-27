package org.springframework.core.io;

/**
 * {@link ResourceLoader}实现, 它将普通路径解析为文件系统资源, 而不是类路径资源
 * (后者是{@link DefaultResourceLoader}的默认策略).
 *
 * <p><b>NOTE:</b> 普通路径将始终被解释为相对于当前VM工作目录, 即使它们以斜杠开头.
 * (这与Servlet容器中的语义一致.)
 * <b>使用显式 "file:" 前缀来强制执行绝对文件路径.</b>
 *
 * <p>{@link org.springframework.context.support.FileSystemXmlApplicationContext}
 * 是一个成熟的ApplicationContext实现, 它提供相同的资源路径解析策略.
 */
public class FileSystemResourceLoader extends DefaultResourceLoader {

	/**
	 * 将资源路径解析为文件系统路径.
	 * <p>Note: 即使给定路径以斜杠开头, 它也会被解释为相对于当前VM工作目录.
	 * 
	 * @param path 资源的路径
	 * 
	 * @return 相应的资源句柄
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		if (path != null && path.startsWith("/")) {
			path = path.substring(1);
		}
		return new FileSystemContextResource(path);
	}


	/**
	 * 通过实现ContextResource接口显式表达上下文相关路径.
	 */
	private static class FileSystemContextResource extends FileSystemResource implements ContextResource {

		public FileSystemContextResource(String path) {
			super(path);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}
	}

}
