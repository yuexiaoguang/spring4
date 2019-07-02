package org.springframework.test.context.support;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.springframework.core.annotation.AnnotationUtils.*;
import static org.springframework.test.util.MetaAnnotationUtils.*;

/**
 * 用于从{@link ContextConfiguration @ContextConfiguration}
 * 和{@link ContextHierarchy @ContextHierarchy} 注解中解析{@link ContextConfigurationAttributes},
 * 以与{@link SmartContextLoader SmartContextLoaders}一起使用的实用方法.
 */
abstract class ContextLoaderUtils {

	static final String GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX = "ContextHierarchyLevel#";

	private static final Log logger = LogFactory.getLog(ContextLoaderUtils.class);


	/**
	 * 解析所提供的{@linkplain Class 测试类}及其超类的
	 * {@linkplain ContextConfigurationAttributes 上下文配置属性}的列表的列表,
	 * 同时考虑通过 {@link ContextHierarchy @ContextHierarchy}
	 * 和{@link ContextConfiguration @ContextConfiguration}声明的上下文层次结构.
	 * <p>外部列表表示上下文配置属性的自上而下排序, 其中列表中的每个元素表示在类层次结构中的给定测试类上声明的上下文配置.
	 * 每个嵌套列表都包含通过特定类上的{@code @ContextConfiguration}的单个实例,
	 * 或通过特定类上的{@code @ContextHierarchy}实例上声明的{@code @ContextConfiguration}的多个实例声明的上下文配置属性.
	 * 此外, 每个嵌套列表都维护声明{@code @ContextConfiguration}实例的顺序.
	 * <p>请注意, {@link ContextConfiguration @ContextConfiguration}的
	 * {@link ContextConfiguration#inheritLocations inheritLocations}和
	 * {@link ContextConfiguration#inheritInitializers() inheritInitializers} 标志将<strong>不</strong>考虑在内.
	 * 如果需要遵守这些标志, 则必须在遍历此方法返回的嵌套列表时手动处理.
	 * 
	 * @param testClass 要为其解析上下文层次结构属性的类 (must not be {@code null})
	 * 
	 * @return 指定类的配置属性的列表的列表; never {@code null}
	 * @throws IllegalArgumentException 如果提供的类是{@code null};
	 * 或者, 如果提供的类上没有{@code @ContextConfiguration}和{@code @ContextHierarchy}
	 * @throws IllegalStateException 如果类层次结构中的测试类或组合注解将{@code @ContextConfiguration}和{@code @ContextHierarchy}声明为顶级注解.
	 */
	@SuppressWarnings("unchecked")
	static List<List<ContextConfigurationAttributes>> resolveContextHierarchyAttributes(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		Class<ContextConfiguration> contextConfigType = ContextConfiguration.class;
		Class<ContextHierarchy> contextHierarchyType = ContextHierarchy.class;
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = new ArrayList<List<ContextConfigurationAttributes>>();

		UntypedAnnotationDescriptor desc =
				findAnnotationDescriptorForTypes(testClass, contextConfigType, contextHierarchyType);
		if (desc == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] or [%s] and test class [%s]",
					contextConfigType.getName(), contextHierarchyType.getName(), testClass.getName()));
		}

		while (desc != null) {
			Class<?> rootDeclaringClass = desc.getRootDeclaringClass();
			Class<?> declaringClass = desc.getDeclaringClass();

			boolean contextConfigDeclaredLocally = isAnnotationDeclaredLocally(contextConfigType, declaringClass);
			boolean contextHierarchyDeclaredLocally = isAnnotationDeclaredLocally(contextHierarchyType, declaringClass);

			if (contextConfigDeclaredLocally && contextHierarchyDeclaredLocally) {
				String msg = String.format("Class [%s] has been configured with both @ContextConfiguration " +
						"and @ContextHierarchy. Only one of these annotations may be declared on a test class " +
						"or composed annotation.", declaringClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			List<ContextConfigurationAttributes> configAttributesList = new ArrayList<ContextConfigurationAttributes>();

			if (contextConfigDeclaredLocally) {
				ContextConfiguration contextConfiguration = AnnotationUtils.synthesizeAnnotation(
						desc.getAnnotationAttributes(), ContextConfiguration.class, desc.getRootDeclaringClass());
				convertContextConfigToConfigAttributesAndAddToList(
						contextConfiguration, rootDeclaringClass, configAttributesList);
			}
			else if (contextHierarchyDeclaredLocally) {
				ContextHierarchy contextHierarchy = getAnnotation(declaringClass, contextHierarchyType);
				for (ContextConfiguration contextConfiguration : contextHierarchy.value()) {
					convertContextConfigToConfigAttributesAndAddToList(
							contextConfiguration, rootDeclaringClass, configAttributesList);
				}
			}
			else {
				// This should theoretically never happen...
				String msg = String.format("Test class [%s] has been configured with neither @ContextConfiguration " +
						"nor @ContextHierarchy as a class-level annotation.", rootDeclaringClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			hierarchyAttributes.add(0, configAttributesList);
			desc = findAnnotationDescriptorForTypes(
					rootDeclaringClass.getSuperclass(), contextConfigType, contextHierarchyType);
		}

		return hierarchyAttributes;
	}

	/**
	 * 为提供的{@linkplain Class 测试类} 及其超类构建<em>上下文层次结构映射</em>, 同时考虑通过
	 * {@link ContextHierarchy @ContextHierarchy} 和 {@link ContextConfiguration @ContextConfiguration}声明的上下文层次结构.
	 * <p>Map中的每个值表示上下文层次结构中给定级别(可能跨测试类层次结构)的
	 * {@linkplain ContextConfigurationAttributes 上下文配置属性}的合并列表,
	 * 由上下文层次结构级别的{@link ContextConfiguration#name() 名称}作为键.
	 * <p>如果上下文层次结构中的给定级别没有显式名称 (i.e., 通过{@link ContextConfiguration#name}配置),
	 * 则会通过将数字级别附加到{@link #GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX}来为该层次结构级别生成名称.
	 * 
	 * @param testClass 要为其解析上下文层次结构Map的类 (must not be {@code null})
	 * 
	 * @return 上下文层次结构的上下文配置属性Map, 由上下文层次结构级别名称作为键; never {@code null}
	 * @throws IllegalArgumentException 如果{@code @ContextHierarchy}中每个级别的上下文配置属性列表未在整个层次结构中定义唯一上下文配置.
	 */
	static Map<String, List<ContextConfigurationAttributes>> buildContextHierarchyMap(Class<?> testClass) {
		final Map<String, List<ContextConfigurationAttributes>> map = new LinkedHashMap<String, List<ContextConfigurationAttributes>>();
		int hierarchyLevel = 1;

		for (List<ContextConfigurationAttributes> configAttributesList : resolveContextHierarchyAttributes(testClass)) {
			for (ContextConfigurationAttributes configAttributes : configAttributesList) {
				String name = configAttributes.getName();

				// Assign a generated name?
				if (!StringUtils.hasText(name)) {
					name = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + hierarchyLevel;
				}

				// 遇到新的上下文层次结构级别?
				if (!map.containsKey(name)) {
					hierarchyLevel++;
					map.put(name, new ArrayList<ContextConfigurationAttributes>());
				}

				map.get(name).add(configAttributes);
			}
		}

		// 检查唯一性
		Set<List<ContextConfigurationAttributes>> set = new HashSet<List<ContextConfigurationAttributes>>(map.values());
		if (set.size() != map.size()) {
			String msg = String.format("The @ContextConfiguration elements configured via @ContextHierarchy in " +
					"test class [%s] and its superclasses must define unique contexts per hierarchy level.",
					testClass.getName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}

		return map;
	}

	/**
	 * 解析所提供的{@linkplain Class 测试类}及其超类的{@linkplain ContextConfigurationAttributes 上下文配置属性}列表.
	 * <p>请注意, {@link ContextConfiguration @ContextConfiguration}的
	 * {@link ContextConfiguration#inheritLocations inheritLocations}和
	 * {@link ContextConfiguration#inheritInitializers() inheritInitializers}标志将<strong>不</strong>考虑在内.
	 * 如果需要遵守这些标志, 则必须在遍历此方法返回的列表时手动处理.
	 * 
	 * @param testClass 要为其解析配置属性的类 (must not be {@code null})
	 * 
	 * @return 指定类的配置属性列表, 排序<em>自下而上</em> (i.e., 好像我们正在遍历类层次结构); never {@code null}
	 * @throws IllegalArgumentException 如果提供的类是{@code null}或者提供的类上没有{@code @ContextConfiguration}
	 */
	static List<ContextConfigurationAttributes> resolveContextConfigurationAttributes(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		List<ContextConfigurationAttributes> attributesList = new ArrayList<ContextConfigurationAttributes>();
		Class<ContextConfiguration> annotationType = ContextConfiguration.class;

		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(testClass, annotationType);
		if (descriptor == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
					annotationType.getName(), testClass.getName()));
		}

		while (descriptor != null) {
			convertContextConfigToConfigAttributesAndAddToList(descriptor.synthesizeAnnotation(),
					descriptor.getRootDeclaringClass(), attributesList);
			descriptor = findAnnotationDescriptor(descriptor.getRootDeclaringClass().getSuperclass(), annotationType);
		}

		return attributesList;
	}

	/**
	 * 从提供的{@link ContextConfiguration}注释创建{@link ContextConfigurationAttributes}实例,
	 * 并声明类, 然后将属性添加到提供的列表的便捷方法.
	 */
	private static void convertContextConfigToConfigAttributesAndAddToList(ContextConfiguration contextConfiguration,
			Class<?> declaringClass, final List<ContextConfigurationAttributes> attributesList) {

		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Retrieved @ContextConfiguration [%s] for declaring class [%s].",
					contextConfiguration, declaringClass.getName()));
		}
		ContextConfigurationAttributes attributes =
				new ContextConfigurationAttributes(declaringClass, contextConfiguration);
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved context configuration attributes: " + attributes);
		}
		attributesList.add(attributes);
	}

}
