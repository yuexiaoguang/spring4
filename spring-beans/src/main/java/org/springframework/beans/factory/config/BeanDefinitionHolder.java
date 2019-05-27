package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 持有BeanDefinition的名称和别名. 可以注册为内部bean的占位符.
 *
 * <p>也可以用于内部bean定义注册.
 * 如果你不关心BeanNameAware之类的话, 注册RootBeanDefinition或ChildBeanDefinition就足够了.
 */
public class BeanDefinitionHolder implements BeanMetadataElement {

	private final BeanDefinition beanDefinition;

	private final String beanName;

	private final String[] aliases;


	/**
	 * @param beanDefinition 要包装的BeanDefinition
	 * @param beanName bean的名称, 如bean定义所指定的
	 */
	public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
		this(beanDefinition, beanName, null);
	}

	/**
	 * @param beanDefinition 要包装的BeanDefinition
	 * @param beanName bean的名称, 如bean定义所指定的
	 * @param aliases bean的别名, 或{@code null}
	 */
	public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, String[] aliases) {
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");
		Assert.notNull(beanName, "Bean name must not be null");
		this.beanDefinition = beanDefinition;
		this.beanName = beanName;
		this.aliases = aliases;
	}

	/**
	 * 复制构造函数: 创建一个新的BeanDefinitionHolder, 其内容与给定的BeanDefinitionHolder实例相同.
	 * <p>Note: 包装的BeanDefinition引用按原样使用; 浅拷贝.
	 * 
	 * @param beanDefinitionHolder 要复制的BeanDefinitionHolder
	 */
	public BeanDefinitionHolder(BeanDefinitionHolder beanDefinitionHolder) {
		Assert.notNull(beanDefinitionHolder, "BeanDefinitionHolder must not be null");
		this.beanDefinition = beanDefinitionHolder.getBeanDefinition();
		this.beanName = beanDefinitionHolder.getBeanName();
		this.aliases = beanDefinitionHolder.getAliases();
	}


	/**
	 * 返回包装的BeanDefinition.
	 */
	public BeanDefinition getBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * 返回bean的主名称, 如bean定义所指定的.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回bean的别名, 如bean定义直接指定的.
	 * 
	 * @return 别名数组, 或{@code null}
	 */
	public String[] getAliases() {
		return this.aliases;
	}

	/**
	 * 公开bean定义的源对象.
	 */
	@Override
	public Object getSource() {
		return this.beanDefinition.getSource();
	}

	/**
	 * 给定的候选名称, 是否与bean名称或此bean定义中存储的别名匹配.
	 */
	public boolean matchesName(String candidateName) {
		return (candidateName != null && (candidateName.equals(this.beanName) ||
				candidateName.equals(BeanFactoryUtils.transformedBeanName(this.beanName)) ||
				ObjectUtils.containsElement(this.aliases, candidateName)));
	}


	/**
	 * 返回bean的友好简短描述, 说明名称和别名.
	 */
	public String getShortDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("Bean definition with name '").append(this.beanName).append("'");
		if (this.aliases != null) {
			sb.append(" and aliases [").append(StringUtils.arrayToCommaDelimitedString(this.aliases)).append("]");
		}
		return sb.toString();
	}

	/**
	 * 返回bean的长描述, 包括名称和别名以及包含的{@link BeanDefinition}的描述.
	 */
	public String getLongDescription() {
		StringBuilder sb = new StringBuilder(getShortDescription());
		sb.append(": ").append(this.beanDefinition);
		return sb.toString();
	}

	/**
	 * 此实现返回长描述. 可以重写以返回简短描述或任何类型的自定义描述.
	 */
	@Override
	public String toString() {
		return getLongDescription();
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BeanDefinitionHolder)) {
			return false;
		}
		BeanDefinitionHolder otherHolder = (BeanDefinitionHolder) other;
		return this.beanDefinition.equals(otherHolder.beanDefinition) &&
				this.beanName.equals(otherHolder.beanName) &&
				ObjectUtils.nullSafeEquals(this.aliases, otherHolder.aliases);
	}

	@Override
	public int hashCode() {
		int hashCode = this.beanDefinition.hashCode();
		hashCode = 29 * hashCode + this.beanName.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.aliases);
		return hashCode;
	}

}
