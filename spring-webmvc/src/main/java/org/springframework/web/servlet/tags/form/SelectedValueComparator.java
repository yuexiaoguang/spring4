package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.BindStatus;

/**
 * 用于测试候选值是否与 {@link BindStatus#getValue 数据绑定值}匹配的工具类.
 * 实时地尝试通过多种途径来证明比较, 以处理实例不等式, 逻辑 (基于字符串表示) 相等, 和基于{@link PropertyEditor}的比较等问题.
 *
 * <p>全面支持比较数组, {@link Collection Collections}和{@link Map Maps}.
 *
 * <p><h1><a name="equality-contract">Equality Contract</a></h1>
 * 对于单值对象, 首先使用标准{@link Object#equals Java equality}测试相等性.
 * 因此, 用户代码应该尽量实现{@link Object#equals}以加速比较过程.
 * 如果{@link Object#equals}返回{@code false}, 则尝试{@link #exhaustiveCompare 详尽比较}.
 *
 * <p>接下来, 尝试比较候选值和绑定值的{@code String}表示.
 * 在许多情况下, 这可能导致{@code true}, 因为当向用户显示时, 这两个值都将表示为{@code Strings}.
 *
 * <p>接下来, 如果候选值是{@code String}, 则尝试将绑定值与将相应的{@link PropertyEditor}应用于候选者之后的结果进行比较.
 * 这个比较可以执行两次, 第一次针对直接{@code String}实例, 如果第一次比较为{@code false}, 然后针对{@code String}表示.
 */
abstract class SelectedValueComparator {

	/**
	 * 如果提供的候选值等于绑定到提供的{@link BindStatus}的值, 则返回{@code true}.
	 * 在这种情况下, 相等与标准Java相等不同, <a href="#equality-contract">here</a>有更详细地描述.
	 */
	public static boolean isSelected(BindStatus bindStatus, Object candidateValue) {
		if (bindStatus == null) {
			return (candidateValue == null);
		}

		// 首先检查与候选者的明显等式匹配, 包括渲染后的值和原始值.
		Object boundValue = bindStatus.getValue();
		if (ObjectUtils.nullSafeEquals(boundValue, candidateValue)) {
			return true;
		}
		Object actualValue = bindStatus.getActualValue();
		if (actualValue != null && actualValue != boundValue &&
				ObjectUtils.nullSafeEquals(actualValue, candidateValue)) {
			return true;
		}
		if (actualValue != null) {
			boundValue = actualValue;
		}
		else if (boundValue == null) {
			return false;
		}

		// 非null值但与候选值没有明显的相等: 进行更详尽的比较.
		boolean selected = false;
		if (boundValue.getClass().isArray()) {
			selected = collectionCompare(CollectionUtils.arrayToList(boundValue), candidateValue, bindStatus);
		}
		else if (boundValue instanceof Collection) {
			selected = collectionCompare((Collection<?>) boundValue, candidateValue, bindStatus);
		}
		else if (boundValue instanceof Map) {
			selected = mapCompare((Map<?, ?>) boundValue, candidateValue, bindStatus);
		}
		if (!selected) {
			selected = exhaustiveCompare(boundValue, candidateValue, bindStatus.getEditor(), null);
		}
		return selected;
	}

	private static boolean collectionCompare(Collection<?> boundCollection, Object candidateValue, BindStatus bindStatus) {
		try {
			if (boundCollection.contains(candidateValue)) {
				return true;
			}
		}
		catch (ClassCastException ex) {
			// Probably from a TreeSet - ignore.
		}
		return exhaustiveCollectionCompare(boundCollection, candidateValue, bindStatus);
	}

	private static boolean mapCompare(Map<?, ?> boundMap, Object candidateValue, BindStatus bindStatus) {
		try {
			if (boundMap.containsKey(candidateValue)) {
				return true;
			}
		}
		catch (ClassCastException ex) {
			// Probably from a TreeMap - ignore.
		}
		return exhaustiveCollectionCompare(boundMap.keySet(), candidateValue, bindStatus);
	}

	private static boolean exhaustiveCollectionCompare(
			Collection<?> collection, Object candidateValue, BindStatus bindStatus) {

		Map<PropertyEditor, Object> convertedValueCache = new HashMap<PropertyEditor, Object>(1);
		PropertyEditor editor = null;
		boolean candidateIsString = (candidateValue instanceof String);
		if (!candidateIsString) {
			editor = bindStatus.findEditor(candidateValue.getClass());
		}
		for (Object element : collection) {
			if (editor == null && element != null && candidateIsString) {
				editor = bindStatus.findEditor(element.getClass());
			}
			if (exhaustiveCompare(element, candidateValue, editor, convertedValueCache)) {
				return true;
			}
		}
		return false;
	}

	private static boolean exhaustiveCompare(Object boundValue, Object candidate,
			PropertyEditor editor, Map<PropertyEditor, Object> convertedValueCache) {

		String candidateDisplayString = ValueFormatter.getDisplayString(candidate, editor, false);
		if (boundValue != null && boundValue.getClass().isEnum()) {
			Enum<?> boundEnum = (Enum<?>) boundValue;
			String enumCodeAsString = ObjectUtils.getDisplayString(boundEnum.name());
			if (enumCodeAsString.equals(candidateDisplayString)) {
				return true;
			}
			String enumLabelAsString = ObjectUtils.getDisplayString(boundEnum.toString());
			if (enumLabelAsString.equals(candidateDisplayString)) {
				return true;
			}
		}
		else if (ObjectUtils.getDisplayString(boundValue).equals(candidateDisplayString)) {
			return true;
		}

		if (editor != null && candidate instanceof String) {
			// 尝试基于PE的比较 (PE 应该不允许逃避创建线程)
			String candidateAsString = (String) candidate;
			Object candidateAsValue;
			if (convertedValueCache != null && convertedValueCache.containsKey(editor)) {
				candidateAsValue = convertedValueCache.get(editor);
			}
			else {
				editor.setAsText(candidateAsString);
				candidateAsValue = editor.getValue();
				if (convertedValueCache != null) {
					convertedValueCache.put(editor, candidateAsValue);
				}
			}
			if (ObjectUtils.nullSafeEquals(boundValue, candidateAsValue)) {
				return true;
			}
		}
		return false;
	}

}
