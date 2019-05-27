package org.springframework.beans;

import org.springframework.core.AttributeAccessorSupport;

/**
 * {@link org.springframework.core.AttributeAccessorSupport}的扩展,
 * 将属性保存为{@link BeanMetadataAttribute}对象以跟踪定义源.
 */
@SuppressWarnings("serial")
public class BeanMetadataAttributeAccessor extends AttributeAccessorSupport implements BeanMetadataElement {

	private Object source;


	/**
	 * 设置此元数据元素的配置源.
	 * <p>对象的确切类型取决于所使用的配置机制.
	 */
	public void setSource(Object source) {
		this.source = source;
	}

	@Override
	public Object getSource() {
		return this.source;
	}


	/**
	 * 将给定的BeanMetadataAttribute添加到此访问者的属性集中.
	 * 
	 * @param attribute 要注册的BeanMetadataAttribute对象
	 */
	public void addMetadataAttribute(BeanMetadataAttribute attribute) {
		super.setAttribute(attribute.getName(), attribute);
	}

	/**
	 * 在此访问者的属性集中查找给定的BeanMetadataAttribute.
	 * 
	 * @param name 属性名
	 * 
	 * @return 相应的BeanMetadataAttribute对象, 如果没有定义这样的属性, 则为{@code null}
	 */
	public BeanMetadataAttribute getMetadataAttribute(String name) {
		return (BeanMetadataAttribute) super.getAttribute(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		super.setAttribute(name, new BeanMetadataAttribute(name, value));
	}

	@Override
	public Object getAttribute(String name) {
		BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.getAttribute(name);
		return (attribute != null ? attribute.getValue() : null);
	}

	@Override
	public Object removeAttribute(String name) {
		BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.removeAttribute(name);
		return (attribute != null ? attribute.getValue() : null);
	}
}
