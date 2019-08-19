package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link ServletRequestDataBinder}的子类, 它将URI模板变量添加到用于数据绑定的值.
 */
public class ExtendedServletRequestDataBinder extends ServletRequestDataBinder {

	/**
	 * 使用默认的对象名称.
	 * 
	 * @param target 绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 */
	public ExtendedServletRequestDataBinder(Object target) {
		super(target);
	}

	/**
	 * @param target 绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 * @param objectName 目标对象的名称
	 */
	public ExtendedServletRequestDataBinder(Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将URI变量合并到属性值中以用于数据绑定.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
		String attr = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVars = (Map<String, String>) request.getAttribute(attr);
		if (uriVars != null) {
			for (Entry<String, String> entry : uriVars.entrySet()) {
				if (mpvs.contains(entry.getKey())) {
					if (logger.isWarnEnabled()) {
						logger.warn("Skipping URI variable '" + entry.getKey() +
								"' since the request contains a bind value with the same name.");
					}
				}
				else {
					mpvs.addPropertyValue(entry.getKey(), entry.getValue());
				}
			}
		}
	}

}
