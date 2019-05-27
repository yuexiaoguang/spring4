package org.springframework.beans.factory.support;

import java.util.Properties;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;

/**
 * 表示支持合并父/子定义的Spring管理的{@link Properties}实例的标签类.
 */
@SuppressWarnings("serial")
public class ManagedProperties extends Properties implements Mergeable, BeanMetadataElement {

	private Object source;

	private boolean mergeEnabled;


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
	public Object merge(Object parent) {
		if (!this.mergeEnabled) {
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof Properties)) {
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		Properties merged = new ManagedProperties();
		merged.putAll((Properties) parent);
		merged.putAll(this);
		return merged;
	}

}
