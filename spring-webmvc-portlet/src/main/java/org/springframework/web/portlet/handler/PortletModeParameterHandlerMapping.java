package org.springframework.web.portlet.handler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;

import org.springframework.beans.BeansException;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.web.portlet.HandlerMapping}接口的实现,
 * 从当前的PortletMode和请求参数映射到请求处理器bean.
 * 映射由两个级别组成: 首先是PortletMode, 然后是参数值.
 * 为了映射, 两个元素必须匹配映射定义.
 *
 * <p>这是{@link PortletModeHandlerMapping PortletModeHandlerMapping}
 * 和{@link ParameterHandlerMapping ParameterHandlerMapping}中使用的方法的组合.
 * 与这两个类不同, 此映射无法使用属性进行初始化, 因为它需要两级映射.
 *
 * <p>参数的默认名称是"action", 但可以使用{@link #setParameterName setParameterName()}进行更改.
 *
 * <p>默认情况下, 相同的参数值不能用于两种不同的portlet模式.
 * 这样, 如果portal本身更改了portlet模式, 则该请求将不再在映射中有效.
 * 可以使用{@link #setAllowDuplicateParameters setAllowDupParameters()}更改此行为.
 *
 * <p>此映射的bean配置看起来像这样:
 *
 * <pre class="code">
 * &lt;bean id="portletModeParameterHandlerMapping" class="org.springframework.web.portlet.handler.PortletModeParameterHandlerMapping"&gt;
 *   &lt;property name="portletModeParameterMap"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="view"&gt; &lt;!-- portlet mode: view --&gt;
 *         &lt;map&gt;
 *           &lt;entry key="add"&gt;&lt;ref bean="addItemHandler"/&gt;&lt;/entry&gt;
 *           &lt;entry key="edit"&gt;&lt;ref bean="editItemHandler"/&gt;&lt;/entry&gt;
 *           &lt;entry key="delete"&gt;&lt;ref bean="deleteItemHandler"/&gt;&lt;/entry&gt;
 *         &lt;/map&gt;
 *       &lt;/entry&gt;
 *       &lt;entry key="edit"&gt; &lt;!-- portlet mode: edit --&gt;
 *         &lt;map&gt;
 *           &lt;entry key="prefs"&gt;&lt;ref bean="preferencesHandler"/&gt;&lt;/entry&gt;
 *           &lt;entry key="resetPrefs"&gt;&lt;ref bean="resetPreferencesHandler"/&gt;&lt;/entry&gt;
 *         &lt;/map&gt;
 *       &lt;/entry&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>这个映射可以在{@link PortletModeHandlerMapping PortletModeHandlerMapping}之前链接,
 * 然后可以为每个模式提供默认值, 也可以提供整体默认值.
 *
 * <p>Thanks to Rainer Schmitz and Yujin Kim for suggesting this mapping strategy!
 */
public class PortletModeParameterHandlerMapping extends AbstractMapBasedHandlerMapping<PortletModeParameterLookupKey> {

	/**
	 * 用于映射到处理器的默认请求参数名称: "action".
	 */
	public final static String DEFAULT_PARAMETER_NAME = "action";


	private String parameterName = DEFAULT_PARAMETER_NAME;

	private Map<String, Map<String, ?>> portletModeParameterMap;

	private boolean allowDuplicateParameters = false;

	private final Set<String> parametersUsed = new HashSet<String>();


	/**
	 * 设置用于映射到处理器的参数的名称.
	 * <p>默认为"action".
	 */
	public void setParameterName(String parameterName) {
		Assert.hasText(parameterName, "'parameterName' must not be empty");
		this.parameterName = parameterName;
	}

	/**
	 * 设置Map, 将Portlet模式名称作为键, 将另一个Map作为值.
	 * 子Map将参数名称作为键, 将处理器bean或bean名称作为值.
	 * <p>方便填充bean引用.
	 * 
	 * @param portletModeParameterMap portlet模式和处理器bean参数的两级映射
	 */
	public void setPortletModeParameterMap(Map<String, Map<String, ?>> portletModeParameterMap) {
		this.portletModeParameterMap = portletModeParameterMap;
	}

	/**
	 * 设置是否允许跨不同portlet模式的重复的参数值.
	 * 默认为"false".
	 * <p>这样做很危险, 因为portal本身可以更改portlet模式, 唯一可以看到这些变化的方法是portlet的重新渲染.
	 * 如果在多种模式下相同的参数值是合法的, 那么模式的改变可能导致匹配的映射不是预期的.
	 */
	public void setAllowDuplicateParameters(boolean allowDuplicateParameters) {
		this.allowDuplicateParameters = allowDuplicateParameters;
	}


	/**
	 * 除了超类的初始化之外, 还调用{@code registerHandlers}方法.
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlersByModeAndParameter(this.portletModeParameterMap);
	}

	/**
	 * 注册Portlet模式映射中为相应模式指定的所有处理器.
	 * 
	 * @param portletModeParameterMap 将模式名称作为键, 将参数Map作为值
	 */
	protected void registerHandlersByModeAndParameter(Map<String, Map<String, ?>> portletModeParameterMap) {
		Assert.notNull(portletModeParameterMap, "'portletModeParameterMap' must not be null");
		for (Map.Entry<String, Map<String, ?>> entry : portletModeParameterMap.entrySet()) {
			PortletMode mode = new PortletMode(entry.getKey());
			registerHandler(mode, entry.getValue());
		}
	}

	/**
	 * 注册给定参数Map中指定的所有处理器.
	 * 
	 * @param parameterMap 将参数名称作为键, 将处理器bean或bean名称作为值
	 */
	protected void registerHandler(PortletMode mode, Map<String, ?> parameterMap) {
		for (Map.Entry<String, ?> entry : parameterMap.entrySet()) {
			registerHandler(mode, entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 在适当的查找键下, 为给定的PortletMode和参数值注册给定的处理器实例.
	 * 
	 * @param mode 此映射有效的PortletMode
	 * @param parameter 此处理器映射到的参数值
	 * @param handler 处理器实例bean
	 * 
	 * @throws BeansException 如果处理器无法注册
	 * @throws IllegalStateException 如果注册的处理器存在冲突
	 */
	protected void registerHandler(PortletMode mode, String parameter, Object handler)
			throws BeansException, IllegalStateException {

		// 检查所有portlet模式下的重复的参数值.
		if (!this.allowDuplicateParameters && this.parametersUsed.contains(parameter)) {
			throw new IllegalStateException(
					"Duplicate entries for parameter [" + parameter + "] in different Portlet modes");
		}
		this.parametersUsed.add(parameter);

		registerHandler(new PortletModeParameterLookupKey(mode, parameter), handler);
	}

	/**
	 * 返回组合当前PortletMode和指定参数的当前值的查找键.
	 */
	@Override
	protected PortletModeParameterLookupKey getLookupKey(PortletRequest request) throws Exception {
		PortletMode mode = request.getPortletMode();
		String parameter = request.getParameter(this.parameterName);
		return new PortletModeParameterLookupKey(mode, parameter);
	}
}
