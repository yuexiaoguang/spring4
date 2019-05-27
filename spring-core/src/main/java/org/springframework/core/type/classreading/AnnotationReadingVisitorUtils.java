package org.springframework.core.type.classreading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * 通过ASM读取注解时使用的内部实用程序类.
 */
abstract class AnnotationReadingVisitorUtils {

	public static AnnotationAttributes convertClassValues(Object annotatedElement,
			ClassLoader classLoader, AnnotationAttributes original, boolean classValuesAsString) {

		if (original == null) {
			return null;
		}

		AnnotationAttributes result = new AnnotationAttributes(original);
		AnnotationUtils.postProcessAnnotationAttributes(annotatedElement, result, classValuesAsString);

		for (Map.Entry<String, Object> entry : result.entrySet()) {
			try {
				Object value = entry.getValue();
				if (value instanceof AnnotationAttributes) {
					value = convertClassValues(
							annotatedElement, classLoader, (AnnotationAttributes) value, classValuesAsString);
				}
				else if (value instanceof AnnotationAttributes[]) {
					AnnotationAttributes[] values = (AnnotationAttributes[]) value;
					for (int i = 0; i < values.length; i++) {
						values[i] = convertClassValues(annotatedElement, classLoader, values[i], classValuesAsString);
					}
					value = values;
				}
				else if (value instanceof Type) {
					value = (classValuesAsString ? ((Type) value).getClassName() :
							classLoader.loadClass(((Type) value).getClassName()));
				}
				else if (value instanceof Type[]) {
					Type[] array = (Type[]) value;
					Object[] convArray =
							(classValuesAsString ? new String[array.length] : new Class<?>[array.length]);
					for (int i = 0; i < array.length; i++) {
						convArray[i] = (classValuesAsString ? array[i].getClassName() :
								classLoader.loadClass(array[i].getClassName()));
					}
					value = convArray;
				}
				else if (classValuesAsString) {
					if (value instanceof Class) {
						value = ((Class<?>) value).getName();
					}
					else if (value instanceof Class[]) {
						Class<?>[] clazzArray = (Class<?>[]) value;
						String[] newValue = new String[clazzArray.length];
						for (int i = 0; i < clazzArray.length; i++) {
							newValue[i] = clazzArray[i].getName();
						}
						value = newValue;
					}
				}
				entry.setValue(value);
			}
			catch (Throwable ex) {
				// 找不到Class - 无法解析注解属性中的类引用.
				result.put(entry.getKey(), ex);
			}
		}

		return result;
	}

	/**
	 * 从提供的{@code attributesMap}中检索给定类型的注解的合并的属性.
	 * <p>在注解层次结构中出现<em>较低</em>的注解属性值 (i.e., 更靠近声明类) 将覆盖注解层次结构中定义的<em>较高</em>的值.
	 * 
	 * @param attributesMap 注解属性列表的Map, 由注解类型名称作为Key
	 * @param metaAnnotationMap 元注解关系的Map, 由注解类型名称作为Key
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return 合并后的注解属性, 或{@code null} 如果{@code attributesMap}中没有匹配的注解
	 */
	public static AnnotationAttributes getMergedAnnotationAttributes(
			LinkedMultiValueMap<String, AnnotationAttributes> attributesMap,
			Map<String, Set<String>> metaAnnotationMap, String annotationName) {

		// 获取目标注解的未合并的属性列表.
		List<AnnotationAttributes> attributesList = attributesMap.get(annotationName);
		if (attributesList == null || attributesList.isEmpty()) {
			return null;
		}

		// 首先, 使用目标注解中的所有属性值的副本填充结果.
		// 副本是必要的, 这样就不会无意中改变传递给此方法的元数据的状态.
		AnnotationAttributes result = new AnnotationAttributes(attributesList.get(0));

		Set<String> overridableAttributeNames = new HashSet<String>(result.keySet());
		overridableAttributeNames.remove(AnnotationUtils.VALUE);

		// 由于Map是LinkedMultiValueMap, 依赖于Map中元素的排序并反转键的顺序以便“向下”遍历注解层次结构.
		List<String> annotationTypes = new ArrayList<String>(attributesMap.keySet());
		Collections.reverse(annotationTypes);

		// 无需重新访问目标注解类型:
		annotationTypes.remove(annotationName);

		for (String currentAnnotationType : annotationTypes) {
			List<AnnotationAttributes> currentAttributesList = attributesMap.get(currentAnnotationType);
			if (!ObjectUtils.isEmpty(currentAttributesList)) {
				Set<String> metaAnns = metaAnnotationMap.get(currentAnnotationType);
				if (metaAnns != null && metaAnns.contains(annotationName)) {
					AnnotationAttributes currentAttributes = currentAttributesList.get(0);
					for (String overridableAttributeName : overridableAttributeNames) {
						Object value = currentAttributes.get(overridableAttributeName);
						if (value != null) {
							// 保存该值, 可能会覆盖注解层次结构中找到的同名属性的值.
							result.put(overridableAttributeName, value);
						}
					}
				}
			}
		}
		return result;
	}
}
