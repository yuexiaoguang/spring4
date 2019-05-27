package org.springframework.beans.support;

import java.io.Serializable;

import org.springframework.util.StringUtils;

/**
 * {@link SortDefinition}接口的可变实现.
 * 支持在再次设置相同属性时切换升序值.
 */
@SuppressWarnings("serial")
public class MutableSortDefinition implements SortDefinition, Serializable {

	private String property = "";

	private boolean ignoreCase = true;

	private boolean ascending = true;

	private boolean toggleAscendingOnProperty = false;


	/**
	 * 创建一个空的MutableSortDefinition, 通过其bean属性填充.
	 */
	public MutableSortDefinition() {
	}

	/**
	 * 克隆构造函数: 创建一个镜像给定排序定义的新MutableSortDefinition.
	 * 
	 * @param source 原始排序定义
	 */
	public MutableSortDefinition(SortDefinition source) {
		this.property = source.getProperty();
		this.ignoreCase = source.isIgnoreCase();
		this.ascending = source.isAscending();
	}

	/**
	 * 创建给定的设置的MutableSortDefinition.
	 * 
	 * @param property 要比较的属性
	 * @param ignoreCase 是否应忽略String值中的大写和小写
	 * @param ascending 是升序 (true) 还是降序 (false)
	 */
	public MutableSortDefinition(String property, boolean ignoreCase, boolean ascending) {
		this.property = property;
		this.ignoreCase = ignoreCase;
		this.ascending = ascending;
	}

	/**
	 * @param toggleAscendingOnSameProperty 如果再次设置相同的属性, 是否切换升序标志
	 * (即, 再次使用已设置的属性名称调用{@code setProperty}).
	 */
	public MutableSortDefinition(boolean toggleAscendingOnSameProperty) {
		this.toggleAscendingOnProperty = toggleAscendingOnSameProperty;
	}


	/**
	 * 设置要比较的属性.
	 * <p>如果属性与当前属性相同, 则如果激活 "toggleAscendingOnProperty", 排序将被反转, 否则将被忽略.
	 */
	public void setProperty(String property) {
		if (!StringUtils.hasLength(property)) {
			this.property = "";
		}
		else {
			// 隐含的升序翻转?
			if (isToggleAscendingOnProperty()) {
				this.ascending = (!property.equals(this.property) || !this.ascending);
			}
			this.property = property;
		}
	}

	@Override
	public String getProperty() {
		return this.property;
	}

	/**
	 * 设置是否应忽略String值中的大写和小写.
	 */
	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	@Override
	public boolean isIgnoreCase() {
		return this.ignoreCase;
	}

	/**
	 * 设置是升序 (true) 还是降序 (false).
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	@Override
	public boolean isAscending() {
		return this.ascending;
	}

	/**
	 * 设置是否在再次设置相同属性时切换升序标志
	 * (即, 再次使用已设置的属性名称调用{@code setProperty}).
	 * <p>这对于通过Web请求进行参数绑定特别有用, 再次单击字段, 可能会触发相同字段, 但相反顺序的重新排序.
	 */
	public void setToggleAscendingOnProperty(boolean toggleAscendingOnProperty) {
		this.toggleAscendingOnProperty = toggleAscendingOnProperty;
	}

	/**
	 * 如果再次设置相同的属性, 则返回是否切换升序标志
	 * (即, 再次使用已设置的属性名称调用{@code setProperty}).
	 */
	public boolean isToggleAscendingOnProperty() {
		return this.toggleAscendingOnProperty;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SortDefinition)) {
			return false;
		}
		SortDefinition otherSd = (SortDefinition) other;
		return (getProperty().equals(otherSd.getProperty()) &&
				isAscending() == otherSd.isAscending() &&
				isIgnoreCase() == otherSd.isIgnoreCase());
	}

	@Override
	public int hashCode() {
		int hashCode = getProperty().hashCode();
		hashCode = 29 * hashCode + (isIgnoreCase() ? 1 : 0);
		hashCode = 29 * hashCode + (isAscending() ? 1 : 0);
		return hashCode;
	}

}
