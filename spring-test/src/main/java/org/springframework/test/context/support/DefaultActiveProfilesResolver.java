package org.springframework.test.context.support;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.springframework.test.util.MetaAnnotationUtils.*;

/**
 * {@link ActiveProfilesResolver}策略的默认实现,
 * 该策略仅基于通过{@link ActiveProfiles#profiles}或{@link ActiveProfiles#value}声明性配置的配置文件,
 * 解析<em>活动Bean定义配置文件</em>.
 */
public class DefaultActiveProfilesResolver implements ActiveProfilesResolver {

	private static final Log logger = LogFactory.getLog(DefaultActiveProfilesResolver.class);


	/**
	 * 基于通过{@link ActiveProfiles#profiles} 或 {@link ActiveProfiles#value} 以声明方式配置的配置文件,
	 * 解析给定{@linkplain Class 测试类}的<em>bean定义配置文件</em>.
	 * 
	 * @param testClass 应该为其解析配置文件的测试类; never {@code null}
	 * 
	 * @return 加载{@code ApplicationContext}时要使用的bean定义配置文件列表; never {@code null}
	 */
	@Override
	public String[] resolve(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		final Set<String> activeProfiles = new LinkedHashSet<String>();

		Class<ActiveProfiles> annotationType = ActiveProfiles.class;
		AnnotationDescriptor<ActiveProfiles> descriptor = findAnnotationDescriptor(testClass, annotationType);

		if (descriptor == null) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
					annotationType.getName(), testClass.getName()));
			}
		}
		else {
			Class<?> declaringClass = descriptor.getDeclaringClass();
			ActiveProfiles annotation = descriptor.synthesizeAnnotation();

			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ActiveProfiles [%s] for declaring class [%s].", annotation,
					declaringClass.getName()));
			}

			for (String profile : annotation.profiles()) {
				if (StringUtils.hasText(profile)) {
					activeProfiles.add(profile.trim());
				}
			}
		}

		return StringUtils.toStringArray(activeProfiles);
	}

}
