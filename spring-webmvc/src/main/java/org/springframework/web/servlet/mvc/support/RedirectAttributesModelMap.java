package org.springframework.web.servlet.mvc.support;

import java.util.Collection;
import java.util.Map;

import org.springframework.ui.ModelMap;
import org.springframework.validation.DataBinder;

/**
 * {@link RedirectAttributes}的{@link ModelMap}实现, 使用{@link DataBinder}将值格式化为字符串.
 * 还提供了存储Flash属性的位置, 以便它们可以在重定向中存活, 而无需嵌入到重定向URL中.
 */
@SuppressWarnings("serial")
public class RedirectAttributesModelMap extends ModelMap implements RedirectAttributes {

	private final DataBinder dataBinder;

	private final ModelMap flashAttributes = new ModelMap();


	/**
	 * 属性值通过 {@link #toString()}转换为String.
	 */
	public RedirectAttributesModelMap() {
		this(null);
	}

	/**
	 * @param dataBinder 用于将属性值格式化为字符串
	 */
	public RedirectAttributesModelMap(DataBinder dataBinder) {
		this.dataBinder = dataBinder;
	}


	/**
	 * 返回flash存储的候选属性或空Map.
	 */
	@Override
	public Map<String, ?> getFlashAttributes() {
		return this.flashAttributes;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加属性值之前将其格式化为String.
	 */
	@Override
	public RedirectAttributesModelMap addAttribute(String attributeName, Object attributeValue) {
		super.addAttribute(attributeName, formatValue(attributeValue));
		return this;
	}

	private String formatValue(Object value) {
		if (value == null) {
			return null;
		}
		return (this.dataBinder != null ? this.dataBinder.convertIfNecessary(value, String.class) : value.toString());
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加属性值之前将其格式化为String.
	 */
	@Override
	public RedirectAttributesModelMap addAttribute(Object attributeValue) {
		super.addAttribute(attributeValue);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前, 每个属性值都被格式化为String.
	 */
	@Override
	public RedirectAttributesModelMap addAllAttributes(Collection<?> attributeValues) {
		super.addAllAttributes(attributeValues);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前, 每个属性值都被格式化为String.
	 */
	@Override
	public RedirectAttributesModelMap addAllAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				addAttribute(key, attributes.get(key));
			}
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前, 每个属性值都被格式化为String.
	 */
	@Override
	public RedirectAttributesModelMap mergeAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				if (!containsKey(key)) {
					addAttribute(key, attributes.get(key));
				}
			}
		}
		return this;
	}

	@Override
	public Map<String, Object> asMap() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前, 该值被格式化为String.
	 */
	@Override
	public Object put(String key, Object value) {
		return super.put(key, formatValue(value));
	}

	/**
	 * {@inheritDoc}
	 * <p>在添加之前, 每个值都被格式化为String.
	 */
	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		if (map != null) {
			for (String key : map.keySet()) {
				put(key, formatValue(map.get(key)));
			}
		}
	}

	@Override
	public RedirectAttributes addFlashAttribute(String attributeName, Object attributeValue) {
		this.flashAttributes.addAttribute(attributeName, attributeValue);
		return this;
	}

	@Override
	public RedirectAttributes addFlashAttribute(Object attributeValue) {
		this.flashAttributes.addAttribute(attributeValue);
		return this;
	}

}
