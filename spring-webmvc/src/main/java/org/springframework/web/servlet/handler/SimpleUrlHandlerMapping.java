package org.springframework.web.servlet.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.util.CollectionUtils;

/**
 * {@link org.springframework.web.servlet.HandlerMapping}接口的实现, 从URL映射到请求处理器bean.
 * 支持映射到bean实例和映射到bean名称; 后者是非单例处理器所必需的.
 *
 * <p>"urlMap"属性适合用bean引用填充处理器映射, e.g. 通过XML bean定义中的map元素.
 *
 * <p>bean名称的映射可以通过"mappings"属性以{@code java.util.Properties}类接受的形式设置, 如下所示:<br>
 * {@code
 * /welcome.html=ticketController
 * /show.html=ticketController
 * }<br>
 * 语法为{@code PATH=HANDLER_BEAN_NAME}. 如果路径不以斜杠开头, 则加一个.
 *
 * <p>支持直接匹配 (给定的 "/test" -> 注册的 "/test") 和"*"模式匹配 (给定的"/test" -> 注册的"/t*").
 * 请注意，默认情况下是在当前servlet映射中映射;
 * 请参阅{@link #setAlwaysUseFullPath "alwaysUseFullPath"}属性.
 * 有关模式选项的详细信息, 请参阅{@link org.springframework.util.AntPathMatcher} javadoc.
 */
public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {

	private final Map<String, Object> urlMap = new LinkedHashMap<String, Object>();


	/**
	 * 映射URL路径到处理器 bean 名称.
	 * 这是配置此HandlerMapping的典型方法.
	 * <p>支持直接URL匹配和Ant样式模式匹配.
	 * 有关语法详细信息, 请参阅{@link org.springframework.util.AntPathMatcher} javadoc.
	 * 
	 * @param mappings 将URL作为键, 将bean名称作为值的属性
	 */
	public void setMappings(Properties mappings) {
		CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
	}

	/**
	 * 将URL路径作为键, 将处理器bean(或处理程序bean名称)作为值.
	 * 方便使用Bean引用填充.
	 * <p>支持直接URL匹配和Ant样式模式匹配.
	 * 有关语法详细信息, 请参阅{@link org.springframework.util.AntPathMatcher} javadoc.
	 * 
	 * @param urlMap 将URL作为键, 将bean作为值
	 */
	public void setUrlMap(Map<String, ?> urlMap) {
		this.urlMap.putAll(urlMap);
	}

	/**
	 * 允许访问URL路径映射, 并添加或覆盖特定条目.
	 * <p>用于直接指定条目, 例如通过"urlMap[myKey]".
	 * 这对于在子bean定义中添加或覆盖条目特别有用.
	 */
	public Map<String, ?> getUrlMap() {
		return this.urlMap;
	}


	/**
	 * 除了超类的初始化之外, 还调用{@link #registerHandlers}方法.
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.urlMap);
	}

	/**
	 * 注册URL映射中为相应路径指定的所有处理器.
	 * 
	 * @param urlMap 将URL路径作为键, 将处理器bean或bean名称作为值
	 * 
	 * @throws BeansException 如果处理器无法注册
	 * @throws IllegalStateException 如果存在冲突的处理器注册
	 */
	protected void registerHandlers(Map<String, Object> urlMap) throws BeansException {
		if (urlMap.isEmpty()) {
			logger.warn("Neither 'urlMap' nor 'mappings' set on SimpleUrlHandlerMapping");
		}
		else {
			for (Map.Entry<String, Object> entry : urlMap.entrySet()) {
				String url = entry.getKey();
				Object handler = entry.getValue();
				// Prepend with slash if not already present.
				if (!url.startsWith("/")) {
					url = "/" + url;
				}
				// Remove whitespace from handler bean name.
				if (handler instanceof String) {
					handler = ((String) handler).trim();
				}
				registerHandler(url, handler);
			}
		}
	}

}
