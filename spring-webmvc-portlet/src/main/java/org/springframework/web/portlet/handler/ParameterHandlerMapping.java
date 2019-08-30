package org.springframework.web.portlet.handler;

import java.util.Map;
import javax.portlet.PortletRequest;

import org.springframework.beans.BeansException;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.web.portlet.HandlerMapping}的实现, 从请求参数映射到请求处理器bean.
 *
 * <p>参数的默认名称是"action", 但可以使用{@link #setParameterName setParameterName()}进行更改.
 *
 * <p>此映射的bean配置看起来像这样:
 *
 * <pre class="code">
 * &lt;bean id="parameterHandlerMapping" class="org.springframework.web.portlet.handler.ParameterHandlerMapping"&gt;
 *   &lt;property name="parameterMap"&gt;
 *     &lt;map&gt;
 * 	     &lt;entry key="add"&gt;&lt;ref bean="addItemHandler"/&gt;&lt;/entry&gt;
 *       &lt;entry key="edit"&gt;&lt;ref bean="editItemHandler"/&gt;&lt;/entry&gt;
 *       &lt;entry key="delete"&gt;&lt;ref bean="deleteItemHandler"/&gt;&lt;/entry&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * Thanks to Rainer Schmitz for suggesting this mapping strategy!
 */
public class ParameterHandlerMapping extends AbstractMapBasedHandlerMapping<String> {

	/**
	 * 用于映射到处理器的默认请求参数名称: "action".
	 */
	public final static String DEFAULT_PARAMETER_NAME = "action";


	private String parameterName = DEFAULT_PARAMETER_NAME;

	private Map<String, ?> parameterMap;


	/**
	 * 设置用于映射到处理器的参数的名称.
	 * <p>默认为"action".
	 */
	public void setParameterName(String parameterName) {
		Assert.hasText(parameterName, "'parameterName' must not be empty");
		this.parameterName = parameterName;
	}

	/**
	 * 设置Map, 将参数作为键, 将处理器bean或bean名称作为值.
	 * 方便填充bean引用.
	 * 
	 * @param parameterMap 将参数作为键, 将bean作为值
	 */
	public void setParameterMap(Map<String, ?> parameterMap) {
		this.parameterMap = parameterMap;
	}


	/**
	 * 除了超类的初始化之外, 还调用{@code registerHandlers}方法.
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.parameterMap);
	}

	/**
	 * 使用指定参数的值作为查找键.
	 */
	@Override
	protected String getLookupKey(PortletRequest request) throws Exception {
		return request.getParameter(this.parameterName);
	}
}
