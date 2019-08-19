package org.springframework.web.servlet.view.freemarker;

import freemarker.ext.jsp.TaglibFactory;
import freemarker.template.Configuration;

/**
 * 由在Web环境中配置和管理FreeMarker配置对象的对象实现的接口.
 * 由{@link FreeMarkerView}检测并使用.
 */
public interface FreeMarkerConfig {

	/**
	 * 返回当前Web应用程序上下文的FreeMarker {@link Configuration}对象.
	 * <p>FreeMarker配置对象可用于设置FreeMarker属性和共享对象, 并允许检索模板.
	 * 
	 * @return FreeMarker Configuration
	 */
	Configuration getConfiguration();

	/**
	 * 返回用于启用从FreeMarker模板访问JSP标记的{@link TaglibFactory}.
	 */
	TaglibFactory getTaglibFactory();

}
