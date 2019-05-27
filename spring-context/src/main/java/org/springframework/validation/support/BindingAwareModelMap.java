package org.springframework.validation.support;

import java.util.Map;

import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;

/**
 * {@link org.springframework.ui.ExtendedModelMap}的子类,
 * 如果通过常规{@link Map}操作替换相应的目标属性, 则会自动删除{@link org.springframework.validation.BindingResult}对象.
 *
 * <p>这是Spring MVC向处理器方法公开的类, 通常通过{@link org.springframework.ui.Model}接口的声明来使用.
 * 无需在用户代码中构建它; 普通的{@link org.springframework.ui.ModelMap},
 * 或者只是一个带有String键的常规{@link Map}就足以返回一个用户模型.
 */
@SuppressWarnings("serial")
public class BindingAwareModelMap extends ExtendedModelMap {

	@Override
	public Object put(String key, Object value) {
		removeBindingResultIfNecessary(key, value);
		return super.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ?> map) {
		for (Map.Entry<? extends String, ?> entry : map.entrySet()) {
			removeBindingResultIfNecessary(entry.getKey(), entry.getValue());
		}
		super.putAll(map);
	}

	private void removeBindingResultIfNecessary(Object key, Object value) {
		if (key instanceof String) {
			String attributeName = (String) key;
			if (!attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
				String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + attributeName;
				BindingResult bindingResult = (BindingResult) get(bindingResultKey);
				if (bindingResult != null && bindingResult.getTarget() != value) {
					remove(bindingResultKey);
				}
			}
		}
	}

}
