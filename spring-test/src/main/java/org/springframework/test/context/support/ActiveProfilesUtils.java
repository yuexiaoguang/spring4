package org.springframework.test.context.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.test.util.MetaAnnotationUtils;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 使用{@link ActiveProfiles @ActiveProfiles} 和 {@link ActiveProfilesResolver ActiveProfilesResolvers}的实用方法.
 *
 * <p>尽管{@code ActiveProfilesUtils}是在Spring Framework 4.1中首次引入的,
 * 但此类中方法的初始实现基于{@code ContextLoaderUtils}中的现有代码.
 */
abstract class ActiveProfilesUtils {

	private static final Log logger = LogFactory.getLog(ActiveProfilesUtils.class);


	/**
	 * 为提供的{@link Class}解析<em>活动bean定义配置文件</em>.
	 * <p>请注意, {@link ActiveProfiles @ActiveProfiles}的{@link ActiveProfiles#inheritProfiles inheritProfiles}标志将被考虑在内.
	 * 具体来说, 如果{@code inheritProfiles}标志设置为{@code true}, 则测试类中定义的配置文件将与超类中定义的配置文件合并.
	 * 
	 * @param testClass 要为其解析活动配置文件的类 (must not be {@code null})
	 * 
	 * @return 指定类的活动配置文件集合, 包括合适的超类中的活动配置文件 (never {@code null})
	 */
	static String[] resolveActiveProfiles(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		final List<String[]> profileArrays = new ArrayList<String[]>();

		Class<ActiveProfiles> annotationType = ActiveProfiles.class;
		AnnotationDescriptor<ActiveProfiles> descriptor =
				MetaAnnotationUtils.findAnnotationDescriptor(testClass, annotationType);
		if (descriptor == null && logger.isDebugEnabled()) {
			logger.debug(String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
					annotationType.getName(), testClass.getName()));
		}

		while (descriptor != null) {
			Class<?> rootDeclaringClass = descriptor.getRootDeclaringClass();
			Class<?> declaringClass = descriptor.getDeclaringClass();
			ActiveProfiles annotation = descriptor.synthesizeAnnotation();

			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ActiveProfiles [%s] for declaring class [%s]",
						annotation, declaringClass.getName()));
			}

			Class<? extends ActiveProfilesResolver> resolverClass = annotation.resolver();
			if (ActiveProfilesResolver.class == resolverClass) {
				resolverClass = DefaultActiveProfilesResolver.class;
			}

			ActiveProfilesResolver resolver;
			try {
				resolver = BeanUtils.instantiateClass(resolverClass, ActiveProfilesResolver.class);
			}
			catch (Exception ex) {
				String msg = String.format("Could not instantiate ActiveProfilesResolver of type [%s] " +
						"for test class [%s]", resolverClass.getName(), rootDeclaringClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg, ex);
			}

			String[] profiles = resolver.resolve(rootDeclaringClass);
			if (profiles == null) {
				String msg = String.format(
						"ActiveProfilesResolver [%s] returned a null array of bean definition profiles",
						resolverClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			profileArrays.add(profiles);

			descriptor = (annotation.inheritProfiles() ? MetaAnnotationUtils.findAnnotationDescriptor(
					rootDeclaringClass.getSuperclass(), annotationType) : null);
		}

		// 反转列表, 以便可以向下遍历层次结构.
		Collections.reverse(profileArrays);

		final Set<String> activeProfiles = new LinkedHashSet<String>();
		for (String[] profiles : profileArrays) {
			for (String profile : profiles) {
				if (StringUtils.hasText(profile)) {
					activeProfiles.add(profile.trim());
				}
			}
		}

		return StringUtils.toStringArray(activeProfiles);
	}

}
