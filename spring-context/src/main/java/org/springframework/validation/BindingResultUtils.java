package org.springframework.validation;

import java.util.Map;

import org.springframework.util.Assert;

/**
 * 在模型Map中查找BindingResults的方法.
 */
public abstract class BindingResultUtils {

	/**
	 * 在给定模型中查找给定名称的BindingResult.
	 * 
	 * @param model 要搜索的模型
	 * @param name 要查找BindingResult的目标对象的名称
	 * 
	 * @return BindingResult, 或{@code null}
	 * @throws IllegalStateException 如果找到的属性不是BindingResult类型
	 */
	public static BindingResult getBindingResult(Map<?, ?> model, String name) {
		Assert.notNull(model, "Model map must not be null");
		Assert.notNull(name, "Name must not be null");
		Object attr = model.get(BindingResult.MODEL_KEY_PREFIX + name);
		if (attr != null && !(attr instanceof BindingResult)) {
			throw new IllegalStateException("BindingResult attribute is not of type BindingResult: " + attr);
		}
		return (BindingResult) attr;
	}

	/**
	 * 在给定模型中查找给定名称所需的BindingResult.
	 * 
	 * @param model 要搜索的模型
	 * @param name 要查找BindingResult的目标对象的名称
	 * 
	 * @return BindingResult (never {@code null})
	 * @throws IllegalStateException 如果没有找到BindingResult
	 */
	public static BindingResult getRequiredBindingResult(Map<?, ?> model, String name) {
		BindingResult bindingResult = getBindingResult(model, name);
		if (bindingResult == null) {
			throw new IllegalStateException("No BindingResult attribute found for name '" + name +
					"'- have you exposed the correct model?");
		}
		return bindingResult;
	}

}
