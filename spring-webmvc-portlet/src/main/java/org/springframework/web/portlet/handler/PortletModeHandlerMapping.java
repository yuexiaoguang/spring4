package org.springframework.web.portlet.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;

import org.springframework.beans.BeansException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link org.springframework.web.portlet.HandlerMapping}接口的实现, 从当前的PortletMode映射到请求处理器bean.
 *
 * <p>此映射的bean配置看起来像这样:
 * <pre class="code">
 * 	&lt;bean id="portletModeHandlerMapping" class="org.springframework.web.portlet.handler.PortletModeHandlerMapping"&gt;
 * 		&lt;property name="portletModeMap"&gt;
 * 			&lt;map&gt;
 * 				&lt;entry key="view"&gt;&lt;ref bean="viewHandler"/&gt;&lt;/entry&gt;
 * 				&lt;entry key="edit"&gt;&lt;ref bean="editHandler"/&gt;&lt;/entry&gt;
 * 				&lt;entry key="help"&gt;&lt;ref bean="helpHandler"/&gt;&lt;/entry&gt;
 * 			&lt;/map&gt;
 * 		&lt;/property&gt;
 * 	&lt;/bean&gt;
 * </pre>
 */
public class PortletModeHandlerMapping extends AbstractMapBasedHandlerMapping<PortletMode> {

	private final Map<String, Object> portletModeMap = new HashMap<String, Object>();


	/**
	 * 设置PortletMode到处理器bean名称的映射.
	 * 
	 * @param mappings 将PortletMode名称作为键, 将bean名称作为值
	 */
	public void setMappings(Properties mappings) {
		CollectionUtils.mergePropertiesIntoMap(mappings, this.portletModeMap);
	}

	/**
	 * 设置Map, 将PortletModes作为键, 将处理器bean作为值.
	 * 方便填充bean 引用.
	 * 
	 * @param portletModeMap 将PortletMode名称作为键, 将bean或bean名称作为值
	 */
	public void setPortletModeMap(Map<String, ?> portletModeMap) {
		this.portletModeMap.putAll(portletModeMap);
	}


	/**
	 * 除了超类的初始化之外, 还调用{@code registerHandlers}方法.
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlersByMode(this.portletModeMap);
	}

	/**
	 * 注册Portlet模式映射中为相应模式指定的所有处理器.
	 * 
	 * @param portletModeMap 将模式名称作为键, 将处理器bean或bean名称作为值
	 */
	protected void registerHandlersByMode(Map<String, Object> portletModeMap) {
		Assert.notNull(portletModeMap, "'portletModeMap' must not be null");
		for (Map.Entry<String, Object> entry : portletModeMap.entrySet()) {
			registerHandler(new PortletMode(entry.getKey()), entry.getValue());
		}
	}


	/**
	 * 使用当前的PortletMode作为查找键.
	 */
	@Override
	protected PortletMode getLookupKey(PortletRequest request) throws Exception {
		return request.getPortletMode();
	}

}
