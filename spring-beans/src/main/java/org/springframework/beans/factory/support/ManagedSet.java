package org.springframework.beans.factory.support;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;

/**
 * 用于保存托管Set值的标签集合类, 可以包括运行时bean引用(要解析为bean对象).
 */
@SuppressWarnings("serial")
public class ManagedSet<E> extends LinkedHashSet<E> implements Mergeable, BeanMetadataElement {

	private Object source;

	private String elementTypeName;

	private boolean mergeEnabled;


	public ManagedSet() {
	}

	public ManagedSet(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * 为此元数据元素设置配置源{@code Object}.
	 * <p>对象的精确类型取决于所使用的配置机制.
	 */
	public void setSource(Object source) {
		this.source = source;
	}

	@Override
	public Object getSource() {
		return this.source;
	}

	/**
	 * 设置要用于此Set的默认元素类型名称(类名).
	 */
	public void setElementTypeName(String elementTypeName) {
		this.elementTypeName = elementTypeName;
	}

	/**
	 * 返回要用于此Set的默认元素类型名称(类名).
	 */
	public String getElementTypeName() {
		return this.elementTypeName;
	}

	/**
	 * 设置是否应为此集合启用合并, 以防存在“父级”集合值.
	 */
	public void setMergeEnabled(boolean mergeEnabled) {
		this.mergeEnabled = mergeEnabled;
	}

	@Override
	public boolean isMergeEnabled() {
		return this.mergeEnabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<E> merge(Object parent) {
		if (!this.mergeEnabled) {
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof Set)) {
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		Set<E> merged = new ManagedSet<E>();
		merged.addAll((Set<E>) parent);
		merged.addAll(this);
		return merged;
	}

}
