package org.springframework.beans.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.util.StringUtils;

/**
 * PropertyComparator执行两个bean的比较, 通过BeanWrapper评估指定的bean属性.
 */
public class PropertyComparator<T> implements Comparator<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final SortDefinition sortDefinition;

	private final BeanWrapperImpl beanWrapper = new BeanWrapperImpl(false);


	/**
	 * 为给定的SortDefinition创建一个新的PropertyComparator.
	 */
	public PropertyComparator(SortDefinition sortDefinition) {
		this.sortDefinition = sortDefinition;
	}

	/**
	 * 创建给定设置的PropertyComparator.
	 * 
	 * @param property 要比较的属性
	 * @param ignoreCase 是否应忽略String值中的大写和小写
	 * @param ascending 是升序 (true) 还是降序 (false)
	 */
	public PropertyComparator(String property, boolean ignoreCase, boolean ascending) {
		this.sortDefinition = new MutableSortDefinition(property, ignoreCase, ascending);
	}

	/**
	 * 返回此比较器使用的SortDefinition.
	 */
	public final SortDefinition getSortDefinition() {
		return this.sortDefinition;
	}


	@Override
	@SuppressWarnings("unchecked")
	public int compare(T o1, T o2) {
		Object v1 = getPropertyValue(o1);
		Object v2 = getPropertyValue(o2);
		if (this.sortDefinition.isIgnoreCase() && (v1 instanceof String) && (v2 instanceof String)) {
			v1 = ((String) v1).toLowerCase();
			v2 = ((String) v2).toLowerCase();
		}

		int result;

		// 在排序结果的末尾放置一个null属性的对象.
		try {
			if (v1 != null) {
				result = (v2 != null ? ((Comparable<Object>) v1).compareTo(v2) : -1);
			}
			else {
				result = (v2 != null ? 1 : 0);
			}
		}
		catch (RuntimeException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Could not sort objects [" + o1 + "] and [" + o2 + "]", ex);
			}
			return 0;
		}

		return (this.sortDefinition.isAscending() ? result : -result);
	}

	/**
	 * 获取给定对象的SortDefinition的属性值.
	 * 
	 * @param obj 要获取属性值的对象
	 * 
	 * @return 属性值
	 */
	private Object getPropertyValue(Object obj) {
		// 如果无法读取嵌套属性, 只需返回 null (类似于 JSTL EL).
		// 如果该属性在第一个地方不存在, 请通过异常.
		try {
			this.beanWrapper.setWrappedInstance(obj);
			return this.beanWrapper.getPropertyValue(this.sortDefinition.getProperty());
		}
		catch (BeansException ex) {
			logger.info("PropertyComparator could not access property - treating as null for sorting", ex);
			return null;
		}
	}


	/**
	 * 根据给定的排序定义对给定的List进行排序.
	 * <p>Note: 包含的对象必须以bean属性的形式提供给定的属性, i.e. a getXXX method.
	 * 
	 * @param source 输入List
	 * @param sortDefinition 要排序的参数
	 * 
	 * @throws java.lang.IllegalArgumentException 如果缺少propertyName
	 */
	public static void sort(List<?> source, SortDefinition sortDefinition) throws BeansException {
		if (StringUtils.hasText(sortDefinition.getProperty())) {
			Collections.sort(source, new PropertyComparator<Object>(sortDefinition));
		}
	}

	/**
	 * 根据给定的排序定义对给定的源进行排序.
	 * <p>Note: 包含的对象必须以bean属性的形式提供给定的属性, i.e. a getXXX method.
	 * 
	 * @param source 输入源
	 * @param sortDefinition 要排序的参数
	 * 
	 * @throws java.lang.IllegalArgumentException 如果缺少propertyName
	 */
	public static void sort(Object[] source, SortDefinition sortDefinition) throws BeansException {
		if (StringUtils.hasText(sortDefinition.getProperty())) {
			Arrays.sort(source, new PropertyComparator<Object>(sortDefinition));
		}
	}

}
