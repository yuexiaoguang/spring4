package org.springframework.web.context.support;

import java.util.Map;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.context.ServletContextAware;

/**
 * 导出器, 接受Spring定义的对象, 并将它们公开为ServletContext属性.
 * 通常, bean引用将用于将Spring定义的bean导出为ServletContext属性.
 *
 * <p>有用的是使Spring定义的bean可用于完全不了解Spring的代码, 而不只是Servlet API.
 * 然后, 客户端代码可以使用普通的ServletContext属性查找来访问这些对象, 尽管它们是在Spring应用程序上下文中定义的.
 *
 * <p>或者, 考虑使用WebApplicationContextUtils类通过WebApplicationContext接口访问Spring定义的bean.
 * 当然, 这使得客户端代码可以识别Spring API.
 */
public class ServletContextAttributeExporter implements ServletContextAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private Map<String, Object> attributes;


	/**
	 * 将ServletContext属性.
	 * 每个键都将被视为ServletContext属性键, 每个值将被用作相应的属性值.
	 * <p>通常, 将对值使用bean引用, 以将Spring定义的bean导出为ServletContext属性.
	 * 当然, 也可以定义要导出的普通值.
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.attributes != null) {
			for (Map.Entry<String, Object> entry : this.attributes.entrySet()) {
				String attributeName = entry.getKey();
				if (logger.isWarnEnabled()) {
					if (servletContext.getAttribute(attributeName) != null) {
						logger.warn("Replacing existing ServletContext attribute with name '" + attributeName + "'");
					}
				}
				servletContext.setAttribute(attributeName, entry.getValue());
				if (logger.isInfoEnabled()) {
					logger.info("Exported ServletContext attribute with name '" + attributeName + "'");
				}
			}
		}
	}

}
