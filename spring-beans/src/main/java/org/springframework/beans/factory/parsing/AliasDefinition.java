package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.util.Assert;

/**
 * 表示在解析过程中已注册的别名.
 */
public class AliasDefinition implements BeanMetadataElement {

	private final String beanName;

	private final String alias;

	private final Object source;


	/**
	 * @param beanName bean的规范名称
	 * @param alias 为bean注册的别名
	 */
	public AliasDefinition(String beanName, String alias) {
		this(beanName, alias, null);
	}

	/**
	 * @param beanName bean的规范名称
	 * @param alias 为bean注册的别名
	 * @param source 源对象 (may be {@code null})
	 */
	public AliasDefinition(String beanName, String alias, Object source) {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(alias, "Alias must not be null");
		this.beanName = beanName;
		this.alias = alias;
		this.source = source;
	}


	/**
	 * 返回bean的规范名称.
	 */
	public final String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回为bean注册的别名.
	 */
	public final String getAlias() {
		return this.alias;
	}

	@Override
	public final Object getSource() {
		return this.source;
	}

}
