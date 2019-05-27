package org.springframework.core.env;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 未找到所需属性时抛出异常.
 */
@SuppressWarnings("serial")
public class MissingRequiredPropertiesException extends IllegalStateException {

	private final Set<String> missingRequiredProperties = new LinkedHashSet<String>();


	void addMissingRequiredProperty(String key) {
		this.missingRequiredProperties.add(key);
	}

	@Override
	public String getMessage() {
		return "The following properties were declared as required but could not be resolved: " +
				getMissingRequiredProperties();
	}

	/**
	 * 返回标记为必需但在验证时不存在的属性集.
	 */
	public Set<String> getMissingRequiredProperties() {
		return this.missingRequiredProperties;
	}

}
